import kotlin.native.concurrent.Worker

interface AffinityManager {
    fun setAffinity(w: Worker, cpus: Set<Int>)
    fun getAffinity(w: Worker): Set<Int>

    fun scheduleShort2(): List<AffinityMap>
    fun scheduleLong2(): List<AffinityMap>
}

expect fun getAffinityManager(): AffinityManager?

fun List<AffinityMap>?.orUnrestricted(actorCount: Int): List<AffinityMap> {
    return this ?: affinityScheduleUnrestricted(actorCount)
}

fun affinityScheduleUnrestricted(actorCount: Int) = listOf(List(actorCount) { (0..7).toSet() })
