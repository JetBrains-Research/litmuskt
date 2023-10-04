package komem.litmus.barriers

interface Barrier {
    fun await()
}

typealias BarrierProducer = (Int) -> Barrier
