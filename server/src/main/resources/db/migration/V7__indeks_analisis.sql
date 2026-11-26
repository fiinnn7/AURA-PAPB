CREATE INDEX IF NOT EXISTS transaksi_penjualan_analisis_idx
    ON transaksi_penjualan (id_toko, status, dibuat_pada);

CREATE INDEX IF NOT EXISTS restock_analisis_idx
    ON restock (id_toko, tanggal);

CREATE INDEX IF NOT EXISTS penyesuaian_stok_analisis_idx
    ON penyesuaian_stok (id_toko, tanggal);

CREATE INDEX IF NOT EXISTS pengeluaran_analisis_idx
    ON pengeluaran (id_toko, jenis, status, tanggal);
