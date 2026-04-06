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
 * A Retrofit converter factory that uses kotlinx.serialization for JSON serialization/deserialization.
 *
 * For a @Serializable class Foo, the Kotlin compiler plugin generates:
 *   - A class Foo with a static field `Companion` holding the Companion instance
 *   - The Companion instance has a public method `serializer()` returning KSerializer<Foo>
 *
 * We obtain the Companion instance via reflection, then call serializer() on it.
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

    /**
     * Resolve the kotlinx.serialization KSerializer for a given java.lang.reflect.Type.
     *
     * Kotlin compiler generates:
     * 1. A static field `Companion` on the @Serializable class (holds the companion object instance)
     * 2. An instance method `serializer()` on the companion object (returns the KSerializer)
     *
     * We get the companion instance from the static field, then call serializer() on it.
     */
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

        try {
            // Step 1: Get the Companion instance from the static field on the outer class.
            // Kotlin stores companion objects as a static field named "Companion".
            val companionField = clazz.getDeclaredField("Companion")
            companionField.isAccessible = true
            val companionInstance = companionField.get(null) // null is OK — Companion is a static field

            // Step 2: Call serializer() on the Companion instance.
            // The kotlinx.serialization compiler plugin generates this instance method.
            val serializerMethod = companionInstance.javaClass.getDeclaredMethod("serializer")
            serializerMethod.isAccessible = true
            return serializerMethod.invoke(companionInstance) as KSerializer<Any>
        } catch (e: NoSuchFieldException) {
            Log.e("KtSerializationCF", "No Companion field on ${clazz.name}", e)
            throw SerializationException(
                "No companion object found for ${clazz.simpleName}. " +
                "Ensure the class is annotated with @Serializable."
            )
        } catch (e: NoSuchMethodException) {
            Log.e("KtSerializationCF", "No serializer() method on Companion of ${clazz.name}", e)
            throw SerializationException(
                "No serializer() method found on companion of ${clazz.simpleName}. " +
                "Ensure the class is annotated with @Serializable."
            )
        } catch (e: SerializationException) {
            throw e
        } catch (e: Exception) {
            Log.e("KtSerializationCF", "Failed to resolve serializer for ${clazz.name}: ${e.message}", e)
            throw SerializationException(
                "Failed to resolve serializer for ${clazz.simpleName}: ${e.message}", e
            )
        }
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
                    "Failed to deserialize response body for ${serializer.descriptor.serialName}: ${e.message}", e
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
