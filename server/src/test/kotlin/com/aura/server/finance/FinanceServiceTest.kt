package com.aura.server.finance

import com.aura.server.auth.Akun
import com.aura.server.auth.Peran
import com.aura.server.auth.StatusAkun
import com.aura.server.plugins.ApiException
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FinanceServiceTest {
    @Test
    fun `cashier cannot create expense or update payment settings`() {
        val repository = FakeFinanceRepository()
        val service = FinanceService(repository)
        val cashier = account(Peran.KASIR)

        assertEquals(
            HttpStatusCode.Forbidden,
            assertFailsWith<ApiException> {
                service.createExpense(cashier, expenseRequest())
            }.status,
        )
        assertEquals(
            HttpStatusCode.Forbidden,
            assertFailsWith<ApiException> {
                service.updatePaymentMethod(cashier, MetodePembayaranRequest())
            }.status,
        )
        assertEquals(0, repository.calls)
    }

    @Test
    fun `custom finance entries allow past dates and reject future dates`() {
        val repository = FakeFinanceRepository()
        val service = FinanceService(repository)
        val owner = account(Peran.OWNER)
        val pastDate = System.currentTimeMillis() - 60_000

        service.createExpense(owner, expenseRequest(tanggal = pastDate))

        assertEquals(pastDate, repository.lastExpenseInput?.tanggal)
        assertEquals(
            "TANGGAL_MASA_DEPAN_TIDAK_DIIJINKAN",
            assertFailsWith<ApiException> {
                service.createIncome(
                    owner,
                    PemasukanRequest(
                        id = UUID.randomUUID(),
                        judul = "Pemasukan lain",
                        nominal = 100,
                        tanggal = System.currentTimeMillis() + 60_000,
                    ),
                )
            }.kode,
        )
        assertEquals(1, repository.calls)
    }

    private fun expenseRequest(tanggal: Long? = null) = PengeluaranRequest(
        id = UUID.randomUUID(),
        judul = "Listrik",
        jenisBiaya = "LISTRIK",
        nominal = 100,
        tanggal = tanggal,
    )

    private fun account(role: Peran) = Akun(
        id = UUID.randomUUID(),
        idToko = "STORE001",
        email = if (role == Peran.OWNER) "owner@example.test" else null,
        nama = if (role == Peran.OWNER) "Owner" else "Kasir",
        namaNormalisasi = if (role == Peran.OWNER) "owner" else "kasir",
        peran = role,
        hash = "hash",
        salt = "salt",
        status = StatusAkun.AKTIF,
        wajibGantiSandi = false,
        nomorHp = null,
        fotoFileId = null,
        gagalLogin = 0,
        terkunciSampai = null,
        terakhirLogin = null,
    )

    private class FakeFinanceRepository : FinanceRepository {
        var calls = 0
        var lastExpenseInput: PengeluaranInput? = null

        override fun createExpense(account: Akun, input: PengeluaranInput, requestHash: String): PengeluaranCreateResponse {
            calls++
            lastExpenseInput = input
            return PengeluaranCreateResponse(
                pengeluaran = PengeluaranResponse(
                    id = input.id,
                    jenis = "KUSTOM",
                    jenisBiaya = input.jenisBiaya,
                    judul = input.judul,
                    barangId = null,
                    supplierId = null,
                    jumlah = null,
                    hargaPerItem = null,
                    nominal = input.nominal,
                    tanggal = input.tanggal,
                    catatan = input.catatan,
                    buktiFileId = input.buktiFileId,
                    status = "TERCATAT",
                    alasanPembatalan = null,
                    referensiId = null,
                    jenisReferensi = null,
                    dibuatOleh = account.id,
                    dibuatPada = input.tanggal,
                    dibatalkanOleh = null,
                    dibatalkanPada = null,
                ),
                saldoKumulatif = 0,
            )
        }

        override fun listExpenses(
            account: Akun,
            from: Long?,
            through: Long?,
            type: String?,
            status: String?,
            limit: Int,
            cursor: FinanceExpenseCursor?,
        ): FinanceExpensePage = unused()

        override fun expenseDetail(storeId: String, id: UUID): PengeluaranResponse? = unused()

        override fun cancelExpense(account: Akun, id: UUID, reason: String): PengeluaranCancelResponse = unused()

        override fun createIncome(account: Akun, input: PemasukanInput, requestHash: String): PemasukanCreateResponse = unused()

        override fun listIncomes(
            account: Akun,
            from: Long?,
            through: Long?,
            type: String?,
            status: String?,
            limit: Int,
            cursor: FinanceIncomeCursor?,
        ): FinanceIncomePage = unused()

        override fun incomeDetail(storeId: String, id: UUID): PemasukanResponse? = unused()

        override fun cancelIncome(account: Akun, id: UUID, reason: String): PemasukanCancelResponse = unused()

        override fun paymentMethod(storeId: String): MetodePembayaranResponse? = unused()

        override fun updatePaymentMethod(account: Akun, request: MetodePembayaranRequest): MetodePembayaranResponse = unused()

        private fun <T> unused(): T = error("Unexpected repository call.")
    }
}
