-- PrintFlow Database Schema
-- Run this against PostgreSQL to create all tables

-- Users
CREATE TABLE IF NOT EXISTS tbl_user (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    user_name TEXT NOT NULL UNIQUE,
    full_name TEXT NOT NULL,
    email TEXT,
    password_hash TEXT,
    user_id_method TEXT NOT NULL DEFAULT 'INTERNAL',
    oauth_provider TEXT,
    oauth_id TEXT,
    roles TEXT[] NOT NULL DEFAULT ARRAY['USER'],
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    blocked_reason TEXT,
    print_quota DECIMAL(10,4) DEFAULT 0,
    print_balance DECIMAL(10,4) DEFAULT 0,
    daily_print_limit INTEGER DEFAULT 0,
    totp_secret TEXT,
    totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    total_print_pages INTEGER DEFAULT 0,
    total_print_jobs INTEGER DEFAULT 0,
    total_print_cost DECIMAL(10,4) DEFAULT 0,
    date_created TIMESTAMP NOT NULL DEFAULT NOW(),
    date_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    date_blocked TIMESTAMP,
    date_deleted TIMESTAMP,
    notes TEXT,
    external_id TEXT
);

CREATE INDEX IF NOT EXISTS idx_user_user_name ON tbl_user(user_name);
CREATE INDEX IF NOT EXISTS idx_user_email ON tbl_user(email);
CREATE INDEX IF NOT EXISTS idx_user_uuid ON tbl_user(uuid);

-- User Accounts (financial)
CREATE TABLE IF NOT EXISTS tbl_user_account (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    user_id INTEGER NOT NULL REFERENCES tbl_user(id) ON DELETE CASCADE,
    account_name TEXT NOT NULL,
    balance DECIMAL(10,4) NOT NULL DEFAULT 0,
    overdraft_limit DECIMAL(10,4) DEFAULT 0
);

-- Sessions
CREATE TABLE IF NOT EXISTS tbl_session (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    user_id INTEGER NOT NULL REFERENCES tbl_user(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL,
    ip_address TEXT,
    user_agent TEXT,
    date_created TIMESTAMP NOT NULL DEFAULT NOW(),
    date_expires TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_session_token ON tbl_session(token_hash);
CREATE INDEX IF NOT EXISTS idx_session_user ON tbl_session(user_id);

-- User Groups
CREATE TABLE IF NOT EXISTS tbl_user_group (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    default_roles TEXT[] DEFAULT ARRAY['USER'],
    date_created TIMESTAMP NOT NULL DEFAULT NOW()
);

-- User Group Members
CREATE TABLE IF NOT EXISTS tbl_user_group_member (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES tbl_user(id) ON DELETE CASCADE,
    group_id INTEGER NOT NULL REFERENCES tbl_user_group(id) ON DELETE CASCADE,
    date_joined TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, group_id)
);

-- User Cards
CREATE TABLE IF NOT EXISTS tbl_user_card (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    user_id INTEGER NOT NULL REFERENCES tbl_user(id) ON DELETE CASCADE,
    card_number TEXT NOT NULL UNIQUE,
    card_name TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    date_created TIMESTAMP NOT NULL DEFAULT NOW()
);

-- User Emails
CREATE TABLE IF NOT EXISTS tbl_user_email (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES tbl_user(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    date_created TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, email)
);

-- User Attributes
CREATE TABLE IF NOT EXISTS tbl_user_attr (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES tbl_user(id) ON DELETE CASCADE,
    attr_key TEXT NOT NULL,
    attr_value TEXT,
    date_created TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, attr_key)
);

-- Accounts (financial)
CREATE TABLE IF NOT EXISTS tbl_account (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    account_type TEXT NOT NULL DEFAULT 'USER',
    account_name TEXT NOT NULL,
    description TEXT,
    balance DECIMAL(10,4) NOT NULL DEFAULT 0,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    default_cost_per_page_mono DECIMAL(10,4) DEFAULT 0.01,
    default_cost_per_page_color DECIMAL(10,4) DEFAULT 0.05,
    date_created TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Account Transactions
CREATE TABLE IF NOT EXISTS tbl_account_trx (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    account_id INTEGER NOT NULL REFERENCES tbl_account(id),
    user_id INTEGER REFERENCES tbl_user(id),
    trx_type TEXT NOT NULL,
    amount DECIMAL(10,4) NOT NULL,
    balance_before DECIMAL(10,4) NOT NULL,
    balance_after DECIMAL(10,4) NOT NULL,
    description TEXT,
    reference_id TEXT,
    date_created TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_account_trx ON tbl_account_trx(account_id);

-- Account Vouchers
CREATE TABLE IF NOT EXISTS tbl_account_voucher (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    voucher_code TEXT NOT NULL UNIQUE,
    account_id INTEGER NOT NULL REFERENCES tbl_account(id),
    amount DECIMAL(10,4) NOT NULL,
    remaining_amount DECIMAL(10,4) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    date_created TIMESTAMP NOT NULL DEFAULT NOW(),
    date_expires TIMESTAMP,
    date_used TIMESTAMP
);

-- Printer Groups
CREATE TABLE IF NOT EXISTS tbl_printer_group (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    display_order INTEGER DEFAULT 0,
    date_created TIMESTAMP NOT NULL DEFAULT NOW()
);

-- IPP Queues
CREATE TABLE IF NOT EXISTS tbl_ipp_queue (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL UNIQUE,
    uri TEXT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    printer_group_id INTEGER REFERENCES tbl_printer_group(id),
    date_created TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Printers
CREATE TABLE IF NOT EXISTS tbl_printer (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    ipp_printer_uri TEXT,
    display_name TEXT,
    printer_type TEXT NOT NULL DEFAULT 'NETWORK',
    printer_status TEXT NOT NULL DEFAULT 'OFFLINE',
    printer_group_id INTEGER REFERENCES tbl_printer_group(id),
    color_mode TEXT DEFAULT 'AUTO',
    supports_duplex BOOLEAN NOT NULL DEFAULT FALSE,
    supports_staple BOOLEAN NOT NULL DEFAULT FALSE,
    supports_punch BOOLEAN NOT NULL DEFAULT FALSE,
    supports_fold BOOLEAN NOT NULL DEFAULT FALSE,
    supports_banner BOOLEAN NOT NULL DEFAULT FALSE,
    max_paper_size TEXT DEFAULT 'A4',
    min_paper_size TEXT DEFAULT 'A5',
    cost_per_page_mono DECIMAL(10,4) DEFAULT 0.01,
    cost_per_page_color DECIMAL(10,4) DEFAULT 0.05,
    cost_per_sheet DECIMAL(10,4) DEFAULT 0,
    fixed_cost DECIMAL(10,4) DEFAULT 0,
    eco_print_cost_per_page DECIMAL(10,4) DEFAULT 0.005,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    require_release BOOLEAN NOT NULL DEFAULT TRUE,
    eco_print_default BOOLEAN NOT NULL DEFAULT FALSE,
    snmp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    snmp_community TEXT,
    snmp_host TEXT,
    total_print_jobs INTEGER DEFAULT 0,
    total_print_pages INTEGER DEFAULT 0,
    date_created TIMESTAMP NOT NULL DEFAULT NOW(),
    date_modified TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Documents (SafePages - incoming)
CREATE TABLE IF NOT EXISTS tbl_doc_in (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    user_id INTEGER NOT NULL REFERENCES tbl_user(id),
    doc_name TEXT NOT NULL,
    doc_type TEXT,
    file_path TEXT,
    file_size BIGINT DEFAULT 0,
    page_count INTEGER DEFAULT 0,
    doc_status TEXT NOT NULL DEFAULT 'PENDING',
    access_token TEXT UNIQUE,
    release_token TEXT UNIQUE,
    date_created TIMESTAMP NOT NULL DEFAULT NOW(),
    date_expires TIMESTAMP,
    date_modified TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_doc_in_user ON tbl_doc_in(user_id);
CREATE INDEX IF NOT EXISTS idx_doc_in_status ON tbl_doc_in(doc_status);
CREATE INDEX IF NOT EXISTS idx_doc_in_access_token ON tbl_doc_in(access_token);

-- Documents (outgoing - printed)
CREATE TABLE IF NOT EXISTS tbl_doc_out (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    doc_in_id INTEGER REFERENCES tbl_doc_in(id),
    user_id INTEGER NOT NULL REFERENCES tbl_user(id),
    printer_id INTEGER NOT NULL REFERENCES tbl_printer(id),
    doc_name TEXT NOT NULL,
    file_path TEXT,
    file_size BIGINT DEFAULT 0,
    page_count INTEGER DEFAULT 0,
    copies INTEGER DEFAULT 1,
    duplex TEXT DEFAULT 'NONE',
    color_mode TEXT DEFAULT 'AUTO',
    n_up TEXT DEFAULT '1',
    paper_size TEXT DEFAULT 'A4',
    eco_print BOOLEAN NOT NULL DEFAULT FALSE,
    cost_per_page DECIMAL(10,4) DEFAULT 0,
    total_cost DECIMAL(10,4) DEFAULT 0,
    doc_status TEXT NOT NULL DEFAULT 'PROCESSING',
    date_created TIMESTAMP NOT NULL DEFAULT NOW(),
    date_printed TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_doc_out_user ON tbl_doc_out(user_id);
CREATE INDEX IF NOT EXISTS idx_doc_out_printer ON tbl_doc_out(printer_id);

-- Print Logs / Audit Trail
CREATE TABLE IF NOT EXISTS tbl_doc_log (
    id SERIAL PRIMARY KEY,
    uuid TEXT NOT NULL UNIQUE,
    user_id INTEGER REFERENCES tbl_user(id),
    doc_in_id INTEGER REFERENCES tbl_doc_in(id),
    doc_out_id INTEGER REFERENCES tbl_doc_out(id),
    printer_id INTEGER REFERENCES tbl_printer(id),
    doc_name TEXT,
    doc_type TEXT,
    doc_page_count INTEGER,
    pages_printed INTEGER,
    sheets_printed INTEGER,
    job_status TEXT,
    total_cost DECIMAL(10,4),
    copies INTEGER DEFAULT 1,
    duplex TEXT,
    color_mode TEXT,
    n_up TEXT,
    eco_print BOOLEAN,
    date_created TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_doc_log_user ON tbl_doc_log(user_id);

-- Config Properties
CREATE TABLE IF NOT EXISTS tbl_config_property (
    id SERIAL PRIMARY KEY,
    prop_key TEXT NOT NULL UNIQUE,
    prop_value TEXT,
    description TEXT,
    category TEXT DEFAULT 'GENERAL',
    date_created TIMESTAMP NOT NULL DEFAULT NOW(),
    date_modified TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Sequences
CREATE TABLE IF NOT EXISTS tbl_sequence (
    id SERIAL PRIMARY KEY,
    seq_name TEXT NOT NULL UNIQUE,
    seq_value INTEGER NOT NULL DEFAULT 1,
    seq_increment INTEGER NOT NULL DEFAULT 1
);

-- App Logs
CREATE TABLE IF NOT EXISTS tbl_app_log (
    id SERIAL PRIMARY KEY,
    log_level TEXT NOT NULL DEFAULT 'INFO',
    log_message TEXT NOT NULL,
    logger_name TEXT,
    stack_trace TEXT,
    user_id INTEGER REFERENCES tbl_user(id),
    session_id INTEGER REFERENCES tbl_session(id),
    date_created TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_app_log_level ON tbl_app_log(log_level);
CREATE INDEX IF NOT EXISTS idx_app_log_date ON tbl_app_log(date_created);

PRINT 'PrintFlow schema created successfully!';
