package work.beltran.sample

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
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
        val mockClassName = "Mock$className"

        val type = klass.toTypeSpec(classInspector = null, className = ClassName(packageName, mockClassName))

        val file = FileSpec.builder(packageName, mockClassName)
            .addType(
                TypeSpec.classBuilder(mockClassName)
                    .addSuperinterface(ClassInspectorUtil.createClassName(klass.name))
                    .addMockFunctions(type.funSpecs)
                    .addMockProperties(type.propertySpecs)
                    .build()
            )
            .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir, "$mockClassName.kt"))
    }

    private fun TypeSpec.Builder.addMockFunctions(funSpecs: List<FunSpec>): TypeSpec.Builder = apply {
        funSpecs.forEach {
            addMockFunction(it)
        }
    }

    private fun TypeSpec.Builder.addMockFunction(funSpec: FunSpec): TypeSpec.Builder = apply {
        addProperty(
            PropertySpec
                .builder(
                    "${funSpec.name}FuncHandler", LambdaTypeName.get(
                        funSpec.receiverType,
                        parameters = funSpec.parameters,
                        returnType = funSpec.returnType ?: Unit::class.asTypeName()
                    ).copy(nullable = true)
                )
                .mutable()
                .initializer("null")
                .build()
        ).addProperty(
            PropertySpec
                .builder("${funSpec.name}CallCount", Int::class.asTypeName())
                .mutable()
                .initializer("0")
                .build()

        ).addProperty(
            PropertySpec
                .builder(
                    "${funSpec.name}FuncArgValues",
                    MUTABLE_LIST.parameterizedBy(
                        List::class.asTypeName().parameterizedBy(Any::class.asTypeName())
                    )
                )
                .mutable()
                .initializer("mutableListOf()")
                .build()
        )


        val params = funSpec.parameters.joinToString(",") { it.name }
        val supportModifiers = setOf(
            KModifier.SUSPEND
        )

        addFunction(
            FunSpec.builder(funSpec.name)
                .addModifiers(KModifier.OVERRIDE)
                .addModifiers(funSpec.modifiers.filter {
                    supportModifiers.contains(it)
                })
                .addParameters(funSpec.parameters)
                .apply { funSpec.returnType?.let { returns(it) } }
                .addStatement("${funSpec.name}CallCount += 1")
                .addStatement("${funSpec.name}FuncArgValues.add(listOf(${params}))")
                .addStatement("return ${funSpec.name}FuncHandler!!(${params})")
                .build()
        )
    }

    private fun TypeSpec.Builder.addMockProperties(propertySpecs: List<PropertySpec>): TypeSpec.Builder = apply {
        propertySpecs.forEach {
            addMockProperty(it)
        }
    }

    private fun TypeSpec.Builder.addMockProperty(propertySpec: PropertySpec): TypeSpec.Builder = apply {
        addProperty(
            PropertySpec
                .builder(
                    "underlying${propertySpec.name.capitalize()}",
                    propertySpec.type.copy(nullable = true)
                )
                .mutable(true)
                .initializer("null")
                .build()
        ).addProperty(
            PropertySpec
                .builder("${propertySpec.name}SetCallCount", Int::class.asTypeName())
                .mutable()
                .initializer("0")
                .build()
        ).addProperty(
            PropertySpec
                .builder(propertySpec.name, propertySpec.type)
                .addModifiers(KModifier.OVERRIDE)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return underlying${propertySpec.name.capitalize()}!!")
                        .build()
                )
                .apply {
                    if (propertySpec.mutable) {
                        mutable(true)
                        setter(
                            FunSpec.setterBuilder()
                                .addParameter("newValue", propertySpec.type)
                                .addStatement("underlying${propertySpec.name.capitalize()} = newValue")
                                .addStatement("${propertySpec.name}SetCallCount += 1")
                                .build()
                        )
                    }
                }
                .build()
        )
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