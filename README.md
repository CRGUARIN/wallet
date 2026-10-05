# Wallet

Wallet es una app Android de finanzas personales que funciona **sin conexión y sin cuenta de usuario**: tus datos viven solo en tu teléfono.

## Funciones

- **Cuentas** de dinero (efectivo, ahorros…) con saldo, color e ícono; se pueden ordenar, archivar y excluir del saldo total.
- **Ingresos, gastos y transferencias** entre cuentas, con fecha, nota y aviso cuando una cuenta quedaría en negativo.
- **Categorías y subcategorías** con colores e íconos; vienen unas por defecto y puedes crear, editar, archivar o reasignar las tuyas.
- **Movimientos** por mes, con filtros (tipo, cuenta, categoría), búsqueda y deshacer al eliminar.
- **Reportes con gráficas**: dona por categoría con drill-down a subcategorías, evolución mensual de ingresos y gastos, y ritmo del mes frente al anterior, con filtro por período, tipo y cuenta. Vista Balance: neto, tasa de ahorro y evolución del saldo.
- **Inicio** con saldo total, resumen del mes, cuentas y últimos movimientos.
- **Backup en JSON**: exporta todos tus datos a un archivo e impórtalos después (reemplaza los datos actuales, con confirmación previa).
- **Español e inglés** (idioma por app), **tema claro, oscuro o del sistema** y color dinámico en Android 12+.
- Accesibilidad: descripciones para TalkBack, encabezados y soporte de fuentes grandes.

Moneda única: COP. El dinero se maneja como enteros en centavos, nunca como decimales.

## Tarjetas de crédito

- Crea tu tarjeta desde Cuentas con su cupo total, día de corte y día límite de pago. Verás la deuda, el cupo disponible y el próximo corte.
- Registra cada compra como un gasto en la tarjeta, con sus cuotas si la pagas a plazos (las cuotas son informativas: el gasto cuenta el día de la compra).
- Paga con una transferencia desde otra cuenta hacia la tarjeta (no es un gasto nuevo); una devolución del comercio es un ingreso en la tarjeta.
- Cuando llegue el extracto, usa "Conciliar extracto": copia el saldo del banco y la app registra la diferencia como intereses y comisiones. La app no calcula intereses, los toma de tu extracto.

## Pagos recurrentes y notificaciones

- Crea una suscripción o servicio desde Ajustes > Datos > Pagos recurrentes: nombre, monto, cuenta (puede ser una tarjeta), categoría, cada cuánto se repite y el próximo cobro.
- **Automático** lo descuenta solo en cada periodo; **Preguntarme antes** te avisa para que confirmes el valor y la fecha (útil si cambia, como los servicios) u omitas ese cobro.
- Un cobro automático con fecha de hoy se registra apenas lo guardas. Si el teléfono estuvo apagado, al abrir la app se registran los cobros pendientes con su fecha real, cada uno como un movimiento normal.
- En Ajustes > Notificaciones eliges la hora de los avisos y qué te avisa: recordatorios antes del cobro, cobros registrados, cobros por confirmar, y de cada tarjeta el corte, la fecha límite de pago y si quedó vencido.
- Los cobros se registran aunque las notificaciones estén apagadas; en Android 13+ la app pide el permiso solo cuando activas los avisos.

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

La app de depuración se instala como `com.crguarin.wallet.debug` ("Wallet Debug"), junto a la release, y no comparte datos con ella.

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
| `ui/` | Tema, navegación, componentes comunes y pantallas (`onboarding`, `home`, `reports`, `accounts`, `transactions`, `categories`, `settings`) |

Stack: Kotlin, Jetpack Compose + Material 3, Hilt, Room, DataStore, Paging 3, Navigation Compose (rutas type-safe), kotlinx.serialization y Coroutines/Flow. Más detalle en [`PLAN.md`](PLAN.md) y [`AGENTS.md`](AGENTS.md).

## Licencia

Licencia MIT; consulta el archivo [`LICENSE`](LICENSE).

---

## English summary

Wallet is an offline, account-free personal finance app for Android (8.0+): multiple accounts, income/expense/transfer tracking, categories and subcategories, credit cards (debt, available limit, installments, payments and statement reconciliation), recurring payments and subscriptions (automatic or confirm-first) with configurable notifications for them and for card dates, monthly transaction lists with filters, reports with a category donut, monthly evolution bars and month pace, balance view with net, savings rate and balance over time, JSON backup export/import, Spanish and English UI, and light/dark/dynamic themes. Build and verify with `./gradlew ktlintCheck lintDebug lintRelease testDebugUnitTest assembleDebug assembleRelease` (JDK 17, Android SDK 37). See "Release firmado" above for signing the release build with `WALLET_KEYSTORE_*` environment variables or a git-ignored `keystore.properties`. The debug build installs as `com.crguarin.wallet.debug` ("Wallet Debug") alongside the release build and does not share data with it.

### Credit cards

Create your card under Accounts with its credit limit, statement day and payment due day to see the debt, available credit and next statement. Record each purchase as an expense on the card, with installments when you pay in monthly payments (installments are informational: the expense counts on the purchase day). Pay with a transfer from another account to the card (not a new expense); a merchant refund is income on the card. When the statement arrives, use "Reconcile statement": copy the bank balance and the app records the difference as interest and fees. The app does not calculate interest, it takes it from your statement.

### Recurring payments and notifications

Create a subscription or bill under Settings > Data > Recurring payments: name, amount, account (it can be a card), category, how often it repeats and the next charge. **Automatic** deducts it on its own each period; **Ask me first** notifies you so you confirm the amount and date (useful when it changes, like utility bills) or skip that charge. If the phone was off, pending charges are recorded with their real date when you open the app, each as a normal transaction. Under Settings > Notifications you choose the daily reminder time and what to be notified about: reminders before the charge, recorded charges, charges to confirm, and per card the statement date, the payment due date and whether it is overdue. Charges are recorded even with notifications off; on Android 13+ the app asks for the permission only when you enable reminders.
