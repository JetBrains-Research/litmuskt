package org.jetbrains.litmuskt

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import org.jetbrains.litmuskt.CliCommon
import org.jetbrains.litmuskt.barriers.CinteropSpinBarrier
import org.jetbrains.litmuskt.PthreadRunner
import org.jetbrains.litmuskt.WorkerRunner
import org.jetbrains.litmuskt.AffinityMap

class CliNative : CliCommon() {
    override val runner by option("-r", "--runner")
        .choice(mapOf("worker" to WorkerRunner(), "pthread" to PthreadRunner()))
        .default(WorkerRunner())

    private val affinityMapChoices = run {
        val schedulesMapped = mutableMapOf<String, List<AffinityMap?>>("none" to listOf(null))
        org.jetbrains.litmuskt.getAffinityManager()?.let {
            schedulesMapped["short"] = it.presetShort()
            schedulesMapped["long"] = it.presetLong()
        }
        schedulesMapped
    }
    override val affinityMapSchedule by option("-a", "--affinity")
        .choice(affinityMapChoices).default(listOf(null))

    override val barrierProducer = ::CinteropSpinBarrier
}
