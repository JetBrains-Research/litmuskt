import komem.litmus.LitmusAutoOutcome
import komem.litmus.LitmusTest
import komem.litmus.generated.LitmusTestRegistry
import kotlin.io.path.*
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses
import kotlin.system.exitProcess

fun main() {
    val jcstressDirectory = Path("../jcstress")

    val tests = LitmusTestRegistry.all()
    val test = tests[1]

    val javaClassName = test.name.replace('.', '_')
    val targetFile = jcstressDirectory / "src/main/java/komem/litmus/$javaClassName.java"
    targetFile.createParentDirectories()
    val targetCode = generateWrapperCode(test, javaClassName)

    targetFile.writeText(targetCode)
    val mvn = ProcessBuilder("mvn", "-f", jcstressDirectory.absolutePathString(), "verify")
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
    mvn.waitFor()

    if (mvn.exitValue() != 0) {
        exitProcess(mvn.exitValue())
    }

    val jcs = ProcessBuilder(
        "java",
        "-jar",
        (jcstressDirectory / "target/jcstress.jar").absolutePathString(),
        "-t",
        javaClassName
    )
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
    jcs.waitFor()
}

val LitmusTest<*>.name get() = LitmusTestRegistry.resolveName(this)

private fun generateWrapperCode(test: LitmusTest<*>, javaClassName: String): String {
    val stateClass = test.stateProducer()!!::class
    require(stateClass.allSuperclasses.contains(LitmusAutoOutcome::class)) {
        "to use JCStress, test state must extend some LitmusAutoOutcome (e.g. LitmusIIOutcome)"
    }

    fun javaTestGetter(): String {
        val parts = test.name.split(".")
        val getter = "get" + parts.last() + "()"
        val className = parts.dropLast(1).last() + "Kt"
        val packages = parts.dropLast(2)
        val packagesLine = if (packages.isEmpty()) "" else packages.joinToString(".", postfix = ".")
        return "$packagesLine$className.$getter"
    }

    fun javaThreadFunctionDecl(index: Int) =
        "\tprivate static final Function1<Object, Unit> fT$index = test.getThreadFunctions().get($index);"

    fun javaActorDecl(index: Int) = """
    @Actor
    public void t$index() {
        fT$index.invoke(state);
    }
    
    """.trimEnd()

    fun javaArbiterDecl(): String {
        val autoOutcomeClassList = stateClass.superclasses
            .filter { it.isSubclassOf(LitmusAutoOutcome::class) }
        require(autoOutcomeClassList.size == 1) { "test state should extend exactly one LitmusAutoOutcome" }
        val autoOutcomeClass = autoOutcomeClassList.first()
        val outcomeTypeName = autoOutcomeClass.simpleName!!
            .removePrefix("Litmus")
            .removeSuffix("Outcome")
        val (varType, varCount) = when (outcomeTypeName) {
            "II" -> "Integer" to 2
            "III" -> "Integer" to 3
            "IIII" -> "Integer" to 4
            else -> error("unknown AutoOutcome type $outcomeTypeName")
        }

        val jcstressResultClassName = outcomeTypeName + "_Result"
        return """
    @Arbiter
    public void a($jcstressResultClassName r) {
        List<$varType> result = (List<$varType>) fA.invoke(state);
${List(varCount) { "        r.r${it + 1} = result.get($it);" }.joinToString("\n")}
    }

        """.trimEnd()
    }

    return """
package komem.litmus;

import komem.litmus.testsuite.*;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.*;

import java.util.List;

@JCStressTest
@State
@Outcome(expect = Expect.ACCEPTABLE)
public class $javaClassName {

    private static final LitmusTest<Object> test = (LitmusTest<Object>) ${javaTestGetter()};
${List(test.threadCount) { javaThreadFunctionDecl(it) }.joinToString("\n")}
    private static final Function1<Object, Object> fA = test.getOutcomeFinalizer();

    public $javaClassName() {
    }

    public Object state = test.getStateProducer().invoke();
${List(test.threadCount) { javaActorDecl(it) }.joinToString("\n")}
${javaArbiterDecl()}
}
    """.trimIndent()
}
