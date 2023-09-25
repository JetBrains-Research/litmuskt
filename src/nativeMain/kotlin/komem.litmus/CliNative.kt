package komem.litmus

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import komem.litmus.barriers.CinteropSpinBarrier

class CliNative : CliCommon() {
    override val runner = WorkerRunner

    private val affinityMapChoices = run {
        val schedulesMapped = mutableMapOf<String, List<AffinityMap?>>("none" to listOf(null))
        getAffinityManager()?.let {
            schedulesMapped["short"] = it.presetShort()
            schedulesMapped["long"] = it.presetLong()
        }
        schedulesMapped
    }
    override val affinityMapSchedule by option("-a", "--affinity")
        .choice(affinityMapChoices).default(listOf(null))

    override val barrierProducer = ::CinteropSpinBarrier
}
