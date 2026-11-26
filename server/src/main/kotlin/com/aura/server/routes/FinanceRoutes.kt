package com.aura.server.routes

import com.aura.server.auth.AuthComponents
import com.aura.server.finance.FinanceComponents
import com.aura.server.finance.MetodePembayaranRequest
import com.aura.server.finance.PemasukanRequest
import com.aura.server.finance.PembatalanKeuanganRequest
import com.aura.server.finance.PengeluaranRequest
import com.aura.server.plugins.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

internal fun Route.financeRoutes(auth: AuthComponents, finance: FinanceComponents) {
    route("/api/v1") {
        authenticate("auth-jwt") {
            get("/toko/metode-pembayaran") {
                val account = call.authenticatedAccount(auth)
                val response = withContext(Dispatchers.IO) { finance.service.paymentMethod(account) }
                call.respond(response)
            }
            put("/toko/metode-pembayaran") {
                val account = call.authenticatedAccount(auth)
                val response = withContext(Dispatchers.IO) {
                    finance.service.updatePaymentMethod(account, call.receive<MetodePembayaranRequest>())
                }
                call.respond(mapOf("metodePembayaran" to response))
            }
            route("/pengeluaran") {
                get {
                    val account = call.authenticatedAccount(auth)
                    val response = withContext(Dispatchers.IO) {
                        finance.service.listExpenses(
                            account = account,
                            from = call.request.queryParameters["dari"]?.let(::parseFinanceTimestamp),
                            through = call.request.queryParameters["sampai"]?.let(::parseFinanceTimestamp),
                            type = call.request.queryParameters["jenis"],
                            status = call.request.queryParameters["status"],
                            limit = call.request.queryParameters["limit"]?.let(::parseFinanceLimit),
                            cursor = call.request.queryParameters["cursor"],
                        )
                    }
                    call.respond(response)
                }
                post {
                    val account = call.authenticatedAccount(auth)
                    val response = withContext(Dispatchers.IO) {
                        finance.service.createExpense(account, call.receive<PengeluaranRequest>())
                    }
                    call.respond(HttpStatusCode.Created, response)
                }
                get("/{id}") {
                    val account = call.authenticatedAccount(auth)
                    val id = call.parameters["id"]?.let { parseFinanceId(it, "ID Pengeluaran tidak valid.") }
                        ?: invalidFinanceId("ID Pengeluaran wajib diisi.")
                    val response = withContext(Dispatchers.IO) { finance.service.expenseDetail(account, id) }
                    call.respond(response)
                }
                post("/{id}/batal") {
                    val account = call.authenticatedAccount(auth)
                    val id = call.parameters["id"]?.let { parseFinanceId(it, "ID Pengeluaran tidak valid.") }
                        ?: invalidFinanceId("ID Pengeluaran wajib diisi.")
                    val response = withContext(Dispatchers.IO) {
                        finance.service.cancelExpense(account, id, call.receive<PembatalanKeuanganRequest>())
                    }
                    call.respond(response)
                }
            }
            route("/pemasukan") {
                get {
                    val account = call.authenticatedAccount(auth)
                    val response = withContext(Dispatchers.IO) {
                        finance.service.listIncomes(
                            account = account,
                            from = call.request.queryParameters["dari"]?.let(::parseFinanceTimestamp),
                            through = call.request.queryParameters["sampai"]?.let(::parseFinanceTimestamp),
                            type = call.request.queryParameters["jenis"],
                            status = call.request.queryParameters["status"],
                            limit = call.request.queryParameters["limit"]?.let(::parseFinanceLimit),
                            cursor = call.request.queryParameters["cursor"],
                        )
                    }
                    call.respond(response)
                }
                post {
                    val account = call.authenticatedAccount(auth)
                    val response = withContext(Dispatchers.IO) {
                        finance.service.createIncome(account, call.receive<PemasukanRequest>())
                    }
                    call.respond(HttpStatusCode.Created, response)
                }
                get("/{id}") {
                    val account = call.authenticatedAccount(auth)
                    val id = call.parameters["id"]?.let { parseFinanceId(it, "ID Pemasukan tidak valid.") }
                        ?: invalidFinanceId("ID Pemasukan wajib diisi.")
                    val response = withContext(Dispatchers.IO) { finance.service.incomeDetail(account, id) }
                    call.respond(response)
                }
                post("/{id}/batal") {
                    val account = call.authenticatedAccount(auth)
                    val id = call.parameters["id"]?.let { parseFinanceId(it, "ID Pemasukan tidak valid.") }
                        ?: invalidFinanceId("ID Pemasukan wajib diisi.")
                    val response = withContext(Dispatchers.IO) {
                        finance.service.cancelIncome(account, id, call.receive<PembatalanKeuanganRequest>())
                    }
                    call.respond(response)
                }
            }
        }
    }
}

private fun parseFinanceTimestamp(value: String): Long = value.toLongOrNull()
    ?: throw ApiException(HttpStatusCode.BadRequest, "TANGGAL_TIDAK_VALID", "Rentang tanggal tidak valid.")

private fun parseFinanceLimit(value: String): Int = value.toIntOrNull()
    ?: throw ApiException(HttpStatusCode.BadRequest, "LIMIT_TIDAK_VALID", "Jumlah data per halaman tidak valid.")

private fun parseFinanceId(value: String, message: String): UUID = runCatching { UUID.fromString(value) }.getOrNull()
    ?: invalidFinanceId(message)

private fun invalidFinanceId(message: String): Nothing =
    throw ApiException(HttpStatusCode.BadRequest, "ID_TIDAK_VALID", message)
