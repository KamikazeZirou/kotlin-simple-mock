package work.beltran.sample

import com.google.auto.service.AutoService
import com.google.common.io.MoreFiles
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmValueParameter
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.signature
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class MockGenerator : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Mockable::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        roundEnv!!.getElementsAnnotatedWith(Mockable::class.java)
            .forEach {
                val klass = it.toKmClass()
                dumpKlass(klass)
                generateMock(klass)
            }
        return true
    }

    private fun Element.toKmClass(): KmClass {
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
        return classMetadata.toKmClass()
    }

    @OptIn(KotlinPoetMetadataPreview::class)
    private fun generateMock(klass: KmClass) {
        val names = klass.name.split("/")
        val packageName = names.dropLast(1).joinToString(".")
        val className = klass.name.split("/").last()
        val fileName = "Mock$className"

        val file = FileSpec.builder(packageName, fileName)
            .addType(
                TypeSpec.classBuilder(fileName)
                    .addSuperinterface(ClassInspectorUtil.createClassName(klass.name))
//                    .addProperties(klass.functions.map {
//                        TODO()
//                    })
                    .addFunctions(
                        klass.functions.map { it.toFunctionSpec() }
                    )
                    .build()
            )
            .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir, "$fileName.kt"))
    }

    @OptIn(KotlinPoetMetadataPreview::class)
    private fun KmClassifier.toClassName(): ClassName {
        val klass = this as KmClassifier.Class
        return ClassInspectorUtil.createClassName(klass.name)
    }

    @KotlinPoetMetadataPreview
    private fun KmFunction.toFunctionSpec(): FunSpec = FunSpec.builder(name)
        .addModifiers(KModifier.OVERRIDE)
        .addParameters(valueParameters.map { it.toParameterSpec() })
        .returns(returnType.classifier.toClassName())
        .addStatement("TODO()")
        .build()


    @OptIn(KotlinPoetMetadataPreview::class)
    private fun KmValueParameter.toParameterSpec(): ParameterSpec =
        ParameterSpec.builder(name, type!!.classifier.toClassName())
            .build()

    private fun dumpKlass(klass: KmClass) {
        println("#\n#Supertypes\n#")
        klass.supertypes.forEach {
            println(it.classifier)
            println(it.arguments)
        }

        println("#\n# Constructors\n#")
        klass.constructors.forEach { constructor ->
            println(constructor.signature)
        }

        klass.properties.forEach { property ->
            println(property.name)
            println(property.returnType.classifier)
            println(property.typeParameters)
        }
        klass.functions.forEach { func ->
            println()
            println(func.name)
            println(func.signature)
            func.valueParameters.forEach {
                println(it.name)
                println(it.type?.classifier)
            }
            println(func.returnType.classifier)
        }
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}