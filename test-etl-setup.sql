-- SQL Skripta za kreiranje test podataka za ETL Pipeline
-- Pokreni ovu skriptu pre testiranja

-- 1. Proveri postojeće videe
SELECT id, title, view_count FROM videos LIMIT 5;

-- 2. Dodaj test preglede za različite videe i dane
-- Zameni video_id sa stvarnim ID-jevima iz tvoje baze

-- Pregledi od danas (težina 8)
INSERT INTO video_views (video_id, user_id, viewed_at)
VALUES
    (1, 1, NOW()),
    (1, 2, NOW() - INTERVAL '2 hours'),
    (1, 3, NOW() - INTERVAL '4 hours'),
    (2, 1, NOW() - INTERVAL '1 hour');

-- Pregledi od juče (težina 7)
INSERT INTO video_views (video_id, user_id, viewed_at)
VALUES
    (1, 4, NOW() - INTERVAL '1 day'),
    (1, 5, NOW() - INTERVAL '1 day' - INTERVAL '3 hours'),
    (2, 2, NOW() - INTERVAL '1 day');

-- Pregledi od pre 2 dana (težina 6)
INSERT INTO video_views (video_id, user_id, viewed_at)
VALUES
    (1, 6, NOW() - INTERVAL '2 days'),
    (3, 1, NOW() - INTERVAL '2 days');

-- Pregledi od pre 3 dana (težina 5)
INSERT INTO video_views (video_id, user_id, viewed_at)
VALUES
    (2, 3, NOW() - INTERVAL '3 days'),
    (2, 4, NOW() - INTERVAL '3 days'),
    (3, 2, NOW() - INTERVAL '3 days');

-- Pregledi od pre 6 dana (težina 2)
INSERT INTO video_views (video_id, user_id, viewed_at)
VALUES
    (2, 5, NOW() - INTERVAL '6 days'),
    (3, 3, NOW() - INTERVAL '6 days');

-- Pregledi od pre 7 dana (težina 1)
INSERT INTO video_views (video_id, user_id, viewed_at)
VALUES
    (3, 4, NOW() - INTERVAL '7 days');

-- 3. Proveri koliko pregleda ima svaki video u poslednjih 7 dana
SELECT
    v.id,
    v.title,
    COUNT(vv.id) as view_count,
    MIN(vv.viewed_at) as oldest_view,
    MAX(vv.viewed_at) as newest_view
FROM videos v
LEFT JOIN video_views vv ON v.id = vv.video_id
WHERE vv.viewed_at >= NOW() - INTERVAL '7 days'
GROUP BY v.id, v.title
ORDER BY view_count DESC;

-- 4. Ručno izračunaj očekivani popularity score (za validaciju)
-- Primer za video ID 1:
-- 3 pregleda danas (8) + 2 pregleda juče (7) + 1 pregled pre 2 dana (6) + 1 pregled pre 6 dana (2)
-- = 3*8 + 2*7 + 1*6 + 1*2 = 24 + 14 + 6 + 2 = 46
