package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.barriers.JvmCyclicBarrier
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.streams.asSequence

/**
 * Note that this 'runner' is severely different from all others.
 */
class JCStressRunner(
    private val jcstressDirectory: Path,
    private val jcstressFreeArgs: List<String>,
) : LitmusRunner() {

    companion object {
        val DEFAULT_LITMUSKT_PARAMS = LitmusRunParams(0, 0, null, ::JvmCyclicBarrier)
    }

    override fun <S : Any> startTest(
        test: LitmusTest<S>,
        states: Array<S>,
        barrierProducer: BarrierProducer,
        syncPeriod: Int,
        affinityMap: AffinityMap?
    ): () -> LitmusResult {
        throw NotImplementedError("jcstress runner should not be called with explicit params like this")
    }

    override fun <S : Any> LitmusRunner.startTestParallel(
        test: LitmusTest<S>,
        params: LitmusRunParams,
        instances: Int
    ): List<() -> LitmusResult> {
        throw NotImplementedError(
            "jcstress runs tests in parallel by default; asking for parallelism explicitly is meaningless"
        )
    }

    internal fun startTests(
        tests: List<LitmusTest<*>>,
        params: LitmusRunParams
    ): () -> List<LitmusResult> {
        val mvn = ProcessBuilder("mvn", "install", "verify", "-U")
            .directory(jcstressDirectory.toFile())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        mvn.waitFor()
        if (mvn.exitValue() != 0) {
            error("mvn exited with code ${mvn.exitValue()}")
        }

        val jcsParams = if (params != DEFAULT_LITMUSKT_PARAMS) {
            arrayOf("strideSize", "${params.syncPeriod}", "strideCount", "${params.batchSize / params.syncPeriod}")
        } else emptyArray()
        val jcs = ProcessBuilder(
            "java",
            "-jar",
            "target/jcstress.jar",
            *(jcsParams + jcstressFreeArgs),
            "-t",
            tests.joinToString("|") { "(${it.javaClassName})" },
        )
            .directory(jcstressDirectory.toFile())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        return handle@{
            jcs.waitFor()
            if (jcs.exitValue() != 0) error("jcstress exited with code ${jcs.exitValue()}")
            return@handle tests.map { test -> parseJCStressResults(test) }
        }
    }

    override fun <S : Any> startTest(test: LitmusTest<S>, params: LitmusRunParams): () -> LitmusResult {
        val handle = startTests(listOf(test), params)
        return { handle().first() }
    }

    /**
     * Parses JCStress HTML output file. Here is the expected structure of the file, using SB as an example:
     *
     * ...
     * <th colspan=3>Observed States</th>    <-- read the number of observed states
     * </tr>
     * <tr>
     * <th colspan=4></th>
     * <th nowrap align='center'>0, 1</th>   <-- read the observed states in this order
     * <th nowrap align='center'>1, 0</th>
     * <th nowrap align='center'>1, 1</th>
     * ...
     * </td>
     * <td align='center' bgColor='green '>OK</td>
     * <td align='right' width='33.333333333333336%' bgColor=#00ff00>3</td>   <-- read the number of times observed
     * <td align='right' width='33.333333333333336%' bgColor=#00ff00>1</td>
     * <td align='right' width='33.333333333333336%' bgColor=#c0c0c0>0</td>
     * </tr>    <-- these lines repeat per each configuration, so the results are summed in the end
     * ...
     */
    private fun parseJCStressResults(test: LitmusTest<*>): LitmusResult {
        val resultsFile = jcstressDirectory / "results" / "${test.javaFQN}.html"
        var lines = Files.lines(resultsFile).asSequence()

        val allOutcomes = test.outcomeSpec.all
        val outcomeStrings = allOutcomes.associateBy { it.toString().trim('(', ')') }

        // get the number of observed outcomes
        lines = lines.dropWhile { !it.contains("Observed States") }
        val observedOutcomesLine = lines.splitFirst().let { (first, rest) -> lines = rest; first }
        val observedSize = Regex("colspan=(\\d+)").find(observedOutcomesLine)!!.groupValues[1].toInt()

        // skip to <tr> with outcomes
        lines = lines.drop(3)
        val linesOutcomes = lines.splitTake(observedSize).let { (first, rest) -> lines = rest; first }
        val outcomesOrdered = linesOutcomes.map {
            val outcomeString = parseElementData(it)
            outcomeStrings[outcomeString] ?: error("unrecognized outcome: $outcomeString")
        }.toList()

        // lines with "bgColor" and "width" are the only ones with data
        val outcomesCounts = lines.filter { it.contains("bgColor") && it.contains("width") }
            .map { parseElementData(it).toLong() }
            .chunked(observedSize)
            .fold(MutableList(observedSize) { 0L }) { acc, counts ->
                acc.also { for (i in acc.indices) acc[i] += counts[i] }
            } as List<Long>

        val results = List(observedSize) { i ->
            val outcome = outcomesOrdered[i]
            LitmusOutcomeStats(outcome, outcomesCounts[i], test.outcomeSpec.getType(outcome))
        }
        return results
    }

    private fun parseElementData(it: String) = it.dropWhile { it != '>' }.dropLastWhile { it != '<' }.trim('>', '<')
}

// does NOT shadow the common extension function, but can be accessed directly
fun JCStressRunner.runTests(
    tests: List<LitmusTest<*>>,
    params: LitmusRunParams,
): List<LitmusResult> = startTests(tests, params).invoke()

/**
 * Split a sequence into two: one with the first [size] elements and one with the rest.
 */
fun <T> Sequence<T>.splitTake(size: Int): Pair<Sequence<T>, Sequence<T>> {
    val iter = iterator()
    val seq1 = iter.asSequence().take(size)
    val seq2 = iter.asSequence()
    return seq1 to seq2
}

/**
 * Split a sequence into its first element and the sequence of rest.
 */
fun <T> Sequence<T>.splitFirst(): Pair<T, Sequence<T>> {
    val iter = iterator()
    return iter.next() to iter.asSequence()
}
