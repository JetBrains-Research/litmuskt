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
        throw NotImplementedError("jcstress runner should not be called like this")
    }

    override fun <S : Any> LitmusRunner.startTestParallel(
        instances: Int,
        params: LitmusRunParams,
        test: LitmusTest<S>
    ): List<() -> LitmusResult> {
        throw NotImplementedError(
            "jcstress runs tests in parallel by default; asking for parallelism explicitly is meaningless"
        )
    }

    // TODO: optimize for many tests (do not invoke jcstress many times)
    override fun <S : Any> startTest(params: LitmusRunParams, test: LitmusTest<S>): () -> LitmusResult {
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
            test.javaClassName,
        )
            .directory(jcstressDirectory.toFile())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        return handle@{
            jcs.waitFor()
            if (jcs.exitValue() != 0) error("jcstress exited with code ${jcs.exitValue()}")
            return@handle parseJCStressResults(test)
        }
    }

    fun parseJCStressResults(test: LitmusTest<*>): LitmusResult {
        val resultsFile = jcstressDirectory / "results" / "org.jetbrains.litmuskt.${test.javaClassName}.html"
        var lines = Files.lines(resultsFile).asSequence()

        val allOutcomes = test.outcomeSpec.all()
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