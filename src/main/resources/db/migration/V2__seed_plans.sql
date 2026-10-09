-- Seed subscription plans. Values are real, enforced server-side limits.
INSERT INTO plans (code, name, storage_bytes, max_file_bytes, sharing_enabled, versioning_enabled, max_versions, api_access, retention_days, max_downloads_per_day, sort_order)
VALUES
    ('FREE',     'Free',     2147483648,        104857600, TRUE,  FALSE, 1,  TRUE,  7,  NULL, 1),
    ('BASIC',    'Basic',    53687091200,       1073741824, TRUE,  FALSE, 3,  TRUE,  14, NULL, 2),
    ('PRO',      'Pro',      536870912000,      10737418240, TRUE, TRUE, 10, TRUE,  30, NULL, 3),
    ('BUSINESS', 'Business', 2199023255552,     53687091200, TRUE,  TRUE, 100, TRUE, 90, NULL, 4)
ON CONFLICT (code) DO NOTHING;
