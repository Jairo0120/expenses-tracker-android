package com.jldev.expensestracker.android.data.remote

import com.jldev.expensestracker.android.data.remote.api.CyclesApi
import com.jldev.expensestracker.android.data.remote.api.ExpensesApi
import com.jldev.expensestracker.android.data.remote.model.CycleStatus
import com.jldev.expensestracker.android.data.remote.model.ExpenseCreate
import com.jldev.expensestracker.android.data.remote.model.ExpenseSource
import com.jldev.expensestracker.android.data.remote.model.ExpenseUpdate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant
import java.time.LocalDate

class ApiTest {

    private val server = MockWebServer()
    private var tokenProvider = AccessTokenProvider { "test-token" }

    private lateinit var expensesApi: ExpensesApi
    private lateinit var cyclesApi: CyclesApi

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { tokenProvider.accessToken() })
            .build()
        val retrofit = Retrofit.Builder()
            // Mirrors the prod base URL, which has a path segment.
            .baseUrl(server.url("/prod/"))
            .client(client)
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
        expensesApi = retrofit.create(ExpensesApi::class.java)
        cyclesApi = retrofit.create(CyclesApi::class.java)
    }

    @After
    fun tearDown() = server.close()

    private fun enqueueJson(body: String, code: Int = 200) = server.enqueue(
        MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build(),
    )

    @Test
    fun `list expenses parses ExpensePublic and sends filters`() = runBlocking {
        enqueueJson(
            """
            [{
              "description": "Mercado", "val_expense": 125000.0,
              "date_expense": "2026-09-05T01:30:00.123456", "source": "Email", "categories": "",
              "id": 7, "budget": {"id": 3, "description": "Comida", "val_budget": 800000.0},
              "cycle": {"id": 2, "description": "Sept", "start_date": "2026-09-01",
                        "end_date": "2026-09-30", "is_active": true},
              "is_recurrent_expense": false
            }]
            """,
        )

        val expenses = expensesApi.list(cycleId = 2, budgetId = 3, skip = 10)

        val request = server.takeRequest()
        assertEquals("/prod/expenses", request.url.encodedPath)
        assertEquals("2", request.url.queryParameter("cycle_id"))
        assertEquals("3", request.url.queryParameter("budget_id"))
        assertEquals("10", request.url.queryParameter("skip"))
        assertEquals("Bearer test-token", request.headers["Authorization"])

        val expense = expenses.single()
        assertEquals(125000.0, expense.valExpense, 0.0)
        // Naive backend datetimes are UTC.
        assertEquals(Instant.parse("2026-09-05T01:30:00.123456Z"), expense.dateExpense)
        assertEquals(ExpenseSource.Email, expense.source)
        assertEquals("Comida", expense.budget?.description)
        assertEquals(LocalDate.of(2026, 9, 30), expense.cycle.endDate)
    }

    @Test
    fun `omitted query params are not sent`() = runBlocking {
        enqueueJson("[]")
        expensesApi.list()
        val url = server.takeRequest().url
        assertNull(url.queryParameter("cycle_id"))
        assertNull(url.queryParameter("budget_id"))
    }

    @Test
    fun `create expense sends snake_case body with UTC date and no null fields`() = runBlocking {
        enqueueJson(EXPENSE_JSON, code = 201)

        expensesApi.create(
            ExpenseCreate(
                description = "Taxi",
                valExpense = 15000.0,
                dateExpense = Instant.parse("2026-09-05T01:30:00Z"),
                budgetId = 3,
            ),
        )

        val body = ApiJson.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertEquals(
            setOf("description", "val_expense", "date_expense", "budget_id", "create_recurrent_expense"),
            body.keys,
        )
        assertEquals("\"2026-09-05T01:30:00Z\"", body["date_expense"].toString())
    }

    @Test
    fun `patch only sends the changed fields`() = runBlocking {
        enqueueJson(EXPENSE_JSON)
        expensesApi.update(7, ExpenseUpdate(valExpense = 20000.0))
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/prod/expenses/7", request.url.encodedPath)
        assertEquals("""{"val_expense":20000.0}""", request.body!!.utf8())
    }

    @Test
    fun `delete ignores the response body`() = runBlocking {
        enqueueJson("""{"detail": "Expense deleted"}""")
        expensesApi.delete(7)
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `http errors expose FastAPI detail`() = runBlocking {
        enqueueJson("""{"detail": "Cycle not found"}""", code = 404)
        try {
            cyclesApi.getStatus(cycleId = 99)
            fail("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals("Cycle not found", e.toUserMessage())
        }
    }

    @Test
    fun `missing session surfaces as NotAuthenticatedException`() = runBlocking {
        tokenProvider = AccessTokenProvider { error("no credentials") }
        try {
            cyclesApi.list()
            fail("Expected NotAuthenticatedException")
        } catch (e: NotAuthenticatedException) {
            assertTrue(e.toUserMessage().contains("sesión"))
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `cycle status derives spent and available like the Expo app`() {
        val withIncome = CycleStatus(
            totalRecurrentExpenses = 1_000.0, totalExpenses = 300.0, totalIncomes = 5_000.0, totalSavings = 500.0,
        )
        assertEquals(300.0, withIncome.spent, 0.0)
        assertEquals(3_500.0, withIncome.moneyAvailable, 0.0)

        val noIncome = withIncome.copy(totalIncomes = 0.0)
        assertEquals(1_300.0, noIncome.spent, 0.0)
        assertEquals(0.0, noIncome.moneyAvailable, 0.0)
        assertFalse(noIncome.moneyAvailable < 0)
    }

    private companion object {
        const val EXPENSE_JSON = """
            {"description": "Taxi", "val_expense": 15000.0, "date_expense": "2026-09-05T01:30:00",
             "source": "App", "categories": "", "id": 7, "budget": null,
             "cycle": {"id": 2, "description": "Sept", "start_date": "2026-09-01", "end_date": "2026-09-30"},
             "is_recurrent_expense": false}
        """
    }
}
