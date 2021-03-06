package mock.simple.kotlin.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import kotlinx.metadata.KmVariance
import kotlinx.metadata.internal.metadata.ProtoBuf
import kotlinx.metadata.internal.metadata.deserialization.Flags
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import mock.simple.kotlin.Mockable
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@OptIn(KotlinPoetMetadataPreview::class)
internal class SimpleMockProcessor : AbstractProcessor() {
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
                generateMock(klass)
            }
        return true
    }

    private fun generateMock(klass: ImmutableKmClass) {
        val names = klass.name.split("/")
        val packageName = names.dropLast(1).joinToString(".")
        val mockClassName = "Mock${names.last()}".replace(".", "")

        val targetType = klass.toTypeSpec(classInspector = null)
        val mockType = klass.toMockClass().toTypeSpec(
            classInspector = null,
            className = ClassName(packageName, mockClassName)
        )

        // don't generate super interface methods and properties.
        // How can we generate methods and properties of super interface?
        val file = FileSpec.builder(packageName, mockClassName)
            .addType(
                mockType.toBuilder()
                    .addSuperinterface(klass.toSuperInterface())
                    .addMockFunctions(targetType.funSpecs)
                    .addMockProperties(targetType.propertySpecs)
                    .build()
            )
            .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            ?: processingEnv.options[DEFAULT_KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir, "$mockClassName.kt"))
    }

    private fun ImmutableKmClass.toSuperInterface(): TypeName {
        return ClassInspectorUtil.createClassName(name)
            .let { className ->
                if (typeParameters.isNotEmpty()) {
                    className.parameterizedBy(typeParameters.map {
                        TypeVariableName(it.name)
                    })
                } else {
                    className
                }
            }
    }

    private fun ImmutableKmClass.toMockClass(): ImmutableKmClass {
        val mockClass = toMutable()

        // drop unnecessary data.
        mockClass.companionObject = null
        mockClass.nestedClasses.clear()
        mockClass.supertypes.clear()

        // drop methods and properties because the mock implementation generates later.
        mockClass.functions.clear()
        mockClass.properties.clear()
        mockClass.flags.isAbstract

        // Interface -> Class
        mockClass.flags = mockClass.flags
            .clearFlag(Flags.CLASS_KIND, ProtoBuf.Class.Kind.INTERFACE.number)
            .clearFlag(Flags.MODALITY, ProtoBuf.Modality.ABSTRACT.number)

        // drop variance because it is unnecessary
        mockClass.typeParameters.forEach {
            it.variance = KmVariance.INVARIANT
        }

        return mockClass.toImmutable()
    }

    private fun Int.clearFlag(field: Flags.FlagField<*>, value: Int): Int {
        return (this and (((1 shl field.bitWidth) - 1) shl field.offset).inv())
    }

    private fun TypeSpec.Builder.addMockFunctions(funSpecs: List<FunSpec>): TypeSpec.Builder = apply {
        val funcNames = funSpecs.map { it.name }.groupingBy { it }.eachCount()
        funSpecs.forEach {
            addMockFunction(it, funcNames.getOrElse(it.name) { 0 } > 1)
        }
    }

    private fun TypeSpec.Builder.addMockFunction(funSpec: FunSpec, overloads: Boolean = false): TypeSpec.Builder =
        apply {
            val funcParamStr = if (overloads) {
                funSpec.parameters.fold(StringBuilder()) { builder, spec ->
                    val typeName = spec.type
                        .toString()
                        .split(".")
                        .last()
                        .replace("?", "Opt")
                        .capitalize()
                    builder.append(spec.name.capitalize() + typeName)
                }.toString()
            } else {
                ""
            }

            val funcHandlerName = "${funSpec.name}${funcParamStr}Handler"
            val counterName = "${funSpec.name}${funcParamStr}CallCount"
            val argCaptureName = "${funSpec.name}${funcParamStr}ArgValues"

            addProperty(
                PropertySpec
                    .builder(
                        funcHandlerName, LambdaTypeName.get(
                            funSpec.receiverType,
                            parameters = funSpec.parameters,
                            returnType = funSpec.returnType ?: Unit::class.asTypeName()
                        ).copy(nullable = true)
                    )
                    .mutable()
                    .initializer("null")
                    .build()
            )

        // Generate a counter to call the method.
        addProperty(
            PropertySpec
                .builder(counterName, Int::class.asTypeName())
                .mutable()
                .initializer("0")
                .build()
        )

        // Generate argument captures if necessary.
        // The type of argument capture is List<List<*>>>.
        if (funSpec.parameters.isNotEmpty()) {
            addProperty(
                PropertySpec
                    .builder(
                        argCaptureName,
                        MUTABLE_LIST.parameterizedBy(
                            List::class.asTypeName().parameterizedBy(
                                WildcardTypeName.producerOf(
                                    Any::class.asTypeName().copy(nullable = true)
                                )
                            )
                        )
                    )
                    .mutable()
                    .initializer("mutableListOf()")
                    .build()
            )
        }

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
                .addStatement("$counterName += 1")
                .apply {
                    // Generate argument captures if necessary.
                    if (funSpec.parameters.isNotEmpty()) {
                        addStatement("${argCaptureName}.add(listOf(${params}))")
                    }
                }
                .addStatement("return ${funcHandlerName}!!(${params})")
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
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kotlin.simple.mock.generated"
        const val DEFAULT_KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}