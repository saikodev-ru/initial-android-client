package ru.saikodev.initial.data.api.converter

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Retrofit converter factory using kotlinx.serialization.
 *
 * Kotlin serialization plugin generates the serializer accessor in the companion object.
 * Depending on the Kotlin/compiler-plugin version, it may be:
 *   - A function `serializer()` on the Companion instance
 *   - A property getter `getSerializer()` on the Companion instance
 *   - A backing field `serializer` on the Companion instance
 *   - The Companion itself may implement KSerializer
 *
 * We try all strategies to ensure compatibility across versions.
 */
class KotlinxSerializationConverterFactory(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val serializer = resolveSerializer(type)
        return KotlinxSerializationResponseBodyConverter(json, serializer)
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        val serializer = resolveSerializer(type)
        return KotlinxSerializationRequestBodyConverter(json, serializer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveSerializer(type: Type): KSerializer<Any> {
        val clazz = when (type) {
            is Class<*> -> type
            is ParameterizedType -> {
                val raw = type.rawType
                if (raw is Class<*>) raw
                else throw IllegalArgumentException("Cannot resolve serializer for type: $type")
            }
            else -> throw IllegalArgumentException("Cannot resolve serializer for type: $type")
        }

        // Get the Companion instance from the static "Companion" field on the class
        val companionInstance: Any? = try {
            val companionField = clazz.getDeclaredField("Companion")
            companionField.isAccessible = true
            companionField.get(null)
        } catch (e: NoSuchFieldException) {
            null
        }

        if (companionInstance != null) {
            // Strategy 1: Companion may directly implement KSerializer
            if (companionInstance is KSerializer<*>) {
                return companionInstance as KSerializer<Any>
            }

            val companionClass = companionInstance.javaClass

            // Strategy 2: Function named "serializer()" (some compiler plugin versions)
            try {
                val method = companionClass.getDeclaredMethod("serializer")
                method.isAccessible = true
                val result = method.invoke(companionInstance)
                if (result is KSerializer<*>) return result as KSerializer<Any>
            } catch (_: Exception) {}

            // Strategy 3: Property getter "getSerializer()" (val serializer property)
            try {
                val method = companionClass.getDeclaredMethod("getSerializer")
                method.isAccessible = true
                val result = method.invoke(companionInstance)
                if (result is KSerializer<*>) return result as KSerializer<Any>
            } catch (_: Exception) {}

            // Strategy 4: Backing field "serializer"
            try {
                val field = companionClass.getDeclaredField("serializer")
                field.isAccessible = true
                val result = field.get(companionInstance)
                if (result is KSerializer<*>) return result as KSerializer<Any>
            } catch (_: Exception) {}

            // Strategy 5: Walk up the class hierarchy and try all strategies again
            var currentClass: Class<*>? = companionClass.superclass
            while (currentClass != null) {
                try {
                    val method = currentClass.getDeclaredMethod("serializer")
                    method.isAccessible = true
                    val result = method.invoke(companionInstance)
                    if (result is KSerializer<*>) return result as KSerializer<Any>
                } catch (_: Exception) {}

                try {
                    val method = currentClass.getDeclaredMethod("getSerializer")
                    method.isAccessible = true
                    val result = method.invoke(companionInstance)
                    if (result is KSerializer<*>) return result as KSerializer<Any>
                } catch (_: Exception) {}

                try {
                    val field = currentClass.getDeclaredField("serializer")
                    field.isAccessible = true
                    val result = field.get(companionInstance)
                    if (result is KSerializer<*>) return result as KSerializer<Any>
                } catch (_: Exception) {}

                currentClass = currentClass.superclass
            }
        }

        Log.e("KtSerializationCF", "Could not resolve serializer for ${clazz.name} using any strategy")
        throw SerializationException(
            "Failed to resolve serializer for ${clazz.simpleName}. " +
            "Ensure the class is annotated with @Serializable."
        )
    }

    private class KotlinxSerializationResponseBodyConverter<T>(
        private val json: Json,
        private val serializer: KSerializer<T>
    ) : Converter<ResponseBody, T> {
        override fun convert(value: ResponseBody): T {
            val string = value.string()
            return try {
                json.decodeFromString(serializer, string)
            } catch (e: SerializationException) {
                throw e
            } catch (e: Exception) {
                throw SerializationException(
                    "Failed to deserialize response for ${serializer.descriptor.serialName}: ${e.message}", e
                )
            } finally {
                value.close()
            }
        }
    }

    private class KotlinxSerializationRequestBodyConverter<T>(
        private val json: Json,
        private val serializer: KSerializer<T>
    ) : Converter<T, RequestBody> {
        private val mediaType = "application/json".toMediaType()

        override fun convert(value: T): RequestBody {
            val string = json.encodeToString(serializer, value)
            return RequestBody.create(mediaType, string)
        }
    }
}
