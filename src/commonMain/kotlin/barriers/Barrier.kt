package barriers

interface Barrier {
    fun wait()
}

typealias BarrierProducer = (Int) -> Barrier
