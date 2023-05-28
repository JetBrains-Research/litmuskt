import kaffinity.*
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.cpu_set_t
import platform.posix.errno
import platform.posix.pthread_t
import platform.posix.strerror
import kotlin.native.concurrent.Worker

private fun Int.callCheck() {
    if (this != 0) {
        val err = strerror(errno)!!.toKString()
        throw IllegalStateException("C call error: $err")
    }
}

private fun setAffinity(thread: pthread_t, cpus: Set<Int>): Unit = memScoped {
    require(cpus.isNotEmpty())
    val set = alloc<cpu_set_t>()
    for (cpu in cpus) cpu_set(cpu, set.ptr)
    set_affinity(thread, set.ptr).callCheck()
}

private fun getAffinity(thread: pthread_t): Set<Int> = memScoped {
    val set = alloc<cpu_set_t>()
    get_affinity(thread, set.ptr).callCheck()
    return (0 until cpu_setsize())
            .filter { cpu_isset(it, set.ptr) != 0 }
            .toSet()
}

@OptIn(ExperimentalStdlibApi::class)
actual fun getAffinityManager(): AffinityManager? = object : AffinityManager {
    override fun setAffinity(w: Worker, cpus: Set<Int>) {
        setAffinity(w.platformThreadId, cpus)
    }

    override fun getAffinity(w: Worker): Set<Int> {
        return getAffinity(w.platformThreadId)
    }
}
