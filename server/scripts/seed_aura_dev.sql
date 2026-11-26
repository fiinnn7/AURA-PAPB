-- Seeder data dummy untuk pengujian tab Analisis pada database PostgreSQL lokal (aura_dev).
-- Jalankan setelah minimal 1 akun OWNER terdaftar lewat aplikasi atau endpoint /auth/register.
-- Script ini otomatis mengambil toko dari akun OWNER pertama yang aktif.

DO $$
DECLARE
    v_toko VARCHAR(8);
    v_owner UUID;
    v_now BIGINT := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    v_day BIGINT := 86400000;
    v_supplier UUID := gen_random_uuid();
    b_indomie UUID := gen_random_uuid();
    b_telur UUID := gen_random_uuid();
    b_kopi UUID := gen_random_uuid();
    b_gula UUID := gen_random_uuid();
    b_esteh UUID := gen_random_uuid();
    b_minyak UUID := gen_random_uuid();
    b_sabun UUID := gen_random_uuid();
    b_pel UUID := gen_random_uuid();
    b_saus UUID := gen_random_uuid();
    v_tx UUID;
    i INT;
    v_ts BIGINT;
BEGIN
    SELECT id_toko, id INTO v_toko, v_owner
    FROM akun
    WHERE peran = 'OWNER' AND status = 'AKTIF'
    ORDER BY dibuat_pada ASC
    LIMIT 1;

    IF v_toko IS NULL THEN
        RAISE EXCEPTION 'Belum ada akun OWNER aktif. Silakan daftar Owner di aplikasi terlebih dahulu.';
    END IF;

    INSERT INTO supplier (id, id_toko, nama, link, catatan, status, dibuat_pada)
    VALUES (v_supplier, v_toko, 'Grosir Berkah Utama', NULL, 'Supplier demo analisis', 'AKTIF', v_now - 45 * v_day);

    INSERT INTO barang (id, id_toko, nama, satuan, sku, harga_jual, stok_minimum, stok_saat_ini, hpp_rata_rata, status, foto_file_id, dibuat_oleh, dibuat_pada, diubah_pada)
    VALUES
        (b_indomie, v_toko, 'Indomie Soto Kuah', 'pcs', 'DM-001', 3500, 15, 24, 2600, 'AKTIF', NULL, v_owner, v_now - 45 * v_day, v_now),
        (b_telur,   v_toko, 'Telur Ayam Ras 1kg', 'kg',  'DM-002', 28000, 5, 4, 24000, 'AKTIF', NULL, v_owner, v_now - 45 * v_day, v_now),
        (b_kopi,    v_toko, 'Kopi Kapal Api 165g', 'pcs','DM-003', 15000, 10, 8, 12000, 'AKTIF', NULL, v_owner, v_now - 45 * v_day, v_now),
        (b_gula,    v_toko, 'Gula Pasir 1kg', 'kg',      'DM-004', 17500, 10, 20, 14500, 'AKTIF', NULL, v_owner, v_now - 45 * v_day, v_now),
        (b_esteh,   v_toko, 'Es Teh Botol 350ml', 'btl', 'DM-005', 4500, 12, 18, 3000, 'AKTIF', NULL, v_owner, v_now - 45 * v_day, v_now),
        (b_minyak,  v_toko, 'Minyak Goreng 1L', 'btl',   'DM-006', 18000, 10, 35, 16500, 'AKTIF', NULL, v_owner, v_now - 45 * v_day, v_now),
        (b_sabun,   v_toko, 'Deterjen Bubuk 800g', 'pcs','DM-007', 22000, 8, 30, 18000, 'AKTIF', NULL, v_owner, v_now - 45 * v_day, v_now),
        (b_pel,     v_toko, 'Pembersih Lantai 750ml','btl','DM-008', 19000, 5, 16, 15000, 'AKTIF', NULL, v_owner, v_now - 45 * v_day, v_now),
        (b_saus,    v_toko, 'Saus Sambal Sachet', 'pcs', 'DM-009', 2000, 10, 40, 1300, 'AKTIF', NULL, v_owner, v_now - 45 * v_day, v_now);

    -- Pergerakan stok awal agar rekonstruksi hari stok aktif >= 28 hari
    INSERT INTO pergerakan_stok (id, id_toko, barang_id, akun_id, jenis, sumber, jumlah, stok_sebelum, stok_sesudah, referensi_id, jenis_referensi, dibuat_pada)
    SELECT gen_random_uuid(), v_toko, id, v_owner, 'MASUK', 'STOK_AWAL', 50, 0, 50, NULL, NULL, v_now - 40 * v_day
    FROM barang WHERE id IN (b_indomie, b_telur, b_kopi, b_gula, b_esteh, b_minyak, b_sabun, b_pel, b_saus);

    -- Restock untuk kontribusi pengeluaran
    INSERT INTO restock (id, id_toko, barang_id, supplier_id, jumlah_masuk, total_dibayar, harga_per_item, tanggal, nota_file_id, catatan, akun_id, dibuat_pada, request_hash, response_snapshot)
    VALUES
        (gen_random_uuid(), v_toko, b_minyak, v_supplier, 40, 660000, 16500, v_now - 5 * v_day, NULL, 'Restock minyak', v_owner, v_now - 5 * v_day, repeat('a', 64), '{}'),
        (gen_random_uuid(), v_toko, b_telur,  v_supplier, 20, 480000, 24000, v_now - 4 * v_day, NULL, 'Restock telur', v_owner, v_now - 4 * v_day, repeat('b', 64), '{}'),
        (gen_random_uuid(), v_toko, b_indomie,v_supplier, 80, 208000, 2600,  v_now - 3 * v_day, NULL, 'Restock indomie', v_owner, v_now - 3 * v_day, repeat('c', 64), '{}'),
        (gen_random_uuid(), v_toko, b_kopi,   v_supplier, 25, 300000, 12000, v_now - 2 * v_day, NULL, 'Restock kopi', v_owner, v_now - 2 * v_day, repeat('d', 64), '{}');

    -- Pengeluaran operasional kustom & penyesuaian stok
    INSERT INTO pengeluaran (id, id_toko, jenis, jenis_biaya, judul, nominal, tanggal, status, dibuat_oleh, dibuat_pada, request_hash, response_snapshot)
    VALUES
        (gen_random_uuid(), v_toko, 'KUSTOM', 'LISTRIK', 'Token Listrik Toko', 150000, v_now - 3 * v_day, 'TERCATAT', v_owner, v_now - 3 * v_day, repeat('e', 64), '{}'),
        (gen_random_uuid(), v_toko, 'KUSTOM', 'LAINNYA', 'Plastik & Kresek', 45000, v_now - 10 * v_day, 'TERCATAT', v_owner, v_now - 10 * v_day, repeat('f', 64), '{}');

    INSERT INTO penyesuaian_stok (id, id_toko, barang_id, stok_fisik, selisih, alasan, nilai_kerugian, catatan, tanggal, akun_id, dibuat_pada, request_hash, response_snapshot)
    VALUES
        (gen_random_uuid(), v_toko, b_telur, 4, -1, 'RUSAK', 24000, 'Telur pecah', v_now - 2 * v_day, v_owner, v_now - 2 * v_day, repeat('g', 64), '{}');

    -- 110 transaksi multi-item selama 35 hari terakhir (Indomie+Telur+Saus, Kopi+Gula, Es Teh tren naik 7 hari terakhir)
    FOR i IN 1..110 LOOP
        v_tx := gen_random_uuid();
        IF i <= 60 THEN
            v_ts := v_now - ((i % 6) + 1) * v_day;
        ELSE
            v_ts := v_now - (8 + (i % 26)) * v_day;
        END IF;

        IF i % 2 = 0 THEN
            INSERT INTO transaksi_penjualan (id, id_toko, nomor_transaksi, akun_id, nama_akun_snapshot, metode_pembayaran, total, uang_diterima, kembalian, status, dibuat_pada, request_hash, response_snapshot)
            VALUES (v_tx, v_toko, 'INV-DM-' || LPAD(i::TEXT, 4, '0'), v_owner, 'Owner Demo', 'TUNAI', 37000, 40000, 3000, 'SELESAI', v_ts, repeat('h', 64), '{}');

            INSERT INTO item_transaksi (id, id_toko, transaksi_id, barang_id, nama_barang_snapshot, satuan_snapshot, jumlah, harga_jual_snapshot, hpp_snapshot)
            VALUES
                (gen_random_uuid(), v_toko, v_tx, b_indomie, 'Indomie Soto Kuah', 'pcs', 2, 3500, 2600),
                (gen_random_uuid(), v_toko, v_tx, b_telur, 'Telur Ayam Ras 1kg', 'kg', 1, 28000, 24000),
                (gen_random_uuid(), v_toko, v_tx, b_saus, 'Saus Sambal Sachet', 'pcs', 1, 2000, 1300);
        ELSE
            INSERT INTO transaksi_penjualan (id, id_toko, nomor_transaksi, akun_id, nama_akun_snapshot, metode_pembayaran, total, uang_diterima, kembalian, status, dibuat_pada, request_hash, response_snapshot)
            VALUES (v_tx, v_toko, 'INV-DM-' || LPAD(i::TEXT, 4, '0'), v_owner, 'Owner Demo', 'QRIS', 32500, NULL, NULL, 'SELESAI', v_ts, repeat('i', 64), '{}');

            INSERT INTO item_transaksi (id, id_toko, transaksi_id, barang_id, nama_barang_snapshot, satuan_snapshot, jumlah, harga_jual_snapshot, hpp_snapshot)
            VALUES
                (gen_random_uuid(), v_toko, v_tx, b_kopi, 'Kopi Kapal Api 165g', 'pcs', 1, 15000, 12000),
                (gen_random_uuid(), v_toko, v_tx, b_gula, 'Gula Pasir 1kg', 'kg', 1, 17500, 14500);
        END IF;

        -- Es Teh laris di 7 hari terakhir (tren naik), Deterjen laris di > 7 hari lalu (tren turun)
        IF i <= 35 THEN
            INSERT INTO item_transaksi (id, id_toko, transaksi_id, barang_id, nama_barang_snapshot, satuan_snapshot, jumlah, harga_jual_snapshot, hpp_snapshot)
            VALUES (gen_random_uuid(), v_toko, v_tx, b_esteh, 'Es Teh Botol 350ml', 'btl', 3, 4500, 3000);
        ELSIF i > 75 THEN
            INSERT INTO item_transaksi (id, id_toko, transaksi_id, barang_id, nama_barang_snapshot, satuan_snapshot, jumlah, harga_jual_snapshot, hpp_snapshot)
            VALUES (gen_random_uuid(), v_toko, v_tx, b_sabun, 'Deterjen Bubuk 800g', 'pcs', 2, 22000, 18000);
        END IF;
    END LOOP;
END $$;
