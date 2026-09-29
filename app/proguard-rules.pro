# Reglas de R8 para Wallet.
#
# No hacen falta reglas propias para el código de la app: se comprobó sobre el mapping del release
# (app/build/outputs/mapping/release) que las librerías ya traen las suyas (consumer rules):
# - kotlinx.serialization conserva los serializadores y `Companion.serializer()` de los DTO del backup
#   y de las rutas de navegación; los nombres JSON salen de strings del descriptor, no de nombres de campo.
# - Los enums guardados por `name` (AccountType, CategoryType, TransactionType) siguen siendo enums con su
#   nombre literal, y proguard-android-optimize.txt conserva `values()`/`valueOf()`.
# - Room (`WalletDatabase_Impl`), Hilt y Navigation incluyen sus propias reglas.
# Agregar aquí solo reglas que se demuestre que faltan, cada una con un comentario del porqué.

# Conservar números de línea en los stack traces de release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
