package com.aura.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuraRepository(AuraDatabase.get(application))
    val products = repository.products().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = repository.suppliers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = repository.transactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactionItems = repository.transactionItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cashRecords = repository.cashRecords().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val restocks = repository.restocks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val adjustments = repository.adjustments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val movements = repository.movements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activities = repository.activities().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notifications = repository.notifications().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accounts = repository.accounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val store = repository.store().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val session = repository.session().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _cart = MutableStateFlow<List<CartLineData>>(emptyList())
    val cart = _cart.asStateFlow()
    private val _lastInvoice = MutableStateFlow("")
    val lastInvoice = _lastInvoice.asStateFlow()
    private val _lastTotal = MutableStateFlow(0L)
    val lastTotal = _lastTotal.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()

    init { viewModelScope.launch { runCatching { repository.clearLegacyDemoData() }; _ready.value = true } }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try { block() } catch (e: Exception) { _message.value = e.message ?: "Terjadi kesalahan." }
            finally { _busy.value = false }
        }
    }
    fun dismissMessage() { _message.value = null }
    fun showMessage(value: String) { _message.value = value }
    fun clearCart() { _cart.value = emptyList() }
    fun addToCart(product: ProductEntity) {
        if (product.stock <= 0) { _message.value = "Stok ${product.name} sedang habis."; return }
        val current=_cart.value.toMutableList()
        val index=current.indexOfFirst { it.productId==product.id }
        if(index>=0) {
            val line=current[index]
            if(line.quantity+1>product.stock) { _message.value="Jumlah melebihi stok tersedia."; return }
            current[index]=line.copy(quantity=line.quantity+1)
        } else current.add(CartLineData(product.id,product.name,product.sellPrice,1))
        _cart.value=current
    }
    fun changeCart(productId:String, delta:Int) {
        val current=_cart.value.toMutableList()
        val i=current.indexOfFirst { it.productId==productId }; if(i<0)return
        val qty=current[i].quantity+delta
        if(qty<=0) current.removeAt(i)
        else {
            val available=products.value.firstOrNull { it.id==productId }?.stock ?: 0
            if(qty>available) { _message.value="Jumlah melebihi stok tersedia."; return }
            current[i]=current[i].copy(quantity=qty)
        }
        _cart.value=current
    }
    fun checkout(method:String, received:Long, onSuccess:()->Unit = {}) {
        val cartNow=_cart.value
        action {
            _lastTotal.value = cartNow.sumOf { it.unitPrice * it.quantity }
            _lastInvoice.value=repository.checkout(cartNow,method,received)
            _cart.value=emptyList()
            onSuccess()
        }
    }
    fun saveProduct(id:String?,sku:String,name:String,categories:String,unit:String,sell:Long,hpp:Long,stock:Int,minStock:Int) {
        action { repository.saveProduct(id,sku,name,categories,unit,sell,hpp,stock,minStock); _message.value="Barang berhasil disimpan." }
    }
    fun archiveProduct(id:String) { action { repository.archiveProduct(id); _message.value="Barang diarsipkan." } }
    fun restock(productId:String,qty:Int,price:Long,supplierId:String,note:String) {
        action { repository.restock(productId,qty,price,supplierId,note); _message.value="Restock berhasil. Stok, HPP rata-rata, pengeluaran, dan saldo telah diperbarui." }
    }
    fun adjustStock(productId:String,stock:Int,reason:String,note:String) {
        action { repository.adjustStock(productId,stock,reason,note); _message.value="Penyesuaian stok berhasil disimpan." }
    }
    fun expense(category:String,amount:Long,note:String,isPrive:Boolean) {
        action { repository.recordExpense(category,amount,note,isPrive); _message.value="Pengeluaran berhasil dicatat." }
    }
    fun income(category:String,amount:Long,note:String,isCapital:Boolean) {
        action { repository.recordIncome(category,amount,note,isCapital); _message.value="Pemasukan berhasil dicatat." }
    }
    fun cancelTransaction(invoice:String,reason:String,isCashier:Boolean) {
        action { if(isCashier) repository.requestCancel(invoice,reason) else repository.cancelTransaction(invoice,reason); _message.value=if(isCashier) "Pengajuan pembatalan dikirim ke Owner." else "Transaksi berhasil dibatalkan dan stok dikembalikan." }
    }
    fun saveSupplier(id:String?,name:String,phone:String,address:String,note:String) {
        action { repository.saveSupplier(id,name,phone,address,note); _message.value="Supplier berhasil disimpan." }
    }
    fun deleteSupplier(id:String) { action { repository.deleteSupplier(id); _message.value="Supplier dihapus." } }
    fun updateBankInfo(value:String) { action { repository.updateBankInfo(value); _message.value="Informasi rekening diperbarui." } }
    fun updateQris(uri:String) { action { repository.updateQris(uri); _message.value="Gambar QRIS diperbarui." } }
    fun readNotification(id:String) { action { repository.markNotificationRead(id) } }
    fun readAllNotifications() { action { repository.markAllNotificationsRead() } }
}
