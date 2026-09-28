package com.crguarin.wallet

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StringResourcesTest {
    @Test
    fun `values and values-en have identical string resource keys`() {
        val esFile = findResFile("src/main/res/values/strings.xml")
        val enFile = findResFile("src/main/res/values-en/strings.xml")

        assertThat(esFile.exists()).isTrue()
        assertThat(enFile.exists()).isTrue()

        val esKeys = parseStringKeys(esFile)
        val enKeys = parseStringKeys(enFile)

        assertThat(esKeys).isNotEmpty()
        assertThat(esKeys).isEqualTo(enKeys)
    }

    private fun findResFile(relativePath: String): File {
        val fileDirect = File(relativePath)
        if (fileDirect.exists()) return fileDirect
        val fileInApp = File("app", relativePath)
        if (fileInApp.exists()) return fileInApp
        val parentFile = File("../", relativePath)
        if (parentFile.exists()) return parentFile
        return fileDirect
    }

    private fun parseStringKeys(file: File): Set<String> {
        val keys = mutableSetOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        FileInputStream(file).use { stream ->
            parser.setInput(stream, "UTF-8")
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "string") {
                    val name = parser.getAttributeValue(null, "name")
                    if (name != null) {
                        keys.add(name)
                    }
                }
                eventType = parser.next()
            }
        }
        return keys
    }
}
