package org.jetbrains.litmuskt

import kaffinity.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.cpu_set_t
import platform.posix.pthread_t
import kotlin.native.concurrent.ObsoleteWorkersApi

@OptIn(ExperimentalStdlibApi::class, ObsoleteWorkersApi::class)
actual val affinityManager: AffinityManager? = object : AffinityManager {

    // TODO: affinity for k_pthread

    override fun setAffinity(threadlike: Threadlike, cpus: Set<Int>): Boolean {
        when (threadlike) {
            is WorkerThreadlike -> setPthreadDirectAffinity(threadlike.worker.platformThreadId, cpus)
            else -> return false
        }
        return true
    }

    override fun getAffinity(threadlike: Threadlike): Set<Int>? = when (threadlike) {
        is WorkerThreadlike -> getPthreadDirectAffinity(threadlike.worker.platformThreadId)
        else -> null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setPthreadDirectAffinity(thread: pthread_t, cpus: Set<Int>): Unit = memScoped {
        require(cpus.isNotEmpty())
        val set = alloc<cpu_set_t>()
        cpu_zero(set.ptr)
        for (cpu in cpus) cpu_set(cpu, set.ptr)
        set_affinity(thread, set.ptr).syscallCheck()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun getPthreadDirectAffinity(thread: pthread_t): Set<Int> = memScoped {
        val set = alloc<cpu_set_t>()
        get_affinity(thread, set.ptr).syscallCheck()
        return (0..<cpu_setsize())
            .filter { cpu_isset(it, set.ptr) != 0 }
            .toSet()
    }
}
