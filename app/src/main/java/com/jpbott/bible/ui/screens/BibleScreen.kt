package com.jpbott.bible.ui.screens
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpbott.bible.R
import com.jpbott.bible.data.models.*
import com.jpbott.bible.ui.components.TranslationPicker
import com.jpbott.bible.viewmodel.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BibleScreen(s: ReaderState, vm: ReaderViewModel) {
    BackHandler(s.page != Page.TRANSLATIONS) { vm.back() }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(when(s.page) { Page.TRANSLATIONS -> R.string.app_name; Page.BOOKS -> R.string.books; Page.CHAPTERS -> R.string.chapters; Page.READER -> R.string.reader; Page.COMPARE -> R.string.compare })) }, navigationIcon = { if(s.page != Page.TRANSLATIONS) TextButton({ vm.back() }) { Text(stringResource(R.string.back)) } }) }) { inset ->
        Column(Modifier.padding(inset).padding(horizontal = 16.dp).fillMaxSize()) {
            if(s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(s.cached) Text(stringResource(R.string.cached), color = MaterialTheme.colorScheme.secondary)
            if(s.error != null) { Text(stringResource(if(s.error == Problem.MISSING) R.string.missing else R.string.error), color = MaterialTheme.colorScheme.error); Button({ vm.retry() }) { Text(stringResource(R.string.retry)) } }
            if (s.loading && s.page != Page.READER) return@Column
            when(s.page) {
                Page.TRANSLATIONS -> TranslationPicker(s.translations, vm::select)
                Page.COMPARE -> TranslationPicker(s.translations.filter { it.id != s.translation?.id }, vm::compare)
                Page.BOOKS -> { Text(s.translation?.name.orEmpty(), style = MaterialTheme.typography.titleLarge); if(s.books.isEmpty()) Text(stringResource(R.string.empty)); LazyColumn { items(s.books, key = { it.id }) { b -> ListItem(headlineContent = { Text(b.name) }, supportingContent = { Text(b.commonName) }, modifier = Modifier.clickable { vm.book(b) }); HorizontalDivider() } } }
                Page.CHAPTERS -> { Text(s.book?.name.orEmpty(), style = MaterialTheme.typography.titleLarge); LazyColumn { items(s.book?.let { (it.firstChapterNumber..it.lastChapterNumber).toList() }.orEmpty()) { n -> ListItem(headlineContent = { Text(stringResource(R.string.chapter_number, n)) }, modifier = Modifier.clickable { vm.read(n) }); HorizontalDivider() } } }
                Page.READER -> Reader(s, vm)
            }
        }
    }
}
@Composable private fun Reader(s: ReaderState, vm: ReaderViewModel) {
    val chapter = s.chapter
    Text(stringResource(R.string.reader_title, s.book?.name.orEmpty(), s.number), style = MaterialTheme.typography.headlineSmall)
    Text(s.translation?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton({ vm.navigate(-1) }, enabled = !s.loading) { Text(stringResource(R.string.previous)) }
        TextButton({ vm.navigate(1) }, enabled = !s.loading) { Text(stringResource(R.string.next)) }
        TextButton({ vm.font(-2) }) { Text(stringResource(R.string.smaller)) }
        TextButton({ vm.font(2) }) { Text(stringResource(R.string.larger)) }
    }
    Row { OutlinedButton({ vm.comparePicker() }, enabled = !s.loading && chapter != null) { Text(stringResource(R.string.compare)) }
        if(s.comparison != null) TextButton({ vm.clearCompare() }) { Text(stringResource(R.string.close_compare)) }
    }
    if(chapter == null) return
    val first = remember(chapter) { chapter.passages() }
    val second = s.second
    key(s.translation?.id, s.book?.id, s.number) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if(second == null) {
                items(first) { p -> PassageText(p, s.font, s.translation) }
            } else {
                val a = first.filter { it.number != null }.groupBy { it.number!! }
                val b = second.passages().filter { it.number != null }.groupBy { it.number!! }
                items((a.keys + b.keys).sorted()) { number ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.verse_number, number), style = MaterialTheme.typography.titleMedium)
                            Text(s.translation?.name.orEmpty(), style = MaterialTheme.typography.labelMedium)
                            if(a[number].isNullOrEmpty()) Text(stringResource(R.string.verse_missing)) else a[number]?.forEach { PassageText(it, s.font, s.translation, false) }
                            HorizontalDivider()
                            Text(s.comparison?.name.orEmpty(), style = MaterialTheme.typography.labelMedium)
                            if(b[number].isNullOrEmpty()) Text(stringResource(R.string.verse_missing)) else b[number]?.forEach { PassageText(it, s.font, s.comparison, false) }
                        }
                    }
                }
            }
            item { Attribution(s.translation, chapter); if(second != null) Attribution(s.comparison, second); Spacer(Modifier.height(20.dp)) }
        }
    }
}
@Composable private fun PassageText(p: Passage, font: Int, t: Translation?, showNumber: Boolean = true) {
    if(p.lineBreak) Spacer(Modifier.height(6.dp)) else Text(if(showNumber && p.number != null) stringResource(R.string.numbered_text, p.number, p.text) else p.text, style = (if(p.heading) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge).copy(fontSize = font.sp, lineHeight = (font + 9).sp, textDirection = if(t?.textDirection == "rtl") TextDirection.ContentOrRtl else TextDirection.ContentOrLtr))
}
@Composable private fun Attribution(t: Translation?, c: ChapterData) {
    HorizontalDivider(); Text(t?.name.orEmpty(), style = MaterialTheme.typography.titleSmall)
    if(c.footnotes.isNotEmpty()) { Text(stringResource(R.string.footnotes)); c.footnotes.forEach { Text(stringResource(R.string.note_text, it.noteId + 1, it.text), style = MaterialTheme.typography.bodySmall) } }
    t?.licenseNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }; t?.licenseUrl?.let { Text(it, style = MaterialTheme.typography.bodySmall) }; t?.website?.let { Text(it, style = MaterialTheme.typography.bodySmall) }; Text(stringResource(R.string.attribution), style = MaterialTheme.typography.bodySmall)
}
