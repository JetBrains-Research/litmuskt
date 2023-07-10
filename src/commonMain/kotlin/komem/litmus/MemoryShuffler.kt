package komem.litmus

import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

class MemoryShuffler(bufferSize: Int, random: Random = Random) : ReadWriteProperty<LitmusTest, Int> {
    val buffer = MutableList(bufferSize) { 0 }
    val shuffledIndices = buffer.indices.shuffled(random)

    var ptr = 0
    val propIdx = mutableMapOf<KProperty<*>, Int>()

    inline fun provideDelegate(thisRef: LitmusTest, prop: KProperty<*>): ReadWriteProperty<LitmusTest, Int> {
        require(ptr < buffer.size) { "memshuffle buffer is exhausted" }
        propIdx[prop] = shuffledIndices[ptr++]
        return this
    }

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun getValue(thisRef: LitmusTest, property: KProperty<*>): Int {
        return buffer[propIdx[property]!!]
    }

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun setValue(thisRef: LitmusTest, property: KProperty<*>, value: Int) {
        buffer[propIdx[property]!!] = value
    }
}

typealias MemShufflerProducer = () -> MemoryShuffler
