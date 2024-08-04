package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.autooutcomes.LitmusAutoOutcome
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

fun generateWrapperFile(test: LitmusTest<*>, generatedSrc: Path): Boolean {
    val targetFile = run {
        val targetFilePackageFolder = test.qualifiedName.split(".").dropLast(2).joinToString("/")
        val targetFileClassName = test.javaClassName + ".java"
        generatedSrc / "main" / targetFilePackageFolder / targetFileClassName
    }
    targetFile.createParentDirectories()
    val targetCode = try {
        generateWrapperCode(test)
    } catch (e: Throwable) {
        System.err.println("WARNING: could not generate wrapper for ${test.alias} because:\n" + e.stackTraceToString())
        return false
    }
    targetFile.writeText(targetCode)
    return true
}

private fun generateWrapperCode(test: LitmusTest<*>): String {
    val stateClass = test.stateProducer()::class
    require(stateClass.allSuperclasses.contains(LitmusAutoOutcome::class)) {
        "to use JCStress, test state must extend some LitmusAutoOutcome (e.g. LitmusIIOutcome)"
    }

    val autoOutcomeClassList = stateClass.superclasses.filter { it.isSubclassOf(LitmusAutoOutcome::class) }
    require(autoOutcomeClassList.size == 1) { "test state should extend exactly one LitmusAutoOutcome" }
    val outcomeTypeName = autoOutcomeClassList.first().simpleName!!
        .removePrefix("Litmus")
        .removeSuffix("Outcome")

    val outcomeVarTypes = outcomeTypeName.map { c ->
        when (c) {
            'I' -> "Integer"
            'L' -> "Long"
            'Z' -> "Boolean"
            // TODO: add others once they are created
            else -> error("unrecognized outcome type '$c'")
        }
    }

    val javaTestGetter: String = run {
        val (className, testName) = test.alias.split(".")
        val getter = "get${testName.replaceFirstChar { it.uppercaseChar() }}()"
        "$className.INSTANCE.$getter"
    }

    val javaArbiterDecl: String = run {
        val jcstressResultClassName = outcomeTypeName + "_Result"
        """
@Arbiter
public void a($jcstressResultClassName r) {
    List<Object> result = (List<Object>) (Object) ((LitmusAutoOutcome) fA.invoke(state)).toList();
    ${List(outcomeVarTypes.size) { "r.r${it + 1} = (${outcomeVarTypes[it]}) result.get($it);" }.joinToString("\n    ")}
}
        """.trim()
    }

    val jcstressOutcomeDecls: String = run {
        val outcomes = test.outcomeSpec.accepted.associateWith { "ACCEPTABLE" } +
                test.outcomeSpec.interesting.associateWith { "ACCEPTABLE_INTERESTING" } +
                test.outcomeSpec.forbidden.associateWith { "FORBIDDEN" }

        // since only AutoOutcome is allowed, the cast is safe
        outcomes.map { (o, t) ->
            val oId = (o as LitmusAutoOutcome).toList().joinToString(", ")
            "@Outcome(id = \"$oId\", expect = $t)"
        }.joinToString("\n")
    }

    val jcstressDefaultOutcomeType = when (test.outcomeSpec.default) {
        LitmusOutcomeType.ACCEPTED -> "ACCEPTABLE"
        LitmusOutcomeType.FORBIDDEN -> "FORBIDDEN"
        LitmusOutcomeType.INTERESTING -> "ACCEPTABLE_INTERESTING"
    }

    val testParentClassFQN = test.qualifiedName.split(".").dropLast(1).joinToString(".")

    return wrapperCode(
        test,
        jcstressOutcomeDecls,
        jcstressDefaultOutcomeType,
        javaTestGetter,
        javaArbiterDecl,
        testParentClassFQN
    )
}

private fun javaThreadFunctionDecl(index: Int) =
    "private static final Function1<Object, Unit> fT$index = test.getThreadFunctions().get($index);"

private fun javaActorDecl(index: Int) = """
    @Actor
    public void t$index() {
        fT$index.invoke(state);
    }
    """.trimIndent()

fun wrapperCode(
    test: LitmusTest<*>,
    jcstressOutcomeDecls: String,
    jcstressDefaultOutcomeType: String,
    javaTestGetter: String,
    javaArbiterDecl: String,
    testParentClassFQN: String,
) = """
package ${test.qualifiedName.split(".").dropLast(2).joinToString(".")};

import org.jetbrains.litmuskt.*;
import org.jetbrains.litmuskt.autooutcomes.*;

import $testParentClassFQN;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import java.util.List;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.*;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@State
$jcstressOutcomeDecls
@Outcome(expect = $jcstressDefaultOutcomeType)
public class ${test.javaClassName} {

    private static final LitmusTest<Object> test = (LitmusTest<Object>) $javaTestGetter;
    ${List(test.threadCount) { javaThreadFunctionDecl(it) }.joinToString("\n    ")}
    private static final Function1<Object, Object> fA = test.getOutcomeFinalizer();

    public ${test.javaClassName}() {}

    public Object state = test.getStateProducer().invoke();
    
    ${List(test.threadCount) { javaActorDecl(it).padded(4) }.joinToString("\n\n    ")}
    
    ${javaArbiterDecl.padded(4)}
}
""".trimIndent()

private fun String.padded(padding: Int) = replace("\n", "\n" + " ".repeat(padding))
