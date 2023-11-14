package komem.litmus

import kotlinx.cinterop.*
import platform.posix.pthread_create

fun sayHi() {
    println("hi!")
}

object PthreadRunner : LitmusRunner() {
    @OptIn(ExperimentalForeignApi::class)
    override fun <S> runTest(params: LitmusRunParams, test: LitmusTest<S>): LitmusResult {

        memScoped {
            val p = alloc<ULongVar>() // pthread_t = ULong
            pthread_create(
                __newthread = p.ptr,
                __attr = null,
                __start_routine = staticCFunction<COpaquePointer?, COpaquePointer?> {
                    sayHi()
                    return@staticCFunction null
                },
                __arg = null,
            )
        }

        return emptyList()
    }
}