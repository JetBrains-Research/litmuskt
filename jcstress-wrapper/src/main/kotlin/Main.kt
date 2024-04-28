import org.jetbrains.litmuskt.generateWrapperFile
import org.jetbrains.litmuskt.generated.LitmusTestRegistry
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div

@OptIn(ExperimentalPathApi::class)
fun main() {
    var successCnt = 0
    val allTests = LitmusTestRegistry.all()
    val generatedSrc = jcstressDirectory / "generatedSrc"
    try {
        generatedSrc.deleteRecursively()
    } catch (_: Exception) {
    }
    for (test in allTests) {
        val success = generateWrapperFile(test, generatedSrc)
        if (success) successCnt++
    }
    if (successCnt != allTests.size) {
        System.err.println("WARNING: generated wrappers for $successCnt out of ${allTests.size} known tests")
    }
}

// TODO: this is very shaky, only works because all subprojects are on the same level
val jcstressDirectory = Path("../jcstress/")
