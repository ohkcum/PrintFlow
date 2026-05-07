import { describe, it, expect } from "vitest";
import {
  userRoleSchema,
  isAdmin,
  isManager,
  isDelegator,
  isPrintSiteUser,
  hasRole,
} from "../src/user-role.js";
import { userStatusSchema } from "../src/user-status.js";
import { documentStatusSchema } from "../src/document-status.js";
import { printJobStatusSchema } from "../src/print-job-status.js";
import { accountTypeSchema } from "../src/account-type.js";
import { printerStatusSchema } from "../src/printer-status.js";
import { paginationParamsSchema, paginatedResponse } from "../src/pagination.js";

describe("Common — User Roles", () => {
  it("should validate valid roles", () => {
    expect(userRoleSchema.parse("ADMIN")).toBe("ADMIN");
    expect(userRoleSchema.parse("USER")).toBe("USER");
    expect(userRoleSchema.parse("MANAGER")).toBe("MANAGER");
  });

  it("should reject invalid roles", () => {
    expect(() => userRoleSchema.parse("SUPERADMIN")).toThrow();
    expect(() => userRoleSchema.parse("")).toThrow();
  });

  it("should check admin role", () => {
    expect(isAdmin(["ADMIN"])).toBe(true);
    expect(isAdmin(["USER"])).toBe(false);
    expect(isAdmin([])).toBe(false);
  });

  it("should check manager role", () => {
    expect(isManager(["ADMIN"])).toBe(true);
    expect(isManager(["MANAGER"])).toBe(true);
    expect(isManager(["USER"])).toBe(false);
  });

  it("should check delegator role", () => {
    expect(isDelegator(["ADMIN"])).toBe(true);
    expect(isDelegator(["MANAGER"])).toBe(true);
    expect(isDelegator(["DELEGATOR"])).toBe(true);
    expect(isDelegator(["USER"])).toBe(false);
  });

  it("should check print site user", () => {
    expect(isPrintSiteUser(["PRINT_SITE_USER"])).toBe(true);
    expect(isPrintSiteUser(["USER"])).toBe(false);
  });

  it("should check hasRole", () => {
    expect(hasRole(["ADMIN", "USER"], "ADMIN")).toBe(true);
    expect(hasRole(["ADMIN", "USER"], "DELEGATOR")).toBe(false);
    expect(hasRole(["ADMIN"], ["ADMIN", "MANAGER"])).toBe(true);
  });
});

describe("Common — Enums", () => {
  it("should validate user statuses", () => {
    expect(userStatusSchema.parse("ACTIVE")).toBe("ACTIVE");
    expect(userStatusSchema.parse("BLOCKED")).toBe("BLOCKED");
    expect(() => userStatusSchema.parse("INVALID")).toThrow();
  });

  it("should validate document statuses", () => {
    expect(documentStatusSchema.parse("READY")).toBe("READY");
    expect(documentStatusSchema.parse("PROCESSING")).toBe("PROCESSING");
    expect(documentStatusSchema.parse("ERROR")).toBe("ERROR");
    expect(() => documentStatusSchema.parse("UNKNOWN")).toThrow();
  });

  it("should validate print job statuses", () => {
    expect(printJobStatusSchema.parse("QUEUED")).toBe("QUEUED");
    expect(printJobStatusSchema.parse("COMPLETED")).toBe("COMPLETED");
    expect(printJobStatusSchema.parse("FAILED")).toBe("FAILED");
  });

  it("should validate account types", () => {
    expect(accountTypeSchema.parse("USER")).toBe("USER");
    expect(accountTypeSchema.parse("GROUP")).toBe("GROUP");
    expect(accountTypeSchema.parse("SHARED")).toBe("SHARED");
  });

  it("should validate printer statuses", () => {
    expect(printerStatusSchema.parse("ONLINE")).toBe("ONLINE");
    expect(printerStatusSchema.parse("OFFLINE")).toBe("OFFLINE");
    expect(printerStatusSchema.parse("BUSY")).toBe("BUSY");
  });
});

describe("Common — Pagination", () => {
  it("should parse valid pagination params", () => {
    const result = paginationParamsSchema.parse({ page: "2", limit: "25" });
    expect(result.page).toBe(2);
    expect(result.limit).toBe(25);
  });

  it("should use defaults for missing params", () => {
    const result = paginationParamsSchema.parse({});
    expect(result.page).toBe(1);
    expect(result.limit).toBe(20);
    expect(result.sortOrder).toBe("desc");
  });

  it("should reject limit over 100", () => {
    expect(() => paginationParamsSchema.parse({ limit: "500" })).toThrow();
  });

  it("should reject negative page", () => {
    expect(() => paginationParamsSchema.parse({ page: "0" })).toThrow();
  });

  it("should generate paginated response", () => {
    const items = [{ id: 1 }, { id: 2 }, { id: 3 }];
    const result = paginatedResponse(items, 50, { page: 1, limit: 3, sortOrder: "desc" });
    expect(result.data).toHaveLength(3);
    expect(result.total).toBe(50);
    expect(result.totalPages).toBe(17);
    expect(result.hasNext).toBe(true);
    expect(result.hasPrev).toBe(false);
  });

  it("should handle last page correctly", () => {
    const items = [{ id: 1 }];
    const result = paginatedResponse(items, 50, { page: 17, limit: 3, sortOrder: "desc" });
    expect(result.hasNext).toBe(false);
    expect(result.hasPrev).toBe(true);
  });
});
