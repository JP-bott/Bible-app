package com.jpbott.bible.data.repository
import com.jpbott.bible.data.api.BibleApi
import com.jpbott.bible.data.models.*
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
class MissingChapter : IOException()
class InvalidResponse : IOException()
data class Loaded<T>(val value: T, val cached: Boolean)
class BibleRepository(private val api: BibleApi, private val cache: File) {
    companion object {
        val json = Json { ignoreUnknownKeys = true }
        fun create(cache: File): BibleRepository {
            val client = OkHttpClient.Builder().cache(Cache(File(cache.parentFile, "http-cache"), 20L * 1024 * 1024)).followRedirects(false).followSslRedirects(false).build()
            val api = Retrofit.Builder().baseUrl("https://bible.helloao.org/").client(client).build().create(BibleApi::class.java)
            return BibleRepository(api, cache)
        }
    }
    private suspend inline fun <reified T> load(key: String, crossinline request: suspend () -> Response<ResponseBody>): Loaded<T> = withContext(Dispatchers.IO) {
        val file = File(cache, key + ".json")
        try {
            val response = request()
            if (response.code() == 404) { response.errorBody()?.close(); throw MissingChapter() }
            if (!response.isSuccessful) { response.errorBody()?.close(); throw IOException() }
            val raw = response.body()?.use { it.string() } ?: throw InvalidResponse()
            val parsed = json.decodeFromString<T>(raw)
            runCatching { cache.mkdirs(); val tmp = File.createTempFile("bible", ".tmp", cache); try { tmp.writeText(raw); if (!tmp.renameTo(file)) file.writeText(raw) } finally { tmp.delete() } }
            Loaded(parsed, false)
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            if (e is MissingChapter) throw e
            if (file.isFile) {
                try { return@withContext Loaded(json.decodeFromString<T>(file.readText()), true) } catch (_: Exception) { file.delete() }
            }
            throw e
        }
    }
    private fun key(id: String): String = id.toByteArray().joinToString("") { "%02x".format(it.toInt() and 255) }
    suspend fun translations() = load<TranslationList>("translations") { api.translations() }
    suspend fun books(id: String) = load<BookList>("books-" + key(id)) { api.books(id) }
    suspend fun chapter(id: String, book: String, number: Int) = load<ChapterResponse>("chapter-" + key(id) + "-" + key(book) + "-" + number) { api.chapter(id, book, number) }
}
