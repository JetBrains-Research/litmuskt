package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusIOutcome
import org.jetbrains.litmuskt.LitmusTest
import org.jetbrains.litmuskt.accept
import org.jetbrains.litmuskt.litmusTest
import kotlin.concurrent.Volatile

val UPUBVolatile: LitmusTest<*> = litmusTest({
    object : LitmusIOutcome() {
        @Volatile
        var h: IntHolder? = null
    }
}) {
    thread {
        h = IntHolder(0)
    }
    thread {
        r1 = h?.x ?: -1
    }
    spec {
        accept(0)
        accept(-1)
    }
}

val UPUBArray: LitmusTest<*> = litmusTest({
    object : LitmusIOutcome() {
        var arr: Array<Int>? = null
    }
}) {
    thread {
        arr = Array(10) { 0 }
    }
    thread {
        r1 = arr?.get(0) ?: -1
    }
    spec {
        accept(0)
        accept(-1)
    }
}

private class UPUBRefInner(val x: Int)
private class UPUBRefHolder(val ref: UPUBRefInner)

val UPUBRef: LitmusTest<*> = litmusTest({
    object : LitmusIOutcome() {
        var h: UPUBRefHolder? = null
    }
}) {
    thread {
        val ref = UPUBRefInner(1)
        h = UPUBRefHolder(ref)
    }
    thread {
        val t = h
        r1 = t?.ref?.x ?: -1
    }
    spec {
        accept(1)
        accept(-1)
    }
}

private class UPUBIntHolderInnerLeaking {
    var ih: InnerHolder? = null

    inner class InnerHolder {
        val x: Int

        init {
            x = 1
            ih = this
        }
    }
}

val UBUBCtorLeaking: LitmusTest<*> = litmusTest({
    object : LitmusIOutcome() {
        var h = UPUBIntHolderInnerLeaking()
    }
}) {
    thread {
        h.InnerHolder()
    }
    thread {
        r1 = h.ih?.x ?: -1
    }
    spec {
        accept(1)
        accept(0)
        accept(-1)
    }
}
