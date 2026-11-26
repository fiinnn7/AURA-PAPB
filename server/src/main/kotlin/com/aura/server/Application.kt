package com.aura.server

import com.aura.server.config.AppConfig
import com.aura.server.db.DatabaseFactory
import com.aura.server.auth.authComponents
import com.aura.server.catalog.catalogComponents
import com.aura.server.stock.stockComponents
import com.aura.server.sales.salesComponents
import com.aura.server.finance.financeComponents
import com.aura.server.plugins.configureAuthentication
import com.aura.server.plugins.configureDefaultHeaders
import com.aura.server.plugins.configureLogging
import com.aura.server.plugins.configureSerialization
import com.aura.server.plugins.configureStatusPages
import com.aura.server.routes.configureRouting
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.netty.EngineMain
import io.ktor.server.routing.Routing
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val config = AppConfig.fromEnvironment()
    val dataSource = DatabaseFactory.initialize(config)
    val auth = authComponents(config.jwtSecret)
    val catalog = catalogComponents(config.uploadDirectory)
    val stock = stockComponents(catalog.service)
    val sales = salesComponents()
    val finance = financeComponents()
    val deletionWorker = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "aura-owner-deletion-worker").apply { isDaemon = true }
    }
    deletionWorker.scheduleAtFixedRate(
        { runCatching { auth.repository.purgeExpiredStores() }.onFailure { this.log.error("Scheduled account cleanup failed (${it::class.simpleName ?: "unknown"}).") } },
        0,
        1,
        TimeUnit.MINUTES,
    )
    this.monitor.subscribe(ApplicationStopped) {
        deletionWorker.shutdownNow()
        dataSource.close()
    }
    configureHttp(auth, catalog = catalog, stock = stock, sales = sales, finance = finance)
}

internal fun Application.configureHttp(
    auth: com.aura.server.auth.AuthComponents? = null,
    catalog: com.aura.server.catalog.CatalogComponents? = null,
    additionalRoutes: Routing.() -> Unit = {},
    stock: com.aura.server.stock.StockComponents? = null,
    sales: com.aura.server.sales.SalesComponents? = null,
    finance: com.aura.server.finance.FinanceComponents? = null,
) {
    configureSerialization()
    configureStatusPages()
    auth?.let(::configureAuthentication)
    configureLogging()
    configureDefaultHeaders()
    configureRouting(auth, catalog, additionalRoutes, stock, sales, finance)
}
