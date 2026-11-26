package com.aura.server.auth

import com.aura.server.db.AkunTable
import com.aura.server.db.FileTable
import com.aura.server.db.LogAktivitasTable
import com.aura.server.db.PercobaanLoginTable
import com.aura.server.db.SesiTable
import com.aura.server.db.TokoTable
import com.aura.server.plugins.ApiException
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

internal interface AuthRepository {
    fun createOwner(
        email: String,
        password: PasswordDigest,
        storeName: String,
        businessType: String?,
        openingBalance: Long,
        profilePhotoId: UUID?,
    ): Pair<Akun, TokoRingkas>

    fun findOwnerByEmail(email: String): Akun?
    fun findCashier(storeId: String, username: String): Akun?
    fun findAccount(accountId: UUID): Akun?
    fun findStore(storeId: String): TokoRingkas?
    fun registerSession(account: Akun, session: TokenSession)
    fun isSessionActive(jti: UUID, accountId: UUID, storeId: String, role: String, now: Long = System.currentTimeMillis()): Boolean
    fun logout(account: Akun, jti: UUID): Boolean
    fun revokeOtherSessions(account: Akun, currentJti: UUID): Int
    fun updatePassword(account: Akun, digest: PasswordDigest, forceChange: Boolean = false, keepJti: UUID? = null)
    fun updateProfile(account: Akun, name: String?, phone: String?, photoId: UUID?, email: String? = null): Akun
    fun updateStore(account: Akun, name: String?, businessType: String?, address: String?, logoId: UUID?): TokoRingkas
    fun updateOpeningBalance(account: Akun, newBalance: Long): TokoRingkas
    fun createCashier(storeId: String, accountId: UUID, name: String, digest: PasswordDigest): Akun
    fun listCashiers(storeId: String): List<Akun>
    fun setCashierStatus(storeId: String, accountId: UUID, status: StatusAkun): Boolean
    fun softDeleteCashier(storeId: String, accountId: UUID): Akun?
    fun scheduleOwnerDeletion(account: Akun): Pair<Long, Long>
    fun purgeExpiredStores(now: Long = System.currentTimeMillis()): Int
    fun recordFailedLogin(loginKey: String): Long?
    fun loginLocked(loginKey: String): Boolean
    fun clearLoginFailures(loginKey: String)

    companion object {
        const val OWNER_DELETION_GRACE_MILLIS = 14L * 24 * 60 * 60 * 1000
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_MILLIS = 5L * 60 * 1000
        fun normalize(value: String) = value.trim().lowercase()
    }
}

internal class ExposedAuthRepository : AuthRepository {
    override fun createOwner(
        email: String,
        password: PasswordDigest,
        storeName: String,
        businessType: String?,
        openingBalance: Long,
        profilePhotoId: UUID?,
    ): Pair<Akun, TokoRingkas> = transaction {
        val storeId = generateStoreId()
        val ownerId = UUID.randomUUID()
        val now = System.currentTimeMillis()
        TokoTable.insert {
            it[idToko] = storeId
            it[nama] = storeName
            it[jenisUsaha] = businessType
            it[alamat] = null
            it[TokoTable.logoFileId] = null
            it[saldoAwal] = openingBalance
            it[saldoKumulatif] = openingBalance
            it[hapusPada] = null
            it[dibuatPada] = now
        }
        AkunTable.insert {
            it[AkunTable.id] = ownerId.toKotlinUuid()
            it[AkunTable.idToko] = storeId
            it[AkunTable.email] = email
            it[nama] = storeName
            it[namaNormalisasi] = AuthRepository.normalize(storeName)
            it[peran] = Peran.OWNER.name
            it[sandiHash] = password.hash
            it[sandiSalt] = password.salt
            it[status] = StatusAkun.AKTIF.name
            it[wajibGantiSandi] = false
            it[nomorHp] = null
            it[AkunTable.fotoFileId] = profilePhotoId?.toKotlinUuid()
            it[gagalLogin] = 0
            it[terkunciSampai] = null
            it[terakhirLogin] = null
            it[dibuatPada] = now
            it[dihapusPada] = null
        }
        log(storeId, ownerId, storeName, "AKUN", "OWNER_DIBUAT", now)
        val account = findAccount(ownerId)!!
        account to findStore(storeId)!!
    }

    override fun findOwnerByEmail(email: String): Akun? = transaction {
        AkunTable.selectAll().where { (AkunTable.email eq email) and (AkunTable.peran eq Peran.OWNER.name) }
            .singleOrNull()?.toAccount()
    }

    override fun findCashier(storeId: String, username: String): Akun? = transaction {
        AkunTable.selectAll().where {
            (AkunTable.idToko eq storeId) and
                (AkunTable.namaNormalisasi eq AuthRepository.normalize(username)) and
                (AkunTable.peran eq Peran.KASIR.name) and
                (AkunTable.status neq StatusAkun.DIHAPUS.name)
        }.singleOrNull()?.toAccount()
    }

    override fun findAccount(accountId: UUID): Akun? = transaction {
        AkunTable.selectAll().where { AkunTable.id eq accountId.toKotlinUuid() }.singleOrNull()?.toAccount()
    }

    override fun findStore(storeId: String): TokoRingkas? = transaction {
        TokoTable.selectAll().where { TokoTable.idToko eq storeId }.singleOrNull()?.toStore()
    }

    override fun registerSession(account: Akun, session: TokenSession) = transaction {
        val now = System.currentTimeMillis()
        SesiTable.insert {
            it[SesiTable.jti] = session.jti.toKotlinUuid()
            it[SesiTable.akunId] = account.id.toKotlinUuid()
            it[dibuatPada] = now
            it[berakhirPada] = session.expiresAt
            it[dicabutPada] = null
        }
        AkunTable.update({ AkunTable.id eq account.id.toKotlinUuid() }) {
            it[terakhirLogin] = now
            it[gagalLogin] = 0
            it[terkunciSampai] = null
        }
        log(account.idToko, account.id, account.nama, "LOGIN", "LOGIN_BERHASIL", now)
        if (account.peran == Peran.OWNER) {
            val canceled = TokoTable.update({ (TokoTable.idToko eq account.idToko) and (TokoTable.hapusPada greater now) }) {
                it[hapusPada] = null
            } > 0
            if (canceled) {
                log(account.idToko, account.id, account.nama, "AKUN", "OWNER_HAPUS_DIBATALKAN", now)
            }
        }
    }

    override fun isSessionActive(jti: UUID, accountId: UUID, storeId: String, role: String, now: Long): Boolean = transaction {
        val account = AkunTable.selectAll().where { AkunTable.id eq accountId.toKotlinUuid() }.singleOrNull() ?: return@transaction false
        if (account[AkunTable.idToko] != storeId || account[AkunTable.peran] != role || account[AkunTable.status] != StatusAkun.AKTIF.name) return@transaction false
        val store = TokoTable.selectAll().where { TokoTable.idToko eq storeId }.singleOrNull() ?: return@transaction false
        if (store[TokoTable.hapusPada] != null) return@transaction false
        SesiTable.selectAll().where {
            (SesiTable.jti eq jti.toKotlinUuid()) and (SesiTable.akunId eq accountId.toKotlinUuid()) and
                (SesiTable.dicabutPada.isNull()) and (SesiTable.berakhirPada greater now)
        }.singleOrNull() != null
    }

    override fun logout(account: Akun, jti: UUID): Boolean = transaction {
        val now = System.currentTimeMillis()
        val changed = SesiTable.update({
            (SesiTable.jti eq jti.toKotlinUuid()) and
                (SesiTable.akunId eq account.id.toKotlinUuid()) and SesiTable.dicabutPada.isNull()
        }) { it[dicabutPada] = now } > 0
        if (changed) log(account.idToko, account.id, account.nama, "LOGIN", "LOGOUT", now)
        changed
    }

    override fun revokeOtherSessions(account: Akun, currentJti: UUID): Int = transaction {
        val now = System.currentTimeMillis()
        val count = SesiTable.update({
            (SesiTable.akunId eq account.id.toKotlinUuid()) and
                (SesiTable.jti neq currentJti.toKotlinUuid()) and
                SesiTable.dicabutPada.isNull()
        }) {
            it[dicabutPada] = now
        }
        log(account.idToko, account.id, account.nama, "LOGIN", "SESI_LAIN_DICABUT", now)
        count
    }

    override fun updatePassword(account: Akun, digest: PasswordDigest, forceChange: Boolean, keepJti: UUID?) = transaction {
        AkunTable.update({ AkunTable.id eq account.id.toKotlinUuid() }) {
            it[sandiHash] = digest.hash
            it[sandiSalt] = digest.salt
            it[wajibGantiSandi] = forceChange
        }
        val now = System.currentTimeMillis()
        SesiTable.update({
            val base = (SesiTable.akunId eq account.id.toKotlinUuid()) and SesiTable.dicabutPada.isNull()
            if (keepJti != null) base and (SesiTable.jti neq keepJti.toKotlinUuid()) else base
        }) {
            it[dicabutPada] = now
        }
        log(account.idToko, account.id, account.nama, "AKUN", "SANDI_DIUBAH", now)
    }

    override fun updateProfile(account: Akun, name: String?, phone: String?, photoId: UUID?, email: String?): Akun = transaction {
        val updatedName = name ?: account.nama
        AkunTable.update({ AkunTable.id eq account.id.toKotlinUuid() }) {
            it[AkunTable.nama] = updatedName
            it[AkunTable.namaNormalisasi] = AuthRepository.normalize(updatedName)
            if (phone != null) it[AkunTable.nomorHp] = phone
            if (photoId != null) it[AkunTable.fotoFileId] = photoId.toKotlinUuid()
            if (email != null && account.peran == Peran.OWNER) it[AkunTable.email] = email
        }
        val now = System.currentTimeMillis()
        log(account.idToko, account.id, updatedName, "AKUN", "PROFIL_DIUBAH", now)
        findAccount(account.id)!!
    }

    override fun updateStore(account: Akun, name: String?, businessType: String?, address: String?, logoId: UUID?): TokoRingkas = transaction {
        if (logoId != null) {
            val fileExists = FileTable.selectAll().where {
                (FileTable.idToko eq account.idToko) and (FileTable.id eq logoId.toKotlinUuid())
            }.singleOrNull() != null
            if (!fileExists) {
                throw ApiException(HttpStatusCode.NotFound, "FILE_TIDAK_DITEMUKAN", "File logo tidak ditemukan.")
            }
        }
        val updated = TokoTable.update({ TokoTable.idToko eq account.idToko }) {
            if (name != null) it[TokoTable.nama] = name
            if (businessType != null) it[TokoTable.jenisUsaha] = businessType
            if (address != null) it[TokoTable.alamat] = address
            if (logoId != null) it[TokoTable.logoFileId] = logoId.toKotlinUuid()
        }
        if (updated != 1) throw ApiException(HttpStatusCode.NotFound, "TOKO_TIDAK_DITEMUKAN", "Toko tidak ditemukan.")
        log(account.idToko, account.id, account.nama, "AKUN", "TOKO_DIUBAH", System.currentTimeMillis())
        findStore(account.idToko)!!
    }

    override fun updateOpeningBalance(account: Akun, newBalance: Long): TokoRingkas = transaction {
        val current = lockStore(account.idToko)
        val newCumulativeBalance = try {
            Math.addExact(
                current[TokoTable.saldoKumulatif],
                Math.subtractExact(newBalance, current[TokoTable.saldoAwal]),
            )
        } catch (_: ArithmeticException) {
            throw ApiException(HttpStatusCode.BadRequest, "SALDO_TIDAK_VALID", "Perubahan saldo melebihi batas yang didukung.")
        }
        TokoTable.update({ TokoTable.idToko eq account.idToko }) {
            it[saldoAwal] = newBalance
            it[saldoKumulatif] = newCumulativeBalance
        }
        log(account.idToko, account.id, account.nama, "KEUANGAN", "SALDO_AWAL_DIUBAH", System.currentTimeMillis())
        findStore(account.idToko)!!
    }

    override fun createCashier(storeId: String, accountId: UUID, name: String, digest: PasswordDigest): Akun = transaction {
        val now = System.currentTimeMillis()
        AkunTable.insert {
            it[AkunTable.id] = accountId.toKotlinUuid()
            it[AkunTable.idToko] = storeId
            it[email] = null
            it[nama] = name
            it[namaNormalisasi] = AuthRepository.normalize(name)
            it[peran] = Peran.KASIR.name
            it[sandiHash] = digest.hash
            it[sandiSalt] = digest.salt
            it[status] = StatusAkun.AKTIF.name
            it[wajibGantiSandi] = true
            it[nomorHp] = null
            it[AkunTable.fotoFileId] = null
            it[gagalLogin] = 0
            it[terkunciSampai] = null
            it[terakhirLogin] = null
            it[dibuatPada] = now
            it[dihapusPada] = null
        }
        log(storeId, accountId, name, "AKUN", "KASIR_DIBUAT", now)
        findAccount(accountId)!!
    }

    override fun listCashiers(storeId: String): List<Akun> = transaction {
        AkunTable.selectAll().where {
            (AkunTable.idToko eq storeId) and (AkunTable.peran eq Peran.KASIR.name) and (AkunTable.status neq StatusAkun.DIHAPUS.name)
        }.map { it.toAccount() }
    }

    override fun setCashierStatus(storeId: String, accountId: UUID, status: StatusAkun): Boolean = transaction {
        val account = findAccount(accountId) ?: return@transaction false
        if (account.idToko != storeId || account.peran != Peran.KASIR || account.status == StatusAkun.DIHAPUS) return@transaction false
        AkunTable.update({ (AkunTable.id eq accountId.toKotlinUuid()) and (AkunTable.idToko eq storeId) }) {
            it[AkunTable.status] = status.name
        }
        if (status != StatusAkun.AKTIF) revokeAll(accountId)
        log(storeId, accountId, account.nama, "AKUN", if (status == StatusAkun.AKTIF) "KASIR_AKTIF" else "KASIR_NONAKTIF", System.currentTimeMillis())
        true
    }

    override fun softDeleteCashier(storeId: String, accountId: UUID): Akun? = transaction {
        val account = findAccount(accountId) ?: return@transaction null
        if (account.idToko != storeId || account.peran != Peran.KASIR || account.status == StatusAkun.DIHAPUS) return@transaction null
        val now = System.currentTimeMillis()
        AkunTable.update({ AkunTable.id eq accountId.toKotlinUuid() }) {
            it[status] = StatusAkun.DIHAPUS.name
            it[dihapusPada] = now
        }
        revokeAll(accountId)
        log(storeId, accountId, account.nama, "AKUN", "KASIR_DIHAPUS", now)
        account.copy(status = StatusAkun.DIHAPUS)
    }

    override fun scheduleOwnerDeletion(account: Akun): Pair<Long, Long> = transaction {
        val now = System.currentTimeMillis()
        val deleteAt = now + AuthRepository.OWNER_DELETION_GRACE_MILLIS
        TokoTable.update({ TokoTable.idToko eq account.idToko }) { it[hapusPada] = deleteAt }
        revokeAllForStore(account.idToko)
        log(account.idToko, account.id, account.nama, "AKUN", "OWNER_HAPUS_TERJADWAL", now)
        now to deleteAt
    }

    override fun purgeExpiredStores(now: Long): Int = transaction {
        val expired = TokoTable.selectAll().where { TokoTable.hapusPada lessEq now }.map { it[TokoTable.idToko] }
        expired.forEach { storeId -> TokoTable.deleteWhere { idToko eq storeId } }
        expired.size
    }

    override fun recordFailedLogin(loginKey: String): Long? = transaction {
        val key = hashKey(loginKey)
        val now = System.currentTimeMillis()
        val existing = PercobaanLoginTable.selectAll().where { PercobaanLoginTable.kunci eq key }.singleOrNull()
        val count = (existing?.get(PercobaanLoginTable.jumlahGagal) ?: 0) + 1
        val lockUntil = if (count >= AuthRepository.MAX_FAILED_ATTEMPTS) now + AuthRepository.LOCKOUT_MILLIS else null
        if (existing == null) {
            PercobaanLoginTable.insert {
                it[kunci] = key
                it[jumlahGagal] = if (lockUntil == null) count else 0
                it[terkunciSampai] = lockUntil
                it[diperbaruiPada] = now
            }
        } else {
            PercobaanLoginTable.update({ PercobaanLoginTable.kunci eq key }) {
                it[jumlahGagal] = if (lockUntil == null) count else 0
                it[terkunciSampai] = lockUntil
                it[diperbaruiPada] = now
            }
        }
        lockUntil
    }

    override fun loginLocked(loginKey: String): Boolean = transaction {
        val key = hashKey(loginKey)
        val row = PercobaanLoginTable.selectAll().where { PercobaanLoginTable.kunci eq key }.singleOrNull()
        val until = row?.get(PercobaanLoginTable.terkunciSampai)
        until != null && until > System.currentTimeMillis()
    }

    override fun clearLoginFailures(loginKey: String) {
        transaction {
            val key = hashKey(loginKey)
            PercobaanLoginTable.deleteWhere { kunci eq key }
        }
    }

    private fun hashKey(loginKey: String): String =
        MessageDigest.getInstance("SHA-256").digest(loginKey.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun revokeAll(accountId: UUID) {
        val now = System.currentTimeMillis()
        SesiTable.update({ (SesiTable.akunId eq accountId.toKotlinUuid()) and SesiTable.dicabutPada.isNull() }) { it[dicabutPada] = now }
    }

    private fun revokeAllForStore(storeId: String) {
        val accountIds = AkunTable.selectAll().where { AkunTable.idToko eq storeId }.map { it[AkunTable.id] }
        val now = System.currentTimeMillis()
        accountIds.forEach { id -> SesiTable.update({ (SesiTable.akunId eq id) and SesiTable.dicabutPada.isNull() }) { it[dicabutPada] = now } }
    }

    private fun lockStore(storeId: String): org.jetbrains.exposed.v1.core.ResultRow {
        while (true) {
            val row = TokoTable.selectAll().where { TokoTable.idToko eq storeId }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.NotFound, "TOKO_TIDAK_DITEMUKAN", "Toko tidak ditemukan.")
            val balance = row[TokoTable.saldoKumulatif]
            val locked = TokoTable.update({
                (TokoTable.idToko eq storeId) and (TokoTable.saldoKumulatif eq balance)
            }) {
                it[saldoKumulatif] = balance
            }
            if (locked == 1) return row
        }
    }

    private fun log(storeId: String, accountId: UUID?, name: String, category: String, action: String, at: Long) {
        LogAktivitasTable.insert {
            it[LogAktivitasTable.id] = UUID.randomUUID().toKotlinUuid()
            it[LogAktivitasTable.idToko] = storeId
            it[LogAktivitasTable.akunId] = accountId?.toKotlinUuid()
            it[namaAkunSnapshot] = name.take(80)
            it[kategori] = category
            it[aksi] = action
            it[dibuatPada] = at
        }
    }

    private fun generateStoreId(): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        repeat(20) {
            val candidate = buildString { repeat(8) { append(alphabet[random.nextInt(alphabet.length)]) } }
            if (TokoTable.selectAll().where { TokoTable.idToko eq candidate }.count() == 0L) return candidate
        }
        error("Tidak dapat membuat ID Toko unik.")
    }

    private fun ResultRow.toAccount(): Akun {
        val storeId = this[AkunTable.idToko]
        return Akun(
            id = this[AkunTable.id].toJavaUuid(), idToko = storeId, email = this[AkunTable.email],
            nama = this[AkunTable.nama], namaNormalisasi = this[AkunTable.namaNormalisasi],
            peran = Peran.valueOf(this[AkunTable.peran]), hash = this[AkunTable.sandiHash], salt = this[AkunTable.sandiSalt],
            status = StatusAkun.valueOf(this[AkunTable.status]), wajibGantiSandi = this[AkunTable.wajibGantiSandi],
            nomorHp = this[AkunTable.nomorHp], fotoFileId = this[AkunTable.fotoFileId]?.toJavaUuid(),
            gagalLogin = this[AkunTable.gagalLogin], terkunciSampai = this[AkunTable.terkunciSampai],
            terakhirLogin = this[AkunTable.terakhirLogin],
            hapusTokoPada = TokoTable.selectAll().where { TokoTable.idToko eq storeId }.singleOrNull()?.get(TokoTable.hapusPada),
        )
    }

    private fun ResultRow.toStore() = TokoRingkas(
        idToko = this[TokoTable.idToko], namaToko = this[TokoTable.nama], jenisUsaha = this[TokoTable.jenisUsaha],
        alamat = this[TokoTable.alamat], logoFileId = this[TokoTable.logoFileId]?.toJavaUuid(), saldoAwal = this[TokoTable.saldoAwal],
        saldoKumulatif = this[TokoTable.saldoKumulatif],
    )

    companion object {
        private val random = SecureRandom()
    }
}
