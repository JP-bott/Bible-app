package com.jpbott.bible
import com.jpbott.bible.data.api.BibleApi
import com.jpbott.bible.data.models.*
import com.jpbott.bible.data.repository.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
class RepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private fun fixture(name: String) = checkNotNull(javaClass.classLoader?.getResourceAsStream(name)).bufferedReader().use { it.readText() }
    @Test fun parsing() {
        val json = BibleRepository.json
        assertEquals("eng", json.decodeFromString<TranslationList>(fixture("translations.json")).translations.single().language)
        assertEquals(50, json.decodeFromString<BookList>(fixture("books.json")).books.single().lastChapterNumber)
        val chapter = json.decodeFromString<ChapterResponse>(fixture("chapter.json")).chapter
        val parts = chapter.passages()
        assertTrue(parts.first().heading); assertTrue(parts[1].lineBreak)
        assertTrue(parts.first { it.number == 5 }.text.contains("[1]"))
        assertTrue(parts.first { it.number == 21 }.text.contains("\n"))
        assertTrue(parts.first { it.number == 43 }.text.contains("Follow me."))
        assertEquals(5, parts.size); assertEquals(0, chapter.footnotes.single().noteId)
    }
    @Test fun cachedChapterSurvivesMalformedRefresh() = runTest {
        MockWebServer().use { server ->
            server.start()
            val api = Retrofit.Builder().baseUrl(server.url("/")).build().create(BibleApi::class.java)
            val repo = BibleRepository(api, folder.newFolder())
            server.enqueue(MockResponse().setBody(fixture("chapter.json")))
            assertFalse(repo.chapter("eng_web", "JHN", 1).cached)
            assertEquals("/api/eng_web/JHN/1.json", server.takeRequest().path)
            server.enqueue(MockResponse().setBody("not json"))
            assertTrue(repo.chapter("eng_web", "JHN", 1).cached)
            server.enqueue(MockResponse().setResponseCode(503))
            assertTrue(repo.chapter("eng_web", "JHN", 1).cached)
            server.enqueue(MockResponse().setResponseCode(404))
            try { repo.chapter("eng_web", "JHN", 1); fail("Expected MissingChapter") } catch (_: MissingChapter) { }
        }
    }
    @Test fun malformedWithoutCacheFails() = runTest {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setBody("{}"))
            val api = Retrofit.Builder().baseUrl(server.url("/")).build().create(BibleApi::class.java)
            try { BibleRepository(api, folder.newFolder()).translations(); fail("Expected invalid data") } catch (_: kotlinx.serialization.SerializationException) { }
        }
    }
}
