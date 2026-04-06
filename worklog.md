---
## Task ID: fix/chatlist-avatar-qr - main-agent
### Work Task
Fix 3 critical issues: (1) Chat list avatars not loading due to raw media keys not converted to full URLs, (2) User avatars from getMe()/verifyCode() not resolved, (3) Replace QR code generation with QR scanner button on login screen.

### Work Summary

**Files Modified (5):**

1. **ChatRepositoryImpl.kt** — Added `resolveMediaUrl()` helper; updated `ChatDto.toDomain()` and `UserDto.toDomain()` to accept `token` param and resolve avatar URLs via `MediaUtils.getMediaUrl()`; `loadChats()` now fetches token first and passes it; `getMessages()` chat list conversion passes token; added `Log.e` in catch block.

2. **ProfileRepositoryImpl.kt** — Added `resolveMediaUrl()` helper; updated `UserDto.toDomain()` to accept token and resolve `avatar_url`; `getMe()` fetches token and passes it to `toDomain(token)`.

3. **AuthRepositoryImpl.kt** — Added `MediaUtils` import and `resolveMediaUrl()` helper; updated `UserDto.toDomain()` to accept token; `verifyCode()` passes `res.token`, `createProfile()` passes saved token, `getSavedUser()` passes saved token, `consumeQrLink()` passes `res.auth_token ?: savedToken`.

4. **ChatListViewModel.kt** — Added `android.util.Log` import; `loadChats()` now logs success count and failure errors; `startPolling()` only updates `_chats` when result is non-empty to avoid clearing data on errors.

5. **QrLoginScreen.kt** — Complete rewrite: removed QR code generation, polling, `LaunchedEffect`, `QrCodeUtils` dependency; replaced with clean login screen showing "Initial." logo + "Безопасный мессенджер" subtitle + primary "Сканировать QR-код" Button + outlined "Войти по Email" OutlinedButton.

**Committed and pushed to `fix/chatlist-avatar-qr` branch.**
