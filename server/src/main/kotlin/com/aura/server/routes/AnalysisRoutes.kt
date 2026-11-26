package com.aura.server.routes

import com.aura.server.analysis.AnalysisComponents
import com.aura.server.auth.AuthComponents
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun Route.analysisRoutes(auth: AuthComponents, analysis: AnalysisComponents) {
    route("/api/v1/analisis") {
        authenticate("auth-jwt") {
            get("/ringkasan") {
                val account = call.authenticatedAccount(auth)
                val periode = call.request.queryParameters["periode"]
                val response = withContext(Dispatchers.IO) { analysis.service.overview(account, periode) }
                call.respond(response)
            }
            get("/pergerakan") {
                val account = call.authenticatedAccount(auth)
                val periode = call.request.queryParameters["periode"]
                val jenis = call.request.queryParameters["jenis"]
                val response = withContext(Dispatchers.IO) { analysis.service.movement(account, periode, jenis) }
                call.respond(response)
            }
            get("/umur-stok") {
                val account = call.authenticatedAccount(auth)
                val periode = call.request.queryParameters["periode"]
                val response = withContext(Dispatchers.IO) { analysis.service.stockRunway(account, periode) }
                call.respond(response)
            }
            get("/proporsi") {
                val account = call.authenticatedAccount(auth)
                val periode = call.request.queryParameters["periode"]
                val jenis = call.request.queryParameters["jenis"]
                val response = withContext(Dispatchers.IO) { analysis.service.proportion(account, periode, jenis) }
                call.respond(response)
            }
            get("/tren") {
                val account = call.authenticatedAccount(auth)
                val periode = call.request.queryParameters["periode"]
                val arah = call.request.queryParameters["arah"]
                val response = withContext(Dispatchers.IO) { analysis.service.trend(account, periode, arah) }
                call.respond(response)
            }
            get("/korelasi") {
                val account = call.authenticatedAccount(auth)
                val periode = call.request.queryParameters["periode"]
                val response = withContext(Dispatchers.IO) { analysis.service.correlations(account, periode) }
                call.respond(response)
            }
        }
    }
}
