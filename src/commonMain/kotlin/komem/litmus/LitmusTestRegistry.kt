package komem.litmus

import komem.litmus.testsuite.*

// TODO: incomplete list; adding tests automatically would be much better
object LitmusTestRegistry {
    val tests = mapOf(
        "atom" to ATOM,
        "sb" to SB,
        "mp" to MP,
        "iriw" to IRIW,
        "corr" to CoRR,
        "upub" to UPUB,
    )
}
