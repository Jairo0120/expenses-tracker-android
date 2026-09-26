package com.jldev.expensestracker.android.data.remote

import com.jldev.expensestracker.android.data.sync.SyncApi
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Auth and error handling around the API client. */
class ApiTest {

    private val server = MockWebServer()
    private var tokenProvider = AccessTokenProvider { "test-token" }
    private lateinit var api: SyncApi

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { tokenProvider.accessToken() })
            .build()
        api = Retrofit.Builder()
            // Mirrors the prod base URL, which has a path segment.
            .baseUrl(server.url("/prod/"))
            .client(client)
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SyncApi::class.java)
    }

    @After
    fun tearDown() = server.close()

    private fun enqueueJson(body: String, code: Int = 200) = server.enqueue(
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build(),
    )

    @Test
    fun `requests carry the access token`() = runBlocking {
        enqueueJson("""{"cursor": 0, "has_more": false, "changes": {}}""")
        api.pull(since = 0)
        val request = server.takeRequest()
        assertEquals("/prod/sync", request.url.encodedPath)
        assertEquals("Bearer test-token", request.headers["Authorization"])
    }

    @Test
    fun `http errors expose FastAPI detail`() = runBlocking {
        enqueueJson("""{"detail": "Could not validate credentials"}""", code = 401)
        try {
            api.pull(since = 0)
            fail("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals("Could not validate credentials", e.toUserMessage())
        }
    }

    @Test
    fun `missing session surfaces as NotAuthenticatedException`() = runBlocking {
        tokenProvider = AccessTokenProvider { error("no credentials") }
        try {
            api.pull(since = 0)
            fail("Expected NotAuthenticatedException")
        } catch (e: NotAuthenticatedException) {
            assertTrue(e.toUserMessage().contains("sesión"))
        }
        assertEquals(0, server.requestCount)
    }
}
