package com.aura.server.routes

import com.aura.server.auth.AuthComponents
import com.aura.server.plugins.ApiException
import com.aura.server.stock.PenyesuaianStokRequest
import com.aura.server.stock.StockComponents
import com.aura.server.stock.RestockRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

internal fun Route.stockRoutes(auth: AuthComponents, stock: StockComponents) {
    route("/api/v1") {
        authenticate("auth-jwt") {
            post("/restock") {
                val account = call.authenticatedAccount(auth)
                val response = withContext(Dispatchers.IO) {
                    stock.service.createRestock(account, call.receive<RestockRequest>())
                }
                call.respond(HttpStatusCode.Created, response)
            }
            post("/penyesuaian-stok") {
                val account = call.authenticatedAccount(auth)
                val response = withContext(Dispatchers.IO) {
                    stock.service.createAdjustment(account, call.receive<PenyesuaianStokRequest>())
                }
                call.respond(HttpStatusCode.Created, response)
            }
            get("/pergerakan-stok") {
                val account = call.authenticatedAccount(auth)
                val response = withContext(Dispatchers.IO) {
                    stock.service.listMovements(
                        account = account,
                        from = call.request.queryParameters["dari"]?.let(::parseTimestamp),
                        through = call.request.queryParameters["sampai"]?.let(::parseTimestamp),
                        type = call.request.queryParameters["jenis"],
                        barangId = call.request.queryParameters["barangId"]?.let { parseId(it, "ID Barang tidak valid.") },
                        limit = call.request.queryParameters["limit"]?.let(::parseLimit),
                        cursor = call.request.queryParameters["cursor"],
                    )
                }
                call.respond(response)
            }
            get("/barang/{barangId}/kartu-stok") {
                val account = call.authenticatedAccount(auth)
                val barangId = call.parameters["barangId"]?.let { parseId(it, "ID Barang tidak valid.") }
                    ?: invalidId("ID Barang wajib diisi.")
                val response = withContext(Dispatchers.IO) {
                    stock.service.listCardMovements(
                        account = account,
                        barangId = barangId,
                        from = call.request.queryParameters["dari"]?.let(::parseTimestamp),
                        through = call.request.queryParameters["sampai"]?.let(::parseTimestamp),
                        limit = call.request.queryParameters["limit"]?.let(::parseLimit),
                        cursor = call.request.queryParameters["cursor"],
                    )
                }
                call.respond(response)
            }
        }
    }
}

private fun parseTimestamp(value: String): Long = value.toLongOrNull()
    ?: throw ApiException(HttpStatusCode.BadRequest, "TANGGAL_TIDAK_VALID", "Rentang tanggal tidak valid.")

private fun parseLimit(value: String): Int = value.toIntOrNull()
    ?: throw ApiException(HttpStatusCode.BadRequest, "LIMIT_TIDAK_VALID", "Jumlah data per halaman tidak valid.")

private fun parseId(value: String, message: String): UUID = runCatching { UUID.fromString(value) }.getOrNull()
    ?: invalidId(message)

private fun invalidId(message: String): Nothing =
    throw ApiException(HttpStatusCode.BadRequest, "ID_TIDAK_VALID", message)
