package komem.litmus

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import komem.litmus.barriers.BarrierProducer
import kotlin.time.Duration

abstract class CliCommon : CliktCommand(
    name = "komem-litmus",
    printHelpOnEmptyArgs = true
) {
    private val batchSizeSchedule by option("-b", "--batchSize")
        .int().varargValues().default(listOf(1_000_000))

    private val syncEverySchedule by option("-s", "--syncEvery")
        .int().varargValues().default(listOf(100))

    private val tests by option("-t", "--tests")
        .convert { arg ->
            val regex = arg.toRegex()
            LitmusTestRegistry.tests.filterKeys { regex.matches(it) }.values
        }
        .default(LitmusTestRegistry.tests.values)
        .check("no tests were selected by regex") { it.isNotEmpty() }

    private val PARALLELISM_DISABLED = Int.MAX_VALUE - 1
    private val PARALLELISM_AUTO = Int.MAX_VALUE - 2
    private val parallelism by option("-p", "--parallelism")
        .int().optionalValue(PARALLELISM_AUTO).default(PARALLELISM_DISABLED)
        .check("value must be in range 2..100") {
            it in 2..100 || it == PARALLELISM_DISABLED || it == PARALLELISM_AUTO
        }

    private val duration by option("-d", "--duration")
        .convert { Duration.parse(it) }
        .check("value must be positive") { it.isPositive() }

    protected abstract val affinityMapSchedule: List<AffinityMap?>
    protected abstract val runner: LitmusRunner
    protected abstract val barrierProducer: BarrierProducer
    // TODO: we don't talk about memshuffler for now

    override fun run() {
        val paramsList = variateRunParams(
            batchSizeSchedule = batchSizeSchedule,
            affinityMapSchedule = affinityMapSchedule,
            syncPeriodSchedule = syncEverySchedule,
            barrierSchedule = listOf(barrierProducer),
        ).toList()
        if (paramsList.isEmpty()) {
            echo("parameters list is empty; ensure no empty lists are used", err = true)
            return
        }
        echo("params list size: ${paramsList.size}")
        echo("selected tests count: ${tests.size}")
        echo()

        for (test in tests) {
            echo("running next test...")
            // TODO: handle exceptions
            paramsList.map { params ->
                // TODO: print ETA (later: calculate based on part of run)
                runTest(params, test)
            }.mergeResults().let {
                echo(it.generateTable())
            }
            echo()
        }
    }

    private fun runTest(params: LitmusRunParams, test: LitmusTest<*>): LitmusResult {
        val timeLimit = duration
        return when (parallelism) {
            PARALLELISM_DISABLED -> {
                if (timeLimit == null) {
                    runner.runTest(params, test)
                } else {
                    runner.runTest(timeLimit, params, test)
                }
            }

            PARALLELISM_AUTO -> {
                if (timeLimit == null) {
                    runner.runTestParallel(params, test)
                } else {
                    runner.runTestParallel(timeLimit, params, test)
                }
            }

            else -> {
                if (timeLimit == null) {
                    runner.runTestParallel(parallelism, params, test)
                } else {
                    runner.runTestParallel(parallelism, timeLimit, params, test)
                }
            }
        }
    }
}

fun commonMain(args: Array<String>, cli: CliCommon) {
    try {
        cli.main(args)
    } catch (e: Exception) {
        cli.echo(e.stackTraceToString(), err = true, trailingNewline = true)
    }
}
