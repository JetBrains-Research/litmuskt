object SyncPeriodSchedules {
    fun quadGrowing(until: Int) = generateSequence(1) { (it * 1.2).toInt() + 1 }.takeWhile { it < until }.toList()
}

fun variateParameters(
        affinitySchedule: List<AffinityMap>,
        syncPeriodSchedule: List<Int>,
        memShufflerProducerSchedule: List<(() -> MemShuffler)?>
) = sequence<LitmusTestParameters> {
    for (affinity in affinitySchedule) {
        for (syncPeriod in syncPeriodSchedule) {
            for(memShuffler in memShufflerProducerSchedule) {
                yield(LitmusTestParameters(affinity, syncPeriod, memShuffler))
            }
        }
    }
}
