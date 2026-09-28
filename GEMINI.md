# Wallet — Instrucciones para Antigravity / Gemini

Las reglas de este proyecto están en **`AGENTS.md`** y el plan completo en **`PLAN.md`**. Léelos antes de cualquier tarea; este archivo no agrega reglas propias.

Puntos clave (detalle en `PLAN.md` §16.1):
- Trabajas en local: SDK en `~/Android/Sdk`, compila con `export JAVA_HOME=/opt/android-studio/jbr`.
- Una rama `feat/tarea-N-<tema>` por tarea, desde `main` actualizado.
- Antes de terminar debe pasar: `./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug`.
- Al terminar, detente y avisa. No hagas push a `main` ni merge.
