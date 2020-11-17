package work.beltran.sample

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement


data class Taco(val seasoning: String, val soft: Boolean) {
    fun foo(): List<String> = listOf()
}


@AutoService(Processor::class)
@KotlinPoetMetadataPreview
class MockGenerator : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Mockable::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    @KotlinPoetMetadataPreview
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        roundEnv!!.getElementsAnnotatedWith(Mockable::class.java)
            .forEach {
                val klass = it.toKmClass()
                generateMock(klass)
            }
        return true
    }

    private fun generateMock(klass: ImmutableKmClass) {
        val names = klass.name.split("/")
        val packageName = names.dropLast(1).joinToString(".")
        val className = klass.name.split("/").last()
        val fileName = "Mock$className"

        val type = klass.toTypeSpec(classInspector = null, className = ClassName(packageName, fileName))

        val file = FileSpec.builder(packageName, fileName)
            .addType(
                TypeSpec.classBuilder(fileName)
                    .addSuperinterface(ClassInspectorUtil.createClassName(klass.name))
                    .addProperties(type.funSpecs.map { funSpec ->
                        PropertySpec
                            .builder(
                                "${funSpec.name}FuncHandler", LambdaTypeName.get(
                                    funSpec.receiverType,
                                    parameters = funSpec.parameters,
                                    returnType = funSpec.returnType!!
                                ).copy(nullable = true)
                            )
                            .mutable()
                            .initializer("null")
                            .build()
                    })
                    .addProperties(type.funSpecs.map { funSpec ->
                        PropertySpec
                            .builder("${funSpec.name}CallCount", Int::class.asTypeName())
                            .mutable()
                            .initializer("0")
                            .build()
                    })
                    .addFunctions(type.funSpecs.map { funSpec ->
                        val params = funSpec.parameters.joinToString(",") { it.name }
                        FunSpec.builder(funSpec.name)
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameters(funSpec.parameters)
                            .apply { funSpec.returnType?.let { returns(it) } }
                            .addStatement("${funSpec.name}CallCount += 1")
                            .addStatement("return ${funSpec.name}FuncHandler!!(${params})")
                            .build()
                    })
                    .build()
            )
            .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir, "$fileName.kt"))
    }

    private fun Element.toKmClass(): ImmutableKmClass {
        val metadata = getAnnotation(Metadata::class.java)
        val header = KotlinClassHeader(
            kind = metadata.kind,
            metadataVersion = metadata.metadataVersion,
            bytecodeVersion = metadata.bytecodeVersion,
            data1 = metadata.data1,
            data2 = metadata.data2,
            extraString = metadata.extraString,
            packageName = metadata.packageName,
            extraInt = metadata.extraInt
        )
        val kotlinMetadata = KotlinClassMetadata.read(header)
        val classMetadata = kotlinMetadata as KotlinClassMetadata.Class
        return classMetadata.toImmutableKmClass()
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}