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
        accepted = setOf(0, 1)
    }
}

val SB: LTDefinition<*> = litmusTest({
    object {
        var x = 0
        var y = 0
        var r1 = 0
        var r2 = 0
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
    outcome {
        listOf(r1, r2)
    }
    // no need for explicit outcome{}
    spec {
        accepted = setOf(
            listOf(0, 0),
            listOf(0, 1),
            listOf(1, 1),
        )
        interesting = setOf(
            listOf(1, 0)
        )
    }
}

val SBVolatile: LTDefinition<*> = litmusTest({
    object {
        @Volatile
        var x = 0

        @Volatile
        var y = 0

        var r1 = 0
        var r2 = 0
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
    outcome {
        listOf(r1, r2)
    }
    spec {
        accepted = setOf(
            listOf(0, 0),
            listOf(0, 1),
            listOf(1, 1)
        )
    }
}

val MP: LTDefinition<*> = litmusTest({
    object {
        var x = 0
        var y = 0

        var r1 = 0
        var r2 = 0
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
    outcome {
        listOf(r1, r2)
    }
    spec {
        accepted = setOf(
            listOf(0, 0),
            listOf(0, 1),
            listOf(1, 1),
        )
        interesting = setOf(
            listOf(1, 0)
        )
    }
}

val MPVolatile: LTDefinition<*> = litmusTest({
    object {
        @Volatile
        var x = 0

        @Volatile
        var y = 0

        var r1 = 0
        var r2 = 0
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
    outcome {
        listOf(r1, r2)
    }
    spec {
        accepted = setOf(
            listOf(0, 0),
            listOf(0, 1),
            listOf(1, 1),
        )
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
        accepted = setOf(1, -1)
    }
}

val CoRR: LTDefinition<*> = litmusTest({
    object {
        var x = 0

        var r1 = 0
        var r2 = 0
    }
}) {
    thread {
        x = 1
    }
    thread {
        r1 = x
        r2 = x
    }
    outcome {
        listOf(r1, r2)
    }
    spec {
        accepted = setOf(
            listOf(0, 0),
            listOf(0, 1),
            listOf(1, 1)
        )
        interesting = setOf(
            listOf(1, 0)
        )
    }
}

val CoRR_CSE: LTDefinition<*> = litmusTest({
    data class Holder(var x: Int)
    object {
        val holder1 = Holder(0)
        val holder2 = holder1

        var r1 = 0
        var r2 = 0
        var r3 = 0
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
    outcome {
        listOf(r1, r2, r3)
    }
    spec {
        interesting = setOf(
            listOf(1, 0, 0),
            listOf(1, 1, 0),
        )
        default = LTOutcomeType.ACCEPTED
    }
}

val IRIW: LTDefinition<*> = litmusTest({
    object {
        var x = 0
        var y = 0

        var a = 0
        var b = 0
        var c = 0
        var d = 0
    }
}) {
    thread {
        x = 1
    }
    thread {
        y = 1
    }
    thread {
        a = x
        b = y
    }
    thread {
        c = y
        d = x
    }
    outcome {
        listOf(a, b, c, d)
    }
    spec {
        interesting = setOf(
            listOf(1, 0, 1, 0),
            listOf(0, 1, 0, 1),
        )
        default = LTOutcomeType.ACCEPTED
    }
}

val IRIWVolatile: LTDefinition<*> = litmusTest({
    object {
        @Volatile
        var x = 0

        @Volatile
        var y = 0

        var a = 0
        var b = 0
        var c = 0
        var d = 0
    }
}) {
    thread {
        x = 1
    }
    thread {
        y = 1
    }
    thread {
        a = x
        b = y
    }
    thread {
        c = y
        d = x
    }
    spec {
        forbidden = setOf(
            listOf(1, 0, 1, 0),
            listOf(0, 1, 0, 1)
        )
        default = LTOutcomeType.ACCEPTED
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
        accepted = setOf(0, -1)
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
    spec {
        accepted = setOf(1, -1)
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
        accepted = setOf(listOf(0, 0))
    }
}

val LB: LTDefinition<*> = litmusTest({
    object {
        var x = 0
        var y = 0

        var r1 = 0
        var r2 = 0
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
    outcome {
        listOf(r1, r2)
    }
    spec {
        accepted = setOf(
            listOf(0, 0),
            listOf(1, 0),
            listOf(0, 1)
        )
        interesting = setOf(
            listOf(1, 1)
        )
    }
}

val LBVolatile: LTDefinition<*> = litmusTest({
    object {
        @Volatile
        var x = 0

        @Volatile
        var y = 0

        var r1 = 0
        var r2 = 0
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
    outcome {
        listOf(r1, r2)
    }
    spec {
        accepted = setOf(
            listOf(0, 0),
            listOf(1, 0),
            listOf(0, 1)
        )
    }
}

val LBFakeDEPS: LTDefinition<*> = litmusTest({
    object {
        var x = 0
        var y = 0

        var r1 = 0
        var r2 = 0
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
        accepted = setOf(
            listOf(0, 0),
            listOf(0, 1)
        )
    }
}
