# Initial — Android Client

Полноценный Android-клиент мессенджера Initial на Jetpack Compose.

## 🏗 Архитектура

**MVVM + Clean Architecture** с Hilt DI

```
ru.saikodev.initial/
├── di/                        # Hilt модули (DI)
│   ├── NetworkModule.kt       # Retrofit, OkHttp, API
│   └── RepositoryModule.kt    # Привязка репозиториев
├── data/
│   ├── api/
│   │   ├── InitialApi.kt      # Retrofit интерфейс (26 эндпоинтов)
│   │   ├── dto/               # DTO классы (kotlinx.serialization)
│   │   └── interceptor/       # Auth interceptor
│   ├── local/
│   │   └── preferences/       # TokenManager, SettingsManager
│   └── repository/            # Реализация репозиториев
├── domain/
│   ├── model/                 # Domain модели (User, Chat, Message, etc.)
│   └── repository/            # Интерфейсы репозиториев
├── ui/
│   ├── auth/                  # Экраны авторизации
│   │   ├── QrLoginScreen      # QR-код вход
│   │   ├── EmailLoginScreen   # Вход по email
│   │   ├── CodeVerificationScreen  # Верификация кода
│   │   └── ProfileSetupScreen # Создание профиля
│   ├── chatlist/              # Список чатов + поиск
│   ├── chat/                  # Чат / сообщения
│   ├── settings/              # Настройки
│   ├── theme/                 # Тема (Dark/Light/AMOLED)
│   ├── navigation/            # Навигация
│   └── components/            # Общие компоненты
└── util/                      # Утилиты (MediaUtils)
```

## 📱 Возможности

### Авторизация
- ✅ Вход по QR-коду (генерация + сканирование + поллинг)
- ✅ Вход по email (отправка кода + верификация)
- ✅ Создание профиля (имя + Signal ID)

### Чаты
- ✅ Список чатов с онлайн-статусом
- ✅ Пин чатов (drag-to-reorder)
- ✅ Индикатор «печатает…»
- ✅ Непрочитанные счётчики
- ✅ Поиск по чатам и пользователям
- ✅ Спецчаты: «Заметки» (saved messages), @initial (системный)

### Сообщения
- ✅ Отправка текста
- ✅ Ответ на сообщение (reply)
- ✅ Редактирование сообщений
- ✅ Удаление сообщений
- ✅ Реакции (эмодзи)
- ✅ Read receipts (галочки)
- ✅ Бесконечная прокрутка истории
- ✅ Группировка по дате
- ✅ Спойлеры текста

### Медиа
- ✅ Отправка изображений
- ✅ Отправка видео
- ✅ Отправка документов
- ✅ Загрузка файлов с прогрессом
- ✅ Media viewer (просмотр фото/видео)

### Настройки
- ✅ 3 темы: Тёмная, Светлая, AMOLED
- ✅ Уведомления (push, звук, анонимность)
- ✅ Настройки ввода (Enter отправляет)
- ✅ Размер шрифта
- ✅ Редактирование профиля

## 🛠 Технологический стек

| Компонент | Технология |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Навигация | Navigation Compose |
| DI | Hilt |
| Сеть | Retrofit + OkHttp + kotlinx.serialization |
| Кэш | DataStore Preferences + SharedPreferences |
| Изображения | Coil |
| Камера | CameraX + ZXing (QR) |
| Медиа | Media3 ExoPlayer |
| Асинхронность | Kotlin Coroutines + Flow |
| Мин. SDK | 26 (Android 8.0) |
| Целевой SDK | 34 (Android 14) |

## 🔌 API

Бэкенд: `https://initial.su/api/`

Авторизация: Bearer token в заголовке `Authorization`.

### Основные эндпоинты:
- `POST /send_code` — отправка кода на email
- `POST /verify_code` — верификация кода
- `POST /qr_create` — создание QR-кода для входа
- `GET /qr_poll` — поллинг статуса QR
- `GET /get_messages` — получение сообщений
- `POST /send_message` — отправка сообщения
- `POST /edit_message` — редактирование
- `POST /delete_message` — удаление
- `POST /react_message` — реакции
- `GET /search_user` — поиск пользователей
- `POST /upload_media` — загрузка медиа
- `GET /get_me` — текущий пользователь
- `POST /update_profile` — обновление профиля

## 🚀 Сборка

```bash
# Клонировать
git clone https://github.com/saikodev-ru/initial-android-client.git
cd initial-android-client

# Открыть в Android Studio и собрать
# или через командную строку:
./gradlew assembleDebug
```

## 📝 Структура данных

### User
```kotlin
data class User(
    val id: Int,
    val email: String,
    val nickname: String,
    val signalId: String?,
    val avatarUrl: String?,
    val bio: String?,
    val isVerified: Boolean,
    val isTeamSignal: Boolean
)
```

### Chat
```kotlin
data class Chat(
    val chatId: Int,
    val partnerName: String?,
    val partnerSignalId: String?,
    val partnerAvatar: String?,
    val partnerLastSeen: Long?,
    val partnerIsTyping: Boolean,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val unreadCount: Int,
    val lastMessage: String?,
    val lastTime: Long?
)
```

### Message
```kotlin
data class Message(
    val id: Int,
    val senderId: Int?,
    val body: String?,
    val sentAt: Long?,
    val isRead: Boolean,
    val isEdited: Boolean,
    val mediaUrl: String?,
    val mediaType: String?, // "image", "video", "voice", "document"
    val replyTo: Int?,
    val batchId: String?,
    val reactions: List<Reaction>
)
```

## 📄 Лицензия

MIT License

---

Разработано [saiko.dev](https://initial.su) laboratories
