package com.jpbott.bible.ui.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jpbott.bible.R
import com.jpbott.bible.data.models.Translation
@Composable fun TranslationPicker(translations: List<Translation>, onSelect: (Translation) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }; var language by rememberSaveable { mutableStateOf("") }; var expanded by remember { mutableStateOf(false) }
    val languages = remember(translations) { translations.groupBy { it.language }.map { it.key to it.value.first().languageLabel }.sortedBy { it.second.lowercase() } }
    val results = remember(translations, query, language) { translations.filter { t -> (language.isEmpty() || t.language == language) && listOf(t.name, t.englishName, t.id, t.shortName, t.languageLabel, t.languageName.orEmpty()).any { it.contains(query.trim(), ignoreCase = true) } }.sortedBy { it.name.lowercase() } }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search_translations)) }, singleLine = true)
        Box { OutlinedButton({ expanded = true }) { Text(languages.firstOrNull { it.first == language }?.second ?: stringResource(R.string.all_languages)) }
            DropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem({ Text(stringResource(R.string.all_languages)) }, { language = ""; expanded = false })
                languages.forEach { (id, label) -> DropdownMenuItem({ Text(label) }, { language = id; expanded = false }) }
            }
        }
        if (results.isEmpty()) Text(stringResource(R.string.empty))
        LazyColumn(Modifier.weight(1f)) { items(results, key = { it.id }) { t ->
            ListItem(headlineContent = { Text(t.name) }, supportingContent = { Text(stringResource(R.string.translation_detail, t.languageLabel, t.id)) }, modifier = Modifier.clickable { onSelect(t) })
            HorizontalDivider()
        } }
    }
}
