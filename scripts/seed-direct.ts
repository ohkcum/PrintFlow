// Direct seed runner
import postgres from "postgres";

const sql = postgres("postgresql://printflow:printflow@localhost:5432/printflow");

async function run() {
  console.log("Starting seed...");

  try {
    // Admin user
    const [admin] = await sql`
      INSERT INTO tbl_user (uuid, user_name, full_name, email, password_hash, user_id_method, roles, status, print_quota, print_balance)
      VALUES (
        ${crypto.randomUUID()},
        'admin',
        'System Administrator',
        'admin@printflow.local',
        '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.GZFGH4l5eGzK1m',
        'INTERNAL',
        ARRAY['ADMIN'],
        'ACTIVE',
        '1000',
        '1000'
      )
      ON CONFLICT (user_name) DO UPDATE SET full_name = EXCLUDED.full_name
      RETURNING id, user_name
    `;
    console.log("Admin:", admin);

    // Demo user
    const [demo] = await sql`
      INSERT INTO tbl_user (uuid, user_name, full_name, email, password_hash, user_id_method, roles, status, print_quota, print_balance)
      VALUES (
        ${crypto.randomUUID()},
        'demo',
        'Demo User',
        'demo@printflow.local',
        '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.GZFGH4l5eGzK1m',
        'INTERNAL',
        ARRAY['USER'],
        'ACTIVE',
        '100',
        '100'
      )
      ON CONFLICT (user_name) DO UPDATE SET full_name = EXCLUDED.full_name
      RETURNING id, user_name
    `;
    console.log("Demo:", demo);

    // Printer groups
    const [bwGroup] = await sql`
      INSERT INTO tbl_printer_group (uuid, name, description, display_order)
      VALUES (${crypto.randomUUID()}, 'Black & White', 'Black and white printers', 1)
      ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description
      RETURNING id
    `;
    const [colorGroup] = await sql`
      INSERT INTO tbl_printer_group (uuid, name, description, display_order)
      VALUES (${crypto.randomUUID()}, 'Color Printers', 'Color printing devices', 2)
      ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description
      RETURNING id
    `;
    console.log("Printer groups created");

    // IPP Queues
    await sql`
      INSERT INTO tbl_ipp_queue (uuid, name, uri, is_enabled, is_default, printer_group_id)
      VALUES (${crypto.randomUUID()}, 'bw-printer-queue', 'ipp://localhost:631/printers/bw-printer', true, true, ${bwGroup.id})
      ON CONFLICT (name) DO NOTHING
    `;
    await sql`
      INSERT INTO tbl_ipp_queue (uuid, name, uri, is_enabled, is_default, printer_group_id)
      VALUES (${crypto.randomUUID()}, 'color-printer-queue', 'ipp://localhost:631/printers/color-printer', true, false, ${colorGroup.id})
      ON CONFLICT (name) DO NOTHING
    `;
    console.log("IPP queues created");

    // Printers
    await sql`
      INSERT INTO tbl_printer (uuid, name, description, ipp_printer_uri, display_name, printer_type, printer_status, printer_group_id, color_mode, supports_duplex, supports_staple, max_paper_size, min_paper_size, cost_per_page_mono, cost_per_page_color, eco_print_cost_per_page, is_enabled, is_public, require_release)
      VALUES (
        ${crypto.randomUUID()}, 'BW-HP-LaserJet', 'HP LaserJet Pro B&W', 'ipp://localhost:631/printers/bw-printer', 'Black & White Laser',
        'NETWORK', 'OFFLINE', ${bwGroup.id}, 'MONOCHROME', true, true, 'A4', 'A5', '0.01', '0.05', '0.005', true, true, true
      )
      ON CONFLICT (name) DO NOTHING
    `;
    await sql`
      INSERT INTO tbl_printer (uuid, name, description, ipp_printer_uri, display_name, printer_type, printer_status, printer_group_id, color_mode, supports_duplex, max_paper_size, min_paper_size, cost_per_page_mono, cost_per_page_color, eco_print_cost_per_page, is_enabled, is_public, require_release)
      VALUES (
        ${crypto.randomUUID()}, 'Color-Epson-WorkForce', 'Epson WorkForce Color Printer', 'ipp://localhost:631/printers/color-printer', 'Color Printer',
        'NETWORK', 'OFFLINE', ${colorGroup.id}, 'AUTO', true, 'A4', 'A5', '0.01', '0.10', '0.005', true, true, true
      )
      ON CONFLICT (name) DO NOTHING
    `;
    console.log("Printers created");

    // Config
    await sql`INSERT INTO tbl_config_property (prop_key, prop_value, description, category) VALUES
      ('app.name', 'PrintFlow', 'Application name', 'GENERAL'),
      ('app.version', '0.1.0', 'Application version', 'GENERAL'),
      ('document.retention.days', '30', 'Days before documents expire', 'DOCUMENT'),
      ('document.max.upload.size', '52428800', 'Max upload size in bytes', 'DOCUMENT'),
      ('print.default.copies', '1', 'Default number of copies', 'PRINT'),
      ('auth.session.timeout', '3600', 'Session timeout in seconds', 'AUTH'),
      ('auth.totp.enabled', 'false', 'Require TOTP for all users', 'AUTH')
      ON CONFLICT (prop_key) DO UPDATE SET prop_value = EXCLUDED.prop_value
    `;
    console.log("Config created");

    // User groups
    await sql`INSERT INTO tbl_user_group (uuid, name, description, default_roles) VALUES (${crypto.randomUUID()}, 'Students', 'Default student group', ARRAY['USER']) ON CONFLICT (name) DO NOTHING`;
    await sql`INSERT INTO tbl_user_group (uuid, name, description, default_roles) VALUES (${crypto.randomUUID()}, 'Staff', 'Staff and faculty', ARRAY['USER', 'DELEGATOR']) ON CONFLICT (name) DO NOTHING`;

    console.log("Seed completed successfully!");
  } catch (err) {
    console.error("Seed error:", err);
  }

  await sql.end();
}

run();
