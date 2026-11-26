package com.aura.server.routes

import com.aura.server.auth.AuthComponents
import com.aura.server.catalog.CatalogComponents
import com.aura.server.stock.StockComponents
import com.aura.server.sales.SalesComponents
import com.aura.server.finance.FinanceComponents
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

internal data class HealthResponse(val status: String)

internal fun Application.configureRouting(
    auth: AuthComponents? = null,
    catalog: CatalogComponents? = null,
    additionalRoutes: Routing.() -> Unit = {},
    stock: StockComponents? = null,
    sales: SalesComponents? = null,
    finance: FinanceComponents? = null,
) {
    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }
        auth?.let { authRoutes(it) }
        if (auth != null && catalog != null) catalogRoutes(auth, catalog)
        if (auth != null && stock != null) stockRoutes(auth, stock)
        if (auth != null && sales != null) salesRoutes(auth, sales)
        if (auth != null && finance != null) financeRoutes(auth, finance)
        additionalRoutes()
    }
}
