package komem.litmus

/**
 * A convenience interface to simplify specifying outcomes.
 *
 * All classes implementing this interface provide some r1, r2, ... variables
 * to write the outcome into. If a litmus test's state extends one of these classes,
 * specifying `outcome { ... }` is not necessary, as it will be inferred from r1, r2, ...
 *
 * Children classes should override `hashCode()` and `equals()` so that they are compared
 * based on their outcome only. They should also override `toString()` so that they only display
 * their outcome when printed. For these reasons the functions are overridden in this
 * interface such that their implementation is forced in children.
 *
 * These classes are also used as outcomes themselves in order to better utilize resources.
 */
sealed interface LitmusAutoOutcome {
    override fun toString(): String
    override fun hashCode(): Int

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun equals(o: Any?): Boolean
}

open class LitmusIOutcome(
    var r1: Int = 0,
) : LitmusAutoOutcome {
    // single values are handled differently (see LitmusOutcomeSpecScope)
    final override fun toString() = "$r1"
    final override fun hashCode() = r1
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusIOutcome) return false
        return r1 == o.r1
    }

    // note: if S is LitmusAutoState, even single values should only be interpreted as r1
    // hence no overloads for accept() and so on
}

open class LitmusIIOutcome(
    var r1: Int = 0,
    var r2: Int = 0
) : LitmusAutoOutcome {
    final override fun toString() = "($r1, $r2)"
    final override fun hashCode() = r1 shl 16 + r2
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusIIOutcome) return false
        return r1 == o.r1 && r2 == o.r2
    }
}

fun <S : LitmusIIOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Int, r2: Int) =
    accept(LitmusIIOutcome(r1, r2))

fun <S : LitmusIIOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Int, r2: Int) =
    interesting(LitmusIIOutcome(r1, r2))

fun <S : LitmusIIOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Int, r2: Int) =
    forbid(LitmusIIOutcome(r1, r2))

open class LitmusIIIOutcome(
    var r1: Int = 0,
    var r2: Int = 0,
    var r3: Int = 0,
) : LitmusAutoOutcome {
    final override fun toString() = "($r1, $r2, $r3)"
    final override fun hashCode() = r1 shl 20 + r2 shl 10 + r3
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusIIIOutcome) return false
        return r1 == o.r1 && r2 == o.r2 && r3 == o.r3
    }
}

fun <S : LitmusIIIOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Int, r2: Int, r3: Int) =
    accept(LitmusIIIOutcome(r1, r2, r3))

fun <S : LitmusIIIOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Int, r2: Int, r3: Int) =
    interesting(LitmusIIIOutcome(r1, r2, r3))

fun <S : LitmusIIIOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Int, r2: Int, r3: Int) =
    forbid(LitmusIIIOutcome(r1, r2, r3))

open class LitmusIIIIOutcome(
    var r1: Int = 0,
    var r2: Int = 0,
    var r3: Int = 0,
    var r4: Int = 0,
) : LitmusAutoOutcome {
    final override fun toString() = "($r1, $r2, $r3, $r4)"
    final override fun hashCode() = r1 shl 24 + r2 shl 16 + r3 shl 8 + r4
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusIIIIOutcome) return false
        return r1 == o.r1 && r2 == o.r2 && r3 == o.r3 && r4 == o.r4
    }
}

fun <S : LitmusIIIIOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Int, r2: Int, r3: Int, r4: Int) =
    accept(LitmusIIIIOutcome(r1, r2, r3, r4))

fun <S : LitmusIIIIOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Int, r2: Int, r3: Int, r4: Int) =
    interesting(LitmusIIIIOutcome(r1, r2, r3, r4))

fun <S : LitmusIIIIOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Int, r2: Int, r3: Int, r4: Int) =
    forbid(LitmusIIIIOutcome(r1, r2, r3, r4))
