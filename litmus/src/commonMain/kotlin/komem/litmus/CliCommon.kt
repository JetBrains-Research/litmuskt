package komem.litmus

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import komem.litmus.barriers.BarrierProducer
import komem.litmus.generated.LitmusTestRegistry
import kotlin.time.Duration

abstract class CliCommon : CliktCommand(
    name = "litmuskt"
) {
    private val batchSizeSchedule by option("-b", "--batchSize")
        .int().varargValues().default(listOf(1_000_000))

    private val syncEverySchedule by option("-s", "--syncEvery")
        .int().varargValues().default(listOf(100))

    private val tests by argument("tests")
        .multiple(required = true)
        .transformAll { args ->
            val regexes = args.map {
                try {
                    Regex(it)
                } catch (_: IllegalArgumentException) {
                    fail("invalid regex: $it")
                }
            }
            regexes.flatMap { LitmusTestRegistry[it] }.toSet()
        }
        .check("no tests were selected") { it.isNotEmpty() }

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
        echo("selected tests: \n" + tests.joinToString("\n") { " - " + LitmusTestRegistry.resolveName(it) })
        echo("in total: ${tests.size} tests")
        echo()

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
        echo("parameter combinations per each test: ${paramsList.size}")
        echo()

        for (test in tests) {
            echo("running test ${LitmusTestRegistry.resolveName(test)}...")
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
