# Wallet — Instrucciones para Agentes (AGENTS.md)

**LEER `PLAN.md` ANTES DE EMPEZAR CUALQUIER TAREA.**

## Resumen del proyecto
Wallet es una app Android de finanzas personales, offline y sin cuenta de usuario, que permite manejar varias cuentas de dinero, registrar ingresos, gastos y transferencias, organizar con categorías/subcategorías, ver el saldo, y exportar/importar un backup en JSON. La interfaz está en Español e Inglés.

## Stack Tecnológico
- **Lenguaje**: Kotlin 2.x (Compose compiler plugin).
- **UI**: Jetpack Compose + Material 3 (Compose BOM, soporte dynamic color, edge-to-edge).
- **Arquitectura**: MVVM + UDF (capas data / domain / ui). Un solo módulo `:app`.
- **DI**: Hilt (`hilt-navigation-compose`).
- **Persistencia**: Room con KSP (`exportSchema = true`).
- **Preferencias**: DataStore (Preferences).
- **Asincronía**: Coroutines + Flow (`StateFlow` en ViewModels, `collectAsStateWithLifecycle` en UI).
- **Navegación**: Navigation Compose con rutas type-safe (`@Serializable`).
- **Serialización**: kotlinx.serialization.
- **Fechas**: java.time (`LocalDate`, `Instant`, `ZoneId`).
- **Idiomas**: strings.xml (ES por defecto, EN en values-en) + per-app language (`locales_config.xml`).
- **Tests**: JUnit4, kotlinx-coroutines-test, Turbine, Truth, Robolectric, Compose UI Test.
- **Calidad**: ktlint (`org.jlleitschuh.gradle.ktlint`) + Android Lint.
- **Build**: Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`).
- **Configuración Android**: `applicationId` `com.crguarin.wallet`, `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`.

## Estructura de paquetes (app/src/main/java/com/crguarin/wallet/)
- `core/`: utilidades de dinero, tiempo, resultado de dominio.
- `data/`: Room db (entities, daos), repositorios (implementaciones), backup, prefs, seed.
- `domain/`: modelos, interfaces de repositorios, casos de uso (puro Kotlin, sin dependencias de Android).
- `di/`: módulos Hilt.
- `ui/`: theme, navigation, components, onboarding, home, accounts, transactions, categories, settings.

## Comandos de Build y Test
Antes de cada PR, **deben pasar** los siguientes comandos:
```bash
./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug
```

## Reglas de estilo (Convenciones de código)
- Kotlin oficial style + ktlint.
- Nombres en **inglés** en el código; textos de UI solo en `strings.xml`. **Ningún string hardcodeado en Compose**.
- Un archivo por clase pública relevante; composables de pantalla en `ui/<feature>/`.
- ViewModels: `@HiltViewModel`, sin referencias a `Context`; exponen `StateFlow<UiState>`.
- No usar `GlobalScope`; usar `viewModelScope` y dispatchers inyectados (`@IoDispatcher`).
- Commits en formato **Conventional Commits** (`feat:`, `fix:`, `test:`, `chore:`, `docs:`).
- No agregar dependencias fuera de la sección 2 del PLAN.md sin justificarlo.
- Nada de secretos, keystores ni `local.properties` en git.

## Notas de build (leer antes de tocar Gradle)
- **AGP 9** trae Kotlin integrado: **no** aplicar `org.jetbrains.kotlin.android` en `app/build.gradle.kts` (solo se declara con `apply false` en la raíz para fijar la versión de Kotlin). Usar `kotlin { compilerOptions { ... } }`, no `kotlinOptions`.
- Todas las versiones viven en `gradle/libs.versions.toml`; los plugins se aplican siempre con `alias(libs.plugins.xxx)`, nunca con `buildscript { classpath(...) }`.
- `compileSdk`/`targetSdk = 37` (paquete del SDK: `platforms;android-37.0`). **No bajarlo** para esquivar errores; si algo falla, reportarlo.
- Tests de Robolectric: anotar con `@Config(sdk = [35])` hasta que Robolectric soporte la API 37.
- Entorno: `source ./setup_sdk.sh` instala el SDK y genera `local.properties`.
- El repositorio espejo de Google (`maven-central.storage-download.googleapis.com`) está primero en `settings.gradle.kts` para evitar errores 429 de Maven Central; no quitarlo.
