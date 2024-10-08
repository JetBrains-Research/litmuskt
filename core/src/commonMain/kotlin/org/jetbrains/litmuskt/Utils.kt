package org.jetbrains.litmuskt

import kotlin.time.Duration
import kotlin.time.TimeSource

fun List<List<String>>.tableFormat(hasHeader: Boolean = false): String {
    val columnCount = maxOf { it.size }
    val columnSizes = (0..<columnCount).map { i ->
        this.mapNotNull { it.getOrNull(i) }.maxOf { it.length } + 2
    }
    return buildString {
        this@tableFormat.forEach { row ->
            row.forEachIndexed { i, word ->
                val startPadding = (columnSizes[i] - word.length) / 2
                val endPadding = columnSizes[i] - word.length - startPadding
                append(" ".repeat(startPadding))
                append(word)
                append(" ".repeat(endPadding))
                if (i != row.size - 1) append("|")
            }
            appendLine()
            if (hasHeader && row === this@tableFormat.first()) {
                appendLine("-".repeat(columnSizes.sum() + columnCount - 1))
            }
        }
    }
}

expect fun cpuCount(): Int

@Suppress("UNCHECKED_CAST")
fun <S> TypedArray(size: Int, init: (Int) -> S): Array<S> = Array<Any?>(size, init) as Array<S>

/**
 * Returns a lazy iterable that iterates over a portion of the underlying array.
 */
fun <S> Array<S>.view(range: IntRange): Iterable<S> {
    return Iterable {
        object : Iterator<S> {
            private val delegate = range.iterator()
            override fun hasNext() = delegate.hasNext()
            override fun next(): S = this@view[delegate.nextInt()]
        }
    }
}

/**
 * Split a range into [n] parts of equal (+/- 1) length.
 */
fun IntRange.splitEqual(n: Int): List<IntRange> {
    val size = endInclusive - start + 1
    val len = size / n // base length of each sub-range
    val remainder = size % n
    val delim = start + (len + 1) * remainder // delimiter between lengths (l+1) and l
    return List(n) { i ->
        if (i < remainder) {
            (start + i * (len + 1))..<(start + (i + 1) * (len + 1))
        } else {
            val j = i - remainder
            (delim + j * len)..<(delim + (j + 1) * len)
        }
    }
}

/**
 * A future that blocks on calling [await].
 */
fun interface BlockingFuture<T> {
    fun await(): T
}

/**
 * Repeat a function for at least the given [duration]. Runs the function at least once.
 */
inline fun <T> repeatFor(duration: Duration, crossinline f: () -> T): List<T> = buildList {
    val start = TimeSource.Monotonic.markNow()
    do {
        add(f())
    } while (start.elapsedNow() < duration)
}
