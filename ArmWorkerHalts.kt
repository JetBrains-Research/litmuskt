import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

fun main() {
    repeat(100_000) { i ->
        val w = Worker.start()
        val f = w.execute(TransferMode.SAFE, { i }) {
            5 + it
        }
        // w.requestTermination()
        val r = f.result
        println("future $i, result $r")
    }
}