package komem.litmus

/**
 * A convenience interface to simplify specifying outcomes.
 *
 * All classes implementing this interface provide some r1, r2, ... variables
 * to write the outcome into. If a litmus test's state extends one of these classes,
 * specifying `outcome { ... }` is not necessary, as it will be inferred from r1, r2, ...
 *
 * These classes are also used as outcomes themselves to better utilize resources.
 */
interface LitmusAutoState {
    override fun toString(): String
    override fun hashCode(): Int

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun equals(o: Any?): Boolean
}

fun LitmusAutoState(values: List<LitmusOutcome>): LitmusAutoState {
    if (values.size == 1) {
        val o1 = values[0]
        if (o1 is Int) return LitmusIState(o1)
    }
    if (values.size == 2) {
        val o1 = values[0]
        val o2 = values[1]
        if (o1 is Int && o2 is Int) return LitmusIIState(o1, o2)
    }
    if (values.size == 3) {
        val o1 = values[0]
        val o2 = values[1]
        val o3 = values[2]
        if (o1 is Int && o2 is Int && o3 is Int) return LitmusIIIState(o1, o2, o3)
    }
    if (values.size == 4) {
        val o1 = values[0]
        val o2 = values[1]
        val o3 = values[2]
        val o4 = values[3]
        if (o1 is Int && o2 is Int && o3 is Int && o4 is Int) return LitmusIIIIState(o1, o2, o3, o4)
    }
    error("no LitmusAutoOutcome to support values $values")
}

open class LitmusIState(
    var r1: Int = 0,
) : LitmusAutoState {
    // single values are handled differently (see LitmusOutcomeSpecScope)
    final override fun toString() = "($r1)"
    final override fun hashCode() = r1
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusIState) return false
        return r1 == o.r1
    }
}

open class LitmusIIState(
    var r1: Int = 0,
    var r2: Int = 0
) : LitmusAutoState {
    final override fun toString() = "($r1, $r2)"
    final override fun hashCode() = r1 shl 16 + r2
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusIIState) return false
        return r1 == o.r1 && r2 == o.r2
    }
}

open class LitmusIIIState(
    var r1: Int = 0,
    var r2: Int = 0,
    var r3: Int = 0,
) : LitmusAutoState {
    final override fun toString() = "($r1, $r2, $r3)"
    final override fun hashCode() = r1 shl 20 + r2 shl 10 + r3
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusIIIState) return false
        return r1 == o.r1 && r2 == o.r2 && r3 == o.r3
    }
}

open class LitmusIIIIState(
    var r1: Int = 0,
    var r2: Int = 0,
    var r3: Int = 0,
    var r4: Int = 0,
) : LitmusAutoState {
    final override fun toString() = "($r1, $r2, $r3, $r4)"
    final override fun hashCode() = r1 shl 24 + r2 shl 16 + r3 shl 8 + r4
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusIIIIState) return false
        return r1 == o.r1 && r2 == o.r2 && r3 == o.r3 && r4 == o.r4
    }
}
