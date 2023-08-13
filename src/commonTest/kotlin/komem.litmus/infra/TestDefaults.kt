package komem.litmus.infra

import komem.litmus.LTDefinition
import komem.litmus.LTOutcomeType
import komem.litmus.LTRunParams
import komem.litmus.LTRunner
import kotlin.test.assertTrue

expect val defaultParams: LTRunParams
expect val defaultRunner: LTRunner

fun LTDefinition<*>.run(
    params: LTRunParams = defaultParams,
    runner: LTRunner = defaultRunner,
) {
    val results = runner.runTest(params, this)
    assertTrue { results.none { it.type == LTOutcomeType.FORBIDDEN } }
    if (results.any { it.type == LTOutcomeType.INTERESTING }) {
        println("interesting cases detected") // TODO: provide test name (?)
    }
}
