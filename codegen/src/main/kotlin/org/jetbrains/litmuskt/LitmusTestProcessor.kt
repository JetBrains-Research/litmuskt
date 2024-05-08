package org.jetbrains.litmuskt

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class LitmusTestProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return LitmusTestProcessor(environment.codeGenerator, environment.logger)
    }
}

private const val testContainerAnnotationFQN = "org.jetbrains.litmuskt.LitmusTestContainer"
private const val basePackage = "org.jetbrains.litmuskt"
private const val generatedPackage = "$basePackage.generated"
private const val registryFileName = "LitmusTestRegistry"

class LitmusTestProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val containerClassDecls = resolver.getSymbolsWithAnnotation(testContainerAnnotationFQN)
            .filterIsInstance<KSClassDeclaration>()
            .toList()
        // check for name collisions
        containerClassDecls
            .groupingBy { it.simpleName.asString() }
            .eachCount()
            .filter { it.value > 1 }
            .takeIf { it.isNotEmpty() }
            ?.let { logger.error("container class name collision: $it") }
        // check that all containers are objects
        containerClassDecls
            .filterNot { it.classKind == ClassKind.OBJECT }
            .takeIf { it.isNotEmpty() }
            ?.let { logger.error("container class must be an object: $it") }

        val decls = containerClassDecls
            .flatMap { it.getAllProperties() }
            .filter { it.type.resolve().declaration.simpleName.asString() == "LitmusTest" }
            .toList()
        // should prevent extra rounds, or else createNewFile() will throw
        if (decls.isEmpty()) return emptyList()

        val inputFiles = decls.mapNotNull { it.containingFile }.toSet()
        val dependencies = Dependencies(true, *inputFiles.toTypedArray())
        val registryFile = codeGenerator.createNewFile(dependencies, generatedPackage, registryFileName)

        val namedTestsMap = decls.associate {
            val testAlias = run {
                val parentClassDecl = it.parentDeclaration
                    ?: error("test declaration at ${it.location} has no parent container class")
                parentClassDecl.simpleName.asString() + "." + it.simpleName.asString()
            }
            val testFQN = it.qualifiedName!!.asString()
            testAlias to testFQN
        }

        val registryCode = """
package $generatedPackage
import $basePackage.LitmusTest

actual object LitmusTestRegistry {

    private data class TestData(
        val alias: String,
        val fqn: String,
    )

    private val tests: Map<LitmusTest<*>, TestData> = mapOf(
        ${
            namedTestsMap.entries.joinToString(",\n" + " ".repeat(8)) { (alias, fqn) ->
                "$fqn to TestData(\"$alias\", \"$fqn\")"
            }
        }
    )
    
    operator fun get(regex: Regex): List<LitmusTest<*>> = 
        tests.entries.filter { regex.matches(it.value.alias) }.map { it.key }
    
    fun all(): List<LitmusTest<*>> = tests.keys.toList()
    
    fun getAlias(test: LitmusTest<*>): String = tests[test]?.alias ?: error("unknown test")
    fun getFQN(test: LitmusTest<*>): String = tests[test]?.fqn ?: error("unknown test") 
}

        """.trimIndent()

        registryFile.write(registryCode.toByteArray())
        return emptyList()
    }
}
