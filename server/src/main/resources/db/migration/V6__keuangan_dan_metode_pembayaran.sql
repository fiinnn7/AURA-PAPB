ALTER TABLE toko
    ADD COLUMN nama_penerima VARCHAR(120),
    ADD COLUMN info_rekening VARCHAR(500),
    ADD COLUMN qris_file_id UUID;

ALTER TABLE pengeluaran
    ADD COLUMN request_hash CHAR(64),
    ADD COLUMN response_snapshot TEXT;

CREATE TABLE pemasukan_kustom (
    id UUID PRIMARY KEY,
    id_toko VARCHAR(8) NOT NULL REFERENCES toko(id_toko) ON DELETE CASCADE,
    judul VARCHAR(120) NOT NULL,
    nominal BIGINT NOT NULL CHECK (nominal >= 0),
    tanggal BIGINT NOT NULL,
    catatan VARCHAR(1000),
    bukti_file_id UUID,
    status VARCHAR(16) NOT NULL DEFAULT 'TERCATAT'
        CHECK (status IN ('TERCATAT', 'DIBATALKAN')),
    alasan_pembatalan VARCHAR(500),
    dibuat_oleh UUID REFERENCES akun(id) ON DELETE SET NULL,
    dibuat_pada BIGINT NOT NULL,
    dibatalkan_oleh UUID REFERENCES akun(id) ON DELETE SET NULL,
    dibatalkan_pada BIGINT,
    request_hash CHAR(64) NOT NULL,
    response_snapshot TEXT NOT NULL DEFAULT '',
    UNIQUE (id, id_toko),
    FOREIGN KEY (bukti_file_id, id_toko) REFERENCES file(id, id_toko)
);
CREATE INDEX pemasukan_kustom_toko_tanggal_idx
    ON pemasukan_kustom (id_toko, tanggal DESC);
