# Samsung Health Clone

Android-приложение-клон Samsung Health с функцией подкрутки шагов через секретное меню.

## Особенности

- 📊 **Главный экран**: Отображение шагов за сегодня, часов ходьбы, расстояния и калорий
- 📈 **График недели**: Столбчатая диаграмма шагов за последние 7 дней
- 📅 **История**: Горизонтальный список дней с шагами
- 📊 **Статистика месяца**: Общее количество шагов и среднее за месяц
- 🔐 **Секретное меню**: Накрутка шагов и часов ходьбы (доступно 5-кратным тапом по меню "Настройки")

## Секретное меню

Для доступа к секретному меню:
1. Откройте меню (три точки в правом верхнем углу)
2. Нажмите на пункт "Настройки" 5 раз быстро (в течение секунды)
3. Откроется секретное меню с функциями:
   - Установка точного количества шагов (0-50,000)
   - Установка часов ходьбы (0-8.3ч)
   - Добавление случайных шагов (1000-5000)
   - Установка максимума (50,000 шагов)
   - Сброс на 0
   - Копирование вчерашних шагов

## Технологии

- **Kotlin** - основной язык
- **Room Database** - локальное хранение данных
- **MPAndroidChart** - графики
- **Material Components** - UI
- **Coroutines** - асинхронность
- **ViewBinding** - биндинг views
- **Foreground Service** - фоновый подсчет шагов через датчик

---

## 🚀 Сборка через GitHub Actions (рекомендуется)

### Автоматическая сборка при каждом пуше

1. **Создайте репозиторий на GitHub**
2. **Загрузите проект:**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/ВАШ_НИК/SamsungHealthClone.git
   git push -u origin main
   ```

3. **GitHub Actions автоматически:**
   - Соберет **Debug APK** при каждом пуше в `main`/`master`
   - Сохранит APK как **Artifact** (доступен 30 дней)
   - Покажет статус в вкладке **Actions**

### 📥 Скачать готовый APK

1. Зайдите в репозиторий → вкладка **Actions**
2. Выберите последний успешный запуск (зеленая галочка)
3. Внизу в **Artifacts** → скачайте `app-debug-apk.zip`
4. Распакуйте → `app-debug.apk` готов к установке

---

### 🏷 Автоматический Release при теге

Для создания релиза с подписанным APK:

```bash
# Создайте тег версии
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

GitHub Actions:
1. Соберет **Release APK** (оптимизированный, без отладочного кода)
2. Создаст **GitHub Release** с прикрепленным APK
3. Сгенерирует заметки релиза автоматически

> **Release APK** доступен на странице **Releases** репозитория.

---

### 🔐 Подписание Release APK (опционально)

Для подписанного APK (для Play Store / распространения):

1. Создайте keystore:
   ```bash
   keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Закодируйте в Base64:
   ```bash
   base64 -i release.keystore -o keystore.b64
   ```

3. В GitHub репозитории: **Settings** → **Secrets and variables** → **Actions** → **New repository secret**:
   | Name | Value |
   |------|-------|
   | `KEYSTORE_BASE64` | содержимое `keystore.b64` |
   | `KEYSTORE_PASSWORD` | пароль keystore |
   | `KEY_ALIAS` | `release` |
   | `KEY_PASSWORD` | пароль ключа |

---

## 💻 Локальная сборка

### Требования
- **Java 17+** (Temurin/OpenJDK)
- **Android SDK** (API 34, Build Tools 34.0.0)

### Windows
```cmd
gradlew.bat assembleDebug        # Debug APK
gradlew.bat assembleRelease      # Release APK (неподписанный)
```

### Linux / macOS
```bash
chmod +x gradlew
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
```

### Результат
| Тип | Путь |
|-----|------|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `app/build/outputs/apk/release/app-release.apk` |

---

## 📱 Установка на телефон

1. **Передайте APK на телефон** (Telegram, кабель, облако)
2. **Включите "Неизвестные источники":**
   - Настройки → Безопасность → Установка неизвестных приложений
   - Разрешите для файлового менеджера / браузера
3. **Откройте APK** → Установить

---

## 🛠 Структура проекта

```
SamsungHealthClone/
├── .github/workflows/
│   ├── build.yml      # Debug сборка при пуше
│   └── release.yml    # Release сборка при теге
├── app/
│   ├── src/main/java/com/samsunghealthclone/
│   │   ├── data/      # Room: StepEntry, StepDao, Repository
│   │   ├── ui/        # ViewModels, RecyclerView Adapter
│   │   ├── MainActivity.kt
│   │   ├── SecretMenuActivity.kt
│   │   ├── StepCounterService.kt
│   │   ├── BootReceiver.kt
│   │   └── HealthApplication.kt
│   ├── src/main/res/  # Layouts, Colors, Themes, Drawables
│   └── build.gradle.kts
├── gradle/wrapper/
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── README.md
```

---

## ⚙️ Требования к телефону
- Android 7.0 (API 24)+
- Разрешения при первом запуске:
  - **Активность тела** (ACTIVITY_RECOGNITION)
  - **Датчики тела** (BODY_SENSORS)

---

## 📄 Лицензия
MIT License — используйте свободно для обучения и личных проектов.