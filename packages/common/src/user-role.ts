import { z } from "zod";

// Rôles mirrors PrintFlowLite's SpRole enum
// ADMIN: full system access
// MANAGER: manage users/groups, view reports
// DELEGATOR: delegate print jobs on behalf of others
// USER: basic user — upload, release own jobs
// JOB_TICKET_ISSUER: can create/manage job tickets
// MAIL_TICKET_ISSUER: can create/manage mail tickets
// PRINT_SITE_USER: anonymous user at public print station
// PGP_USER: can sign/encrypt PDFs with PGP

export const userRoles = [
  "ADMIN",
  "MANAGER",
  "DELEGATOR",
  "USER",
  "JOB_TICKET_ISSUER",
  "MAIL_TICKET_ISSUER",
  "PRINT_SITE_USER",
  "PGP_USER",
] as const;

export type UserRole = (typeof userRoles)[number];

export const userRoleSchema = z.enum(userRoles);
export type UserRoleSchema = z.infer<typeof userRoleSchema>;

export function isAdmin(roles: UserRole[]): boolean {
  return roles.includes("ADMIN");
}

export function isManager(roles: UserRole[]): boolean {
  return roles.includes("ADMIN") || roles.includes("MANAGER");
}

export function isDelegator(roles: UserRole[]): boolean {
  return roles.includes("ADMIN") || roles.includes("MANAGER") || roles.includes("DELEGATOR");
}

export function isPrintSiteUser(roles: UserRole[]): boolean {
  return roles.includes("PRINT_SITE_USER");
}

export function hasRole(
  userRoles: UserRole[],
  required: UserRole | UserRole[]
): boolean {
  const roles = Array.isArray(required) ? required : [required];
  return roles.some((r) => userRoles.includes(r));
}
