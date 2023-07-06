package komem.litmus.barriers

interface Barrier {
    fun wait()
}

typealias BarrierProducer = (Int) -> Barrier
