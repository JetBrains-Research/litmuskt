package komem.litmus

interface AutoOutcome {
    fun getOutcome(): Any?
}

open class IIOutcome(
    var r1: Int = 0,
    var r2: Int = 0
) : AutoOutcome {
    override fun getOutcome() = listOf(r1, r2)
}

open class IIIOutcome(
    var r1: Int = 0,
    var r2: Int = 0,
    var r3: Int = 0,
) : AutoOutcome {
    override fun getOutcome() = listOf(r1, r2, r3)
}

open class IIIIOutcome(
    var r1: Int = 0,
    var r2: Int = 0,
    var r3: Int = 0,
    var r4: Int = 0,
) : AutoOutcome {
    override fun getOutcome() = listOf(r1, r2, r3, r4)
}