package org.jetbrains.litmuskt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import org.jetbrains.litmuskt.generated.LitmusTestRegistry
import org.jetbrains.litmuskt.*
import kotlin.time.Duration

abstract class CliCommon : CliktCommand(
    name = "litmuskt",
    printHelpOnEmptyArgs = true,
) {
    companion object {
        const val DEFAULT_BATCH_SIZE = 1_000_000
        const val DEFAULT_SYNC_EVERY = 100
    }

    protected open val batchSizeSchedule by option("-b", "--batchSize")
        .int().varargValues().default(listOf(DEFAULT_BATCH_SIZE))

    protected open val syncEverySchedule by option("-s", "--syncEvery")
        .int().varargValues().default(listOf(DEFAULT_SYNC_EVERY))

    protected open val tests by argument("tests")
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
        .check("no tests were selected") { it.isNotEmpty() || listOnly }

    protected val PARALLELISM_DISABLED = Int.MAX_VALUE - 1
    protected val PARALLELISM_AUTO = Int.MAX_VALUE - 2
    protected open val parallelism by option("-p", "--parallelism")
        .int().optionalValue(PARALLELISM_AUTO).default(PARALLELISM_DISABLED)
        .check("value must be in range 2..1000") {
            it in 2..1000 || it == PARALLELISM_DISABLED || it == PARALLELISM_AUTO
        }

    protected open val duration by option("-d", "--duration")
        .convert { Duration.parse(it) }
        .check("value must be positive") { it.isPositive() }

    protected abstract val affinityMapSchedule: List<AffinityMap?>
    protected abstract val runner: LitmusRunner
    protected abstract val barrierProducer: BarrierProducer
    // TODO: we don't talk about memshuffler for now

    protected val listOnly by option("-l", "--listOnly").flag()
    // TODO: dry run = simply list tests

    override fun run() {
        if (listOnly) {
            runListOnly()
            return
        }
        echo("selected tests: \n" + tests.joinToString("\n") { " - " + it.name })
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

    private fun runListOnly() {
        echo("all known tests:\n" + LitmusTestRegistry.all().joinToString("\n") { " * " + it.name })
        echo()
        echo("selected tests:\n" + tests.joinToString("\n") { " - " + it.name })
    }
}

fun commonMain(args: Array<String>, cli: CliCommon) {
    try {
        cli.main(args)
    } catch (e: Exception) {
        cli.echo(e.stackTraceToString(), err = true, trailingNewline = true)
    }
}
