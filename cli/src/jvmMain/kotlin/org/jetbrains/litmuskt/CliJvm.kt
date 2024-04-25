package org.jetbrains.litmuskt

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import jcstressDirectory
import org.jetbrains.litmuskt.barriers.JvmSpinBarrier

private sealed class RunnerOptions : OptionGroup() {
    abstract val runner: LitmusRunner
}

private class JvmThreadRunnerOptions : RunnerOptions() {
    override val runner = JvmThreadRunner()
}

private class JCStressRunnerOptions : RunnerOptions() {
    private val jcstressFreeArgs by option("-j", "--jcsargs")
        .convert { it.split(" ") }
        .default(emptyList())

    override val runner get() = JCStressRunner(jcstressDirectory, jcstressFreeArgs)
}

class CliJvm : CliCommon() {
    private val runnerOptions by option("-r", "--runner").groupChoice(
        "thread" to JvmThreadRunnerOptions(),
        "jcstress" to JCStressRunnerOptions(),
    ).required()
    override val runner get() = runnerOptions.runner

    override val barrierProducer = ::JvmSpinBarrier
    override val affinityMapSchedule = listOf(null)

    private val allowJCStressReruns by option("--allow-jcs-reruns")
        .flag()

    override fun run() = if (runner is JCStressRunner) jcstressRun() else super.run()

    private fun jcstressRun() {
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
            val jcsParams = if (
                params.batchSize == DEFAULT_BATCH_SIZE &&
                params.syncPeriod == DEFAULT_SYNC_EVERY
            ) JCStressRunner.DEFAULT_LITMUSKT_PARAMS else params // jcstress defaults are different

            for (test in tests) {
                val results = runner.runTest(jcsParams, test)
                echo("\n" + results.generateTable())
            }
        }
    }
}
