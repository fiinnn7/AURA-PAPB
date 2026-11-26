package com.aura.app.data

import androidx.room.withTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AuraRepository(private val db: AuraDatabase) {
    private val dao = db.dao()
    private fun id() = UUID.randomUUID().toString()
    private fun todayCode(): String = SimpleDateFormat("yyMMdd", Locale.US).format(Date())

    suspend fun clearLegacyDemoData() = db.withTransaction {
        dao.clearLegacyTransactionItems()
        dao.clearLegacyTransactions()
        dao.clearLegacyProducts()
        dao.clearLegacySuppliers()
        dao.clearLegacyCashRecords()
        dao.clearLegacyRestocks()
        dao.clearLegacyAdjustments()
        dao.clearLegacyMovements()
        dao.clearLegacyActivities()
        dao.clearLegacyNotifications()
        dao.clearLegacyAccounts()
        dao.clearLegacySessions()
        dao.clearLegacyStores()
    }

    private suspend fun log(actor: String, action: String, detail: String) {
        dao.putActivity(ActivityLogEntity(id(), actor, action, detail, System.currentTimeMillis()))
    }

    private suspend fun currentStore(): StoreEntity? = dao.store() ?: dao.anyStore()

    suspend fun saveProduct(id: String?, sku: String, name: String, categories: String, unit: String,
                            sellPrice: Long, hpp: Long, stock: Int, minStock: Int) = db.withTransaction {
        require(name.isNotBlank() && sku.isNotBlank() && sellPrice >= 0 && stock >= 0)
        val key = id ?: "BRG-${UUID.randomUUID().toString().take(8).uppercase()}"
        val old = id?.let { dao.product(it) }
        dao.putProduct(ProductEntity(key, sku, name, categories, unit, sellPrice, hpp, stock, minStock,
            old?.sold30Days ?: 0))
        log(dao.session()?.name ?: "Owner", if (old == null) "Tambah barang" else "Edit barang", name)
    }
    suspend fun archiveProduct(productId: String) = db.withTransaction {
        dao.archiveProduct(productId)
        log(dao.session()?.name ?: "Owner", "Arsip barang", productId)
    }
    suspend fun restock(productId: String, quantity: Int, purchasePrice: Long, supplierId: String, note: String) = db.withTransaction {
        require(quantity > 0 && purchasePrice >= 0)
        val p = dao.product(productId) ?: error("Barang tidak ditemukan")
        val store = currentStore() ?: error("Toko tidak ditemukan")
        val newStock = p.stock + quantity
        val newHpp = if (newStock == 0) purchasePrice else
            ((p.stock.toLong() * p.hpp) + (quantity.toLong() * purchasePrice)) / newStock
        val total = quantity.toLong() * purchasePrice
        dao.putProduct(p.copy(stock = newStock, hpp = newHpp))
        val now = System.currentTimeMillis()
        val ref = "RST-${todayCode()}-${now.toString().takeLast(5)}"
        dao.putRestock(RestockEntity(ref, productId, supplierId, quantity, purchasePrice, total, now, note))
        dao.changeBalance(store.id, -total)
        dao.putMovement(StockMovementEntity(id(), productId, p.name, "Restock", quantity, p.stock, newStock, ref, now))
        log(dao.session()?.name ?: "Owner", "Restock barang", "${p.name}: +$quantity")
        pruneRestocks()
    }
    suspend fun adjustStock(productId: String, newStock: Int, reason: String, note: String) = db.withTransaction {
        require(newStock >= 0 && reason.isNotBlank())
        val p = dao.product(productId) ?: error("Barang tidak ditemukan")
        val diff = newStock - p.stock
        val loss = if (diff < 0 && reason in listOf("Rusak", "Hilang", "Kadaluarsa")) (-diff).toLong() * p.hpp else 0L
        val now = System.currentTimeMillis()
        dao.putProduct(p.copy(stock = newStock))
        dao.putAdjustment(StockAdjustmentEntity(id(), productId, p.stock, newStock, diff, reason, note, loss, now))
        dao.putMovement(StockMovementEntity(id(), productId, p.name, "Penyesuaian: $reason", diff, p.stock, newStock, "ADJ-${now}", now))
        log(dao.session()?.name ?: "Owner", "Penyesuaian stok", "${p.name}: ${p.stock} → $newStock ($reason)")
        val store = dao.store()
        if (newStock <= p.minStock) dao.putNotification(NotificationEntity(id(), "Stok ${p.name} perlu diperhatikan",
            "Stok saat ini $newStock ${p.unit}.", "stock", now))
        pruneAdjustments()
    }
    suspend fun recordExpense(category: String, amount: Long, note: String, isPrive: Boolean) = db.withTransaction {
        require(category.isNotBlank() && amount > 0)
        val store = currentStore() ?: error("Toko tidak ditemukan")
        val now = System.currentTimeMillis()
        dao.putCashRecord(CashRecordEntity(id(), "expense", category, amount, note, now, isPrive = isPrive))
        dao.changeBalance(store.id, -amount)
        log(dao.session()?.name ?: "Owner", "Catat pengeluaran", "$category: $amount")
        pruneCash("expense")
    }
    suspend fun recordIncome(category: String, amount: Long, note: String, isCapital: Boolean) = db.withTransaction {
        require(category.isNotBlank() && amount > 0)
        val store = currentStore() ?: error("Toko tidak ditemukan")
        val now = System.currentTimeMillis()
        dao.putCashRecord(CashRecordEntity(id(), "income", category, amount, note, now, isCapital = isCapital))
        dao.changeBalance(store.id, amount)
        log(dao.session()?.name ?: "Owner", "Catat pemasukan", "$category: $amount")
        pruneCash("income")
    }
    suspend fun checkout(cart: List<CartLineData>, method: String, received: Long): String = db.withTransaction {
        require(cart.isNotEmpty())
        val store = currentStore() ?: error("Toko tidak ditemukan")
        val current = cart.map { line ->
            val p = dao.product(line.productId) ?: error("Barang ${line.name} tidak ditemukan")
            require(p.stock >= line.quantity) { "Stok ${p.name} tidak mencukupi. Tersedia ${p.stock}." }
            p
        }
        val total = cart.sumOf { it.unitPrice * it.quantity }
        if (method == "Tunai") require(received >= total) { "Uang diterima kurang dari total tagihan." }
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val sequence = dao.transactionCount(startOfDay, calendar.timeInMillis) + 1
        val invoice = "INV-${todayCode()}-${sequence.toString().padStart(4,'0')}"
        val cashier = dao.session()?.name ?: "Kasir"
        dao.putTransaction(TransactionEntity(invoice, now, cashier, method, total, "Selesai"))
        dao.putTransactionItems(cart.map { line ->
            val p = current.first { it.id == line.productId }
            TransactionItemEntity(invoiceNo=invoice, productId=p.id, productName=p.name,
                quantity=line.quantity, unitPrice=line.unitPrice, unitCost=p.hpp)
        })
        cart.forEach { line ->
            val p = dao.product(line.productId)!!
            val after = p.stock - line.quantity
            dao.putProduct(p.copy(stock = after, sold30Days = p.sold30Days + line.quantity))
            dao.putMovement(StockMovementEntity(id(), p.id, p.name, "Penjualan", -line.quantity, p.stock, after, invoice, now))
            if (after <= p.minStock) dao.putNotification(NotificationEntity(id(), "Stok ${p.name} menipis",
                "Sisa stok $after ${p.unit}. Segera lakukan restock.", "stock", now))
        }
        dao.changeBalance(store.id, total)
        log(cashier, "Transaksi berhasil", "$invoice • $total")
        pruneTransactions()
        invoice
    }
    suspend fun requestCancel(invoice: String, reason: String) = db.withTransaction {
        require(reason.isNotBlank())
        val tx = dao.transaction(invoice) ?: error("Transaksi tidak ditemukan")
        require(tx.status == "Selesai") { "Transaksi tidak dapat diajukan." }
        dao.putTransaction(tx.copy(status = "Perlu Ditinjau", cancelRequestReason = reason))
        dao.putNotification(NotificationEntity(id(), "Pengajuan pembatalan transaksi",
            "$invoice: $reason", "transaction", System.currentTimeMillis()))
        log(dao.session()?.name ?: "Kasir", "Ajukan pembatalan", invoice)
    }
    suspend fun cancelTransaction(invoice: String, reason: String) = db.withTransaction {
        require(reason.isNotBlank())
        val tx = dao.transaction(invoice) ?: error("Transaksi tidak ditemukan")
        require(tx.status == "Selesai" || tx.status == "Perlu Ditinjau") { "Transaksi sudah dibatalkan." }
        val store = currentStore() ?: error("Toko tidak ditemukan")
        dao.transactionItems(invoice).forEach { item ->
            val p = dao.product(item.productId) ?: return@forEach
            val after = p.stock + item.quantity
            dao.putProduct(p.copy(stock = after, sold30Days = (p.sold30Days-item.quantity).coerceAtLeast(0)))
            dao.putMovement(StockMovementEntity(id(), p.id, p.name, "Pembatalan transaksi", item.quantity,
                p.stock, after, invoice, System.currentTimeMillis()))
        }
        dao.putTransaction(tx.copy(status = "Dibatalkan", cancelReason = reason))
        if (tx.balancePosted) dao.changeBalance(store.id, -tx.total)
        log(dao.session()?.name ?: "Owner", "Batalkan transaksi", "$invoice • $reason")
    }
    suspend fun saveSupplier(id: String?, name: String, phone: String, address: String, note: String) = db.withTransaction {
        require(name.isNotBlank())
        val key = id ?: "SUP-${UUID.randomUUID().toString().take(8)}"
        dao.putSupplier(SupplierEntity(key, name, phone, address, note))
        log(dao.session()?.name ?: "Owner", "Simpan supplier", name)
    }
    suspend fun deleteSupplier(id: String) = db.withTransaction {
        dao.deleteSupplier(id)
        log(dao.session()?.name ?: "Owner", "Hapus supplier", id)
    }
    suspend fun updateBankInfo(info: String) = db.withTransaction {
        val store = currentStore() ?: return@withTransaction
        dao.updateBankInfo(store.id, info)
        log(dao.session()?.name ?: "Owner", "Ubah rekening bank", "Informasi rekening diperbarui")
    }
    suspend fun updateQris(uri: String) = db.withTransaction {
        val store = currentStore() ?: return@withTransaction
        dao.updateQris(store.id, uri)
        log(dao.session()?.name ?: "Owner", "Ubah QRIS", "Gambar QRIS diperbarui")
    }
    suspend fun markNotificationRead(id: String) = dao.readNotification(id)
    suspend fun markAllNotificationsRead() = dao.readAllNotifications()
    suspend fun notificationRead(id: String) = dao.readNotification(id)

    private suspend fun pruneCash(kind: String) {
        val ids = dao.oldCashIds(kind); if (ids.isNotEmpty()) dao.deleteCashRecords(ids)
    }
    private suspend fun pruneTransactions() {
        val ids = dao.oldTransactionIds()
        if (ids.isNotEmpty()) { dao.deleteTransactionItems(ids); dao.deleteTransactions(ids) }
    }
    private suspend fun pruneRestocks() { val ids=dao.oldRestockIds(); if(ids.isNotEmpty()) dao.deleteRestocks(ids) }
    private suspend fun pruneAdjustments() { val ids=dao.oldAdjustmentIds(); if(ids.isNotEmpty()) dao.deleteAdjustments(ids) }

    fun products() = dao.observeProducts()
    fun suppliers() = dao.observeSuppliers()
    fun transactions() = dao.observeTransactions()
    fun transactionItems() = dao.observeAllTransactionItems()
    fun cashRecords() = dao.observeCashRecords()
    fun restocks() = dao.observeRestocks()
    fun adjustments() = dao.observeAdjustments()
    fun movements() = dao.observeMovements()
    fun activities() = dao.observeActivities()
    fun notifications() = dao.observeNotifications()
    fun accounts() = dao.observeAccounts()
    fun store() = dao.observeStore()
    fun session() = dao.observeSession()
    suspend fun items(invoice: String) = dao.transactionItems(invoice)
}

data class CartLineData(val productId: String, val name: String, val unitPrice: Long, val quantity: Int)
