package com.example

import com.example.core.common.CurrencyFormatter
import com.example.core.data.BackupCrypto
import com.example.core.model.AppCurrency
import com.example.core.model.BackupPayload
import com.example.core.model.NumberFormatStyle
import com.example.core.model.ThemeMode
import com.example.core.model.UserPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.AEADBadTagException

class CurrencyAndCryptoUnitTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `bdt south asian lakh crore formatting works for english and bangla digits`() {
        val en = CurrencyFormatter.format(
            amount = 125000.50,
            currency = AppCurrency.BDT,
            numberFormat = NumberFormatStyle.ENGLISH
        )
        assertEquals("৳1,25,000.50", en)

        val bn = CurrencyFormatter.format(
            amount = 125000.50,
            currency = AppCurrency.BDT,
            numberFormat = NumberFormatStyle.BANGLA
        )
        assertEquals("৳১,২৫,০০০.৫০", bn)

        val hidden = CurrencyFormatter.format(
            amount = 5000.0,
            currency = AppCurrency.BDT,
            numberFormat = NumberFormatStyle.ENGLISH,
            hideAmount = true
        )
        assertEquals("৳ ••••••", hidden)
    }

    @Test
    fun `aes gcm backup encryption round trip and wrong passphrase rejection`() {
        val payload = """{"schemaVersion":1,"accounts":[{"id":1,"name":"Cash"}]}"""
        val encrypted = BackupCrypto.encrypt(payload, "StrongPass123")
        val decrypted = BackupCrypto.decrypt(encrypted, "StrongPass123")
        assertEquals(payload, decrypted)

        assertThrows(AEADBadTagException::class.java) {
            BackupCrypto.decrypt(encrypted, "WrongPassphrase")
        }
    }

    @Test
    fun `backup payload supports v2 preferences and v1 backward compatibility`() {
        val v2 = BackupPayload(
            schemaVersion = 2,
            exportedAt = 1_760_000_000_000L,
            accounts = emptyList(),
            categories = emptyList(),
            transactions = emptyList(),
            budgets = emptyList(),
            preferences = UserPreferences(
                currency = AppCurrency.USD,
                themeMode = ThemeMode.DARK
            )
        )
        val encoded = json.encodeToString(v2)
        val decodedV2 = json.decodeFromString<BackupPayload>(encoded)
        assertEquals(2, decodedV2.schemaVersion)
        assertEquals(AppCurrency.USD, decodedV2.preferences?.currency)
        assertEquals(ThemeMode.DARK, decodedV2.preferences?.themeMode)

        val v1Json = """{"schemaVersion":1,"exportedAt":1750000000000,"accounts":[],"categories":[],"transactions":[],"budgets":[]}"""
        val decodedV1 = json.decodeFromString<BackupPayload>(v1Json)
        assertEquals(1, decodedV1.schemaVersion)
        assertNull(decodedV1.preferences)
    }
}
