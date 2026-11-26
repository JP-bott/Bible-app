package com.jpbott.bible
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.jpbott.bible.data.models.Translation
import com.jpbott.bible.ui.components.TranslationPicker
import com.jpbott.bible.ui.theme.BibleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
class TranslationPickerTest {
    @get:Rule val compose = createComposeRule()
    @Test fun searchAndSelect() {
        var selected = ""
        compose.setContent { BibleTheme { TranslationPicker(listOf(Translation("BSB", "Berean Standard Bible", "eng"), Translation("sample", "Sample Tagalog", "tgl"))) { selected = it.id } } }
        compose.onNodeWithText("Search translations").performTextInput("Tagalog")
        compose.onNodeWithText("Berean Standard Bible").assertDoesNotExist()
        compose.onNodeWithText("Sample Tagalog").performClick()
        compose.runOnIdle { assertEquals("sample", selected) }
    }
}
