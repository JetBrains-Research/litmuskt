package tests

import BasicLitmusTest
import setupOutcomes

class MP_NoDRF_Test : BasicLitmusTest("MP + broken DRF") {

    var x = 0
    var y = 0

    override  fun actor1() {
        y = 1
        x = 1
    }

    override  fun actor2() {
        if (y != 0) {
            outcome = x
        }
    }

    init {
        setupOutcomes {
            accepted = setOf(1, null)
             interesting = setOf(0)
        }
    }
}
