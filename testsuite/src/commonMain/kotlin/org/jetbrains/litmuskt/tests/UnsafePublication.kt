package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusIOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.autooutcomes.forbid
import org.jetbrains.litmuskt.autooutcomes.interesting
import org.jetbrains.litmuskt.litmusTest
import kotlin.concurrent.Volatile

@LitmusTestContainer
object UnsafePublication {

    private data class IntHolder(val x: Int = 0)

    val Plain = litmusTest({
        object : LitmusIOutcome() {
            var h: IntHolder? = null
        }
    }) {
        thread {
            h = IntHolder()
        }
        thread {
            r1 = h?.x ?: -1
        }
        spec {
            accept(0)
            accept(-1)
        }
    }

    val VolatileAnnotated = litmusTest({
        object : LitmusIOutcome() {
            @Volatile
            var h: IntHolder? = null
        }
    }) {
        thread {
            h = IntHolder()
        }
        thread {
            r1 = h?.x ?: -1
        }
        spec {
            accept(0)
            accept(-1)
        }
    }

    val PlainWithConstructor = litmusTest({
        object : LitmusIOutcome() {
            var h: IntHolder? = null
        }
    }) {
        thread {
            h = IntHolder(x = 1)
        }
        thread {
            r1 = h?.x ?: -1
        }
        spec {
            accept(1)
            accept(-1)
            interesting(0) // seeing the default value
        }
    }

    val PlainArray = litmusTest({
        object : LitmusIOutcome() {
            var arr: Array<Int>? = null
        }
    }) {
        thread {
            arr = Array(1) { 1 }
        }
        thread {
            r1 = arr?.get(0) ?: -1
        }
        spec {
            accept(1)
            // 0 is the default value for `Int`. However, since Int-s in `Array<Int>` are boxed, we don't get to see a 0.
            // On JVM, a NullPointerException here is technically valid. Currently, there is no support for exceptions as accepted outcomes.
            // On Native, there is no NullPointerException, so we can see a segmentation fault.
            interesting(0)
            accept(-1)
        }
    }

    val PlainIntArray = litmusTest({
        object : LitmusIOutcome() {
            var arr: IntArray? = null
        }
    }) {
        thread {
            arr = IntArray(1) { 1 }
        }
        thread {
            r1 = arr?.get(0) ?: -1
        }
        spec {
            accept(1)
            interesting(0)
            accept(-1)
        }
    }

    private class RefHolder(val ref: IntHolder)

    val Reference = litmusTest({
        object : LitmusIOutcome() {
            var h: RefHolder? = null
        }
    }) {
        thread {
            val ref = IntHolder(x = 1)
            h = RefHolder(ref)
        }
        thread {
            val t = h
            r1 = if (t != null) {
                val ref = t.ref
                // Despite what IDEA says, this `if` can fail because the default value for a reference is null.
                // Also, this `if` is not removed by the compiler even in release mode.
                if (ref != null) ref.x else 0
            } else -1
        }
        spec {
            accept(1)
            accept(-1)
            // Before the question about full construction guarantee is settled, keep the null outcome
            // as forbidden so that it shows up in CI runs.
            forbid(0)
        }
    }

    private class LeakingIntHolderContext {
        var ih: LeakingIntHolder? = null

        inner class LeakingIntHolder {
            val x: Int = 1

            init {
                ih = this
            }
        }
    }

    val PlainWithLeakingConstructor = litmusTest({
        object : LitmusIOutcome() {
            var ctx = LeakingIntHolderContext()
        }
    }) {
        thread {
            ctx.LeakingIntHolder()
        }
        thread {
            r1 = ctx.ih?.x ?: -1
        }
        spec {
            accept(1)
            accept(0)
            accept(-1)
        }
    }

}
