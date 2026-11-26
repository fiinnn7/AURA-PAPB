package com.aura.server.analysis

import java.util.UUID

internal enum class AnalisisPeriode(val key: String, val days: Int) {
    MINGGUAN("mingguan", 7),
    BULANAN("bulanan", 30);

    companion object {
        fun fromQuery(raw: String?): AnalisisPeriode = when (raw?.trim()?.lowercase()) {
            null, "", "mingguan" -> MINGGUAN
            "bulanan" -> BULANAN
            else -> throw IllegalArgumentException("Periode harus mingguan atau bulanan.")
        }
    }
}

internal data class RentangWaktu(
    val startMs: Long,
    val endMs: Long,
    val prevStartMs: Long,
    val prevEndMs: Long,
    val startIso: String,
    val endIso: String,
)

internal data class TitikHarianDto(
    val tanggal: String,
    val omzet: Long,
    val labaKotor: Long,
)

internal data class RingkasanFinansialDto(
    val omzet: Long,
    val omzetPerubahanPct: Double?,
    val labaKotor: Long,
    val labaKotorPerubahanPct: Double?,
    val labaBersih: Long,
    val labaBersihPerubahanPct: Double?,
    val grossMarginPct: Double?,
    val netMarginPct: Double?,
    val biayaOperasional: Long,
    val kerugianPenyesuaian: Long,
    val modalTertahan: Long,
    val harian: List<TitikHarianDto>,
)

internal data class PergerakanBarangItemDto(
    val barangId: String,
    val nama: String,
    val fotoFileId: String?,
    val satuan: String,
    val stok: Int,
    val qtyTerjual: Int,
    val kecepatan: Double,
    val strPct: Double?,
    val umurStokHari: Int?,
    val tidakLaku: Boolean,
    val modalTertahan: Long,
)

internal data class PergerakanRingkasDto(
    val status: String,
    val fastCount: Int,
    val slowCount: Int,
    val fastPreview: List<PergerakanBarangItemDto>,
    val slowPreview: List<PergerakanBarangItemDto>,
    val totalModalSlow: Long,
    val runwayKritisCount: Int,
)

internal data class UmurStokItemDto(
    val barangId: String,
    val nama: String,
    val fotoFileId: String?,
    val satuan: String,
    val stok: Int,
    val rataHarian: Double,
    val umurStokHari: Int?,
    val kategori: String,
)

internal data class ProporsiItemDto(
    val barangId: String,
    val nama: String,
    val nominal: Long,
    val persentase: Double,
    val kelasAbc: String?,
    val isRugi: Boolean,
)

internal data class ProporsiRingkasDto(
    val totalProfit: Long,
    val profitTop: List<ProporsiItemDto>,
    val profitLainnyaPct: Double,
    val totalPengeluaran: Long,
    val pengeluaranTop: List<ProporsiItemDto>,
    val pengeluaranLainnyaPct: Double,
)

internal data class TrenItemDto(
    val barangId: String,
    val nama: String,
    val fotoFileId: String?,
    val satuan: String,
    val lajuSekarang: Double,
    val lajuPembanding: Double,
    val perubahanPct: Double,
    val arah: String,
    val sparkline: List<Int>,
)

internal data class TrenRingkasDto(
    val naikCount: Int,
    val turunCount: Int,
    val naikPreview: List<TrenItemDto>,
    val turunPreview: List<TrenItemDto>,
)

internal data class KorelasiTerkaitDto(
    val barangId: String,
    val nama: String,
    val fotoFileId: String?,
    val support: Double,
    val confidence: Double,
    val lift: Double,
    val kekuatan: String,
    val pesan: String,
)

internal data class KorelasiProdukDto(
    val barangId: String,
    val nama: String,
    val fotoFileId: String?,
    val maxLift: Double,
    val terkait: List<KorelasiTerkaitDto>,
)

internal data class KorelasiRingkasDto(
    val status: String,
    val jumlahProduk: Int,
    val keranjangDitemukan: Int,
    val keranjangDibutuhkan: Int,
)

internal data class AnalisisRingkasanResponse(
    val periode: String,
    val rentangMulai: String,
    val rentangSelesai: String,
    val diperbaruiPada: String,
    val finansial: RingkasanFinansialDto,
    val pergerakan: PergerakanRingkasDto,
    val proporsi: ProporsiRingkasDto,
    val tren: TrenRingkasDto,
    val korelasi: KorelasiRingkasDto,
)

internal data class PergerakanDetailResponse(
    val jenis: String,
    val periode: String,
    val status: String,
    val totalModalSlow: Long,
    val diperbaruiPada: String,
    val items: List<PergerakanBarangItemDto>,
)

internal data class UmurStokDetailResponse(
    val periode: String,
    val diperbaruiPada: String,
    val items: List<UmurStokItemDto>,
)

internal data class ProporsiDetailResponse(
    val jenis: String,
    val periode: String,
    val totalNominal: Long,
    val diperbaruiPada: String,
    val items: List<ProporsiItemDto>,
)

internal data class TrenDetailResponse(
    val arah: String,
    val periode: String,
    val keteranganPembanding: String,
    val diperbaruiPada: String,
    val items: List<TrenItemDto>,
)

internal data class KorelasiDetailResponse(
    val status: String,
    val periode: String,
    val keranjangDitemukan: Int,
    val keranjangDibutuhkan: Int,
    val diperbaruiPada: String,
    val produk: List<KorelasiProdukDto>,
)

internal data class RawBarang(
    val id: UUID,
    val nama: String,
    val fotoFileId: UUID?,
    val satuan: String,
    val stokSaatIni: Int,
    val hppRataRata: Long,
    val status: String,
    val dibuatPada: Long,
)

internal data class RawSaleItem(
    val transaksiId: UUID,
    val barangId: UUID,
    val jumlah: Int,
    val hargaJual: Long,
    val hppSnapshot: Long,
    val dibuatPada: Long,
)

internal data class RawRestock(
    val barangId: UUID,
    val totalDibayar: Long,
    val tanggal: Long,
)

internal data class RawMovement(
    val barangId: UUID,
    val stokSesudah: Int,
    val dibuatPada: Long,
)

internal data class RawExpense(
    val nominal: Long,
    val tanggal: Long,
)

internal data class RawAdjustmentLoss(
    val nilaiKerugian: Long,
    val alasan: String,
    val tanggal: Long,
)

internal data class StoreAnalysisDataset(
    val barangList: List<RawBarang>,
    val saleItems: List<RawSaleItem>,
    val restocks: List<RawRestock>,
    val movements: List<RawMovement>,
    val expenses: List<RawExpense>,
    val adjustmentLosses: List<RawAdjustmentLoss>,
)
