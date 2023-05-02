class JCS01 : BasicLitmusTest("data races") {

    var x = 0

    override  fun actor1() {
        x = 1
    }

    override  fun actor2() {
        outcome = x
    }
}

class JCS05 : BasicLitmusTest("coherence") {

    var x = 0

    override  fun actor1() {
        x = 1
    }

    override  fun actor2() {
        val a = x
        val b = x
        outcome = a to b
    }
}

class JCS06_MP : BasicLitmusTest("causality == MP") {
    var x = 0
    var y = 0

    init {
        setupOutcomes {
            interesting = setOf(1 to 0)
        }
    }

    override  fun actor1() {
        x = 1
        y = 1
    }

    override  fun actor2() {
        val r1 = y
        val r2 = x
        outcome = r1 to r2
    }
}

class JCS07_SB : BasicLitmusTest("consensus == SB") {
    var x = 0 // by memShuffler!!
    var y = 0 // by memShuffler!!
    var o1 = 0
    var o2 = 0

    init {
        setupOutcomes {
            interesting = setOf(0 to 0)
        }
    }

    override  fun actor1() {
        x = 1
        o1 = y
    }

    override  fun actor2() {
        y = 1
        o2 = x
    }

    override fun arbiter() {
        outcome = o1 to o2
    }
}

class LB : BasicLitmusTest("load buffering") {

    var x = 0
    var y = 0

    data class DoubleOutcome(var o1: Int, var o2: Int)

    init {
        outcome = DoubleOutcome(0, 0)
    }

    override  fun actor1() {
        val r1 = y
        x = 1
        (outcome as DoubleOutcome).o1 = r1
    }

    override  fun actor2() {
        val r2 = x
        y = 1
        (outcome as DoubleOutcome).o2 = r2
    }

}

class JCS08Custom : BasicLitmusTest("UPUB+Ctor") {

    class Holder {
        var x = 1
    }

    var h: Holder? = null

    override  fun actor1() {
        h = Holder()
    }

    override  fun actor2() {
        val t = h
        if (t != null) {
            outcome = t.x
        }
    }

    init {
        setupOutcomes {
            accepted = setOf(1, null)
            interesting = setOf(0)
            forbidOther = true
        }
    }
}

class JCS08 : BasicLitmusTest("finals publishing") {

    val v = 1
    var obj: MyObject? = null

    override  fun actor1() {
        obj = MyObject(v)
    }

    override  fun actor2() {
        val r = MutableList(4) { -1 }
        val o = obj
        if(o != null) {
            r[0] = o.x1
            r[1] = o.x2
            r[2] = o.x3
            r[3] = o.x4
        }
        outcome = r
    }

    class MyObject(v: Int) {
        val x1: Int
        val x2: Int
        val x3: Int
        val x4: Int

        init {
            x1 = v
            x2 = x1 + v
            x3 = x2 + v
            x4 = x3 + v
        }
    }

}

class JCS10 : BasicLitmusTest("oota") {
    var x = 0
    var y = 0

    override  fun actor1() {
        if (x == 1) y = 1
    }

    override  fun actor2() {
        if (y == 1) x = 1
    }

    override fun arbiter() {
        outcome = x to y
    }
}
