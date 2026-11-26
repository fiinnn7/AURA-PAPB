# Dokumentasi Lengkap Perubahan Sejak `git clone` & Panduan Integrasi Modul Analisis AURA

> **Konteks Penting (Untuk Developer & AI Coding Assistant pada Branch Tujuan):**
> 1. **Otoritas Modul Analisis:** Seluruh fitur, perhitungan backend, endpoint REST API, dan layar UI yang berkaitan dengan **Tab Analisis Bisnis** merupakan tanggung jawab penuh PIC Analisis (pemilik perubahan di dokumen ini).
> 2. **Aturan Utama Integrasi:**
>    - **Semua yang berkaitan dengan Tab Analisis (Kategori A) WAJIB menggunakan implementasi dari versi ini.** Jika pada branch tujuan sudah terdapat implementasi Analisis versi lain (baik versi lokal Room, versi dummy, maupun endpoint analisis berbeda), **ganti/timpa bagian Analisis tersebut dengan versi di dokumen ini**.
>    - **Perubahan di luar Tab Analisis (Kategori C) BERSIFAT OPSIONAL / TIDAK MEMAKSA.** Jangan memaksakan perubahan Kategori C jika berbenturan dengan pekerjaan teman pada layar Kasir, Katalog, atau Manajemen di branch tujuan.

---

## 1. Ringkasan Arsitektur Fitur Analisis (Yang Diimplementasikan)

Fitur Analisis AURA dirancang dengan arsitektur **100% Read-Only di sisi Server (Kotlin + Ktor + Exposed + PostgreSQL)** tanpa mengubah tabel atau logika transaksi modul lain (Katalog, Kasir, Stok, Keuangan tetap utuh).

* **Filter Periode:** `Mingguan` (7 hari terakhir vs 7 hari sebelumnya) dan `Bulanan` (30 hari terakhir vs 30 hari sebelumnya) dalam zona waktu **WIB (`Asia/Jakarta`)**.
* **Layout Utama (`AnalysisScreen.kt`):** 1 halaman *vertical scroll* terpadu yang memuat **5 Bagian Analisis**, dilengkapi **5 Layar Detail (`AnalisisDetailScreens.kt`)** dan grafik berbasis **`Canvas` bawaan Jetpack Compose** (tanpa ketergantungan library chart eksternal).
* **5 Bagian Analisis & 6 Endpoint Backend (`/api/v1/analisis/*`):**
  1. **Bagian 1 — Ringkasan Finansial:** Omzet, Laba Kotor (dari `hpp_snapshot`), Biaya Operasional (`pengeluaran` jenis `KUSTOM` berstatus `TERCATAT`), Laba Bersih, Gross/Net Margin (%), Kerugian Penyesuaian Stok (`RUSAK`/`HILANG`/`KADALUARSA`), Modal Tertahan (`stok_saat_ini × hpp_rata_rata`), `% perubahan vs periode lalu`, dan Grafik Garis Harian Omzet vs Laba Kotor.
  2. **Bagian 2 — Volume & Pergerakan Barang:**
     * **Fast Moving & Slow Moving:** Kecepatan jual = `qty_terjual / hari_stok_tersedia` (direkonstruksi dari `pergerakan_stok` agar barang yang habis karena laris tidak salah dilabeli Slow Moving). Mengecualikan barang berusia `< 14 hari`; minimal 5 barang aktif. Dilengkapi **Sell-Through Rate (`STR = terjual / (terjual + stok) × 100%`)** dan total modal tertahan di Slow Moving.
     * **Estimasi Umur Stok (*Stock Runway*):** `FLOOR(stok_saat_ini / rata_jual_harian)` dengan kategori `Kritis (≤ 3 hari)`, `Perlu diperhatikan (4–7 hari)`, `Aman (> 7 hari)`, atau `Tidak dapat dihitung` jika pembagi 0.
  3. **Bagian 3 — Proporsi Penjualan:** Donut Chart (Top 5 + Lainnya) dengan toggle **Kontribusi ke Profit** (Laba Kotor per produk + Klasifikasi Pareto **ABC**: A ≤ 80%, B ≤ 95%, C > 95%, serta penanda produk rugi) dan **Kontribusi ke Pengeluaran** (berdasarkan `restock.total_dibayar` per produk).
  4. **Bagian 4 — Tren Penjualan:** Membandingkan laju jual harian periode sekarang vs pembanding (Mingguan: 7 hari vs rata-rata 28 hari; Bulanan: 30 hari vs 30 hari sebelumnya; syarat minimal terjual ≥ 5 unit). `Rasio ≥ 1,25` = **Tren Naik**, `Rasio ≤ 0,75` = **Tren Turun**, dilengkapi grafik *sparkline* harian per produk.
  5. **Bagian 5 — Korelasi Antar Produk (*Association Rule / Market Basket*):** Algoritma *Pair Counting* pada nota selesai yang memuat `≥ 2` produk unik (syarat minimum keranjang: 50 untuk Mingguan, 100 untuk Bulanan; `min_support = 0.02 / 0.015`, `min_confidence = 0.30`, dan **`lift > 1.2`**). Ditampilkan di Android dengan komponen **Single-Expand Accordion (View-Only)**: menekan satu produk membuka *dropdown* daftar produk berkorelasi kuat, menekan produk lain otomatis menutup *dropdown* sebelumnya, tanpa navigasi ke halaman edit produk.

---

## 2. Daftar Lengkap 36 File yang Berubah Sejak `git clone` (Base Commit `3f3c688`)

Seluruh perubahan dibagi menjadi **3 Kategori** agar mudah diintegrasikan ke branch teman tanpa merusak fitur lain.

---

### KATEGORI A — WAJIB DITERAPKAN (Inti Fitur Analisis Bisnis)

#### A1. File Baru Analisis (15 File — Salin Langsung, Nol Konflik)

**Sisi Backend (`server/`):**
1. `server/src/main/resources/db/migration/V7__indeks_analisis.sql`
   * Membuat 4 index `IF NOT EXISTS` untuk mempercepat query analisis.
   * *Catatan untuk AI teman:* Jika di branch teman sudah ada file migrasi `V7__...sql` lain, cukup ganti nama file ini menjadi nomor urut berikutnya (misal `V8__indeks_analisis.sql`).
2. `server/src/main/kotlin/com/aura/server/analysis/AnalysisModels.kt`
   * Seluruh DTO respons JSON analisis dan model dataset mentah.
3. `server/src/main/kotlin/com/aura/server/analysis/AnalysisCalculator.kt`
   * Seluruh rumus perhitungan 5 bagian analisis, rekonstruksi hari stok tersedia, persentil Fast/Slow, ABC Pareto, rasio tren, dan *pair counting* korelasi produk.
4. `server/src/main/kotlin/com/aura/server/analysis/AnalysisRepository.kt`
   * Interface repository analisis.
5. `server/src/main/kotlin/com/aura/server/analysis/ExposedAnalysisRepository.kt`
   * Implementasi query Exposed read-only dari tabel `barang`, `transaksi_penjualan`, `item_transaksi`, `restock`, `pergerakan_stok`, `pengeluaran`, dan `penyesuaian_stok`.
6. `server/src/main/kotlin/com/aura/server/analysis/AnalysisService.kt`
   * Service analisis dengan validasi RBAC (`Peran.OWNER` wajib, `KASIR` ditolak `403 Forbidden`) dan *in-memory cache* 5 menit per toko.
7. `server/src/main/kotlin/com/aura/server/routes/AnalysisRoutes.kt`
   * Definisi 6 endpoint Ktor di bawah `/api/v1/analisis`:
     * `GET /api/v1/analisis/ringkasan?periode=mingguan|bulanan`
     * `GET /api/v1/analisis/pergerakan?periode=mingguan|bulanan&jenis=fast|slow`
     * `GET /api/v1/analisis/umur-stok?periode=mingguan|bulanan`
     * `GET /api/v1/analisis/proporsi?periode=mingguan|bulanan&jenis=profit|pengeluaran`
     * `GET /api/v1/analisis/tren?periode=mingguan|bulanan&arah=naik|turun`
     * `GET /api/v1/analisis/korelasi?periode=mingguan|bulanan`
8. `server/src/test/kotlin/com/aura/server/analysis/AnalysisCalculatorTest.kt`
   * Unit test lengkap perhitungan finansial, Fast/Slow Moving, Pair Counting Lift > 1,2, dan proteksi role Kasir.
9. `server/scripts/seed_aura_dev.sql` *(opsional)*
   * Script SQL pengisi data dummy (9 produk + 110 transaksi multi-item) untuk pengujian lokal di PostgreSQL `aura_dev`.

**Sisi Android (`app/`):**
10. `app/src/main/java/com/aura/app/data/remote/dto/AnalisisDtos.kt`
    * DTO Gson untuk ke-6 endpoint analisis.
11. `app/src/main/java/com/aura/app/data/remote/api/AnalisisApiService.kt`
    * Interface Retrofit `AnalisisApiService`.
12. `app/src/main/java/com/aura/app/data/repository/AnalisisRepository.kt`
    * Repository Android dengan cache in-memory per periode dan fungsi `clearCache()` saat ganti sesi/toko.
13. `app/src/main/java/com/aura/app/ui/analisis/AnalisisViewModel.kt`
    * ViewModel pengelola state `Mingguan/Bulanan`, data overview, data 5 layar detail, `expandedCorrelationId` (untuk accordion single-expand), dan `sambungkanSesi(idToko)`.
14. `app/src/main/java/com/aura/app/ui/screens/analisis/AnalisisComponents.kt`
    * Komponen visual analisis: `PeriodSegmentedBar`, `FinancialLineChart` (`Canvas`), `ProportionDonutChart` (`Canvas`), `TrendSparkline` (`Canvas`), `StrProgressBar`, `ChangeBadge`, `StatusChip`, dan `ClickableAnalysisCard`.
15. `app/src/main/java/com/aura/app/ui/screens/analisis/AnalisisDetailScreens.kt`
    * 5 layar detail: `MovementListScreen`, `StockRunwayScreen`, `ProportionListScreen`, `TrendListScreen`, dan `CorrelationScreen` (accordion view-only).

---

#### A2. File Layar Utama Analisis yang Wajib Diganti Total (1 File)

16. `app/src/main/java/com/aura/app/ui/screens/analisis/AnalysisScreen.kt`
    * **Tindakan:** Timpa penuh dengan `AnalysisScreen.kt` dari branch ini. Tidak lagi membaca parameter Room lokal (`products, tx, items, ...`), melainkan membaca `AnalisisUiState` dari `AnalisisViewModel`.

---

#### A3. File Titik Sambung (*Wiring*) yang Perlu Di-merge Bagian Analisisnya (6 File)

Jangan menimpa seluruh isi 6 file ini secara buta jika di branch teman ada penambahan lain; cukup pastikan potongan kode analisis berikut masuk:

17. **`server/src/main/kotlin/com/aura/server/routes/Routing.kt`**
    * Tambahkan parameter `analysis: AnalysisComponents? = null` pada `configureRouting` (letakkan `additionalRoutes: Routing.() -> Unit = {}` di parameter paling akhir) dan panggil:
      ```kotlin
      if (auth != null && analysis != null) analysisRoutes(auth, analysis)
      ```

18. **`server/src/main/kotlin/com/aura/server/Application.kt`**
    * Di dalam `Application.module()`, inisialisasi `val analysis = analysisComponents()` lalu teruskan `analysis = analysis` ke `configureHttp(...)` dan `configureRouting(...)`.

19. **`app/src/main/java/com/aura/app/data/remote/RetrofitClient.kt`**
    * Tambahkan factory Retrofit untuk `AnalisisApiService`:
      ```kotlin
      fun createAnalisisApiService(
          baseUrl: String = BuildConfig.BASE_URL,
          tokenProvider: () -> String?,
          onUnauthorized: () -> Unit,
      ): AnalisisApiService = createRetrofit(baseUrl, tokenProvider, onUnauthorized)
          .create(AnalisisApiService::class.java)
      ```

20. **`app/src/main/java/com/aura/app/ui/AuraApp.kt`**
    * Buat instance `analisisApiService` dan `analisisRepository = remember { AnalisisRepository(analisisApiService) }`, lalu kirim `analisisRepository = analisisRepository` ke `AuraNavHost(...)`.

21. **`app/src/main/java/com/aura/app/navigation/Routes.kt`**
    * Pastikan terdapat konstanta route analisis berikut (dan masukkan ke `Routes.all`):
      ```kotlin
      const val ANALISIS = "analisis"
      const val ANALISIS_PERGERAKAN = "analisis_pergerakan"
      const val ANALISIS_UMUR_STOK = "analisis_umur_stok"
      const val ANALISIS_PROPORSI = "analisis_proporsi"
      const val ANALISIS_TREN = "analisis_tren"
      const val ANALISIS_KORELASI = "analisis_korelasi"
      ```

22. **`app/src/main/java/com/aura/app/navigation/AuraNavHost.kt`**
    * Terima parameter `analisisRepository: AnalisisRepository`.
    * Masukkan ke-5 route detail analisis ke `ownerRoutes`, `RouteNavHost` (`owner_tab_graph`), dan `routeTitle`.
    * Inisialisasi ViewModel:
      ```kotlin
      val analisisVm: AnalisisViewModel = viewModel(
          factory = AnalisisViewModel.factory(analisisRepository),
      )
      val analisisState by analisisVm.uiState.collectAsStateWithLifecycle()
      var analisisMovementJenis by rememberSaveable { mutableStateOf("fast") }
      var analisisProporsiJenis by rememberSaveable { mutableStateOf("profit") }
      var analisisTrenArah by rememberSaveable { mutableStateOf("naik") }
      ```
    * Di dalam `LaunchedEffect(sesiAktif?.akunId, sesiAktif?.idToko)` tambahkan:
      ```kotlin
      analisisVm.sambungkanSesi(sesiAktif?.idToko)
      ```
    * Hubungkan `Routes.ANALISIS`, `Routes.ANALISIS_PERGERAKAN`, `Routes.ANALISIS_UMUR_STOK`, `Routes.ANALISIS_PROPORSI`, `Routes.ANALISIS_TREN`, dan `Routes.ANALISIS_KORELASI` ke layar masing-masing (lihat implementasi di `AuraNavHost.kt` baris 690–756).

---

### KATEGORI B — KOMPONEN PENDUKUNG TOMBOL & WARNA (2 File)

Layar `AnalysisScreen.kt` dan `AnalisisDetailScreens.kt` menggunakan komponen tombol `AuraRedButton` dan warna `PressedRed` / `PressedSoft` / `DisabledRed` agar tombol memiliki efek tekan yang jelas:

23. **`app/src/main/java/com/aura/app/ui/theme/AuraColors.kt`**
    * Menambahkan 3 konstanta warna:
      ```kotlin
      internal val PressedRed = Color(0xFFE04848)
      internal val PressedSoft = Color(0xFFFDE8E8)
      internal val DisabledRed = Color(0xFFD7C8C6)
      ```
24. **`app/src/main/java/com/aura/app/ui/components/PrimaryButton.kt`**
    * Menambahkan `@Composable internal fun AuraRedButton(...)` yang memberikan animasi perubahan warna dari `Red (#930000)` ke `PressedRed (#E04848)` dan skala `0.97f` saat ditekan, serta memperbarui `Primary(...)` agar memakai `AuraRedButton`.

---

### KATEGORI C — OPSIONAL / TIDAK WAJIB DIPAKSAKAN (Perbaikan di Luar Tab Analisis — 12 File)

Perubahan di bawah ini dibuat untuk menyempurnakan UX dan memperbaiki bug kecil di luar tab Analisis. **Jika di branch teman file-file ini sudah dimodifikasi oleh teman, bagian ini TIDAK WAJIB diterapkan** (atau dapat diambil sebagian sesuai kebutuhan):

25. **`app/src/main/java/com/aura/app/ui/screens/kasir/CashierScreen.kt`** *(Opsional — UX Kasir)*
    * Menambahkan kotak indikator angka jumlah item yang diambil di sebelah kanan Harga & Stok.
    * Mengganti tombol `+` tunggal menjadi tombol stepper ganda `−` | `+` (terkunci di rentang `0` s.d. `stokSaatIni`) dengan efek tekan `PressedRed`.
    * Menampilkan total item di tombol `"Lihat Keranjang & Pembayaran (N)"`.
26. **`app/src/main/java/com/aura/app/ui/screens/kasir/CartScreen.kt`** *(Opsional — UX Keranjang)*
    * Mengunci tombol `+` di keranjang agar tidak bisa ditekan melebihi `stokSaatDipilih`.
27. **`app/src/main/java/com/aura/app/ui/screens/manajemen/ManagementScreen.kt`** *(Opsional — Pintasan Menu)*
    * Menambahkan item `"Catat restock" to "catat_restock"` di daftar Menu Manajemen.
28. **Perbaikan Bug `selectedProductId` di `AuraNavHost.kt`** *(Disarankan)*
    * Mengubah nilai awal `selectedProductId` dari `"BRG-001"` menjadi `""` dan mereset `selectedProductId = ""` saat membuka `PERGERAKAN_STOK` atau `CATAT_RESTOCK` dari `MENU_MANAJEMEN` agar server tidak menolak dengan error `"ID Barang tidak valid."`.
29. **`app/src/main/java/com/aura/app/ui/components/SecondaryButton.kt`** *(Opsional)*
    * Menambahkan efek warna latar `PressedSoft` saat tombol sekunder ditekan.
30–35. **Penggantian `Button` merah menjadi `AuraRedButton` pada layar Katalog/Supplier** *(Opsional)*:
    * `CategoryManagementScreen.kt`
    * `ProductDetailScreen.kt`
    * `ProductFormScreen.kt`
    * `ProductListScreen.kt`
    * `RestockScreen.kt`
    * `SupplierDetailScreen.kt`
    * `SupplierScreen.kt`
36. **`gradle/wrapper/gradle-wrapper.properties`** *(Perbaikan Build)*
    * Memperbaiki `distributionSha256Sum` ke checksum resmi Gradle 9.6.0 (`bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01`).

---

## 3. Prompt Siap Pakai untuk Diberikan ke AI Teman

Anda dapat menyalin prompt di bawah ini dan memberikannya kepada AI di laptop teman Anda saat ingin menggabungkan fitur Analisis ke branch miliknya:

```text
Tolong integrasikan fitur Analisis Bisnis dari branch `analisis` (atau mengacu pada file `DOKUMENTASI_INTEGRASI_ANALISIS.md`) ke branch ini dengan aturan ketat berikut:
1. Untuk segala hal yang berkaitan dengan Tab Analisis (Backend `/api/v1/analisis/*` di `com.aura.server.analysis` & `AnalysisRoutes.kt`, serta Android DTO, ApiService, Repository, `AnalisisViewModel`, `AnalisisComponents.kt`, `AnalysisScreen.kt`, `AnalisisDetailScreens.kt`, dan routing analisis di `Routes.kt` / `AuraNavHost.kt`), WAJIB menggunakan implementasi dari branch `analisis` milik PIC Analisis dan menggantikan implementasi analisis yang lama di branch ini.
2. Tambahkan juga `PressedRed`, `PressedSoft`, `DisabledRed` di `AuraColors.kt` serta fungsi `AuraRedButton` di `PrimaryButton.kt` karena dipakai oleh layar Analisis.
3. Untuk perubahan di luar fitur Analisis (Kategori C di dokumentasi, seperti layar Kasir, Katalog, atau Manajemen), JANGAN menimpa pekerjaan yang sudah ada di branch ini jika berkonflik; cukup terapkan jika aman atau biarkan menggunakan versi branch ini.
4. Setelah selesai, jalankan test di `server` dan `app` untuk memastikan kompilasi berhasil tanpa error.
```
