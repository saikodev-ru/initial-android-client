package ru.saikodev.initial.data.api

import okhttp3.MultipartBody
import retrofit2.http.*
import ru.saikodev.initial.data.api.dto.*

interface InitialApi {
    // Auth
    @POST("send_code")
    suspend fun sendCode(@Body request: SendCodeRequest): SendCodeResponse

    @POST("verify_code")
    suspend fun verifyCode(@Body request: VerifyCodeRequest): VerifyCodeResponse

    @POST("qr_create")
    suspend fun qrCreate(): QrCreateResponse

    @GET("qr_poll")
    suspend fun qrPoll(@Query("token") token: String): QrPollResponse

    @POST("qr_approve")
    suspend fun qrApprove(@Body request: QrApproveRequest): QrApproveResponse

    @POST("qr_link_create")
    suspend fun qrLinkCreate(): QrLinkCreateResponse

    @POST("qr_link_consume")
    suspend fun qrLinkConsume(@Body request: QrLinkConsumeRequest): QrLinkConsumeResponse

    // Profile
    @GET("get_me")
    suspend fun getMe(): GetMeResponse

    @POST("update_profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiError

    @Multipart
    @POST("upload_avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): UploadMediaResponse

    @GET("sessions")
    suspend fun getSessions(): SessionsResponse

    // Messages
    @GET("get_messages")
    suspend fun getMessages(
        @Query("chat_id") chatId: Int,
        @Query("init") init: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("after_id") afterId: Int? = null,
        @Query("before_id") beforeId: Int? = null,
        @Query("mark_read") markRead: Int? = null,
        @Query("skip_chats") skipChats: Int? = null,
        @Query("check_ids") checkIds: String? = null
    ): GetMessagesResponse

    @POST("send_message")
    suspend fun sendMessage(@Body request: SendMessageRequest): SendMessageResponse

    @POST("edit_message")
    suspend fun editMessage(@Body request: EditMessageRequest): EditMessageResponse

    @POST("delete_message")
    suspend fun deleteMessage(@Body request: DeleteMessageRequest): DeleteMessageResponse

    // Reactions
    @POST("react_message")
    suspend fun reactMessage(@Body request: ReactMessageRequest): ReactMessageResponse

    @GET("get_reactions")
    suspend fun getReactions(@Query("ids") ids: String): GetReactionsResponse

    // Search
    @GET("search_user")
    suspend fun searchUser(@Query("q") query: String): SearchUserResponse

    // Chat management
    @POST("pin_chat")
    suspend fun pinChat(@Body request: PinChatRequest): PinChatResponse

    @POST("mute_chat")
    suspend fun muteChat(@Body request: MuteChatRequest): MuteChatResponse

    @POST("delete_chat")
    suspend fun deleteChat(@Body request: DeleteChatRequest): DeleteChatResponse

    // Media
    @Multipart
    @POST("upload_media")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): UploadMediaResponse

    @Multipart
    @POST("upload_file")
    suspend fun uploadFile(@Part file: MultipartBody.Part): UploadMediaResponse

    // Presence
    @POST("update_presence")
    suspend fun updatePresence(): UpdatePresenceResponse

    // Link preview
    @GET("link_preview")
    suspend fun getLinkPreview(@Query("url") url: String): LinkPreviewResponse
}
