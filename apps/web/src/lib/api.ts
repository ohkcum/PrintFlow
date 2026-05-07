const API_URL = process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001";

function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("printflow_token");
}

function getHeaders(): Record<string, string> {
  const token = getToken();
  return token
    ? { Authorization: `Bearer ${token}`, "Content-Type": "application/json" }
    : { "Content-Type": "application/json" };
}

async function apiFetch<T = any>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const res = await fetch(`${API_URL}/api/v1${path}`, {
    ...options,
    headers: {
      ...getHeaders(),
      ...((options.headers as Record<string, string>) ?? {}),
    },
  });
  const data = await res.json();
  if (!data.success) {
    throw new Error(data.error?.message ?? "Request failed");
  }
  return data;
}

// ─── Auth ────────────────────────────────────────────────────────────────────

export const authApi = {
  me: () => apiFetch("/auth/me"),
  login: (body: { userName: string; password: string; totpToken?: string }) =>
    apiFetch("/auth/login", { method: "POST", body: JSON.stringify(body) }),
  logout: () => apiFetch("/auth/logout", { method: "POST" }),
};

// ─── Users ───────────────────────────────────────────────────────────────────

export type UserSummary = {
  id: number;
  uuid: string;
  userName: string;
  fullName: string;
  email: string | null;
  roles: string[];
  status: string;
  printBalance: string;
  printQuota: string;
  totpEnabled: boolean;
  dateCreated: string;
};

export type UserDetail = UserSummary & {
  account: { id: number; balance: string } | null;
  cards: Array<{
    id: number;
    cardId: string;
    cardType: string;
    cardName?: string;
    isActive: boolean;
  }>;
  groups: Array<{ id: number; name: string }>;
  blockedReason?: string;
};

export type PaginatedResponse<T> = {
  success: boolean;
  data: {
    data: T[];
    total: number;
    page: number;
    limit: number;
    totalPages: number;
    hasNext: boolean;
    hasPrev: boolean;
  };
  timestamp: string;
};

export const usersApi = {
  list: (params?: {
    page?: number;
    limit?: number;
    search?: string;
    status?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.search) q.set("search", params.search);
    if (params?.status) q.set("status", params.status);
    return apiFetch<PaginatedResponse<UserSummary>>(`/users?${q}`);
  },
  get: (id: number) => apiFetch<UserDetail>(`/users/${id}`),
  create: (body: {
    userName: string;
    fullName: string;
    email?: string;
    password?: string;
    roles?: string[];
    printQuota?: string;
  }) =>
    apiFetch<UserSummary>("/users", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  update: (
    id: number,
    body: {
      fullName?: string;
      email?: string;
      roles?: string[];
      status?: string;
      printQuota?: string;
      blockedReason?: string;
    },
  ) =>
    apiFetch<UserSummary>(`/users/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),
  delete: (id: number) => apiFetch(`/users/${id}`, { method: "DELETE" }),
  addCard: (
    userId: number,
    body: { cardId: string; cardType?: string; cardName?: string },
  ) =>
    apiFetch(`/users/${userId}/cards`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
  listGroups: () =>
    apiFetch<Array<{ id: number; name: string; description: string }>>(
      "/users/groups",
    ),
};

// ─── Documents ───────────────────────────────────────────────────────────────

export type DocIn = {
  id: number;
  uuid: string;
  userId: number;
  userName: string;
  docName: string;
  docType: string;
  docStatus: string;
  filePath: string;
  fileSize: number;
  mimeType: string;
  pageCount: number;
  thumbnailPath: string | null;
  createdBy: string;
  defaultCopies: number;
  defaultDuplex: string;
  defaultColorMode: string;
  expiresAt: string | null;
  dateCreated: string;
  dateModified: string;
};

export const documentsApi = {
  list: (params?: {
    page?: number;
    limit?: number;
    status?: string;
    search?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.status) q.set("status", params.status);
    if (params?.search) q.set("search", params.search);
    return apiFetch<PaginatedResponse<DocIn>>(`/documents?${q}`);
  },
  get: (id: number) => apiFetch<DocIn>(`/documents/${id}`),
  delete: (id: number) => apiFetch(`/documents/${id}`, { method: "DELETE" }),
  listLogs: (params?: { page?: number; limit?: number }) => {
    const q = new URLSearchParams();
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    return apiFetch<PaginatedResponse<any>>(`/documents/logs?${q}`);
  },
  getOverlay: (id: number) =>
    apiFetch<{ svg64: string | null; json64: string | null }>(
      `/documents/${id}/overlay`,
    ),
  saveOverlay: (
    id: number,
    body: { svgBase64?: string; jsonBase64?: string },
  ) =>
    apiFetch(`/documents/${id}/overlay`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
};

// ─── Printers ────────────────────────────────────────────────────────────────

export type Printer = {
  id: number;
  uuid: string;
  name: string;
  description: string | null;
  displayName: string;
  printerType: string;
  printerStatus: string;
  colorMode: string;
  supportsDuplex: boolean;
  supportsStaple: boolean;
  supportsPunch: boolean;
  supportsFold: boolean;
  supportsBanner: boolean;
  maxPaperSize: string;
  minPaperSize: string;
  costPerPageMono: string;
  costPerPageColor: string;
  costPerSheet: string;
  fixedCost: string;
  isEnabled: boolean;
  isPublic: boolean;
  requireRelease: boolean;
  ippPrinterUri?: string;
  cupsPrinterName?: string;
  printerGroupId?: string | number;
  ecoPrintDefault?: boolean;
  snmpEnabled?: boolean;
  snmpCommunity?: string;
  totalPrintJobs: number;
  totalPrintPages: number;
  dateCreated: string;
};

export type PrinterGroup = {
  id: number;
  name: string;
  description: string | null;
  displayOrder: number;
};

export const printersApi = {
  list: (params?: { groupId?: number; status?: string }) => {
    const q = new URLSearchParams();
    if (params?.groupId) q.set("groupId", String(params.groupId));
    if (params?.status) q.set("status", params.status);
    return apiFetch<{ success: boolean; data: Printer[]; timestamp: string }>(
      `/printers?${q}`,
    );
  },
  get: (id: number) => apiFetch<Printer>(`/printers/${id}`),
  listGroups: () =>
    apiFetch<{ success: boolean; data: PrinterGroup[]; timestamp: string }>(
      "/printers/groups",
    ),
  create: (body: Partial<Printer> & { name: string }) =>
    apiFetch<Printer>("/printers", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  update: (id: number, body: Partial<Printer>) =>
    apiFetch<Printer>(`/printers/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),
  delete: (id: number) => apiFetch(`/printers/${id}`, { method: "DELETE" }),
};

// ─── Accounts ────────────────────────────────────────────────────────────────

export type Account = {
  id: number;
  uuid: string;
  accountType: "USER" | "GROUP" | "SHARED" | "SYSTEM";
  userId: number | null;
  groupId: number | null;
  accountName: string;
  description: string | null;
  balance: string;
  overdraftLimit: string;
  creditLimit: string;
  isEnabled: boolean;
  defaultCostPerPageMono: string | null;
  defaultCostPerPageColor: string | null;
  dateCreated: string;
  dateModified: string;
};

export type AccountBalance = {
  balance: number;
  overdraftLimit: number;
  creditLimit: number;
  availableBalance: number;
  isEnabled: boolean;
};

export type AccountTransaction = {
  id: number;
  uuid: string;
  accountId: number;
  userId: number | null;
  trxType:
    | "PRINT_JOB"
    | "MANUAL_ADD"
    | "MANUAL_DEDUCT"
    | "VOUCHER_REDEEM"
    | "REFUND"
    | "TRANSFER_IN"
    | "TRANSFER_OUT"
    | "INITIAL";
  amount: string;
  balanceBefore: string;
  balanceAfter: string;
  referenceId: number | null;
  referenceType: string | null;
  description: string | null;
  notes: string | null;
  isReversed: boolean;
  dateCreated: string;
};

export type AccountVoucher = {
  id: number;
  uuid: string;
  voucherCode: string;
  accountId: number;
  nominalValue: string;
  remainingValue: string;
  validFrom: string;
  validUntil: string | null;
  isSingleUse: boolean;
  maxPrintPages: number;
  isActive: boolean;
  usedByUserId: number | null;
  usedAt: string | null;
  createdBy: number | null;
  dateCreated: string;
};

export type FinancialSummary = {
  success: boolean;
  data: {
    totalAccounts: number;
    totalBalance: number;
    userAccountCount: number;
    totalTransactions: number;
    recentTransactions: Array<{
      id: number;
      accountId: number;
      trxType: string;
      amount: string;
      balanceAfter: string;
      description: string | null;
      dateCreated: string;
      userName: string | null;
    }>;
  };
  timestamp: string;
};

export const accountsApi = {
  summary: () => apiFetch<FinancialSummary>("/accounts/summary"),

  list: (params?: {
    page?: number;
    limit?: number;
    search?: string;
    type?: string;
    sortBy?: string;
    sortOrder?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.search) q.set("search", params.search);
    if (params?.type) q.set("type", params.type);
    if (params?.sortBy) q.set("sortBy", params.sortBy);
    if (params?.sortOrder) q.set("sortOrder", params.sortOrder);
    return apiFetch<PaginatedResponse<Account>>(`/accounts?${q}`);
  },

  get: (id: number) => apiFetch<Account>(`/accounts/${id}`),

  balance: (id: number) => apiFetch<AccountBalance>(`/accounts/${id}/balance`),

  transactions: (
    id: number,
    params?: {
      page?: number;
      limit?: number;
      type?: string;
      dateFrom?: string;
      dateTo?: string;
    },
  ) => {
    const q = new URLSearchParams();
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.type) q.set("type", params.type);
    if (params?.dateFrom) q.set("dateFrom", params.dateFrom);
    if (params?.dateTo) q.set("dateTo", params.dateTo);
    return apiFetch<PaginatedResponse<AccountTransaction>>(
      `/accounts/${id}/transactions?${q}`,
    );
  },

  refill: (
    id: number,
    body: { amount: number; description?: string; notes?: string },
  ) =>
    apiFetch<{
      accountId: number;
      oldBalance: number;
      amount: number;
      newBalance: number;
    }>(`/accounts/${id}/refill`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  deduct: (
    id: number,
    body: { amount: number; description?: string; notes?: string },
  ) =>
    apiFetch<{
      accountId: number;
      oldBalance: number;
      amount: number;
      newBalance: number;
    }>(`/accounts/${id}/deduct`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  transfer: (
    id: number,
    body: { toAccountId: number; amount: number; description?: string },
  ) =>
    apiFetch<{
      fromAccountId: number;
      toAccountId: number;
      amount: number;
      fromNewBalance: number;
      toNewBalance: number;
    }>(`/accounts/${id}/transfer`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  // Voucher operations
  vouchers: (params?: {
    page?: number;
    limit?: number;
    search?: string;
    status?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.search) q.set("search", params.search);
    if (params?.status) q.set("status", params.status);
    return apiFetch<PaginatedResponse<AccountVoucher>>(
      `/accounts/vouchers?${q}`,
    );
  },

  createVouchers: (body: {
    accountId: number;
    nominalValue: number;
    count?: number;
    validUntil?: string;
    isSingleUse?: boolean;
    maxPrintPages?: number;
    description?: string;
  }) =>
    apiFetch<AccountVoucher[]>("/accounts/vouchers", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  lookupVoucher: (code: string) =>
    apiFetch<AccountVoucher & { isExpired: boolean; isRedeemable: boolean }>(
      `/accounts/vouchers/${code}`,
    ),

  redeemVoucher: (body: { voucherCode: string }) =>
    apiFetch<{
      message: string;
      voucherCode: string;
      nominalValue: number;
      newBalance: number;
    }>("/accounts/vouchers/redeem", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  deactivateVoucher: (id: number) =>
    apiFetch("/accounts/vouchers/" + id, { method: "DELETE" }),
};

// ─── Upload ─────────────────────────────────────────────────────────────────

export const uploadApi = {
  upload: async (
    file: File,
    onProgress?: (pct: number) => void,
  ): Promise<{ id: number; docName: string }> => {
    const token = getToken();
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("POST", `${API_URL}/api/v1/documents/upload`);

      if (token) xhr.setRequestHeader("Authorization", `Bearer ${token}`);

      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable && onProgress) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
      };

      xhr.onload = () => {
        try {
          const data = JSON.parse(xhr.responseText);
          if (data.success) resolve(data.data);
          else reject(new Error(data.error?.message ?? "Upload failed"));
        } catch {
          reject(new Error("Invalid response"));
        }
      };

      xhr.onerror = () => reject(new Error("Network error"));
      const formData = new FormData();
      formData.append("file", file);
      xhr.send(formData);
    });
  },
};

// ─── Job Tickets ────────────────────────────────────────────────────────────────

export type JobTicket = {
  uuid: string;
  ticketNumber: string;
  userId: number;
  docName: string;
  copies: number;
  printerName: string;
  printerRedirect: string | null;
  status: "PENDING" | "PRINTING" | "COMPLETED" | "CANCELLED";
  copiesPrinted: number;
  totalCost: string;
  submitTime: string;
  deliveryTime: string;
  label: string | null;
  domain: string | null;
  use: string | null;
  tag: string | null;
  isReopened: boolean;
  createdAt: string;
};

export type JobTicketSummary = {
  totalPending: number;
  printJobs: number;
  copyJobs: number;
  totalCost: string;
};

export type PaginatedJobTickets = PaginatedResponse<JobTicket>;

export const jobTicketsApi = {
  summary: () => apiFetch<JobTicketSummary>("/job-tickets/summary"),
  list: (params?: {
    page?: number;
    limit?: number;
    userId?: number;
    search?: string;
    status?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.userId) q.set("userId", String(params.userId));
    if (params?.search) q.set("search", params.search);
    if (params?.status) q.set("status", params.status);
    return apiFetch<PaginatedJobTickets>(`/job-tickets?${q}`);
  },
  get: (uuid: string) => apiFetch<JobTicket>(`/job-tickets/${uuid}`),
  create: (body: {
    docName: string;
    copies?: number;
    printerName: string;
    label?: string;
    domain?: string;
    use?: string;
    tag?: string;
    deliveryDate?: string;
  }) =>
    apiFetch<JobTicket>("/job-tickets", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  update: (uuid: string, body: Partial<JobTicket>) =>
    apiFetch<JobTicket>(`/job-tickets/${uuid}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),
  print: (uuid: string, body?: { printerId?: number; printerName?: string }) =>
    apiFetch<{ message: string; ticket: JobTicket }>(
      `/job-tickets/${uuid}/print`,
      { method: "POST", body: JSON.stringify(body ?? {}) },
    ),
  settle: (uuid: string) =>
    apiFetch<{ message: string; ticket: JobTicket }>(
      `/job-tickets/${uuid}/settle`,
      { method: "POST" },
    ),
  complete: (uuid: string) =>
    apiFetch<{ message: string; ticket: JobTicket }>(
      `/job-tickets/${uuid}/complete`,
      { method: "POST" },
    ),
  cancel: (uuid: string) =>
    apiFetch<{ message: string; ticket: JobTicket }>(
      `/job-tickets/${uuid}/cancel`,
      { method: "POST" },
    ),
  reopen: (uuid: string, body?: { extraCopies?: number }) =>
    apiFetch<JobTicket>(`/job-tickets/${uuid}/reopen`, {
      method: "POST",
      body: JSON.stringify(body ?? {}),
    }),
  delete: (uuid: string) =>
    apiFetch<{ message: string }>(`/job-tickets/${uuid}`, { method: "DELETE" }),
  searchNumbers: (q: string, limit = 10) =>
    apiFetch<{ data: string[] }>(
      `/job-tickets/numbers/search?q=${encodeURIComponent(q)}&limit=${limit}`,
    ),
};

// ─── IPP Print Server ──────────────────────────────────────────────────────────

export type IppQueue = {
  id: number;
  name: string;
  displayName: string;
  ippPrinterUri: string | null;
  isEnabled: boolean;
  requireRelease: boolean;
  printerType: string;
  printerStatus: string;
  activeJobCount?: number;
};

export type IppPrintJob = {
  id: number;
  uuid: string;
  docInId: number;
  userId: number;
  userName: string;
  printerId: number;
  printerName: string;
  copyCount: number;
  duplex: string;
  colorMode: string;
  paperSize: string;
  estimatedPages: number;
  estimatedCost: string;
  status: string;
  dateCreated: string;
};

export type PaginatedIppJobs = PaginatedResponse<IppPrintJob>;

export const ippApi = {
  status: () =>
    apiFetch<{
      status: string;
      version: string;
      totalPrinters: number;
      activeJobs: number;
    }>("/ipp/status"),
  listQueues: (status = "all") =>
    apiFetch<{ data: IppQueue[] }>(`/ipp/queues?status=${status}`),
  getQueue: (name: string) =>
    apiFetch<IppQueue>(`/ipp/queues/${encodeURIComponent(name)}`),
  submitJob: (body: {
    printerName: string;
    userId?: number;
    userName?: string;
    jobName?: string;
    copies?: number;
    duplex?: "NONE" | "PORTRAIT" | "LANDSCAPE";
    colorMode?: "AUTO" | "MONOCHROME" | "COLOR";
    pageSize?: string;
    documentBase64?: string;
    documentUrl?: string;
    options?: Record<string, string | number | boolean>;
  }) =>
    apiFetch<{
      jobId: number;
      docId: number;
      printerName: string;
      totalCost: string;
      pagesPrinted: number;
      status: string;
    }>("/ipp/print", { method: "POST", body: JSON.stringify(body) }),
  listJobs: (params?: {
    page?: number;
    limit?: number;
    printerId?: number;
    status?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.printerId) q.set("printerId", String(params.printerId));
    if (params?.status) q.set("status", params.status);
    return apiFetch<PaginatedIppJobs>(`/ipp/jobs?${q}`);
  },
  getJob: (id: number) => apiFetch<IppPrintJob>(`/ipp/jobs/${id}`),
  cancelJob: (id: number) =>
    apiFetch<{ message: string; jobId: number }>(`/ipp/jobs/${id}/cancel`, {
      method: "POST",
    }),
};

// ─── Email Configuration ───────────────────────────────────────────────────────

export const emailApi = {
  send: (body: {
    to: string;
    toName?: string;
    subject: string;
    body: string;
    bodyHtml?: string;
    cc?: string[];
    bcc?: string[];
  }) =>
    apiFetch<{ messageId: string; accepted: string[] }>("/config/email/send", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  testConnection: () =>
    apiFetch<{ connected: boolean }>("/config/email/test", { method: "POST" }),
};

// ─── SOffice / LibreOffice Configuration ──────────────────────────────────────

export type SOfficeStatus = {
  enabled: boolean;
  running: boolean;
  numWorkers: number;
};

export const sofficeApi = {
  status: () =>
    apiFetch<SOfficeStatus>("/config/soffice/status", { method: "POST" }),
  start: () =>
    apiFetch<{ message: string }>("/config/soffice/start", { method: "POST" }),
  stop: () =>
    apiFetch<{ message: string }>("/config/soffice/stop", { method: "POST" }),
  convert: (body: {
    inputPath: string;
    outputFormat?: string;
    options?: Record<string, string | number | boolean>;
  }) =>
    apiFetch<{ success: boolean; outputPath: string; error?: string }>(
      "/config/soffice/convert",
      {
        method: "POST",
        body: JSON.stringify(body),
      },
    ),
};

// ─── PGP / GPG Configuration ───────────────────────────────────────────────────

export type PgpKeyInfo = {
  keyId: string;
  userId: string;
  fingerprint: string;
  algorithm: string;
  created: string;
  expires: string | null;
  isEncryptionKey: boolean;
  isSigningKey: boolean;
};

export const pgpApi = {
  status: () =>
    apiFetch<{ configured: boolean; keyInfo?: PgpKeyInfo }>(
      "/config/pgp/status",
    ),
  importSecretKey: (body: { armoredKey: string; passphrase: string }) =>
    apiFetch<{ message: string }>("/config/pgp/import-secret", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  importPublicKey: (body: { armoredKey: string }) =>
    apiFetch<{ message: string }>("/config/pgp/import-public", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  encrypt: (body: {
    plaintext: string;
    recipientKeyId: string;
    sign?: boolean;
  }) =>
    apiFetch<{ encrypted: string }>("/config/pgp/encrypt", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  decrypt: (body: { encrypted: string }) =>
    apiFetch<{ plaintext: string }>("/config/pgp/decrypt", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  sign: (body: { content: string }) =>
    apiFetch<{ signature: string }>("/config/pgp/sign", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  verify: (body: { content: string; signature: string }) =>
    apiFetch<{ valid: boolean }>("/config/pgp/verify", {
      method: "POST",
      body: JSON.stringify(body),
    }),
};

// ─── Reports ────────────────────────────────────────────────────────────────

export type ReportsSummary = {
  overview: {
    totalUsers: number;
    totalTransactions: number;
    totalPrintJobs: number;
    totalPrinters: number;
    totalTickets: number;
  };
  monthlyStats: Array<{ month: string; count: number; totalAmount: string }>;
  topPrinters: Array<{
    printerName: string;
    jobCount: number;
    totalPages: number;
  }>;
  topUsers: Array<{
    userId: number;
    userName: string;
    fullName: string;
    jobCount: number;
    totalPages: number;
    totalCost: string;
  }>;
  dailyPrints: Array<{ date: string; jobs: number; pages: number }>;
  trxByType: Array<{ trxType: string; count: number; total: string }>;
  ticketsByStatus: Array<{ status: string; count: number }>;
};

export const reportsApi = {
  summary: () => apiFetch<{ data: ReportsSummary }>("/reports/summary"),
  accountTrx: (params?: {
    accountType?: string;
    trxType?: string;
    dateFrom?: string;
    dateTo?: string;
    sortField?: string;
    sortOrder?: string;
    page?: number;
    limit?: number;
    format?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.accountType) q.set("accountType", params.accountType);
    if (params?.trxType) q.set("trxType", params.trxType);
    if (params?.dateFrom) q.set("dateFrom", params.dateFrom);
    if (params?.dateTo) q.set("dateTo", params.dateTo);
    if (params?.sortField) q.set("sortField", params.sortField);
    if (params?.sortOrder) q.set("sortOrder", params.sortOrder);
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.format) q.set("format", params.format);
    return apiFetch<any>(`/reports/account-trx?${q}`);
  },
  userPrintout: (params?: {
    groupBy?: string;
    aspect?: string;
    dateFrom?: string;
    dateTo?: string;
    sortField?: string;
    sortOrder?: string;
    page?: number;
    limit?: number;
    format?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.groupBy) q.set("groupBy", params.groupBy);
    if (params?.aspect) q.set("aspect", params.aspect);
    if (params?.dateFrom) q.set("dateFrom", params.dateFrom);
    if (params?.dateTo) q.set("dateTo", params.dateTo);
    if (params?.sortField) q.set("sortField", params.sortField);
    if (params?.sortOrder) q.set("sortOrder", params.sortOrder);
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.format) q.set("format", params.format);
    return apiFetch<any>(`/reports/user-printout?${q}`);
  },
  vouchers: () => apiFetch<any>("/reports/vouchers"),
  documents: (params?: {
    dateFrom?: string;
    dateTo?: string;
    page?: number;
    limit?: number;
    format?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.dateFrom) q.set("dateFrom", params.dateFrom);
    if (params?.dateTo) q.set("dateTo", params.dateTo);
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.format) q.set("format", params.format);
    return apiFetch<any>(`/reports/documents?${q}`);
  },
};

// ─── POS ────────────────────────────────────────────────────────────────────

export type PosItem = {
  id: number;
  name: string;
  cost: number;
  category?: string;
  isActive: boolean;
  createdAt: string;
};

export type PosPurchase = {
  id: number;
  uuid: string;
  receiptNumber: string;
  userId: number | null;
  userName: string | null;
  items: Array<{
    itemIndex: number;
    name: string;
    quantity: number;
    unitCost: number;
  }>;
  totalCost: number;
  paymentType: string;
  comment: string | null;
  createdAt: string;
};

export type PosSummary = {
  totalRevenue: number;
  todayRevenue: number;
  totalSales: number;
  todaySales: number;
  avgTransaction: number;
  revenueByPaymentType: Record<string, number>;
  topItems: Array<{ name: string; revenue: number }>;
  activeItems: number;
};

export const posApi = {
  items: () => apiFetch<{ data: { items: PosItem[] } }>("/pos/items"),
  itemsAll: () => apiFetch<{ data: { items: PosItem[] } }>("/pos/items/all"),
  createItem: (body: { name: string; cost: number; category?: string }) =>
    apiFetch<{ data: { item: PosItem } }>("/pos/items", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  updateItem: (id: number, body: Partial<PosItem>) =>
    apiFetch<{ data: { item: PosItem } }>(`/pos/items/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),
  deleteItem: (id: number) =>
    apiFetch(`/pos/items/${id}`, { method: "DELETE" }),
  sell: (body: {
    userId?: number;
    items: Array<{ itemId: number; quantity: number }>;
    paymentType?: string;
    comment?: string;
  }) =>
    apiFetch<{ data: { purchase: PosPurchase } }>("/pos/sales", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  deposit: (body: {
    userId: number;
    amount: number;
    paymentType?: string;
    comment?: string;
  }) =>
    apiFetch<{ data: { purchase: PosPurchase } }>("/pos/deposits", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  purchases: (params?: {
    userId?: string;
    page?: number;
    limit?: number;
    dateFrom?: string;
    dateTo?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.userId) q.set("userId", params.userId);
    if (params?.page) q.set("page", String(params.page));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.dateFrom) q.set("dateFrom", params.dateFrom);
    if (params?.dateTo) q.set("dateTo", params.dateTo);
    return apiFetch<any>(`/pos/purchases?${q}`);
  },
  summary: () => apiFetch<{ data: PosSummary }>("/pos/summary"),
  receipt: (uuid: string) =>
    apiFetch<{ data: { purchase: PosPurchase } }>(`/pos/receipt/${uuid}`),
};

// ─── QR Code Release ─────────────────────────────────────────────────────────

export const qrApi = {
  validate: (uuid: string) =>
    apiFetch<{
      data: {
        valid: boolean;
        documentId: number;
        docName: string;
        pageCount: number;
        status: string;
        expired: boolean;
        message: string;
      };
    }>(`/qr/validate/${uuid}`),
  release: (uuid: string) =>
    apiFetch<{
      data: {
        message: string;
        documentId: number;
        docName: string;
        pageCount: number;
      };
    }>(`/qr/release/${uuid}`),
  getInfo: (uuid: string) =>
    apiFetch<{ data: { document: any } }>(`/qr/release/${uuid}`),
};

// ─── Telegram ────────────────────────────────────────────────────────────────

export const telegramApi = {
  status: () =>
    apiFetch<{
      data: {
        configured: boolean;
        enabled: boolean;
        botUsername: string | null;
      };
    }>("/telegram/status"),
  send: (body: { telegramId: string; message: string }) =>
    apiFetch("/telegram/send", { method: "POST", body: JSON.stringify(body) }),
  notifyUser: (body: { userId: number; message: string }) =>
    apiFetch("/telegram/notify-user", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  link: (body: { userId: number; telegramId: string }) =>
    apiFetch("/telegram/link", { method: "POST", body: JSON.stringify(body) }),
  unlink: (userId: number) =>
    apiFetch(`/telegram/link/${userId}`, { method: "DELETE" }),
};

// ─── OAuth ─────────────────────────────────────────────────────────────────

export type OAuthProvider = {
  id: string;
  name: string;
  configured: boolean;
  icon: string;
};

export const oauthApi = {
  providers: () =>
    apiFetch<{ data: { providers: OAuthProvider[] } }>("/oauth/providers"),
  authorizeUrl: (provider: string, redirectUri?: string) => {
    const q = redirectUri
      ? `?redirect_uri=${encodeURIComponent(redirectUri)}`
      : "";
    return `${process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001"}/api/v1/oauth/authorize/${provider}${q}`;
  },
};

// ─── SNMP ─────────────────────────────────────────────────────────────────

export type SnmpPrinterStatus = {
  printerId: number;
  printerName: string;
  host: string;
  snmpStatus: string;
  snmpStatusCode: number | null;
  errorState: string | null;
  supplies: {
    type: number | null;
    level: number | null;
    max: number | null;
    unit: number | null;
  };
  polledAt: string;
};

export type SnmpPrinterSupplies = {
  printerId: number;
  printerName: string;
  supplies: Array<{
    index: number;
    type: string | null;
    level: number | null;
    max: number | null;
    unit: string | null;
    percentage: number | null;
  }>;
};

export const snmpApi = {
  status: (
    printerId: number,
    params?: { host?: string; community?: string },
  ) => {
    const q = new URLSearchParams();
    if (params?.host) q.set("host", params.host);
    if (params?.community) q.set("community", params.community);
    return apiFetch<{ data: SnmpPrinterStatus }>(
      `/snmp/printer/${printerId}/status?${q}`,
    );
  },
  supplies: (
    printerId: number,
    params?: { host?: string; community?: string },
  ) => {
    const q = new URLSearchParams();
    if (params?.host) q.set("host", params.host);
    if (params?.community) q.set("community", params.community);
    return apiFetch<{ data: SnmpPrinterSupplies }>(
      `/snmp/printer/${printerId}/supplies?${q}`,
    );
  },
  counters: (
    printerId: number,
    params?: { host?: string; community?: string },
  ) => {
    const q = new URLSearchParams();
    if (params?.host) q.set("host", params.host);
    if (params?.community) q.set("community", params.community);
    return apiFetch<any>(`/snmp/printer/${printerId}/counters?${q}`);
  },
  discover: (body: {
    network?: string;
    community?: string;
    timeout?: number;
  }) =>
    apiFetch<any>("/snmp/discover", {
      method: "POST",
      body: JSON.stringify(body),
    }),
};
