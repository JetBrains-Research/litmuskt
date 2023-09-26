package komem.litmus

import platform.posix._SC_NPROCESSORS_ONLN
import platform.posix.sysconf

actual fun cpuCount(): Int = sysconf(_SC_NPROCESSORS_ONLN).toInt()
