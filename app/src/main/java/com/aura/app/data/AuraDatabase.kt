package com.aura.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "accounts", indices = [Index(value = ["storeId", "username"], unique = true)])
data class AccountEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val name: String,
    val email: String,
    val username: String,
    val passwordHash: String,
    val role: String,
    val active: Boolean = true,
    val phone: String = ""
)

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val businessType: String,
    val balance: Long,
    val bankInfo: String = "",
    val qrisImageUri: String = ""
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: Int = 1,
    val accountId: String,
    val role: String,
    val name: String,
    val storeId: String
)

@Entity(tableName = "products", indices = [Index("sku")])
data class ProductEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val name: String,
    val categories: String,
    val unit: String,
    val sellPrice: Long,
    val hpp: Long,
    val stock: Int,
    val minStock: Int,
    val sold30Days: Int = 0,
    val archived: Boolean = false
) {
    val status: String get() = when {
        stock <= 0 -> "Habis"
        stock <= minStock -> "Menipis"
        else -> "Aman"
    }
}

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val address: String,
    val note: String = ""
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val invoiceNo: String,
    val createdAt: Long,
    val cashierName: String,
    val paymentMethod: String,
    val total: Long,
    val status: String,
    val cancelReason: String = "",
    val cancelRequestReason: String = "",
    val balancePosted: Boolean = true
)

@Entity(tableName = "transaction_items", indices = [Index("invoiceNo")])
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNo: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long,
    val unitCost: Long
)

@Entity(tableName = "cash_records", indices = [Index("kind"), Index("createdAt")])
data class CashRecordEntity(
    @PrimaryKey val id: String,
    val kind: String, // expense | income
    val category: String,
    val amount: Long,
    val note: String,
    val createdAt: Long,
    val isPrive: Boolean = false,
    val isCapital: Boolean = false
)

@Entity(tableName = "restocks")
data class RestockEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val supplierId: String,
    val quantity: Int,
    val purchasePrice: Long,
    val total: Long,
    val createdAt: Long,
    val note: String
)

@Entity(tableName = "stock_adjustments")
data class StockAdjustmentEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val beforeStock: Int,
    val afterStock: Int,
    val difference: Int,
    val reason: String,
    val note: String,
    val lossValue: Long,
    val createdAt: Long
)

@Entity(tableName = "stock_movements", indices = [Index("createdAt")])
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val productName: String,
    val type: String,
    val quantity: Int,
    val beforeStock: Int,
    val afterStock: Int,
    val reference: String,
    val createdAt: Long
)

@Entity(tableName = "activity_logs", indices = [Index("createdAt")])
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val actor: String,
    val action: String,
    val detail: String,
    val createdAt: Long
)

@Entity(tableName = "notifications", indices = [Index("createdAt")])
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: String,
    val createdAt: Long,
    val isRead: Boolean = false
)

@Dao
interface AuraDao {
    @Query("SELECT * FROM accounts WHERE email = :email AND passwordHash = :hash AND role = 'owner' AND active = 1 LIMIT 1")
    suspend fun ownerLogin(email: String, hash: String): AccountEntity?
    @Query("SELECT * FROM accounts WHERE storeId = :storeId AND username = :username AND passwordHash = :hash AND role = 'kasir' AND active = 1 LIMIT 1")
    suspend fun cashierLogin(storeId: String, username: String, hash: String): AccountEntity?
    @Query("SELECT * FROM accounts ORDER BY role, name") fun observeAccounts(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1") suspend fun account(id: String): AccountEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAccount(value: AccountEntity)
    @Query("UPDATE accounts SET passwordHash = :hash WHERE id = :id") suspend fun resetPassword(id: String, hash: String)
    @Query("UPDATE accounts SET active = :active WHERE id = :id") suspend fun setAccountActive(id: String, active: Boolean)

    @Query("SELECT * FROM stores LIMIT 1") suspend fun anyStore(): StoreEntity?
    @Query("SELECT * FROM stores WHERE id = (SELECT storeId FROM sessions WHERE id = 1) LIMIT 1") suspend fun store(): StoreEntity?
    @Query("SELECT stores.* FROM stores INNER JOIN sessions ON stores.id = sessions.storeId WHERE sessions.id = 1 LIMIT 1") fun observeStore(): Flow<StoreEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putStore(value: StoreEntity)
    @Query("UPDATE stores SET balance = balance + :delta WHERE id = :storeId") suspend fun changeBalance(storeId: String, delta: Long)
    @Query("UPDATE stores SET bankInfo = :info WHERE id = :storeId") suspend fun updateBankInfo(storeId: String, info: String)
    @Query("UPDATE stores SET qrisImageUri = :uri WHERE id = :storeId") suspend fun updateQris(storeId: String, uri: String)

    @Query("SELECT * FROM sessions WHERE id = 1 LIMIT 1") fun observeSession(): Flow<SessionEntity?>
    @Query("SELECT * FROM sessions WHERE id = 1 LIMIT 1") suspend fun session(): SessionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSession(value: SessionEntity)
    @Query("DELETE FROM sessions") suspend fun clearSession()

    @Query("SELECT * FROM products WHERE archived = 0 ORDER BY name") fun observeProducts(): Flow<List<ProductEntity>>
    @Query("SELECT * FROM products WHERE archived = 0 ORDER BY name") suspend fun products(): List<ProductEntity>
    @Query("SELECT * FROM products WHERE id = :id LIMIT 1") suspend fun product(id: String): ProductEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putProduct(value: ProductEntity)
    @Query("UPDATE products SET archived = 1 WHERE id = :id") suspend fun archiveProduct(id: String)
    @Query("UPDATE products SET stock = :stock WHERE id = :id") suspend fun updateStock(id: String, stock: Int)
    @Query("SELECT COUNT(*) FROM products") suspend fun productCount(): Int

    @Query("SELECT * FROM suppliers ORDER BY name") fun observeSuppliers(): Flow<List<SupplierEntity>>
    @Query("SELECT * FROM suppliers ORDER BY name") suspend fun suppliers(): List<SupplierEntity>
    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1") suspend fun supplier(id: String): SupplierEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSupplier(value: SupplierEntity)
    @Query("DELETE FROM suppliers WHERE id = :id") suspend fun deleteSupplier(id: String)

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC") fun observeTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM transactions WHERE invoiceNo = :invoice LIMIT 1") suspend fun transaction(invoice: String): TransactionEntity?
    @Query("SELECT COUNT(*) FROM transactions WHERE createdAt >= :start AND createdAt < :end") suspend fun transactionCount(start: Long, end: Long): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTransaction(value: TransactionEntity)
    @Query("SELECT * FROM transaction_items WHERE invoiceNo = :invoice") suspend fun transactionItems(invoice: String): List<TransactionItemEntity>
    @Query("SELECT * FROM transaction_items ORDER BY id DESC") fun observeAllTransactionItems(): Flow<List<TransactionItemEntity>>
    @Insert suspend fun putTransactionItems(values: List<TransactionItemEntity>)
    @Query("SELECT invoiceNo FROM transactions ORDER BY createdAt DESC LIMIT -1 OFFSET 5000") suspend fun oldTransactionIds(): List<String>
    @Query("DELETE FROM transactions WHERE invoiceNo IN (:ids)") suspend fun deleteTransactions(ids: List<String>)
    @Query("DELETE FROM transaction_items WHERE invoiceNo IN (:ids)") suspend fun deleteTransactionItems(ids: List<String>)

    @Query("SELECT * FROM cash_records ORDER BY createdAt DESC") fun observeCashRecords(): Flow<List<CashRecordEntity>>
    @Query("SELECT id FROM cash_records WHERE kind = :kind ORDER BY createdAt DESC LIMIT -1 OFFSET 5000") suspend fun oldCashIds(kind: String): List<String>
    @Insert suspend fun putCashRecord(value: CashRecordEntity)
    @Query("SELECT * FROM cash_records WHERE kind = :kind ORDER BY createdAt DESC") suspend fun cashRecords(kind: String): List<CashRecordEntity>
    @Query("DELETE FROM cash_records WHERE id IN (:ids)") suspend fun deleteCashRecords(ids: List<String>)

    @Query("SELECT * FROM restocks ORDER BY createdAt DESC") fun observeRestocks(): Flow<List<RestockEntity>>
    @Insert suspend fun putRestock(value: RestockEntity)
    @Query("SELECT id FROM restocks ORDER BY createdAt DESC LIMIT -1 OFFSET 5000") suspend fun oldRestockIds(): List<String>
    @Query("DELETE FROM restocks WHERE id IN (:ids)") suspend fun deleteRestocks(ids: List<String>)

    @Query("SELECT * FROM stock_adjustments ORDER BY createdAt DESC") fun observeAdjustments(): Flow<List<StockAdjustmentEntity>>
    @Insert suspend fun putAdjustment(value: StockAdjustmentEntity)
    @Query("SELECT id FROM stock_adjustments ORDER BY createdAt DESC LIMIT -1 OFFSET 5000") suspend fun oldAdjustmentIds(): List<String>
    @Query("DELETE FROM stock_adjustments WHERE id IN (:ids)") suspend fun deleteAdjustments(ids: List<String>)

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC") fun observeMovements(): Flow<List<StockMovementEntity>>
    @Insert suspend fun putMovement(value: StockMovementEntity)
    @Query("SELECT * FROM activity_logs ORDER BY createdAt DESC") fun observeActivities(): Flow<List<ActivityLogEntity>>
    @Insert suspend fun putActivity(value: ActivityLogEntity)
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC") fun observeNotifications(): Flow<List<NotificationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putNotification(value: NotificationEntity)
    @Query("UPDATE notifications SET isRead = 1") suspend fun readAllNotifications()
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id") suspend fun readNotification(id: String)

    // This database belongs to the retired demo/local-only implementation. The
    // server-backed repositories in AppDatabase are now the source of truth.
    @Query("DELETE FROM accounts") suspend fun clearLegacyAccounts()
    @Query("DELETE FROM stores") suspend fun clearLegacyStores()
    @Query("DELETE FROM sessions") suspend fun clearLegacySessions()
    @Query("DELETE FROM products") suspend fun clearLegacyProducts()
    @Query("DELETE FROM suppliers") suspend fun clearLegacySuppliers()
    @Query("DELETE FROM transactions") suspend fun clearLegacyTransactions()
    @Query("DELETE FROM transaction_items") suspend fun clearLegacyTransactionItems()
    @Query("DELETE FROM cash_records") suspend fun clearLegacyCashRecords()
    @Query("DELETE FROM restocks") suspend fun clearLegacyRestocks()
    @Query("DELETE FROM stock_adjustments") suspend fun clearLegacyAdjustments()
    @Query("DELETE FROM stock_movements") suspend fun clearLegacyMovements()
    @Query("DELETE FROM activity_logs") suspend fun clearLegacyActivities()
    @Query("DELETE FROM notifications") suspend fun clearLegacyNotifications()
}

@Database(
    entities = [AccountEntity::class, StoreEntity::class, SessionEntity::class, ProductEntity::class,
        SupplierEntity::class, TransactionEntity::class, TransactionItemEntity::class,
        CashRecordEntity::class, RestockEntity::class, StockAdjustmentEntity::class,
        StockMovementEntity::class, ActivityLogEntity::class, NotificationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun dao(): AuraDao
    companion object {
        @Volatile private var INSTANCE: AuraDatabase? = null
        fun get(context: Context): AuraDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AuraDatabase::class.java, "aura_local.db")
                .fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
