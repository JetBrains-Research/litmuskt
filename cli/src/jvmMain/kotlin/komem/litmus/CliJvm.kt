package komem.litmus

import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import jcstressDirectory
import komem.litmus.barriers.JvmSpinBarrier
import komem.litmus.jcstress.JCStressRunner

class CliJvm : CliCommon() {
    override val runner by option("-r", "--runner")
        .choice(mapOf("thread" to JvmThreadRunner, "jcstress" to JCStressRunner))
        .required()
    override val barrierProducer = ::JvmSpinBarrier
    override val affinityMapSchedule = listOf(null)

    private val allowJCStressReruns by option("--allow-jcs-reruns")
        .flag()
    private val jcstressFreeArgs by option("-j", "--jcsargs")
        .convert { it.split(" ") }
        .default(emptyList())

    override fun run() = if (runner == JCStressRunner) jcstressRun() else super.run()

    private fun jcstressRun() {
        println("haha: $jcstressFreeArgs")

        val paramsList = variateRunParams(
            batchSizeSchedule = batchSizeSchedule,
            affinityMapSchedule = affinityMapSchedule,
            syncPeriodSchedule = syncEverySchedule,
            barrierSchedule = listOf(barrierProducer),
        ).toList()
        when (paramsList.size) {
            0 -> {
                echo("parameters list is empty; ensure no empty lists are used", err = true)
                return
            }

            1 -> {} // ok
            else -> {
                if (!allowJCStressReruns) {
                    echo(
                        "you likely don't want to run JCStress multiple times;" +
                                " if you're sure, enable --allow-jcs-reruns",
                        err = true
                    )
                    return
                }
            }
        }

        for (params in paramsList) {
            val nonDefaultParams = if (
                params.batchSize == DEFAULT_BATCH_SIZE &&
                params.syncPeriod == DEFAULT_SYNC_EVERY
            ) null else params // jcstress defaults are different
            JCStressRunner.runJCStress(
                nonDefaultParams,
                tests,
                jcstressDirectory,
                jcstressFreeArgs,
            )
        }
    }
}
