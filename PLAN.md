# Wallet — Plan de desarrollo (Android)

> Documento maestro para que **Jules** implemente la app paso a paso.
> Cada tarea de la sección 12 está pensada para ser **un PR independiente**, con criterios de aceptación verificables.
> Si algo en este documento es ambiguo, elegir la opción más simple que cumpla los criterios y dejarlo anotado en la descripción del PR.

---

## 1. Visión del producto

**Wallet** es una app Android de finanzas personales, **offline y sin cuenta de usuario**, que permite:

1. Manejar **varias cuentas** de dinero. En la v1 solo hay dos tipos: **Efectivo** y **Cuenta de ahorros**.
2. Registrar **ingresos** y **gastos** en cada cuenta.
3. Organizar ingresos y gastos con **categorías** y **subcategorías** creadas por el usuario (con un set inicial por defecto).
4. Registrar **transferencias** entre cuentas propias (no cuentan como ingreso ni gasto).
5. Ver el **saldo** de cada cuenta y el **saldo total**.
6. **Exportar / importar** un backup en JSON.
7. Interfaz en **Español** e **Inglés**.

### 1.1 Fuera de alcance en la v1 (no implementar)
- Login, sincronización en la nube, multi-dispositivo.
- Multi-moneda (pero el modelo guarda el código de moneda para poder agregarla después).
- Tarjetas de crédito, préstamos, inversiones.
- Presupuestos, metas de ahorro, transacciones recurrentes.
- Lectura de SMS/notificaciones bancarias, OCR de recibos.
- Widgets de pantalla de inicio.

(Ver sección 14 "Roadmap futuro".)

---

## 2. Stack tecnológico

| Área | Elección | Notas |
|---|---|---|
| Lenguaje | **Kotlin 2.x** (última estable) | Compose compiler plugin de Kotlin (`org.jetbrains.kotlin.plugin.compose`). |
| UI | **Jetpack Compose + Material 3** | Usar Compose BOM. Material Icons Extended. Soporte dynamic color (Android 12+) con fallback a paleta propia. |
| Arquitectura | **MVVM + UDF** (Unidirectional Data Flow) con capas `data` / `domain` / `ui` | Un solo módulo `:app` organizado por paquetes (más simple para un agente). |
| DI | **Hilt** | `hilt-navigation-compose` para `hiltViewModel()`. |
| Persistencia | **Room** (con KSP) | `exportSchema = true`, esquemas en `app/schemas/`. |
| Preferencias | **DataStore (Preferences)** | Tema, onboarding completado, etc. |
| Asincronía | **Coroutines + Flow** | `StateFlow` en ViewModels; `collectAsStateWithLifecycle()` en Compose. |
| Navegación | **Navigation Compose** con rutas type-safe (`@Serializable`) | |
| Serialización | **kotlinx.serialization** | Rutas de navegación y backup JSON. |
| Fechas | **java.time** (`LocalDate`, `Instant`, `ZoneId`) | minSdk 26 lo soporta sin desugaring. |
| Idiomas | `strings.xml` (ES por defecto, EN en `values-en`) + **per-app language** (`AppCompatDelegate.setApplicationLocales`, `locales_config.xml`) | |
| Tests | JUnit4, **kotlinx-coroutines-test**, **Turbine**, **Truth**, **Robolectric** (DAO y UI sin emulador), Compose UI Test | |
| Calidad | **ktlint** (plugin `org.jlleitschuh.gradle.ktlint`) + Android Lint | |
| Build | Gradle Kotlin DSL + **Version Catalog** (`gradle/libs.versions.toml`) | AGP última estable, JDK 17. |
| CI | GitHub Actions | build + tests + lint en cada PR. |

**Configuración Android**
- `applicationId`: `com.crguarin.wallet`
- `namespace`: `com.crguarin.wallet`
- `minSdk = 26`, `targetSdk = 37` y `compileSdk = 37` (**Android 17**, versión estable más reciente, lanzada el 16 de junio de 2026).
- La UI debe funcionar bien en vertical (portrait); horizontal es "best effort".
- Edge-to-edge activado (`enableEdgeToEdge()`).

---

## 3. Estructura del proyecto

```
Wallet/
├── AGENTS.md                      # Instrucciones para agentes (Tarea 0)
├── PLAN.md                        # Este documento
├── README.md
├── .github/workflows/ci.yml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
└── app/
    ├── build.gradle.kts
    ├── schemas/                   # Esquemas exportados de Room (versionados en git)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/
        │   │   ├── values/strings.xml        # Español (idioma por defecto)
        │   │   ├── values-en/strings.xml     # Inglés
        │   │   └── xml/locales_config.xml
        │   └── java/com/crguarin/wallet/
        │       ├── WalletApp.kt              # @HiltAndroidApp
        │       ├── MainActivity.kt
        │       ├── core/
        │       │   ├── money/                # Money, MoneyFormatter, MoneyParser
        │       │   ├── time/                 # Clock abstraction, DateUtils
        │       │   └── result/               # DomainResult / errores de dominio
        │       ├── data/
        │       │   ├── db/                   # WalletDatabase, entities, daos, converters, migrations
        │       │   ├── repository/           # Implementaciones de repositorios
        │       │   ├── prefs/                # UserPreferencesRepository (DataStore)
        │       │   ├── backup/               # BackupDto, BackupSerializer, BackupRepository
        │       │   └── seed/                 # DefaultCategoriesSeeder
        │       ├── domain/
        │       │   ├── model/                # Account, Category, Transaction, enums
        │       │   ├── repository/           # Interfaces
        │       │   └── usecase/              # Casos de uso
        │       ├── di/                       # Módulos Hilt
        │       └── ui/
        │           ├── theme/
        │           ├── navigation/           # WalletNavHost, rutas
        │           ├── components/           # Composables reutilizables
        │           ├── onboarding/
        │           ├── home/
        │           ├── accounts/             # list, detail, edit
        │           ├── transactions/         # list, edit
        │           ├── categories/           # list, edit
        │           └── settings/
        ├── test/                             # Unit + Robolectric
        └── androidTest/                      # (opcional) instrumentados
```

Reglas:
- `domain` **no** depende de Android ni de Room (Kotlin puro).
- `ui` solo habla con `domain` (casos de uso / interfaces de repositorio), nunca con DAOs.
- Entities de Room ≠ modelos de dominio: mapear con funciones `toDomain()` / `toEntity()`.

---

## 4. Manejo de dinero (crítico)

- **Nunca usar `Double`/`Float` para dinero.**
- Montos se guardan como `Long` en **unidades menores** (centavos). COP tiene 2 decimales ISO (`Currency.getInstance("COP").defaultFractionDigits == 2`), así que `$15.000` se guarda como `1_500_000`.
- `value class Money(val minorUnits: Long)` en `core/money` con operaciones `plus`, `minus`, `unaryMinus`, `isZero`, `isNegative`, `compareTo`.
- **Formateo** (`MoneyFormatter`): usar `NumberFormat.getCurrencyInstance(locale)` con `currency = COP`.
  - Para COP mostrar **0 decimales** por defecto (práctica local), p. ej. `$ 15.000`. Si el valor tiene centavos distintos de 0, mostrar 2 decimales.
  - Separadores según locale del idioma activo (es-CO: `.` miles, `,` decimales; en: `,` miles, `.` decimales).
- **Parseo** (`MoneyParser`): convierte el texto del campo de monto a `Long`.
  - Aceptar solo dígitos y un separador decimal (el del locale), máximo 2 decimales.
  - Rechazar vacío, cero, negativos (el signo lo determina el tipo de transacción).
  - Límite máximo: `999_999_999_999_99` minor units (evitar overflow).
- Campo de entrada de monto: `KeyboardType.Decimal`, formateo visual de miles mientras se escribe (`VisualTransformation`).
- La moneda de la app es una constante `AppCurrency = "COP"` en v1, pero **cada cuenta guarda `currencyCode`** para el futuro.

Tests obligatorios: formateo ES/EN, parseo con/sin decimales, separadores, valores límite, entradas inválidas.

---

## 5. Modelo de dominio

### 5.1 Enums
```kotlin
enum class AccountType { CASH, SAVINGS }           // v1. Futuro: CHECKING, CREDIT_CARD, ...
enum class CategoryType { INCOME, EXPENSE }
enum class TransactionType { INCOME, EXPENSE, TRANSFER }
```

### 5.2 Account
| Campo | Tipo | Regla |
|---|---|---|
| id | Long | autogenerado |
| name | String | obligatorio, 1–40 chars, trim, único entre cuentas no archivadas (case-insensitive) |
| type | AccountType | obligatorio |
| currencyCode | String | "COP" en v1 |
| initialBalance | Money | **≥ 0** en v1 |
| color | Int (ARGB) | de una paleta predefinida |
| icon | String | clave de un set de íconos predefinido (ver 5.6) |
| includeInTotal | Boolean | default `true` |
| isArchived | Boolean | default `false` |
| sortOrder | Int | para ordenar manualmente |
| createdAt | Instant | |

**Saldo actual** (no se guarda, se calcula):
```
saldo = initialBalance
      + Σ INCOME   donde accountId = cuenta
      − Σ EXPENSE  donde accountId = cuenta
      − Σ TRANSFER donde accountId = cuenta        (sale)
      + Σ TRANSFER donde toAccountId = cuenta      (entra)
```
**Saldo total** = suma de saldos de cuentas con `includeInTotal = true` y `isArchived = false`.

### 5.3 Category
| Campo | Tipo | Regla |
|---|---|---|
| id | Long | |
| name | String? | nombre escrito por el usuario (null si es categoría del sistema sin renombrar) |
| systemKey | String? | clave para categorías sembradas (p. ej. `"food"`); si no es null y `name` es null se muestra el string traducido |
| type | CategoryType | INCOME o EXPENSE |
| parentId | Long? | null = categoría raíz; no-null = subcategoría |
| color | Int | subcategorías heredan el color del padre por defecto |
| icon | String | |
| isArchived | Boolean | |
| sortOrder | Int | |

Reglas:
- **Máximo 2 niveles**: una subcategoría no puede tener hijos (validar en caso de uso).
- Una subcategoría tiene siempre el **mismo `type`** que su padre.
- Nombre único entre hermanos (mismo padre y tipo), case-insensitive.
- Nombre mostrado = `name ?: stringResource(systemKey)` → así las categorías por defecto se traducen al cambiar de idioma. Al renombrar una categoría del sistema se llena `name` (y deja de traducirse).
- Una transacción puede apuntar a una categoría raíz **o** a una subcategoría.

### 5.4 Transaction
| Campo | Tipo | Regla |
|---|---|---|
| id | Long | |
| type | TransactionType | |
| amount | Money | **> 0** siempre; el signo lo da el tipo |
| accountId | Long | cuenta origen (para INCOME/EXPENSE es "la cuenta") |
| toAccountId | Long? | **solo TRANSFER**; obligatorio y ≠ accountId |
| categoryId | Long? | **obligatorio para INCOME/EXPENSE**, null para TRANSFER; su `type` debe coincidir (INCOME↔INCOME, EXPENSE↔EXPENSE) |
| date | LocalDate | fecha de la transacción (default: hoy) |
| note | String? | máx. 200 chars |
| createdAt | Instant | |
| updatedAt | Instant | |

Decisión de diseño: **una transferencia es una sola fila** con `accountId` (origen) y `toAccountId` (destino). Esto garantiza consistencia (no puede existir "media transferencia") y simplifica edición/borrado.

### 5.5 Reglas de negocio generales
- Se **permite saldo negativo** (no bloquear), pero al guardar un gasto/transferencia que deja la cuenta en negativo mostrar un aviso no bloqueante ("La cuenta quedará en negativo").
- Cuentas y categorías **archivadas** no aparecen en selectores para nuevas transacciones, pero sus transacciones históricas se siguen mostrando.
- **Eliminar cuenta**: solo si no tiene transacciones (ni como origen ni destino). Si tiene, ofrecer **Archivar**.
- **Eliminar categoría**:
  - Si no tiene transacciones ni subcategorías → eliminar.
  - Si tiene subcategorías → no permitir hasta que se eliminen/muevan (o archivar).
  - Si tiene transacciones → diálogo con dos opciones: **Archivar** o **Reasignar transacciones a otra categoría del mismo tipo y luego eliminar**.
- No se pueden crear transacciones con fecha posterior a hoy + 1 año (validación suave).
- Debe existir al menos una cuenta para registrar transacciones; transferencias requieren al menos 2 cuentas activas.

### 5.6 Íconos y colores
- Paleta de ~12 colores fija (definida en `ui/theme/WalletPalette.kt`).
- Set de íconos: mapa `String -> ImageVector` en `ui/components/IconCatalog.kt` usando Material Icons Extended (p. ej. `"wallet"`, `"savings"`, `"restaurant"`, `"directions_bus"`, `"home"`, `"bolt"`, `"medical"`, `"school"`, `"movie"`, `"shopping_cart"`, `"work"`, `"card_giftcard"`, `"trending_up"`, `"more_horiz"`, …, ~40 íconos). Se guarda la **clave** en la BD, nunca el recurso.

---

## 6. Base de datos (Room)

`WalletDatabase` versión 1, `exportSchema = true`.

### 6.1 Tablas
```sql
accounts(
  id INTEGER PK AUTOINCREMENT,
  name TEXT NOT NULL,
  type TEXT NOT NULL,               -- 'CASH' | 'SAVINGS'
  currency_code TEXT NOT NULL,
  initial_balance INTEGER NOT NULL, -- minor units
  color INTEGER NOT NULL,
  icon TEXT NOT NULL,
  include_in_total INTEGER NOT NULL DEFAULT 1,
  is_archived INTEGER NOT NULL DEFAULT 0,
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL       -- epoch millis
)

categories(
  id INTEGER PK AUTOINCREMENT,
  name TEXT,
  system_key TEXT,
  type TEXT NOT NULL,               -- 'INCOME' | 'EXPENSE'
  parent_id INTEGER REFERENCES categories(id) ON DELETE RESTRICT,
  color INTEGER NOT NULL,
  icon TEXT NOT NULL,
  is_archived INTEGER NOT NULL DEFAULT 0,
  sort_order INTEGER NOT NULL DEFAULT 0
)
INDEX(parent_id), INDEX(type)

transactions(
  id INTEGER PK AUTOINCREMENT,
  type TEXT NOT NULL,               -- 'INCOME' | 'EXPENSE' | 'TRANSFER'
  amount INTEGER NOT NULL,          -- > 0
  account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
  to_account_id INTEGER REFERENCES accounts(id) ON DELETE RESTRICT,
  category_id INTEGER REFERENCES categories(id) ON DELETE RESTRICT,
  date INTEGER NOT NULL,            -- LocalDate.toEpochDay()
  note TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
)
INDEX(account_id), INDEX(to_account_id), INDEX(category_id), INDEX(date)
```
- `TypeConverters` para enums (por `name`), `LocalDate` (epochDay) e `Instant` (epochMillis).
- Foreign keys **RESTRICT**: la lógica de borrado vive en los casos de uso; la BD es la última barrera.

### 6.2 DAOs (consultas clave)
- `AccountDao`
  - `observeAccountsWithBalance(): Flow<List<AccountWithBalance>>` — una sola query con subconsultas:
    ```sql
    SELECT a.*,
      a.initial_balance
      + COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'INCOME'), 0)
      - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'EXPENSE'), 0)
      - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'TRANSFER'), 0)
      + COALESCE((SELECT SUM(amount) FROM transactions WHERE to_account_id = a.id AND type = 'TRANSFER'), 0)
      AS balance
    FROM accounts a
    ORDER BY a.is_archived, a.sort_order, a.name
    ```
  - `observeById(id)`, `insert`, `update`, `delete`, `countTransactions(accountId)`, `existsActiveWithName(name, excludeId)`.
- `CategoryDao`
  - `observeAll(type: CategoryType?)`, `observeById`, `insert`, `update`, `delete`, `countChildren(id)`, `countTransactions(id)`, `reassignTransactions(fromId, toId)`, `existsSiblingWithName(...)`.
- `TransactionDao`
  - `observeTransactions(filter)`: devuelve `TransactionWithDetails` (join con nombre/color/ícono de cuenta, cuenta destino, categoría y categoría padre) ordenado por `date DESC, created_at DESC`. Filtros: rango de fechas, `accountId` (coincide origen **o** destino), `categoryId` (incluye subcategorías si es raíz), `type`, texto en `note`.
  - Paginación: usar **Paging 3** (`PagingSource` de Room) en la lista de movimientos.
  - `observeRecent(limit)`, `observeById`, `insert`, `update`, `delete`.
  - `observeMonthSummary(from, to)`: total ingresos y total gastos del período (excluye transferencias).

### 6.3 Migraciones
- v1 es la base. Toda migración futura debe tener `Migration` explícita **y** test con `MigrationTestHelper`. **Prohibido** `fallbackToDestructiveMigration()` en release.

### 6.4 Seed inicial
En el primer arranque (`RoomDatabase.Callback.onCreate` o, mejor, un `DefaultCategoriesSeeder` invocado por el onboarding) se crean las categorías por defecto con `systemKey`:

**Gastos**
| systemKey | ES | EN | Subcategorías (systemKey: ES / EN) |
|---|---|---|---|
| food | Alimentación | Food | groceries: Mercado / Groceries · restaurants: Restaurantes / Restaurants |
| transport | Transporte | Transport | public_transport: Transporte público / Public transport · fuel: Gasolina / Fuel · taxi: Taxi / Taxi |
| housing | Vivienda | Housing | rent: Arriendo / Rent · utilities: Servicios públicos / Utilities |
| health | Salud | Health | — |
| education | Educación | Education | — |
| entertainment | Entretenimiento | Entertainment | — |
| shopping | Compras | Shopping | — |
| other_expense | Otros gastos | Other expenses | — |

**Ingresos**
| systemKey | ES | EN |
|---|---|---|
| salary | Salario | Salary |
| freelance | Trabajo independiente | Freelance |
| gifts | Regalos | Gifts |
| interest | Intereses | Interest |
| other_income | Otros ingresos | Other income |

Los nombres se resuelven con `stringResource(R.string.category_<systemKey>)`. El seeder debe ser **idempotente** (no duplicar si ya existen).

---

## 7. Capa de dominio

### 7.1 Interfaces de repositorio (`domain/repository`)
`AccountRepository`, `CategoryRepository`, `TransactionRepository`, `PreferencesRepository`, `BackupRepository`.

### 7.2 Casos de uso (`domain/usecase`)
Cada uno es una clase con `operator fun invoke(...)`, inyectada con Hilt, y devuelve `DomainResult<T>` (sealed: `Success(value)` / `Failure(error: DomainError)`) para las operaciones de escritura.

| Caso de uso | Responsabilidad / validaciones |
|---|---|
| `ObserveAccountsUseCase` | Flow de cuentas con saldo; parámetro `includeArchived`. |
| `ObserveTotalBalanceUseCase` | Suma saldos de cuentas activas con `includeInTotal`. |
| `SaveAccountUseCase` | Crear/editar. Valida nombre (vacío, largo, duplicado), saldo inicial ≥ 0. |
| `ArchiveAccountUseCase` / `UnarchiveAccountUseCase` | |
| `DeleteAccountUseCase` | Falla con `AccountHasTransactions` si tiene movimientos. |
| `ReorderAccountsUseCase` | Actualiza `sortOrder`. |
| `ObserveCategoryTreeUseCase` | Devuelve `List<CategoryNode(category, children)>` por tipo; parámetro `includeArchived`. |
| `SaveCategoryUseCase` | Valida nombre, unicidad entre hermanos, máx. 2 niveles, tipo del hijo = tipo del padre, no cambiar tipo si tiene transacciones. |
| `DeleteCategoryUseCase` | Ver reglas 5.5. Devuelve `HasChildren` / `HasTransactions(count)`. |
| `ReassignAndDeleteCategoryUseCase` | En una transacción de BD: mueve movimientos a `targetId` (mismo tipo) y borra. |
| `ArchiveCategoryUseCase` | Archiva categoría y sus subcategorías. |
| `SaveTransactionUseCase` | Crear/editar INCOME/EXPENSE/TRANSFER con todas las reglas de 5.4. Devuelve además `willBeNegative: Boolean` para el aviso. |
| `DeleteTransactionUseCase` | |
| `ObserveTransactionsUseCase` | Paging con filtros. |
| `ObserveMonthSummaryUseCase` | Ingresos, gastos y neto del mes seleccionado. |
| `ExportBackupUseCase` / `ImportBackupUseCase` | Ver sección 9. |
| `SeedDefaultCategoriesUseCase` | Idempotente. |

`DomainError` (sealed): `NameEmpty`, `NameTooLong`, `NameDuplicated`, `AmountInvalid`, `SameAccountTransfer`, `CategoryRequired`, `CategoryTypeMismatch`, `MaxDepthExceeded`, `AccountHasTransactions`, `CategoryHasChildren`, `CategoryHasTransactions(count)`, `NotFound`, `BackupInvalid(reason)`, `BackupVersionUnsupported`. La UI mapea cada error a un string traducido.

Inyectar un `Clock`/`TimeProvider` para poder testear fechas.

---

## 8. UI y navegación

### 8.1 Principios
- Material 3, soporte **modo claro/oscuro/sistema**, dynamic color opcional (switch en ajustes).
- Cada pantalla = `XxxScreen` (stateless, recibe `UiState` + lambdas) + `XxxRoute` (conecta con `hiltViewModel()`). Esto permite previews y tests.
- `UiState` inmutable (`data class`) expuesto como `StateFlow`; eventos de un solo uso (snackbar, navegar atrás) vía `Channel`/`SharedFlow`.
- Todo texto visible en `strings.xml` (ES/EN). **Ningún string hardcodeado** en Compose.
- Montos: ingresos en verde, gastos en rojo, transferencias en color neutro, con signo (`+ $ 50.000`, `− $ 20.000`, `$ 100.000` con ícono ⇄).
- Accesibilidad: `contentDescription` en íconos, targets ≥ 48dp, textos escalables.
- `@Preview` (claro y oscuro) para cada pantalla principal.

### 8.2 Navegación
Barra inferior con 4 destinos: **Inicio**, **Movimientos**, **Cuentas**, **Ajustes**. FAB "+" en Inicio y Movimientos para nueva transacción.

Rutas (`@Serializable`):
```
Onboarding
Home
Transactions(accountId: Long? = null, categoryId: Long? = null)
TransactionEdit(transactionId: Long? = null, initialType: TransactionType = EXPENSE, accountId: Long? = null)
Accounts
AccountDetail(accountId: Long)
AccountEdit(accountId: Long? = null)
Categories(type: CategoryType = EXPENSE)
CategoryEdit(categoryId: Long? = null, parentId: Long? = null, type: CategoryType)
CategoryPicker(type: CategoryType)       // bottom sheet o pantalla
Settings
```

### 8.3 Pantallas

**Onboarding** (solo si no hay cuentas / flag en DataStore)
1. Bienvenida + selector de idioma (ES/EN).
2. Crear primera cuenta: nombre (sugerido "Efectivo"), tipo, saldo inicial.
3. Al terminar: sembrar categorías por defecto, marcar onboarding completo, ir a Home.

**Home (Inicio)**
- Tarjeta de **Saldo total**.
- Resumen del **mes actual**: ingresos, gastos, neto.
- Carrusel/lista horizontal de **cuentas** activas con saldo (tap → AccountDetail).
- **Últimos 10 movimientos** (tap → editar) + botón "Ver todos".
- FAB: nueva transacción. Acciones rápidas: "Gasto", "Ingreso", "Transferencia".
- Estado vacío amigable si no hay movimientos.

**Movimientos (lista)**
- Lista paginada agrupada por **día** (encabezado con fecha y total neto del día).
- Selector de **mes** (← Septiembre 2026 →) y chips de filtro: tipo, cuenta, categoría; búsqueda por nota.
- Cada ítem: ícono/color de categoría (o ⇄ para transferencias), nombre de categoría ("Alimentación › Mercado"), cuenta ("Efectivo" o "Efectivo → Ahorros"), nota, monto con color/signo.
- Swipe para eliminar con **Deshacer** en snackbar.

**Nueva / editar transacción**
- Pestañas segmentadas: **Gasto | Ingreso | Transferencia**.
- Campo **monto** grande y enfocado al abrir.
- Gasto/Ingreso: selector de **cuenta**, selector de **categoría** (bottom sheet con árbol: raíces expandibles para ver subcategorías; tap selecciona; opción "+ Nueva categoría" inline).
- Transferencia: **Desde** y **Hacia** (no permite la misma; botón para intercambiar).
- **Fecha** (DatePicker M3; default hoy; atajos "Hoy", "Ayer").
- **Nota** opcional.
- Guardar deshabilitado hasta que el formulario sea válido; errores inline traducidos.
- Si queda saldo negativo: diálogo de confirmación no bloqueante.
- En modo edición: botón eliminar con confirmación. Cambiar de pestaña conserva monto, fecha y nota.
- Recordar la última cuenta usada (DataStore) como default.

**Cuentas (lista)**
- Saldo total arriba. Lista de cuentas activas (ícono, nombre, tipo, saldo). Sección colapsable "Archivadas".
- Reordenar (drag & drop o botones ↑↓ en modo edición).
- FAB: nueva cuenta.

**Detalle de cuenta**
- Encabezado: nombre, tipo, saldo actual, ingresos/gastos del mes en esa cuenta.
- Lista de movimientos de la cuenta (incluye transferencias entrantes y salientes).
- Menú: Editar, Archivar/Desarchivar, Eliminar (si aplica, con diálogos de 5.5), "Transferir desde esta cuenta".

**Crear / editar cuenta**
- Nombre, tipo (Efectivo / Ahorros con íconos), saldo inicial, color, ícono, "Incluir en saldo total".
- Al editar el saldo inicial avisar que afecta el saldo actual.

**Categorías**
- Tabs **Gastos | Ingresos**. Árbol: categoría raíz con sus subcategorías indentadas.
- Tap → editar. Botón "+ Subcategoría" en cada raíz. FAB: nueva categoría raíz.
- Sección "Archivadas". Eliminación con los diálogos de 5.5 (incluye selector de categoría destino para reasignar).

**Crear / editar categoría**
- Nombre, tipo (bloqueado si es subcategoría o si tiene movimientos), categoría padre opcional (solo raíces del mismo tipo; deshabilitado si la categoría tiene hijos), color, ícono.

**Ajustes**
- Idioma: Sistema / Español / English (per-app language).
- Tema: Sistema / Claro / Oscuro; Dynamic color on/off.
- Categorías → pantalla Categorías.
- Backup: **Exportar** / **Importar**.
- Acerca de: versión de la app.

---

## 9. Backup (exportar / importar)

- Formato JSON con kotlinx.serialization:
```json
{
  "schemaVersion": 1,
  "appVersion": "1.0.0",
  "exportedAt": "2026-09-28T15:00:00Z",
  "currency": "COP",
  "accounts": [ { "id": 1, "name": "Efectivo", "type": "CASH", "currencyCode": "COP", "initialBalance": 5000000, "color": -16711936, "icon": "wallet", "includeInTotal": true, "isArchived": false, "sortOrder": 0, "createdAt": 1790000000000 } ],
  "categories": [ { "id": 1, "name": null, "systemKey": "food", "type": "EXPENSE", "parentId": null, "color": -65536, "icon": "restaurant", "isArchived": false, "sortOrder": 0 } ],
  "transactions": [ { "id": 1, "type": "EXPENSE", "amount": 1500000, "accountId": 1, "toAccountId": null, "categoryId": 1, "date": "2026-09-27", "note": "Almuerzo", "createdAt": 1790000000000, "updatedAt": 1790000000000 } ]
}
```
- **Exportar**: Storage Access Framework `ActivityResultContracts.CreateDocument("application/json")`, nombre sugerido `wallet-backup-YYYYMMDD-HHmm.json`. Sin permisos de almacenamiento.
- **Importar**: `OpenDocument(arrayOf("application/json"))`.
  1. Parsear y **validar completamente** antes de tocar la BD: `schemaVersion` soportado, ids únicos, referencias válidas (accountId, toAccountId, categoryId, parentId existen), reglas de 5.4 (montos > 0, transferencias con destino ≠ origen, tipo de categoría coherente, máx. 2 niveles).
  2. Mostrar resumen ("3 cuentas, 25 categorías, 480 movimientos") y confirmación: **"Esto reemplazará todos tus datos actuales"**.
  3. Reemplazar todo dentro de **una sola transacción de Room** (`withTransaction`): borrar todo e insertar conservando ids.
  4. Si algo falla → rollback y mensaje de error; los datos actuales quedan intactos.
- Trabajo de IO en `Dispatchers.IO`; UI muestra progreso.
- Test obligatorio: exportar → importar → la BD queda idéntica (round-trip), y archivos inválidos son rechazados sin modificar datos.

---

## 10. Internacionalización

- `values/strings.xml` = **Español** (idioma por defecto/fallback). `values-en/strings.xml` = **Inglés**. Ambos archivos deben tener **exactamente las mismas claves** (agregar un test unitario o tarea Lint `MissingTranslation` como error).
- `res/xml/locales_config.xml` con `es` y `en`; declarar `android:localeConfig` en el manifest. Usar `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))` (requiere `AppCompatActivity` o el servicio `AppLocalesMetadataHolderService` con `autoStoreLocales=true` para API < 33).
- Plurales con `<plurals>` (p. ej. "%d movimientos").
- Fechas con `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)` y nombres de mes según locale.
- Formato de moneda siempre COP, pero separadores según idioma activo (ver sección 4).
- Convención de claves: `screen_element_description` (p. ej. `transaction_edit_amount_label`, `error_name_duplicated`, `category_food`).

---

## 11. Testing y calidad

| Nivel | Qué | Herramientas |
|---|---|---|
| Unit | `Money`, `MoneyFormatter`, `MoneyParser`, todos los casos de uso (con repos fake en memoria), ViewModels | JUnit4, coroutines-test, Turbine, Truth |
| DAO | Consultas de saldo, filtros, resúmenes, FKs, reasignación | Room in-memory + **Robolectric** |
| Backup | Round-trip, validaciones | Robolectric |
| UI | Flujos clave: onboarding, crear gasto, crear transferencia, crear subcategoría | Compose UI test + Robolectric |
| Migraciones | Cuando exista v2+ | `MigrationTestHelper` |

Casos de prueba de saldo **obligatorios** (DAO):
1. Cuenta sin movimientos → saldo = saldo inicial.
2. Ingreso 100, gasto 30 → +70.
3. Transferencia 50 de A→B → A −50, B +50, total sin cambios.
4. Editar transferencia cambiando destino B→C → B vuelve, C recibe.
5. Borrar transacción → saldo se recalcula.
6. Cuenta archivada o con `includeInTotal=false` → excluida del total.
7. Resumen mensual excluye transferencias.

Cobertura objetivo: ≥ 80 % en `domain` y `core/money`.

Comandos que **deben pasar** antes de cada PR:
```bash
./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug
```

---

## 12. Plan de implementación por tareas (para Jules)

> Una tarea = un PR. No empezar una tarea sin que la anterior compile y pase los tests. Cada PR debe actualizar el checklist de la sección 15.

### Tarea 0 — Preparación del entorno y AGENTS.md
- Crear `AGENTS.md` en la raíz con: resumen del proyecto, stack, estructura de paquetes, comandos de build/test (sección 11), reglas de estilo (sección 13) y "leer PLAN.md antes de cada tarea".
- Script de setup para la VM de Jules (configurar en "Environment setup" de Jules):
  ```bash
  # JDK 17 y Android SDK
  export ANDROID_HOME=$HOME/android-sdk
  mkdir -p $ANDROID_HOME/cmdline-tools
  cd $ANDROID_HOME/cmdline-tools
  curl -sSLo tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q tools.zip && mv cmdline-tools latest && rm tools.zip
  export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
  yes | sdkmanager --licenses
  sdkmanager "platform-tools" "platforms;android-37" "build-tools;37.0.0"
  ```
  (Si `build-tools;37.0.0` no existe, usar la última `build-tools;37.x` que muestre `sdkmanager --list`. Usar una versión de AGP compatible con API 37. Generar `local.properties` con `sdk.dir` si hace falta; **no** commitear `local.properties`.)
- `.gitignore` estándar de Android.
- **Aceptación**: `AGENTS.md` existe; el script instala el SDK sin intervención.

### Tarea 1 — Esqueleto del proyecto
- Proyecto Gradle (Kotlin DSL + version catalog) con módulo `:app`, Compose, Material 3, Hilt, Room (KSP), Navigation, DataStore, kotlinx.serialization, Paging, ktlint, dependencias de test.
- `WalletApp` con `@HiltAndroidApp`, `MainActivity` con edge-to-edge y un `Text` de prueba.
- Tema (`ui/theme`): colores, tipografía, claro/oscuro, dynamic color.
- Gradle wrapper commiteado.
- Workflow `.github/workflows/ci.yml`: JDK 17, cache de Gradle, `./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug`, sube el APK debug como artifact.
- **Aceptación**: `./gradlew assembleDebug` y tests pasan localmente y en CI; la app abre.

### Tarea 2 — Núcleo de dinero y utilidades
- `Money`, `MoneyFormatter`, `MoneyParser`, `TimeProvider`, `DomainResult`, `DomainError`.
- Tests unitarios completos (sección 4).
- **Aceptación**: tests verdes; ningún `Double` para montos en el código.

### Tarea 3 — Base de datos
- Entities, converters, DAOs y `WalletDatabase` (sección 6), módulo Hilt `DatabaseModule`.
- Esquema exportado en `app/schemas/`.
- Tests DAO con Robolectric, incluidos los 7 casos de saldo (sección 11).
- **Aceptación**: tests DAO verdes; esquema JSON commiteado.

### Tarea 4 — Dominio y repositorios
- Modelos de dominio, mappers, interfaces e implementaciones de repositorios, `PreferencesRepository` (DataStore: tema, dynamic color, onboarding completado, última cuenta usada).
- Todos los casos de uso de la sección 7.2 (excepto backup) con validaciones.
- `DefaultCategoriesSeeder` + strings de categorías en ES/EN.
- Tests unitarios de cada caso de uso con repos fake.
- **Aceptación**: todas las reglas de 5.3–5.5 cubiertas por tests.

### Tarea 5 — Navegación, i18n base y componentes comunes
- `WalletNavHost`, bottom bar, rutas type-safe, pantallas placeholder.
- `locales_config.xml`, soporte per-app language, `strings.xml` ES/EN con strings comunes.
- Componentes reutilizables: `MoneyText`, `AmountTextField`, `AccountPickerSheet`, `CategoryPickerSheet` (árbol), `ColorPicker`, `IconPicker`, `ConfirmDialog`, `EmptyState`, `IconCatalog`, `WalletPalette`.
- **Aceptación**: se navega entre las 4 pestañas; cambiar idioma del sistema cambia los textos; previews de componentes.

### Tarea 6 — Onboarding y gestión de cuentas
- Pantallas Onboarding, Cuentas, Detalle de cuenta (sin lista de movimientos aún o con placeholder), Crear/editar cuenta, archivar/eliminar, reordenar.
- ViewModels con tests.
- **Aceptación**: primer arranque muestra onboarding → crea cuenta y siembra categorías → Home. Se pueden crear, editar, archivar, eliminar (sin movimientos) y reordenar cuentas.

### Tarea 7 — Gestión de categorías y subcategorías
- Pantallas Categorías y Crear/editar categoría con árbol, subcategorías, archivar, eliminar con reasignación.
- Tests de ViewModel + test UI de "crear subcategoría".
- **Aceptación**: todas las reglas de 5.3 y 5.5 aplican en la UI; las categorías por defecto cambian de idioma con la app.

### Tarea 8 — Ingresos y gastos
- Pantalla Nueva/editar transacción (pestañas Gasto e Ingreso), selector de categoría con árbol y creación inline, fecha, nota, aviso de saldo negativo, eliminar.
- **Aceptación**: crear/editar/eliminar ingresos y gastos actualiza saldos en tiempo real en cuentas y Home.

### Tarea 9 — Transferencias
- Pestaña Transferencia (desde/hacia, intercambiar), validación misma cuenta, acción "Transferir desde esta cuenta" en el detalle.
- **Aceptación**: transferencias mueven saldo entre cuentas sin afectar el total ni el resumen de ingresos/gastos; editar/eliminar funciona.

### Tarea 10 — Lista de movimientos, detalle de cuenta y Home
- Lista paginada agrupada por día, selector de mes, filtros, búsqueda, swipe-to-delete con deshacer.
- Lista de movimientos en Detalle de cuenta.
- Home completa: saldo total, resumen del mes, cuentas, últimos movimientos, acciones rápidas.
- **Aceptación**: filtros combinables funcionan; el deshacer restaura el movimiento; Home refleja cambios al instante.

### Tarea 11 — Ajustes y backup
- Pantalla Ajustes: idioma, tema, dynamic color, enlace a categorías, versión.
- Export/Import JSON (sección 9) con validación y reemplazo atómico.
- **Aceptación**: round-trip test verde; importar archivo corrupto no modifica datos; idioma y tema persisten tras reiniciar.

### Tarea 12 — Pulido y release
- Estados vacíos, carga y error en todas las pantallas; revisión de accesibilidad (TalkBack básico, tamaños de fuente grandes).
- Ícono de app (adaptive icon) y splash screen (`core-splashscreen`).
- R8/minify en release con reglas para kotlinx.serialization y Room.
- `versionCode`/`versionName` = 1 / "1.0.0". Configuración de firma leída de variables de entorno (sin secretos en el repo).
- Actualizar `README.md`: descripción, capturas, cómo compilar.
- **Aceptación**: `./gradlew assembleRelease` genera APK minificado que funciona; sin warnings de Lint de severidad error.

---

## 13. Convenciones de código

- Kotlin oficial style + ktlint. Nombres en **inglés** en el código; textos de UI solo en `strings.xml`.
- Un archivo por clase pública relevante; composables de pantalla en `ui/<feature>/`.
- ViewModels: `@HiltViewModel`, sin referencias a `Context`; exponen `StateFlow<UiState>`.
- No usar `GlobalScope`; usar `viewModelScope` y dispatchers inyectados (`@IoDispatcher`).
- Commits en formato **Conventional Commits** (`feat:`, `fix:`, `test:`, `chore:`, `docs:`).
- No agregar dependencias fuera de la sección 2 sin justificarlo en el PR.
- Nada de secretos, keystores ni `local.properties` en git.

---

## 14. Roadmap futuro (no implementar en v1)
1. Reportes: gráfico de gastos por categoría (dona) y evolución mensual (barras).
2. Presupuestos mensuales por categoría con alertas.
3. Transacciones recurrentes (WorkManager).
4. Comisión en transferencias (gasto vinculado).
5. Más tipos de cuenta: corriente, tarjeta de crédito, inversión.
6. Multi-moneda con tasas de cambio.
7. Bloqueo con biometría.
8. Backup automático (Google Drive / carpeta local programada).
9. Exportar CSV.
10. Widgets de pantalla de inicio.

---

## 15. Checklist de progreso
- [ ] Tarea 0 — Entorno y AGENTS.md
- [ ] Tarea 1 — Esqueleto del proyecto + CI
- [ ] Tarea 2 — Núcleo de dinero
- [ ] Tarea 3 — Base de datos
- [ ] Tarea 4 — Dominio y repositorios
- [ ] Tarea 5 — Navegación, i18n y componentes
- [ ] Tarea 6 — Onboarding y cuentas
- [ ] Tarea 7 — Categorías y subcategorías
- [ ] Tarea 8 — Ingresos y gastos
- [ ] Tarea 9 — Transferencias
- [ ] Tarea 10 — Movimientos, detalle y Home
- [ ] Tarea 11 — Ajustes y backup
- [ ] Tarea 12 — Pulido y release
