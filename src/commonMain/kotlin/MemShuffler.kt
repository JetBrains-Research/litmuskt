import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

class MemShuffler(bufferSize: Int, random: Random = Random) : ReadWriteProperty<BasicLitmusTest, Int> {
    val buffer = MutableList(bufferSize) { 0 }
    val shuffledIndices = buffer.indices.shuffled(random)
//    val shuffledIndices = run {
//        require(bufferSize % 7 != 0)
//        generateSequence(0) { ((it + 7) % bufferSize).takeUnless { it == 0 } }.toList()
//    }
    var ptr = 0
    val propIdx = mutableMapOf<KProperty<*>, Int>()

    @Suppress("OVERRIDE_BY_INLINE")
    inline fun provideDelegate(thisRef: BasicLitmusTest, prop: KProperty<*>): ReadWriteProperty<BasicLitmusTest, Int> {
        require(ptr < buffer.size) { "memshuffle buffer is exhausted" }
        propIdx[prop] = shuffledIndices[ptr++]
        return this
    }

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun getValue(thisRef: BasicLitmusTest, property: KProperty<*>): Int {
        return buffer[propIdx[property]!!]
    }

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun setValue(thisRef: BasicLitmusTest, property: KProperty<*>, value: Int) {
        buffer[propIdx[property]!!] = value
    }
}
