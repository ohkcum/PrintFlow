-- Seed data for PrintFlow
-- Run: Get-Content seed-data.sql | docker exec -i printflow-postgres psql -U printflow -d printflow

-- Users (password: admin123)
INSERT INTO tbl_user (uuid, user_name, full_name, email, password_hash, user_id_method, roles, status, print_quota, print_balance)
VALUES
  (gen_random_uuid(), 'admin', 'System Administrator', 'admin@printflow.local', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.GZFGH4l5eGzK1m', 'INTERNAL', ARRAY['ADMIN'], 'ACTIVE', '1000', '1000'),
  (gen_random_uuid(), 'demo', 'Demo User', 'demo@printflow.local', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.GZFGH4l5eGzK1m', 'INTERNAL', ARRAY['USER'], 'ACTIVE', '100', '100')
ON CONFLICT (user_name) DO UPDATE SET full_name = EXCLUDED.full_name;

-- User accounts
INSERT INTO tbl_user_account (uuid, user_id, account_name, balance)
SELECT gen_random_uuid(), id, 'Admin Account', '1000' FROM tbl_user WHERE user_name = 'admin'
ON CONFLICT DO NOTHING;
INSERT INTO tbl_user_account (uuid, user_id, account_name, balance)
SELECT gen_random_uuid(), id, 'Demo Account', '100' FROM tbl_user WHERE user_name = 'demo'
ON CONFLICT DO NOTHING;

-- Printer groups
INSERT INTO tbl_printer_group (uuid, name, description, display_order)
VALUES (gen_random_uuid(), 'Black & White', 'Black and white printers', 1)
ON CONFLICT (name) DO NOTHING;
INSERT INTO tbl_printer_group (uuid, name, description, display_order)
VALUES (gen_random_uuid(), 'Color Printers', 'Color printing devices', 2)
ON CONFLICT (name) DO NOTHING;

-- IPP Queues
INSERT INTO tbl_ipp_queue (uuid, name, uri, is_enabled, is_default, printer_group_id)
SELECT gen_random_uuid(), 'bw-printer-queue', 'ipp://localhost:631/printers/bw-printer', true, true, g.id
FROM tbl_printer_group g WHERE g.name = 'Black & White'
ON CONFLICT (name) DO NOTHING;

INSERT INTO tbl_ipp_queue (uuid, name, uri, is_enabled, is_default, printer_group_id)
SELECT gen_random_uuid(), 'color-printer-queue', 'ipp://localhost:631/printers/color-printer', true, false, g.id
FROM tbl_printer_group g WHERE g.name = 'Color Printers'
ON CONFLICT (name) DO NOTHING;

-- Printers
INSERT INTO tbl_printer (uuid, name, description, ipp_printer_uri, display_name, printer_type, printer_status, printer_group_id, color_mode, supports_duplex, supports_staple, max_paper_size, min_paper_size, cost_per_page_mono, cost_per_page_color, eco_print_cost_per_page, is_enabled, is_public, require_release)
SELECT
  gen_random_uuid(), 'BW-HP-LaserJet', 'HP LaserJet Pro B&W', 'ipp://localhost:631/printers/bw-printer', 'Black & White Laser',
  'NETWORK', 'OFFLINE', g.id, 'MONOCHROME', true, true, 'A4', 'A5', '0.01', '0.05', '0.005', true, true, true
FROM tbl_printer_group g WHERE g.name = 'Black & White'
ON CONFLICT (name) DO NOTHING;

INSERT INTO tbl_printer (uuid, name, description, ipp_printer_uri, display_name, printer_type, printer_status, printer_group_id, color_mode, supports_duplex, max_paper_size, min_paper_size, cost_per_page_mono, cost_per_page_color, eco_print_cost_per_page, is_enabled, is_public, require_release)
SELECT
  gen_random_uuid(), 'Color-Epson-WorkForce', 'Epson WorkForce Color Printer', 'ipp://localhost:631/printers/color-printer', 'Color Printer',
  'NETWORK', 'OFFLINE', g.id, 'AUTO', true, 'A4', 'A5', '0.01', '0.10', '0.005', true, true, true
FROM tbl_printer_group g WHERE g.name = 'Color Printers'
ON CONFLICT (name) DO NOTHING;

-- Config
INSERT INTO tbl_config_property (prop_key, prop_value, description, category) VALUES
  ('app.name', 'PrintFlow', 'Application name', 'GENERAL'),
  ('app.version', '0.1.0', 'Application version', 'GENERAL'),
  ('document.retention.days', '30', 'Days before documents expire', 'DOCUMENT'),
  ('document.max.upload.size', '52428800', 'Max upload size in bytes', 'DOCUMENT'),
  ('print.default.copies', '1', 'Default number of copies', 'PRINT'),
  ('auth.session.timeout', '3600', 'Session timeout in seconds', 'AUTH'),
  ('auth.totp.enabled', 'false', 'Require TOTP for all users', 'AUTH')
ON CONFLICT (prop_key) DO UPDATE SET prop_value = EXCLUDED.prop_value;

-- User groups
INSERT INTO tbl_user_group (uuid, name, description, default_roles) VALUES (gen_random_uuid(), 'Students', 'Default student group', ARRAY['USER']) ON CONFLICT (name) DO NOTHING;
INSERT INTO tbl_user_group (uuid, name, description, default_roles) VALUES (gen_random_uuid(), 'Staff', 'Staff and faculty', ARRAY['USER', 'DELEGATOR']) ON CONFLICT (name) DO NOTHING;

-- Select to confirm
SELECT user_name, full_name, status FROM tbl_user;
SELECT name, description FROM tbl_printer_group;
SELECT name, display_name, color_mode FROM tbl_printer;
