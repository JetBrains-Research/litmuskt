package komem.litmus

import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Note that this 'runner' is severely different from all others.
 */
object JCStressRunner : LitmusRunner() {
    override fun <S> runTest(params: LitmusRunParams, test: LitmusTest<S>): LitmusResult =
        throw NotImplementedError("jcstress runner is not supposed to be used like this")

    fun runJCStress(
        params: LitmusRunParams?,
        tests: Collection<LitmusTest<*>>,
        jcstressDirectory: Path,
        jcstressFreeArgs: List<String>,
    ) {

        val mvn = ProcessBuilder("mvn", "install", "verify")
            .directory(jcstressDirectory.toFile())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        mvn.waitFor()
        if (mvn.exitValue() != 0) {
            exitProcess(mvn.exitValue())
        }

        val jcsParams = params?.run {
            arrayOf("strideSize", "$syncPeriod", "strideCount", "${batchSize / syncPeriod}")
        } ?: emptyArray()
        val jcs = ProcessBuilder(
            "java",
            "-jar",
            "target/jcstress.jar",
            *(jcsParams + jcstressFreeArgs),
            "-t",
            tests.joinToString("|") { it.javaClassName },
        )
            .directory(jcstressDirectory.toFile())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        jcs.waitFor()
    }
}
