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

    // for JCStress interop
    fun toList(): List<LitmusOutcome>
}

open class LitmusIOutcome(
    var r1: Int = 0,
) : LitmusAutoOutcome {
    final override fun toString() = "$r1"
    final override fun hashCode() = r1
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusIOutcome) return false
        return r1 == o.r1
    }

    final override fun toList() = listOf(r1)
}

fun <S : LitmusIOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Int) =
    accept(setOf(LitmusIOutcome(r1)))

fun <S : LitmusIOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Int) =
    interesting(setOf(LitmusIOutcome(r1)))

fun <S : LitmusIOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Int) =
    forbid(setOf(LitmusIOutcome(r1)))

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

    final override fun toList() = listOf(r1, r2)
}

fun <S : LitmusIIOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Int, r2: Int) =
    accept(setOf(LitmusIIOutcome(r1, r2)))

fun <S : LitmusIIOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Int, r2: Int) =
    interesting(setOf(LitmusIIOutcome(r1, r2)))

fun <S : LitmusIIOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Int, r2: Int) =
    forbid(setOf(LitmusIIOutcome(r1, r2)))

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

    final override fun toList() = listOf(r1, r2, r3)
}

fun <S : LitmusIIIOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Int, r2: Int, r3: Int) =
    accept(setOf(LitmusIIIOutcome(r1, r2, r3)))

fun <S : LitmusIIIOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Int, r2: Int, r3: Int) =
    interesting(setOf(LitmusIIIOutcome(r1, r2, r3)))

fun <S : LitmusIIIOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Int, r2: Int, r3: Int) =
    forbid(setOf(LitmusIIIOutcome(r1, r2, r3)))

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

    final override fun toList() = listOf(r1, r2, r3, r4)
}

fun <S : LitmusIIIIOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Int, r2: Int, r3: Int, r4: Int) =
    accept(setOf(LitmusIIIIOutcome(r1, r2, r3, r4)))

fun <S : LitmusIIIIOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Int, r2: Int, r3: Int, r4: Int) =
    interesting(setOf(LitmusIIIIOutcome(r1, r2, r3, r4)))

fun <S : LitmusIIIIOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Int, r2: Int, r3: Int, r4: Int) =
    forbid(setOf(LitmusIIIIOutcome(r1, r2, r3, r4)))
