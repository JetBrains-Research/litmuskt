package komem.litmus

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class LitmusTestProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return LitmusTestProcessor(environment.codeGenerator)
    }
}

class LitmusTestProcessor(val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val basePackage = "komem.litmus"
        val registryFileName = "LitmusTestRegistry"

        val testFiles = resolver.getAllFiles().filter { it.packageName.asString() == "$basePackage.testsuite" }.toList()
        val dependencies = Dependencies(true, *testFiles.toTypedArray())

        val registryFile = try {
            codeGenerator.createNewFile(dependencies, "$basePackage.generated", registryFileName)
        } catch (e: FileAlreadyExistsException) { // TODO: this is a workaround
            return emptyList()
        }

        val decls = testFiles.flatMap { it.declarations }.filterIsInstance<KSPropertyDeclaration>()
        val namedTestsMap = decls.associate {
            val relativePackage = it.packageName.asString().removePrefix("$basePackage.testsuite")
            val testAlias = (if (relativePackage.isEmpty()) "" else "$relativePackage.") +
                    it.containingFile!!.fileName.removeSuffix(".kt") +
                    "." + it.simpleName.getShortName()
            val testName = it.qualifiedName!!.asString()
            testAlias to testName
        }

        val registryCode = """
package $basePackage.generated
import $basePackage.LitmusTest

object LitmusTestRegistry {
    private val tests: Map<String, LitmusTest<*>> = mapOf(
        ${namedTestsMap.entries.joinToString(",\n" + " ".repeat(8)) { (a, n) -> "\"$a\" to $n" }}
    )
    
    operator fun get(fullName: String) = tests[fullName]!!
     
    operator fun get(regex: Regex) = tests.filterKeys { regex.matches(it) }.values
    
    fun all() = tests.values
}

        """.trimIndent()

        registryFile.write(registryCode.toByteArray())
        return emptyList()
    }
}
