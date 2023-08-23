package komem.litmus.testsuite

import komem.litmus.*
import kotlin.concurrent.Volatile

data class IntHolder(val x: Int)

class IntHolderCtor {
    val x = 1
}

val ATOM: LTDefinition<*> = litmusTest({
    object {
        var x = 0
        var o = 0
    }
}) {
    thread {
        x = -1 // signed 0xFFFFFFFF
    }
    thread {
        o = x
    }
    outcome {
        o
    }
    spec {
        accept(0)
        accept(-1)
    }
}

val SB: LTDefinition<*> = litmusTest({
    object : IIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        x = 1
        r1 = y
    }
    thread {
        y = 1
        r2 = x
    }
    // no need for explicit outcome{}
    spec {
        accept(0, 1)
        accept(1, 0)
        accept(1, 1)
        interesting(0, 0)
    }
}

val SBVolatile: LTDefinition<*> = litmusTest({
    object : IIOutcome() {
        @Volatile
        var x = 0

        @Volatile
        var y = 0
    }
}) {
    thread {
        x = 1
        r1 = y
    }
    thread {
        y = 1
        r2 = x
    }
    // no need for explicit outcome{}
    spec {
        accept(0, 1)
        accept(1, 0)
        accept(1, 1)
    }
}

val MP: LTDefinition<*> = litmusTest({
    object : IIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        x = 1
        y = 1
    }
    thread {
        r1 = y
        r2 = x
    }
    spec {
        accept(0, 0)
        accept(0, 1)
        accept(1, 1)
        interesting(1, 0)
    }
}

val MPVolatile: LTDefinition<*> = litmusTest({
    object : IIOutcome() {
        @Volatile
        var x = 0

        @Volatile
        var y = 0
    }
}) {
    thread {
        x = 1
        y = 1
    }
    thread {
        r1 = y
        r2 = x
    }
    spec {
        accept(0, 0)
        accept(0, 1)
        accept(1, 1)
    }
}

val MP_DRF: LTDefinition<*> = litmusTest({
    object {
        var x = 0

        @Volatile
        var y = 0
        var o = 0
    }
}) {
    thread {
        x = 1
        y = 1
    }
    thread {
        o = if (y != 0) x else -1
    }
    outcome {
        o
    }
    spec {
        accept(1)
        accept(-1)
    }
}

val CoRR: LTDefinition<*> = litmusTest({
    object : IIOutcome() {
        var x = 0
    }
}) {
    thread {
        x = 1
    }
    thread {
        r1 = x
        r2 = x
    }
    spec {
        accept(0, 0)
        accept(0, 1)
        accept(1, 1)
        interesting(1, 0)
    }
}

val CoRR_CSE: LTDefinition<*> = litmusTest({
    data class Holder(var x: Int)
    object : IIIOutcome() {
        val holder1 = Holder(0)
        val holder2 = holder1
    }
}) {
    thread {
        holder1.x = 1
    }
    thread {
        val h1 = holder1
        val h2 = holder2
        r1 = h1.x
        r2 = h2.x
        r3 = h1.x
    }
    spec {
        interesting(1, 0, 0)
        interesting(1, 1, 0)
        defaultTo = LTOutcomeType.ACCEPTED
    }
}

val IRIW: LTDefinition<*> = litmusTest({
    object : IIIIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        x = 1
    }
    thread {
        y = 1
    }
    thread {
        r1 = x
        r2 = y
    }
    thread {
        r3 = y
        r4 = x
    }
    spec {
        interesting(1, 0, 1, 0)
        interesting(0, 1, 0, 1)
        defaultTo = LTOutcomeType.ACCEPTED
    }
}

val IRIWVolatile: LTDefinition<*> = litmusTest({
    object : IIIIOutcome() {
        @Volatile
        var x = 0

        @Volatile
        var y = 0
    }
}) {
    thread {
        x = 1
    }
    thread {
        y = 1
    }
    thread {
        r1 = x
        r2 = y
    }
    thread {
        r3 = y
        r4 = x
    }
    spec {
        forbid(1, 0, 1, 0)
        defaultTo = LTOutcomeType.ACCEPTED
    }
}

val UPUB: LTDefinition<*> = litmusTest({
    object {
        var h: IntHolder? = null
        var o = 0
    }
}) {
    thread {
        h = IntHolder(0)
    }
    thread {
        o = h?.x ?: -1
    }
    outcome {
        o
    }
    spec {
        accept(0)
        accept(-1)
    }
}

val UPUBCtor: LTDefinition<*> = litmusTest({
    object {
        var h: IntHolderCtor? = null
        var o = 0
    }
}) {
    thread {
        h = IntHolderCtor()
    }
    thread {
        o = h?.x ?: -1
    }
    outcome {
        o
    }
    spec {
        accept(1)
        accept(-1)
    }
}

val LB_DEPS_OOTA: LTDefinition<*> = litmusTest({
    object {
        var x = 0
        var y = 0
        var a = 0
        var b = 0
    }
}) {
    thread {
        a = x
        y = a
    }
    thread {
        b = y
        x = b
    }
    outcome {
        listOf(a, b)
    }
    spec {
        accept(0, 0)
    }
}

val LB: LTDefinition<*> = litmusTest({
    object : IIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        r1 = x
        y = 1
    }
    thread {
        r2 = y
        x = 1
    }
    spec {
        accept(0, 0)
        accept(1, 0)
        accept(0, 1)
        interesting(1, 1)
    }
}

val LBVolatile: LTDefinition<*> = litmusTest({
    object : IIOutcome() {
        @Volatile
        var x = 0

        @Volatile
        var y = 0
    }
}) {
    thread {
        r1 = x
        y = 1
    }
    thread {
        r2 = y
        x = 1
    }
    spec {
        accept(0, 0)
        accept(1, 0)
        accept(0, 1)
    }
}

val LBFakeDEPS: LTDefinition<*> = litmusTest({
    object : IIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        r1 = x
        y = 1 + r1 * 0
    }
    thread {
        r2 = y
        x = r2
    }
    spec {
        accept(0, 0)
        accept(0, 1)
    }
}

val VolatileAsFence: LTDefinition<*> = litmusTest({
    object : IIIIOutcome() {
        @Volatile var x: Int = 0
        @Volatile var y: Int = 0
    }
}) {
    class Local(@Volatile var z: Int = 0)
    fun fakeFence() = Local().run { z = 1 }

    thread {
        r1 = x
        fakeFence()
        r2 = x
    }
    thread {
        r3 = y
        fakeFence()
        r4 = y
    }
    thread {
        x = 2
        fakeFence()
        y = 1
    }
    thread {
        y = 2
        fakeFence()
        x = 1
    }
    spec {
        forbid(1, 2, 1, 2)
        defaultTo = LTOutcomeType.ACCEPTED
    }
}