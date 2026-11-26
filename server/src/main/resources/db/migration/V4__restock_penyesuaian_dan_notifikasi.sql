ALTER TABLE pergerakan_stok
    DROP CONSTRAINT IF EXISTS pergerakan_stok_jumlah_check;

ALTER TABLE pergerakan_stok
    ADD CONSTRAINT pergerakan_stok_jumlah_check
        CHECK (jumlah > 0 OR (jenis = 'PENYESUAIAN' AND jumlah = 0));

CREATE TABLE pengeluaran (
    id UUID PRIMARY KEY,
    id_toko VARCHAR(8) NOT NULL REFERENCES toko(id_toko) ON DELETE CASCADE,
    jenis VARCHAR(20) NOT NULL CHECK (jenis IN ('PEMBELIAN', 'KUSTOM')),
    jenis_biaya VARCHAR(40),
    judul VARCHAR(120) NOT NULL,
    barang_id UUID,
    supplier_id UUID,
    jumlah INTEGER CHECK (jumlah > 0),
    harga_per_item BIGINT CHECK (harga_per_item >= 0),
    nominal BIGINT NOT NULL CHECK (nominal >= 0),
    tanggal BIGINT NOT NULL,
    catatan VARCHAR(1000),
    bukti_file_id UUID,
    status VARCHAR(16) NOT NULL DEFAULT 'TERCATAT' CHECK (status IN ('TERCATAT', 'DIBATALKAN')),
    alasan_pembatalan VARCHAR(500),
    referensi_id UUID,
    jenis_referensi VARCHAR(30),
    dibuat_oleh UUID REFERENCES akun(id) ON DELETE SET NULL,
    dibuat_pada BIGINT NOT NULL,
    dibatalkan_oleh UUID REFERENCES akun(id) ON DELETE SET NULL,
    dibatalkan_pada BIGINT,
    UNIQUE (id, id_toko),
    FOREIGN KEY (barang_id, id_toko) REFERENCES barang(id, id_toko),
    FOREIGN KEY (supplier_id, id_toko) REFERENCES supplier(id, id_toko)
);
CREATE INDEX pengeluaran_toko_tanggal_idx ON pengeluaran (id_toko, tanggal DESC);
CREATE UNIQUE INDEX pengeluaran_restock_unik ON pengeluaran (id_toko, referensi_id)
    WHERE jenis = 'PEMBELIAN' AND jenis_referensi = 'RESTOCK';

CREATE TABLE restock (
    id UUID PRIMARY KEY,
    id_toko VARCHAR(8) NOT NULL REFERENCES toko(id_toko) ON DELETE CASCADE,
    barang_id UUID NOT NULL,
    supplier_id UUID NOT NULL,
    jumlah_masuk INTEGER NOT NULL CHECK (jumlah_masuk > 0),
    total_dibayar BIGINT NOT NULL CHECK (total_dibayar >= 0),
    harga_per_item BIGINT NOT NULL CHECK (harga_per_item >= 0),
    tanggal BIGINT NOT NULL,
    nota_file_id UUID,
    catatan VARCHAR(1000),
    akun_id UUID REFERENCES akun(id) ON DELETE SET NULL,
    dibuat_pada BIGINT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_snapshot TEXT NOT NULL DEFAULT '',
    UNIQUE (id, id_toko),
    FOREIGN KEY (barang_id, id_toko) REFERENCES barang(id, id_toko),
    FOREIGN KEY (supplier_id, id_toko) REFERENCES supplier(id, id_toko)
);
CREATE INDEX restock_toko_tanggal_idx ON restock (id_toko, tanggal DESC);

CREATE TABLE penyesuaian_stok (
    id UUID PRIMARY KEY,
    id_toko VARCHAR(8) NOT NULL REFERENCES toko(id_toko) ON DELETE CASCADE,
    barang_id UUID NOT NULL,
    stok_fisik INTEGER NOT NULL CHECK (stok_fisik >= 0),
    selisih INTEGER NOT NULL,
    alasan VARCHAR(20) NOT NULL CHECK (alasan IN ('RUSAK', 'HILANG', 'KADALUARSA', 'SALAH_CATAT', 'LAINNYA')),
    nilai_kerugian BIGINT NOT NULL CHECK (nilai_kerugian >= 0),
    catatan VARCHAR(1000),
    bukti_file_id UUID,
    tanggal BIGINT NOT NULL,
    akun_id UUID REFERENCES akun(id) ON DELETE SET NULL,
    dibuat_pada BIGINT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_snapshot TEXT NOT NULL DEFAULT '',
    UNIQUE (id, id_toko),
    FOREIGN KEY (barang_id, id_toko) REFERENCES barang(id, id_toko)
);
CREATE INDEX penyesuaian_stok_toko_tanggal_idx ON penyesuaian_stok (id_toko, tanggal DESC);

CREATE TABLE notifikasi (
    id UUID PRIMARY KEY,
    id_toko VARCHAR(8) NOT NULL REFERENCES toko(id_toko) ON DELETE CASCADE,
    jenis VARCHAR(30) NOT NULL,
    judul VARCHAR(160) NOT NULL,
    pesan VARCHAR(500) NOT NULL,
    referensi_id UUID,
    jenis_referensi VARCHAR(30),
    dibuat_pada BIGINT NOT NULL,
    dibaca_pada BIGINT
);
CREATE INDEX notifikasi_toko_belum_dibaca_idx ON notifikasi (id_toko, dibuat_pada DESC)
    WHERE dibaca_pada IS NULL;
