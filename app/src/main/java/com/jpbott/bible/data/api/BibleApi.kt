package com.jpbott.bible.data.api
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
interface BibleApi {
    @GET("api/available_translations.json") suspend fun translations(): Response<ResponseBody>
    @GET("api/{translation}/books.json") suspend fun books(@Path("translation") translation: String): Response<ResponseBody>
    @GET("api/{translation}/{book}/{chapter}.json") suspend fun chapter(@Path("translation") translation: String, @Path("book") book: String, @Path("chapter") chapter: Int): Response<ResponseBody>
}
