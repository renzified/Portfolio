package com.rsalanga.knowledgebase.fragment.mvvm.data.store

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

private const val NETWORK_DELAY_MS = 500L
private const val SAVE5_DISCOUNT_CENTS = 500L

data class Product(val id: Long, val name: String, val priceCents: Long)

data class CartLine(val product: Product, val quantity: Int)

data class Cart(val lines: List<CartLine> = emptyList()) {
    val itemCount: Int get() = lines.sumOf { it.quantity }
}

data class Discount(val code: String, val amountCents: Long)

interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>
}

/** FMVVM-V4: one cart, observed by every screen that shows it. */
interface CartRepository {
    fun observeCart(): Flow<Cart>

    suspend fun add(productId: Long)

    suspend fun clear()
}

interface PromoRepository {
    suspend fun validate(code: String): Result<Discount>
}

class InMemoryProductRepository : ProductRepository {
    private val products = MutableStateFlow(
        listOf(
            Product(1, "Notebook", 450),
            Product(2, "Fountain pen", 2_400),
            Product(3, "Desk lamp", 3_900),
            Product(4, "Sticky notes", 300)
        )
    )

    override fun observeProducts(): Flow<List<Product>> = products.asStateFlow()
}

class InMemoryCartRepository(private val products: ProductRepository) : CartRepository {
    private val quantities = MutableStateFlow<Map<Long, Int>>(emptyMap())

    override fun observeCart(): Flow<Cart> =
        combine(products.observeProducts(), quantities) { all, counts ->
            Cart(all.mapNotNull { product -> counts[product.id]?.let { CartLine(product, it) } })
        }

    override suspend fun add(productId: Long) {
        quantities.update { counts -> counts + (productId to (counts[productId] ?: 0) + 1) }
    }

    override suspend fun clear() {
        delay(NETWORK_DELAY_MS)
        quantities.value = emptyMap()
    }
}

/** Accepts "SAVE5" only, so both the success and failure paths can be tried. */
class InMemoryPromoRepository : PromoRepository {
    override suspend fun validate(code: String): Result<Discount> {
        delay(NETWORK_DELAY_MS)
        return if (code.trim().equals("SAVE5", ignoreCase = true)) {
            Result.success(Discount("SAVE5", SAVE5_DISCOUNT_CENTS))
        } else {
            Result.failure(IllegalArgumentException("Unknown code"))
        }
    }
}
