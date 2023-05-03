package barriers

import kotlin.native.concurrent.AtomicInt

class SpinBarrier(private val threadCount: Int) : Barrier {
    private val waitingCount = AtomicInt(0)
    private val passedBarriersCount = AtomicInt(0)

    override fun wait() {
        val oldPassed = passedBarriersCount.value
        if (waitingCount.addAndGet(1) == threadCount) {
            waitingCount.value = 0
            passedBarriersCount.increment()
        } else {
            while (passedBarriersCount.value == oldPassed) passedBarriersCount.value // spin
        }
    }
}
