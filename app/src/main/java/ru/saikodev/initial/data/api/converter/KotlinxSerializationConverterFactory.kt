package ru.saikodev.initial.data.api.converter

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * A Retrofit converter factory that uses kotlinx.serialization for JSON serialization/deserialization.
 *
 * Uses pure Java reflection (Class.forName) to resolve serializers from @Serializable companion objects.
 * This approach works on all Kotlin versions (1.x and 2.0+) without any deprecated KClass APIs.
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
     * Uses Class.forName to find the Companion class, then invokes its serializer() method.
     */
    @Suppress("UNCHECKED_CAST")
    private fun resolveSerializer(type: Type): KSerializer<Any> {
        val clazz = type as? Class<*>
            ?: throw IllegalArgumentException("Cannot resolve serializer for type: $type, must be a Class")
        try {
            val companionClass = Class.forName("${clazz.name}\$Companion")
            val method = companionClass.getDeclaredMethod("serializer")
            method.isAccessible = true
            return method.invoke(null) as KSerializer<Any>
        } catch (e: ClassNotFoundException) {
            throw SerializationException(
                "No companion object found for ${clazz.simpleName}. " +
                "Ensure the class is annotated with @Serializable."
            )
        } catch (e: SerializationException) {
            throw e
        } catch (e: Exception) {
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
