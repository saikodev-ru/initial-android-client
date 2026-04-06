package ru.saikodev.initial.data.api.converter

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
 * Kotlin compiler generates a static bridge method `serializer()` on the @Serializable class itself
 * (not on the Companion). We use that static method to obtain the KSerializer instance.
 * Also supports ParameterizedType (e.g. List<ChatDto>) by resolving the raw type.
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
     * For a @Serializable class Foo, the Kotlin compiler plugin generates:
     * - A static method `Foo.serializer()` returning KSerializer<Foo>
     * We call this static method directly on the outer class.
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

        return try {
            // The Kotlin serialization compiler plugin generates a static bridge method
            // `public static KSerializer<Foo> serializer()` on the @Serializable class itself.
            val method = clazz.getDeclaredMethod("serializer")
            method.isAccessible = true
            method.invoke(null) as KSerializer<Any>
        } catch (e: NoSuchMethodException) {
            throw SerializationException(
                "No serializer() method found for ${clazz.simpleName}. " +
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
