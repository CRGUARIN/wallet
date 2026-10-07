package com.crguarin.wallet.data.backup

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.DomainLimits
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encodes and decodes backup JSON. Decoding checks `schemaVersion` first so a file from a newer
 * (differently shaped) version is reported as unsupported instead of as corrupt.
 */
@Singleton
class BackupParser
    @Inject
    constructor(
        private val timeProvider: TimeProvider,
    ) {
        private val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        fun encode(dto: BackupDto): String = json.encodeToString(BackupDto.serializer(), dto)

        /** Decodes and fully validates [text]; never throws on malformed input. */
        fun decode(text: String): DomainResult<BackupDto> {
            val root =
                try {
                    json.parseToJsonElement(text).jsonObject
                } catch (e: SerializationException) {
                    return invalid("not a JSON object: ${e.message}")
                } catch (e: IllegalArgumentException) {
                    return invalid("not a JSON object: ${e.message}")
                }
            schemaVersionOf(root)?.let { version ->
                if (version != 1 && version != 2 && version != BackupDto.SCHEMA_VERSION) {
                    return DomainResult.Failure(DomainError.BackupVersionUnsupported)
                }
            }
            val dto =
                try {
                    json.decodeFromJsonElement(BackupDto.serializer(), root)
                } catch (e: SerializationException) {
                    return invalid("malformed content: ${e.message}")
                } catch (e: IllegalArgumentException) {
                    return invalid("malformed content: ${e.message}")
                }
            val maxTransactionDate = DomainLimits.maxTransactionDate(timeProvider.today())
            return BackupValidator.validate(dto, maxTransactionDate)?.let { DomainResult.Failure(it) } ?: DomainResult.Success(dto)
        }

        private fun schemaVersionOf(root: JsonObject): Int? =
            try {
                root["schemaVersion"]?.jsonPrimitive?.int
            } catch (_: IllegalArgumentException) {
                null
            }

        private fun invalid(reason: String): DomainResult.Failure = DomainResult.Failure(DomainError.BackupInvalid(reason))
    }
