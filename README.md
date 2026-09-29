# Wallet

Wallet es una app Android de finanzas personales que funciona **sin conexión y sin cuenta de usuario**: tus datos viven solo en tu teléfono.

## Funciones

- **Cuentas** de dinero (efectivo, ahorros…) con saldo, color e ícono; se pueden ordenar, archivar y excluir del saldo total.
- **Ingresos, gastos y transferencias** entre cuentas, con fecha, nota y aviso cuando una cuenta quedaría en negativo.
- **Categorías y subcategorías** con colores e íconos; vienen unas por defecto y puedes crear, editar, archivar o reasignar las tuyas.
- **Movimientos** por mes, con filtros (tipo, cuenta, categoría), búsqueda y deshacer al eliminar.
- **Inicio** con saldo total, resumen del mes, cuentas y últimos movimientos.
- **Backup en JSON**: exporta todos tus datos a un archivo e impórtalos después (reemplaza los datos actuales, con confirmación previa).
- **Español e inglés** (idioma por app), **tema claro, oscuro o del sistema** y color dinámico en Android 12+.
- Accesibilidad: descripciones para TalkBack, encabezados y soporte de fuentes grandes.

Moneda única: COP. El dinero se maneja como enteros en centavos, nunca como decimales.

## Capturas

<!-- TODO: agregar capturas en docs/screenshots/ (tomadas desde un dispositivo o emulador) y enlazarlas aquí. -->

## Requisitos

- Android 8.0 (API 26) o superior para ejecutar la app.
- JDK 17 (o el JDK de Android Studio).
- Android SDK con la plataforma 37 (`platforms;android-37.0`).

## Compilar y probar

```bash
export JAVA_HOME=/opt/android-studio/jbr   # o tu JDK 17+
./gradlew ktlintCheck lintDebug lintRelease testDebugUnitTest assembleDebug assembleRelease
```

Este comando debe pasar antes de cada cambio (es el mismo que ejecuta el CI). Para instalar la versión de depuración en un dispositivo:

```bash
./gradlew installDebug
```

## Release firmado

`assembleRelease` genera un APK minificado (R8) en `app/build/outputs/apk/release/`. Sin configuración de firma se firma con la clave de depuración y Gradle muestra un aviso: sirve para probarlo en tu teléfono, **no** para publicarlo.

1. Crea una clave (una sola vez) y guárdala **fuera** del repositorio:

   ```bash
   keytool -genkeypair -v -keystore ~/wallet-release.jks -alias wallet -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Pasa la configuración por variables de entorno:

   ```bash
   export WALLET_KEYSTORE_PATH=$HOME/wallet-release.jks
   export WALLET_KEYSTORE_PASSWORD=...
   export WALLET_KEY_ALIAS=wallet
   export WALLET_KEY_PASSWORD=...
   ./gradlew assembleRelease
   ```

   O crea un archivo `keystore.properties` en la raíz (ya está en `.gitignore`):

   ```properties
   storeFile=/ruta/a/wallet-release.jks
   storePassword=...
   keyAlias=wallet
   keyPassword=...
   ```

Nunca subas al repositorio claves, contraseñas ni `keystore.properties`. Si pierdes la clave no podrás actualizar la app publicada.

## Estructura del proyecto

Un solo módulo `:app` (paquete `com.crguarin.wallet`) con arquitectura MVVM + flujo de datos unidireccional:

| Capa | Contenido |
|---|---|
| `core/` | Utilidades de dinero, tiempo y resultado de dominio |
| `data/` | Room (entidades, DAOs), repositorios, backup JSON, preferencias (DataStore), datos iniciales |
| `domain/` | Modelos, interfaces de repositorios y casos de uso (Kotlin puro, sin Android) |
| `di/` | Módulos de Hilt |
| `ui/` | Tema, navegación, componentes comunes y pantallas (`onboarding`, `home`, `accounts`, `transactions`, `categories`, `settings`) |

Stack: Kotlin, Jetpack Compose + Material 3, Hilt, Room, DataStore, Paging 3, Navigation Compose (rutas type-safe), kotlinx.serialization y Coroutines/Flow. Más detalle en [`PLAN.md`](PLAN.md) y [`AGENTS.md`](AGENTS.md).

## Licencia

Licencia MIT; consulta el archivo [`LICENSE`](LICENSE).

---

## English summary

Wallet is an offline, account-free personal finance app for Android (8.0+): multiple accounts, income/expense/transfer tracking, categories and subcategories, monthly transaction lists with filters, JSON backup export/import, Spanish and English UI, and light/dark/dynamic themes. Build and verify with `./gradlew ktlintCheck lintDebug lintRelease testDebugUnitTest assembleDebug assembleRelease` (JDK 17, Android SDK 37). See "Release firmado" above for signing the release build with `WALLET_KEYSTORE_*` environment variables or a git-ignored `keystore.properties`.
