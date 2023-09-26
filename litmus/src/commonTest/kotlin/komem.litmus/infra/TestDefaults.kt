package komem.litmus.infra

import komem.litmus.LitmusOutcomeType
import komem.litmus.LitmusRunParams
import komem.litmus.LitmusRunner
import komem.litmus.LitmusTest
import kotlin.test.assertTrue

expect val defaultParams: LitmusRunParams
expect val defaultRunner: LitmusRunner

fun LitmusTest<*>.run(
    params: LitmusRunParams = defaultParams,
    runner: LitmusRunner = defaultRunner,
) {
    val results = runner.runTest(params, this)
    assertTrue { results.none { it.type == LitmusOutcomeType.FORBIDDEN } }
    if (results.any { it.type == LitmusOutcomeType.INTERESTING }) {
        println("interesting cases detected") // TODO: provide test name (?)
    }
}
