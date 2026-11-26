package com.aura.server.routes

import com.aura.server.auth.AuthComponents
import com.aura.server.plugins.ApiException
import com.aura.server.sales.PembatalanRequest
import com.aura.server.sales.PengajuanPembatalanRequest
import com.aura.server.sales.SalesComponents
import com.aura.server.sales.TransaksiRequest
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

internal fun Route.salesRoutes(auth: AuthComponents, sales: SalesComponents) {
    route("/api/v1/transaksi") {
        authenticate("auth-jwt") {
            get {
                val account = call.authenticatedAccount(auth)
                val response = withContext(Dispatchers.IO) {
                    sales.service.list(
                        account,
                        call.request.queryParameters["hari"],
                        call.request.queryParameters["status"],
                        call.request.queryParameters["limit"]?.let(::parseSalesLimit),
                        call.request.queryParameters["cursor"],
                    )
                }
                call.respond(response)
            }
            post {
                val account = call.authenticatedAccount(auth)
                val response = withContext(Dispatchers.IO) {
                    sales.service.create(account, call.receive<TransaksiRequest>())
                }
                call.respond(HttpStatusCode.Created, response)
            }
            get("/{id}") {
                val account = call.authenticatedAccount(auth)
                val id = call.parameters["id"]?.let(::parseSalesId) ?: invalidSalesId("ID transaksi wajib diisi.")
                val response = withContext(Dispatchers.IO) { sales.service.detail(account, id) }
                call.respond(response)
            }
            post("/{id}/batal") {
                val account = call.authenticatedAccount(auth)
                val id = call.parameters["id"]?.let(::parseSalesId) ?: invalidSalesId("ID transaksi wajib diisi.")
                val response = withContext(Dispatchers.IO) {
                    sales.service.cancel(account, id, call.receive<PembatalanRequest>())
                }
                call.respond(response)
            }
            post("/{id}/ajukan-pembatalan") {
                val account = call.authenticatedAccount(auth)
                val id = call.parameters["id"]?.let(::parseSalesId) ?: invalidSalesId("ID transaksi wajib diisi.")
                val response = withContext(Dispatchers.IO) {
                    sales.service.requestCancellation(account, id, call.receive<PengajuanPembatalanRequest>())
                }
                call.respond(HttpStatusCode.Created, mapOf("notifikasi" to response))
            }
        }
    }
}

private fun parseSalesLimit(value: String): Int = value.toIntOrNull()
    ?: throw ApiException(HttpStatusCode.BadRequest, "LIMIT_TIDAK_VALID", "Jumlah data per halaman tidak valid.")

private fun parseSalesId(value: String): UUID = runCatching { UUID.fromString(value) }.getOrNull()
    ?: invalidSalesId("ID transaksi tidak valid.")

private fun invalidSalesId(message: String): Nothing =
    throw ApiException(HttpStatusCode.BadRequest, "ID_TIDAK_VALID", message)
