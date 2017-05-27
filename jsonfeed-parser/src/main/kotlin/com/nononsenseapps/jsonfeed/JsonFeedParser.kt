package com.nononsenseapps.jsonfeed

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import okio.Okio
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * A parser for JSONFeeds. CacheDirectory and CacheSize are only relevant if feeds are downloaded. They are not used
 * for parsing JSON directly.
 */
class JsonFeedParser(val cacheDirectory: File? = null,
                     val cacheSize: Long = 10 * 1024 * 1024) {
    val httpClient: OkHttpClient by lazy {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()

        if (cacheDirectory != null) {
            builder.cache(Cache(cacheDirectory, cacheSize))
        }

        builder.connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .trustAllCerts()

        builder.build()
    }

    val jsonAdapter: JsonAdapter<Feed> by lazy {
        val builder: Moshi.Builder = Moshi.Builder()
        val moshi: Moshi = builder.build()
        moshi.adapter(Feed::class.java)
    }

    /**
     * Download a JSONFeed and parse it
     */
    fun parseUrl(inUrl: String): Feed {
        val url: String = when {
            (!inUrl.startsWith("http://") && !inUrl.startsWith("https://")) -> "http://" + inUrl
            else -> inUrl
        }

        val request = Request.Builder()
                .url(url)
                .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Failed to retrieve field: $response")
        }

        response.body().source().inputStream().use {
            return parseJsonStream(Okio.buffer(Okio.source(it)))
        }
    }

    /**
     * Parse a JSONFeed
     */
    fun parseJson(json: String): Feed =
            json.byteInputStream().use { return parseJsonStream(Okio.buffer(Okio.source(it))) }

    /**
     * Parse a JSONFeed
     */
    fun parseJsonStream(json: BufferedSource): Feed {
        val result = jsonAdapter.fromJson(json)

        when {
            result != null -> return result
            else -> throw IOException("Failed to parse JSONFeed")
        }
    }
}


data class Feed(val version: String? = "https://jsonfeed.org/version/1",
                val title: String?,
                val home_page_url: String? = null,
                val feed_url: String? = null,
                val description: String? = null,
                val user_comment: String? = null,
                val next_url: String? = null,
                val icon: String? = null,
                val favicon: String? = null,
                val author: Author? = null,
                val expired: Boolean? = null,
                val hubs: List<Hub>? = null,
                val items: List<Item>?)

data class Author(val name: String? = null,
                  val url: String? = null,
                  val avatar: String? = null)

data class Item(val id: String?,
                val url: String? = null,
                val external_url: String? = null,
                val title: String? = null,
                val content_html: String? = null,
                val content_text: String? = null,
                val summary: String? = null,
                val image: String? = null,
                val banner_image: String? = null,
                val date_published: String? = null,
                val date_modified: String? = null,
                val author: Author? = null,
                val tags: List<String>? = null,
                val attachments: List<Attachment>? = null)

data class Attachment(val url: String?,
                      val mime_type: String? = null,
                      val title: String? = null,
                      val size_in_bytes: Long? = null,
                      val duration_in_seconds: Long? = null)

data class Hub(val type: String?,
               val url: String?)
