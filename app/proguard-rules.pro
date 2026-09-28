# Reglas de R8 para Wallet.
# kotlinx.serialization, Room y Hilt incluyen sus propias reglas (consumer rules) en sus artefactos.
# Agregar aquí solo reglas específicas del proyecto (p. ej. DTOs del backup si se usan por reflexión).

# Conservar números de línea en los stack traces de release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
