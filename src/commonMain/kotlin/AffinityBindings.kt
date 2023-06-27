@file:OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class)

import kotlin.native.concurrent.Worker
import kotlin.random.Random

interface AffinityManager {
    fun setAffinity(w: Worker, cpus: Set<Int>)
    fun getAffinity(w: Worker): Set<Int>

    fun mapShifted(shift: Int): AffinityMap = object : AffinityMap {
        private val cpus: List<Set<Int>>

        init {
            val tmp = MutableList(cpuCount()) { setOf<Int>() }
            var i = 0
            repeat(tmp.size) {
                tmp[i] = setOf(i)
                i = (i + shift) % tmp.size
                if (tmp[i].isNotEmpty()) i++
            }
            cpus = tmp
        }

        override fun allowedCores(workerIndex: Int) = cpus[workerIndex]
    }

    fun mapRandom(random: Random = Random): AffinityMap = object : AffinityMap {
        private val cpus = (0 until cpuCount()).shuffled(random).map { setOf(it) }
        override fun allowedCores(workerIndex: Int) = cpus[workerIndex]
    }

    fun presetShort(): List<AffinityMap> = listOf(
        mapShifted(1),
        mapShifted(2),
        mapShifted(4),
    )

    fun presetLong(): List<AffinityMap> = List(cpuCount()) { mapShifted(it) } + listOf(
        object : AffinityMap {
            override fun allowedCores(workerIndex: Int) = setOf(0, 1)
        },
        object : AffinityMap {
            override fun allowedCores(workerIndex: Int) = setOf(1, 2)
        },
        object : AffinityMap {
            override fun allowedCores(workerIndex: Int) = setOf(
                (workerIndex * 2) % cpuCount(),
                (workerIndex * 2 + 1) % cpuCount()
            )
        }
    )
}

expect fun getAffinityManager(): AffinityManager?
