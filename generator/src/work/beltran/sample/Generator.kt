package work.beltran.sample

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.signature
import java.io.File
import java.io.PrintWriter
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class Generator : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        println("getSupportedAnnotationTypes")
        return mutableSetOf(GenName::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        println("process")
        roundEnv!!.getElementsAnnotatedWith(GenName::class.java)
            .forEach {
                val className = it.simpleName.toString()
                println("elm: $it")
                println("Processing: $className")

                val metadata = it.getAnnotation(Metadata::class.java)
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
                val klass = classMetadata.toKmClass()

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

                val pack = processingEnv.elementUtils.getPackageOf(it).toString()
                generateClass(className, pack)
            }
        return true
    }

    private fun generateClass(className: String, pack: String) {
        val fileName = "Generated_$className"
        val file = FileSpec.builder(pack, fileName)
            .addType(
                TypeSpec.classBuilder(fileName)
                    .addFunction(
                        FunSpec.builder("getName")
                            .addStatement("return \"World\"")
                            .build()
                    )
                    .build()
            )
            .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir, "$fileName.kt"))
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}