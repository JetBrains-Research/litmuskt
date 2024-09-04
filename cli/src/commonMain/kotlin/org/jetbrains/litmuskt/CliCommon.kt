package org.jetbrains.litmuskt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import org.jetbrains.litmuskt.generated.LitmusTestRegistry
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
            regexes.flatMap { LitmusTestRegistry[it] }
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
        .default(Duration.ZERO)
        .check("value must not be negative") { !it.isNegative() }

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
        echo("selected ${tests.size} tests: \n" + tests.joinToString("\n") { " - " + it.alias })
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
            echo("running test ${test.alias}...")
            // TODO: handle exceptions
            // TODO: print ETA (later: calculate based on part of run)
            val result = paramsList.map { params ->
                runTest(params, test)
            }.mergeResults()
            echo(result.generateTable(), false)
            echo("total count: ${result.totalCount()}, overall status: ${result.overallStatus()}")
            echo()
        }
    }

    private fun runTest(params: LitmusRunParams, test: LitmusTest<*>): LitmusResult {
        return when (parallelism) {
            PARALLELISM_DISABLED -> {
                // note: not running all tests here because of changing params
                @Suppress("UNCHECKED_CAST")
                runner.runTests(listOf(test) as List<LitmusTest<Any>>, params, duration).first()
            }
            PARALLELISM_AUTO -> {
                runner.runSingleTestParallel(test, params, timeLimit = duration)
            }
            else -> {
                runner.runSingleTestParallel(test, params, timeLimit = duration, instances = parallelism)
            }
        }
    }

    protected fun runListOnly() {
        echo("all known tests:\n" + LitmusTestRegistry.all().joinToString("\n") { " * " + it.alias })
        echo()
        echo("selected tests:\n" + tests.joinToString("\n") { " - " + it.alias })
    }
}

fun commonMain(args: Array<String>, cli: CliCommon) {
    try {
        cli.main(args)
    } catch (e: Exception) {
        cli.echo(e.stackTraceToString(), err = true, trailingNewline = true)
    }
}
