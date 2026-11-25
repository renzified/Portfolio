package com.rsalanga.knowledgebase.fragment.mvvm.data

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

private const val NETWORK_DELAY_MS = 700L
private const val DEFAULT_VOLUME = 0.5f
private const val REVENUE_STEP = 125_000L

// ---- Profile (FMVVM-P3) --------------------------------------------------------------------

data class Profile(val name: String, val email: String)

interface ProfileRepository {
    suspend fun loadProfile(): Result<Profile>
}

/** The first load fails, so the Retry path of FMVVM-P3 can be tried. */
class InMemoryProfileRepository : ProfileRepository {
    private val calls = AtomicInteger()

    override suspend fun loadProfile(): Result<Profile> {
        delay(NETWORK_DELAY_MS)
        return if (calls.incrementAndGet() == 1) {
            Result.failure(IOException("Simulated first-load failure"))
        } else {
            Result.success(Profile(name = "Demo user", email = "demo@example.com"))
        }
    }
}

// ---- Orders (FMVVM-S6) ---------------------------------------------------------------------

enum class OrderTab {
    Active,
    Past
}

data class Order(val id: Long, val title: String, val totalCents: Long)

interface OrdersRepository {
    fun observeOrders(tab: OrderTab): Flow<List<Order>>
}

class InMemoryOrdersRepository : OrdersRepository {
    private val active = listOf(Order(11, "Desk lamp", 3_900), Order(12, "Notebook", 450))
    private val past = listOf(Order(1, "Fountain pen", 2_400))

    override fun observeOrders(tab: OrderTab): Flow<List<Order>> = flow {
        delay(NETWORK_DELAY_MS)
        emit(if (tab == OrderTab.Active) active else past)
    }
}

// ---- Search (FMVVM-I4) ---------------------------------------------------------------------

interface SearchRepository {
    suspend fun search(query: String): Result<List<String>>
}

/** Searching for "error" fails, to show the error phase. */
class InMemorySearchRepository : SearchRepository {
    private val fruits = listOf(
        "Apple", "Apricot", "Banana", "Blackberry", "Blueberry", "Cherry", "Grape", "Kiwi",
        "Lemon", "Lime", "Mango", "Melon", "Orange", "Papaya", "Peach", "Pear", "Plum"
    )

    override suspend fun search(query: String): Result<List<String>> {
        delay(NETWORK_DELAY_MS)
        return if (query.equals("error", ignoreCase = true)) {
            Result.failure(IOException("Simulated search failure"))
        } else {
            Result.success(fruits.filter { it.contains(query, ignoreCase = true) })
        }
    }
}

// ---- Auth (FMVVM-I1) -----------------------------------------------------------------------

interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<Unit>
}

/** "taken@example.com" is already registered. */
class InMemoryAuthRepository : AuthRepository {
    override suspend fun signUp(email: String, password: String): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        return if (email.equals("taken@example.com", ignoreCase = true)) {
            Result.failure(IllegalStateException("Email taken"))
        } else {
            Result.success(Unit)
        }
    }
}

// ---- Report (FMVVM-P8) ---------------------------------------------------------------------

data class ReportRow(val month: String, val revenueCents: Long)

interface ReportRepository {
    suspend fun rows(): List<ReportRow>
}

class InMemoryReportRepository : ReportRepository {
    private val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    override suspend fun rows(): List<ReportRow> {
        delay(NETWORK_DELAY_MS)
        return months.mapIndexed { index, month -> ReportRow(month, (index + 1) * REVENUE_STEP) }
    }
}

// ---- Settings (FMVVM-I2) -------------------------------------------------------------------

interface SettingsRepository {
    fun observeVolume(): Flow<Float>

    suspend fun setVolume(volume: Float)
}

class InMemorySettingsRepository : SettingsRepository {
    private val volume = MutableStateFlow(DEFAULT_VOLUME)

    override fun observeVolume(): Flow<Float> = volume.asStateFlow()

    override suspend fun setVolume(volume: Float) {
        this.volume.value = volume
    }
}
