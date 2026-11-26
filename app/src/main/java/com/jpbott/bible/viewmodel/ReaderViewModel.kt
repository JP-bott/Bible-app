package com.jpbott.bible.viewmodel
import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jpbott.bible.data.models.*
import com.jpbott.bible.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
private val Context.preferences by preferencesDataStore("reader")
private val selectedKey = stringPreferencesKey("translation")
enum class Page { TRANSLATIONS, BOOKS, CHAPTERS, READER, COMPARE }
enum class Problem { GENERAL, MISSING }
data class ReaderState(val page: Page = Page.TRANSLATIONS, val translations: List<Translation> = emptyList(), val translation: Translation? = null, val books: List<Book> = emptyList(), val book: Book? = null, val number: Int = 1, val chapter: ChapterData? = null, val comparison: Translation? = null, val second: ChapterData? = null, val loading: Boolean = false, val error: Problem? = null, val cached: Boolean = false, val font: Int = 20)
class ReaderViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BibleRepository.create(File(app.filesDir, "bible-json"))
    private val prefs = app.preferences
    private val mutable = MutableStateFlow(ReaderState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var retryAction: (() -> Unit)? = null
    init { translations(true) }
    private fun change(transform: (ReaderState) -> ReaderState) = mutable.update(transform)
    private fun work(action: suspend () -> Unit) {
        job?.cancel()
        retryAction = { work(action) }
        job = viewModelScope.launch {
            change { it.copy(loading = true, error = null) }
            try { action(); change { it.copy(loading = false) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { change { it.copy(loading = false, error = if (e is MissingChapter) Problem.MISSING else Problem.GENERAL) } }
        }
    }
    fun retry() { retryAction?.invoke() }
    fun translations(restore: Boolean = false) {
        change { it.copy(page = Page.TRANSLATIONS, cached = false) }
        work {
            val result = repo.translations()
            change { it.copy(translations = result.value.translations, cached = result.cached) }
            if (restore) {
                val id = try { prefs.data.first()[selectedKey] } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
                result.value.translations.firstOrNull { it.id == id }?.let { t ->
                    change { it.copy(page = Page.BOOKS, translation = t, books = emptyList()) }
                    val books = repo.books(t.id)
                    change { it.copy(books = books.value.books.sortedBy { b -> b.order }, cached = result.cached || books.cached) }
                }
            }
        }
    }
    fun select(t: Translation) {
        change { it.copy(page = Page.BOOKS, translation = t, books = emptyList(), book = null, chapter = null, second = null, comparison = null, cached = false) }
        work {
            try { prefs.edit { it[selectedKey] = t.id } } catch (e: CancellationException) { throw e } catch (_: Exception) { }
            val result = repo.books(t.id)
            change { it.copy(books = result.value.books.sortedBy { b -> b.order }, cached = result.cached) }
        }
    }
    fun book(b: Book) { change { it.copy(page = Page.CHAPTERS, book = b, error = null) } }
    fun read(n: Int) { state.value.book?.let { read(n, it) } }
    fun read(n: Int, b: Book) {
        val t = state.value.translation ?: return
        val compare = state.value.comparison
        change { it.copy(page = Page.READER, book = b, number = n, chapter = null, second = null, cached = false) }
        work {
            val result = repo.chapter(t.id, b.id, n)
            change { it.copy(chapter = result.value.chapter, cached = result.cached) }
            if (compare != null) loadSecond(compare, b, n)
        }
    }
    fun navigate(delta: Int) {
        val s = state.value; val b = s.book ?: return
        val n = s.number + delta
        if (n in b.firstChapterNumber..b.lastChapterNumber) read(n, b)
        else { val index = s.books.indexOfFirst { it.id == b.id }; s.books.getOrNull(index + delta)?.let { read(if (delta > 0) it.firstChapterNumber else it.lastChapterNumber, it) } }
    }
    fun font(delta: Int) { change { it.copy(font = (it.font + delta).coerceIn(14, 36)) } }
    fun comparePicker() { change { it.copy(page = Page.COMPARE, error = null) } }
    private suspend fun loadSecond(t: Translation, b: Book, n: Int) {
        val list = repo.books(t.id)
        val match = list.value.books.firstOrNull { it.id == b.id && n in it.firstChapterNumber..it.lastChapterNumber } ?: throw MissingChapter()
        val result = repo.chapter(t.id, match.id, n)
        change { it.copy(second = result.value.chapter, cached = it.cached || list.cached || result.cached) }
    }
    fun compare(t: Translation) {
        val b = state.value.book ?: return; val n = state.value.number
        change { it.copy(page = Page.READER, comparison = t, second = null) }
        work { loadSecond(t, b, n) }
    }
    fun clearCompare() { job?.cancel(); change { it.copy(comparison = null, second = null, error = null, loading = false) } }
    fun back() {
        val s = state.value
        job?.cancel()
        change { it.copy(page = when (s.page) { Page.TRANSLATIONS -> Page.TRANSLATIONS; Page.BOOKS -> Page.TRANSLATIONS; Page.CHAPTERS -> Page.BOOKS; Page.READER -> Page.CHAPTERS; Page.COMPARE -> Page.READER }, loading = false, error = null, comparison = if (s.page == Page.READER) null else it.comparison, second = if (s.page == Page.READER) null else it.second) }
    }
}
