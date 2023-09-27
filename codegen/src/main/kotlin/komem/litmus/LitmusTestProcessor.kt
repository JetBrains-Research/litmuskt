package komem.litmus

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class LitmusTestProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return LitmusTestProcessor(environment.codeGenerator, environment.logger)
    }
}

class LitmusTestProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        generateTestRegistry(resolver)
        generateTestStateStubs(resolver)
        return emptyList()
    }

    private fun generateTestRegistry(resolver: Resolver) {
        val basePackage = "komem.litmus"
        val registryFileName = "LitmusTestRegistry"

        val testFiles = resolver.getAllFiles().filter { it.packageName.asString() == "$basePackage.testsuite" }.toList()
        val dependencies = Dependencies(true, *testFiles.toTypedArray())

        val registryFile = try {
            codeGenerator.createNewFile(dependencies, "$basePackage.generated", registryFileName)
        } catch (e: FileAlreadyExistsException) { // TODO: this is a workaround
            return
        }

        val decls = testFiles.flatMap { it.declarations }
            .filterIsInstance<KSPropertyDeclaration>()
        val registryCode = """
            package $basePackage.generated
            import $basePackage.testsuite.*
            import $basePackage.LitmusTest
            
            object LitmusTestRegistry {
                val tests: List<LitmusTest<*>> = listOf(
                    ${decls.joinToString(separator = ", ") { it.simpleName.asString() }}
                )
            }
        """.trimIndent()

        registryFile.write(registryCode.toByteArray())
    }

    private fun generateTestStateStubs(resolver: Resolver) {
        val decls = resolver.getSymbolsWithAnnotation("kotlin.concurrent.Volatile")
            .toList()
        logger.warn("found anno: ${decls.size}")
//        decls.forEach { decl ->
//            logger.warn("${decl::class}")
//        }
    }
}
