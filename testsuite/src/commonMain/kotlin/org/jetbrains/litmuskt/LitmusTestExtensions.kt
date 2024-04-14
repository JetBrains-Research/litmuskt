package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.generated.LitmusTestRegistry
import org.jetbrains.litmuskt.LitmusTest

val LitmusTest<*>.name get() = LitmusTestRegistry.resolveName(this)
val LitmusTest<*>.javaClassName get() = name.replace('.', '_')
