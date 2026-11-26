package com.jpbott.bible.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable data class Translation(val id: String, val name: String, val language: String, val englishName: String = "", val shortName: String = "", val languageName: String? = null, val languageEnglishName: String? = null, val textDirection: String = "ltr", val licenseNotice: String? = null, val licenseUrl: String? = null, val website: String? = null) {
    val languageLabel get() = languageEnglishName ?: languageName ?: language
}
@Serializable data class TranslationList(val translations: List<Translation>)
@Serializable data class Book(val id: String, val name: String, val commonName: String = "", val order: Int = 0, val numberOfChapters: Int, val firstChapterNumber: Int = 1, val lastChapterNumber: Int = firstChapterNumber + numberOfChapters - 1)
@Serializable data class BookList(val books: List<Book>)
@Serializable data class ChapterResponse(val chapter: ChapterData)
@Serializable data class ChapterData(val number: Int, val content: List<JsonObject>, val footnotes: List<Footnote> = emptyList())
@Serializable data class Footnote(val noteId: Int, val text: String)
data class Passage(val number: Int?, val text: String, val heading: Boolean = false, val lineBreak: Boolean = false)
private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.contentOrNull
private fun JsonElement?.num(): Int? = (this as? JsonPrimitive)?.intOrNull
private fun piece(value: JsonElement): String = when (value) {
    is JsonPrimitive -> if (value.isString) value.content else ""
    is JsonObject -> when {
        value["lineBreak"].str() == "true" -> "\n"
        value["noteId"].num() != null -> "[" + (value["noteId"].num()!! + 1) + "]"
        value["heading"].str() != null -> "\n" + value["heading"].str() + "\n"
        else -> value["text"].str() ?: ""
    }
    is JsonArray -> value.joinToString("") { piece(it) }
}
private fun pieces(value: JsonElement?): String = when (value) {
    is JsonArray -> value.joinToString(" ") { piece(it) }.replace(Regex(" +(?=\\[|[,.!?;:])"), "").replace(Regex(" *\\n *"), "\n")
    null -> ""
    else -> piece(value)
}
fun ChapterData.passages(): List<Passage> = content.mapNotNull { item ->
    when (item["type"].str()) {
        "verse" -> item["number"].num()?.let { Passage(it, pieces(item["content"])) }
        "heading", "hebrew_subtitle" -> Passage(null, pieces(item["content"] ?: item["text"]), heading = true)
        "line_break" -> Passage(null, "", lineBreak = true)
        else -> null
    }
}
