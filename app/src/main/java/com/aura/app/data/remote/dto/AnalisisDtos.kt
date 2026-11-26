package com.aura.app.data.remote.dto

data class TitikHarianDto(
    val tanggal: String = "",
    val omzet: Long = 0L,
    val labaKotor: Long = 0L,
)

data class RingkasanFinansialDto(
    val omzet: Long = 0L,
    val omzetPerubahanPct: Double? = null,
    val labaKotor: Long = 0L,
    val labaKotorPerubahanPct: Double? = null,
    val labaBersih: Long = 0L,
    val labaBersihPerubahanPct: Double? = null,
    val grossMarginPct: Double? = null,
    val netMarginPct: Double? = null,
    val biayaOperasional: Long = 0L,
    val kerugianPenyesuaian: Long = 0L,
    val modalTertahan: Long = 0L,
    val harian: List<TitikHarianDto> = emptyList(),
)

data class PergerakanBarangItemDto(
    val barangId: String = "",
    val nama: String = "",
    val fotoFileId: String? = null,
    val satuan: String = "pcs",
    val stok: Int = 0,
    val qtyTerjual: Int = 0,
    val kecepatan: Double = 0.0,
    val strPct: Double? = null,
    val umurStokHari: Int? = null,
    val tidakLaku: Boolean = false,
    val modalTertahan: Long = 0L,
)

data class PergerakanRingkasDto(
    val status: String = "ok",
    val fastCount: Int = 0,
    val slowCount: Int = 0,
    val fastPreview: List<PergerakanBarangItemDto> = emptyList(),
    val slowPreview: List<PergerakanBarangItemDto> = emptyList(),
    val totalModalSlow: Long = 0L,
    val runwayKritisCount: Int = 0,
)

data class UmurStokItemDto(
    val barangId: String = "",
    val nama: String = "",
    val fotoFileId: String? = null,
    val satuan: String = "pcs",
    val stok: Int = 0,
    val rataHarian: Double = 0.0,
    val umurStokHari: Int? = null,
    val kategori: String = "tidak_diketahui",
)

data class ProporsiItemDto(
    val barangId: String = "",
    val nama: String = "",
    val nominal: Long = 0L,
    val persentase: Double = 0.0,
    val kelasAbc: String? = null,
    val isRugi: Boolean = false,
)

data class ProporsiRingkasDto(
    val totalProfit: Long = 0L,
    val profitTop: List<ProporsiItemDto> = emptyList(),
    val profitLainnyaPct: Double = 0.0,
    val totalPengeluaran: Long = 0L,
    val pengeluaranTop: List<ProporsiItemDto> = emptyList(),
    val pengeluaranLainnyaPct: Double = 0.0,
)

data class TrenItemDto(
    val barangId: String = "",
    val nama: String = "",
    val fotoFileId: String? = null,
    val satuan: String = "pcs",
    val lajuSekarang: Double = 0.0,
    val lajuPembanding: Double = 0.0,
    val perubahanPct: Double = 0.0,
    val arah: String = "naik",
    val sparkline: List<Int> = emptyList(),
)

data class TrenRingkasDto(
    val naikCount: Int = 0,
    val turunCount: Int = 0,
    val naikPreview: List<TrenItemDto> = emptyList(),
    val turunPreview: List<TrenItemDto> = emptyList(),
)

data class KorelasiTerkaitDto(
    val barangId: String = "",
    val nama: String = "",
    val fotoFileId: String? = null,
    val support: Double = 0.0,
    val confidence: Double = 0.0,
    val lift: Double = 0.0,
    val kekuatan: String = "sedang",
    val pesan: String = "",
)

data class KorelasiProdukDto(
    val barangId: String = "",
    val nama: String = "",
    val fotoFileId: String? = null,
    val maxLift: Double = 0.0,
    val terkait: List<KorelasiTerkaitDto> = emptyList(),
)

data class KorelasiRingkasDto(
    val status: String = "ok",
    val jumlahProduk: Int = 0,
    val keranjangDitemukan: Int = 0,
    val keranjangDibutuhkan: Int = 50,
)

data class AnalisisRingkasanResponseDto(
    val periode: String = "mingguan",
    val rentangMulai: String = "",
    val rentangSelesai: String = "",
    val diperbaruiPada: String = "",
    val finansial: RingkasanFinansialDto = RingkasanFinansialDto(),
    val pergerakan: PergerakanRingkasDto = PergerakanRingkasDto(),
    val proporsi: ProporsiRingkasDto = ProporsiRingkasDto(),
    val tren: TrenRingkasDto = TrenRingkasDto(),
    val korelasi: KorelasiRingkasDto = KorelasiRingkasDto(),
)

data class PergerakanDetailResponseDto(
    val jenis: String = "fast",
    val periode: String = "mingguan",
    val status: String = "ok",
    val totalModalSlow: Long = 0L,
    val diperbaruiPada: String = "",
    val items: List<PergerakanBarangItemDto> = emptyList(),
)

data class UmurStokDetailResponseDto(
    val periode: String = "mingguan",
    val diperbaruiPada: String = "",
    val items: List<UmurStokItemDto> = emptyList(),
)

data class ProporsiDetailResponseDto(
    val jenis: String = "profit",
    val periode: String = "mingguan",
    val totalNominal: Long = 0L,
    val diperbaruiPada: String = "",
    val items: List<ProporsiItemDto> = emptyList(),
)

data class TrenDetailResponseDto(
    val arah: String = "naik",
    val periode: String = "mingguan",
    val keteranganPembanding: String = "",
    val diperbaruiPada: String = "",
    val items: List<TrenItemDto> = emptyList(),
)

data class KorelasiDetailResponseDto(
    val status: String = "ok",
    val periode: String = "mingguan",
    val keranjangDitemukan: Int = 0,
    val keranjangDibutuhkan: Int = 50,
    val diperbaruiPada: String = "",
    val produk: List<KorelasiProdukDto> = emptyList(),
)
