package retrofit2.converter.kotlinx.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * A Retrofit converter factory that uses kotlinx.serialization for JSON serialization/deserialization.
 */
class KotlinxSerializationConverterFactory(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val serializer = json.serializersModule.serializer(type)
        return KotlinxSerializationResponseBodyConverter(json, serializer)
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        val serializer = json.serializersModule.serializer(type)
        return KotlinxSerializationRequestBodyConverter(json, serializer)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private class KotlinxSerializationResponseBodyConverter<T>(
        private val json: Json,
        private val serializer: KSerializer<T>
    ) : Converter<ResponseBody, T> {
        override fun convert(value: ResponseBody): T {
            return try {
                json.decodeFromString(serializer, value.string())
            } catch (e: SerializationException) {
                throw e
            } catch (e: Exception) {
                throw SerializationException("Failed to deserialize response body", e)
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
