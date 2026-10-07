package com.crguarin.wallet.data.backup

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Test

class BackupParserTest {
    private val parser = BackupParser(FakeTimeProvider())

    private val planSample =
        """
        {
          "schemaVersion": 1,
          "appVersion": "1.0.0",
          "exportedAt": "2026-09-28T15:00:00Z",
          "currency": "COP",
          "accounts": [ { "id": 1, "name": "Efectivo", "type": "CASH", "currencyCode": "COP", "initialBalance": 5000000, "color": -16711936, "icon": "wallet", "includeInTotal": true, "isArchived": false, "sortOrder": 0, "createdAt": 1790000000000 } ],
          "categories": [ { "id": 1, "name": null, "systemKey": "food", "type": "EXPENSE", "parentId": null, "color": -65536, "icon": "restaurant", "isArchived": false, "sortOrder": 0 } ],
          "transactions": [ { "id": 1, "type": "EXPENSE", "amount": 1500000, "accountId": 1, "toAccountId": null, "categoryId": 1, "date": "2026-09-27", "note": "Almuerzo", "createdAt": 1790000000000, "updatedAt": 1790000000000 } ]
        }
        """.trimIndent()

    private fun decodeToDto(text: String): BackupDto = (parser.decode(text) as DomainResult.Success).value

    private fun error(text: String): DomainError = (parser.decode(text) as DomainResult.Failure).error

    @Test
    fun `the sample of the plan parses`() {
        val dto = decodeToDto(planSample)

        assertThat(dto.schemaVersion).isEqualTo(1)
        assertThat(dto.accounts.single().initialBalance).isEqualTo(5_000_000L)
        assertThat(dto.accounts.single().type).isEqualTo(AccountType.CASH)
        assertThat(dto.categories.single().systemKey).isEqualTo("food")
        assertThat(dto.categories.single().name).isNull()
        assertThat(dto.transactions.single().date).isEqualTo("2026-09-27")
        assertThat(dto.transactions.single().type).isEqualTo(TransactionType.EXPENSE)
    }

    @Test
    fun `nullable fields may be omitted`() {
        val text = planSample.replace("\"name\": null, ", "").replace("\"toAccountId\": null, ", "")
        assertThat(parser.decode(text).isSuccess).isTrue()
    }

    @Test
    fun `unknown keys are ignored`() {
        val text = planSample.replace("\"currency\": \"COP\",", "\"currency\": \"COP\", \"futureField\": {\"a\": 1},")
        assertThat(parser.decode(text).isSuccess).isTrue()
    }

    @Test
    fun `exported json has the documented fields and formats`() {
        val root = Json.parseToJsonElement(parser.encode(validBackupDto())).jsonObject

        assertThat(root.keys)
            .containsAtLeast("schemaVersion", "appVersion", "exportedAt", "currency", "accounts", "categories", "transactions")
        assertThat(root["schemaVersion"]!!.jsonPrimitive.content).isEqualTo("3")
        assertThat(root["currency"]!!.jsonPrimitive.content).isEqualTo("COP")
        assertThat(root["exportedAt"]!!.jsonPrimitive.content).isEqualTo("2026-09-28T15:00:00Z")

        val account = root["accounts"]!!.jsonArray.first().jsonObject
        assertThat(account.keys).containsExactly(
            "id",
            "name",
            "type",
            "currencyCode",
            "initialBalance",
            "color",
            "icon",
            "includeInTotal",
            "isArchived",
            "sortOrder",
            "createdAt",
            "creditLimit",
            "statementDay",
            "paymentDueDay",
        )
        assertThat(account["type"]!!.jsonPrimitive.content).isEqualTo("CASH")
        assertThat(account["initialBalance"]!!.jsonPrimitive.isString).isFalse()
        assertThat(account["initialBalance"]!!.jsonPrimitive.content).isEqualTo("5000000")
        assertThat(account["createdAt"]!!.jsonPrimitive.content).isEqualTo("1790000000000")

        val category = root["categories"]!!.jsonArray.first().jsonObject
        assertThat(category["name"]).isEqualTo(JsonNull)
        assertThat(category["parentId"]).isEqualTo(JsonNull)
        assertThat(category["type"]!!.jsonPrimitive.content).isEqualTo("EXPENSE")

        val transaction = root["transactions"]!!.jsonArray.first().jsonObject
        assertThat(transaction["date"]!!.jsonPrimitive.content).isEqualTo("2026-09-27")
        assertThat(transaction["type"]!!.jsonPrimitive.content).isEqualTo("EXPENSE")
        assertThat(transaction["toAccountId"]).isEqualTo(JsonNull)
        assertThat(transaction["amount"]!!.jsonPrimitive.isString).isFalse()
    }

    @Test
    fun `encode then decode returns the same dto`() {
        val dto = validBackupDto()
        assertThat(decodeToDto(parser.encode(dto))).isEqualTo(dto)
    }

    @Test
    fun `output is pretty printed`() {
        assertThat(parser.encode(validBackupDto())).contains("\n    \"schemaVersion\": 3")
    }

    @Test
    fun `a version 1 backup without the new fields is accepted with card defaults`() {
        val dto = decodeToDto(planSample)

        assertThat(dto.schemaVersion).isEqualTo(1)
        assertThat(dto.cardStatements).isEmpty()
        assertThat(dto.accounts.single().creditLimit).isNull()
        assertThat(dto.accounts.single().statementDay).isNull()
        assertThat(dto.accounts.single().paymentDueDay).isNull()
        assertThat(dto.transactions.single().installments).isNull()
    }

    @Test
    fun `a version 2 backup with card data parses`() {
        val dto = decodeToDto(parser.encode(validCardBackupDto().copy(schemaVersion = 2)))

        assertThat(dto.schemaVersion).isEqualTo(2)
        assertThat(dto.accounts.single { it.type == AccountType.CREDIT_CARD }.creditLimit).isEqualTo(10_000_000L)
        assertThat(dto.transactions.single { it.id == 4L }.installments).isEqualTo(3)
        assertThat(dto.cardStatements).hasSize(2)
        assertThat(dto.recurringPayments).isEmpty()
    }

    @Test
    fun `a version 2 file without the new keys is accepted`() {
        val root = Json.parseToJsonElement(parser.encode(validCardBackupDto())).jsonObject
        val withoutNewKeys =
            buildJsonObject {
                root.forEach { (key, value) ->
                    when (key) {
                        "recurringPayments" -> Unit
                        "transactions" ->
                            put(
                                key,
                                JsonArray(
                                    value.jsonArray.map { tx ->
                                        buildJsonObject {
                                            tx.jsonObject.forEach { (txKey, txValue) ->
                                                if (txKey != "recurringId") put(txKey, txValue)
                                            }
                                        }
                                    },
                                ),
                            )
                        else -> put(key, value)
                    }
                }
                put("schemaVersion", 2)
            }.toString()
        val dto = decodeToDto(withoutNewKeys)

        assertThat(dto.schemaVersion).isEqualTo(2)
        assertThat(dto.recurringPayments).isEmpty()
        assertThat(dto.transactions.single { it.id == 4L }.recurringId).isNull()
    }

    @Test
    fun `a version 3 backup with recurring payments parses`() {
        val dto = decodeToDto(parser.encode(validRecurringBackupDto()))

        assertThat(dto.schemaVersion).isEqualTo(3)
        assertThat(dto.recurringPayments).hasSize(2)
        assertThat(dto.recurringPayments.single { it.id == 1L }.name).isEqualTo("Netflix")
        assertThat(dto.transactions.single { it.id == 6L }.recurringId).isEqualTo(1L)
    }

    @Test
    fun `version 3 writes recurring dates enums and instants with the documented format`() {
        val root = Json.parseToJsonElement(parser.encode(validRecurringBackupDto())).jsonObject
        val payments = root["recurringPayments"]!!.jsonArray

        assertThat(payments).hasSize(2)
        val auto = payments.first().jsonObject
        assertThat(auto["startDate"]!!.jsonPrimitive.content).isEqualTo("2026-09-15")
        assertThat(auto["nextDueDate"]!!.jsonPrimitive.content).isEqualTo("2026-10-15")
        assertThat(auto["endDate"]).isEqualTo(JsonNull)
        assertThat(auto["frequency"]!!.jsonPrimitive.content).isEqualTo("MONTHLY")
        assertThat(auto["mode"]!!.jsonPrimitive.content).isEqualTo("AUTO")
        assertThat(auto["createdAt"]!!.jsonPrimitive.isString).isFalse()
        assertThat(auto["createdAt"]!!.jsonPrimitive.content).isEqualTo("1790000000000")

        val yearly = payments.last().jsonObject
        assertThat(yearly["endDate"]!!.jsonPrimitive.content).isEqualTo("2028-01-10")
        assertThat(yearly["frequency"]!!.jsonPrimitive.content).isEqualTo("YEARLY")
        assertThat(yearly["mode"]!!.jsonPrimitive.content).isEqualTo("CONFIRM")

        val generated = root["transactions"]!!.jsonArray.single { it.jsonObject["id"]!!.jsonPrimitive.content == "6" }
        assertThat(generated.jsonObject["recurringId"]!!.jsonPrimitive.content).isEqualTo("1")
    }

    @Test
    fun `unknown recurring frequency or mode is invalid`() {
        val encoded = parser.encode(validRecurringBackupDto())

        assertThat(error(encoded.replace("\"frequency\": \"MONTHLY\"", "\"frequency\": \"DAILY\"")))
            .isInstanceOf(DomainError.BackupInvalid::class.java)
        assertThat(error(encoded.replace("\"mode\": \"AUTO\"", "\"mode\": \"MANUAL\"")))
            .isInstanceOf(DomainError.BackupInvalid::class.java)
    }

    @Test
    fun `schema versions 1 2 and 3 are accepted`() {
        assertThat(parser.decode(planSample).isSuccess).isTrue()
        assertThat(parser.decode(parser.encode(validCardBackupDto().copy(schemaVersion = 2))).isSuccess).isTrue()
        assertThat(parser.decode(parser.encode(validRecurringBackupDto())).isSuccess).isTrue()
    }

    @Test
    fun `schema versions other than 1 2 and 3 are unsupported`() {
        for (version in listOf(0, 4, 7, -1)) {
            assertThat(error(planSample.replace("\"schemaVersion\": 1", "\"schemaVersion\": $version")))
                .isEqualTo(DomainError.BackupVersionUnsupported)
        }
    }

    @Test
    fun `text that is not json is invalid`() {
        assertThat(error("not json at all")).isInstanceOf(DomainError.BackupInvalid::class.java)
        assertThat(error("")).isInstanceOf(DomainError.BackupInvalid::class.java)
        assertThat(error("[1, 2, 3]")).isInstanceOf(DomainError.BackupInvalid::class.java)
        assertThat(error("{\"schemaVersion\": 1")).isInstanceOf(DomainError.BackupInvalid::class.java)
    }

    @Test
    fun `missing required fields are invalid`() {
        assertThat(error("{\"schemaVersion\": 1}")).isInstanceOf(DomainError.BackupInvalid::class.java)
        val withoutAmount = planSample.replace("\"amount\": 1500000, ", "")
        assertThat(error(withoutAmount)).isInstanceOf(DomainError.BackupInvalid::class.java)
    }

    @Test
    fun `wrong field types are invalid`() {
        val text = planSample.replace("\"amount\": 1500000", "\"amount\": \"lots\"")
        assertThat(error(text)).isInstanceOf(DomainError.BackupInvalid::class.java)
    }

    @Test
    fun `unknown enum values are invalid`() {
        val text = planSample.replace("\"type\": \"CASH\"", "\"type\": \"CRYPTO\"")
        assertThat(error(text)).isInstanceOf(DomainError.BackupInvalid::class.java)
    }

    @Test
    fun `a different schema version is unsupported even if the rest has another shape`() {
        val future: JsonObject =
            buildJsonObject {
                put("schemaVersion", 4)
                put("data", JsonArray(listOf(JsonPrimitive("something else"))))
            }
        assertThat(error(future.toString())).isEqualTo(DomainError.BackupVersionUnsupported)
        assertThat(
            error(planSample.replace("\"schemaVersion\": 1", "\"schemaVersion\": 7")),
        ).isEqualTo(DomainError.BackupVersionUnsupported)
    }

    @Test
    fun `a decoded but inconsistent backup is rejected by the validator`() {
        val text = planSample.replace("\"accountId\": 1", "\"accountId\": 42")
        assertThat(error(text)).isInstanceOf(DomainError.BackupInvalid::class.java)
    }
}
