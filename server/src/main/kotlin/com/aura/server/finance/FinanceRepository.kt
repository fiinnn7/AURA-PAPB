package com.aura.server.finance

import com.aura.server.auth.Akun
import java.util.UUID

internal interface FinanceRepository {
    fun createExpense(account: Akun, input: PengeluaranInput, requestHash: String): PengeluaranCreateResponse
    fun listExpenses(
        account: Akun,
        from: Long?,
        through: Long?,
        type: String?,
        status: String?,
        limit: Int,
        cursor: FinanceExpenseCursor?,
    ): FinanceExpensePage
    fun expenseDetail(storeId: String, id: UUID): PengeluaranResponse?
    fun cancelExpense(account: Akun, id: UUID, reason: String): PengeluaranCancelResponse
    fun createIncome(account: Akun, input: PemasukanInput, requestHash: String): PemasukanCreateResponse
    fun listIncomes(
        account: Akun,
        from: Long?,
        through: Long?,
        type: String?,
        status: String?,
        limit: Int,
        cursor: FinanceIncomeCursor?,
    ): FinanceIncomePage
    fun incomeDetail(storeId: String, id: UUID): PemasukanResponse?
    fun cancelIncome(account: Akun, id: UUID, reason: String): PemasukanCancelResponse
    fun paymentMethod(storeId: String): MetodePembayaranResponse?
    fun updatePaymentMethod(account: Akun, request: MetodePembayaranRequest): MetodePembayaranResponse
}
