ALTER TABLE log_aktivitas DROP CONSTRAINT IF EXISTS log_aktivitas_kategori_check;
ALTER TABLE log_aktivitas
    ADD CONSTRAINT log_aktivitas_kategori_check
        CHECK (kategori IN ('AKUN', 'LOGIN', 'KEUANGAN', 'BARANG', 'STOK', 'PENJUALAN'));

CREATE TABLE transaksi_penjualan (
    id UUID PRIMARY KEY,
    id_toko VARCHAR(8) NOT NULL REFERENCES toko(id_toko) ON DELETE CASCADE,
    nomor_transaksi VARCHAR(32) NOT NULL,
    akun_id UUID REFERENCES akun(id) ON DELETE SET NULL,
    nama_akun_snapshot VARCHAR(80) NOT NULL,
    metode_pembayaran VARCHAR(16) NOT NULL CHECK (metode_pembayaran IN ('TUNAI', 'TRANSFER', 'QRIS')),
    total BIGINT NOT NULL CHECK (total >= 0),
    uang_diterima BIGINT CHECK (uang_diterima >= 0),
    kembalian BIGINT CHECK (kembalian >= 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('SELESAI', 'DIBATALKAN')),
    alasan_pembatalan VARCHAR(500),
    catatan VARCHAR(1000),
    dibuat_pada BIGINT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_snapshot TEXT NOT NULL DEFAULT '',
    UNIQUE (id, id_toko),
    UNIQUE (id_toko, nomor_transaksi)
);
CREATE INDEX transaksi_penjualan_toko_waktu_idx
    ON transaksi_penjualan (id_toko, dibuat_pada DESC);

CREATE TABLE item_transaksi (
    id UUID PRIMARY KEY,
    id_toko VARCHAR(8) NOT NULL,
    transaksi_id UUID NOT NULL,
    barang_id UUID NOT NULL,
    nama_barang_snapshot VARCHAR(120) NOT NULL,
    satuan_snapshot VARCHAR(30) NOT NULL,
    jumlah INTEGER NOT NULL CHECK (jumlah > 0),
    harga_jual_snapshot BIGINT NOT NULL CHECK (harga_jual_snapshot >= 0),
    hpp_snapshot BIGINT NOT NULL CHECK (hpp_snapshot >= 0),
    FOREIGN KEY (transaksi_id, id_toko)
        REFERENCES transaksi_penjualan(id, id_toko) ON DELETE CASCADE,
    FOREIGN KEY (barang_id, id_toko)
        REFERENCES barang(id, id_toko) ON DELETE RESTRICT
);
CREATE INDEX item_transaksi_transaksi_idx ON item_transaksi (id_toko, transaksi_id);
CREATE INDEX item_transaksi_barang_idx ON item_transaksi (id_toko, barang_id);

CREATE TABLE pengajuan_pembatalan (
    id UUID PRIMARY KEY,
    id_toko VARCHAR(8) NOT NULL REFERENCES toko(id_toko) ON DELETE CASCADE,
    transaksi_id UUID NOT NULL,
    akun_id UUID REFERENCES akun(id) ON DELETE SET NULL,
    alasan VARCHAR(500) NOT NULL,
    dibuat_pada BIGINT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_snapshot TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (transaksi_id, id_toko)
        REFERENCES transaksi_penjualan(id, id_toko) ON DELETE CASCADE
);
CREATE INDEX pengajuan_pembatalan_transaksi_idx
    ON pengajuan_pembatalan (id_toko, transaksi_id, dibuat_pada DESC);
