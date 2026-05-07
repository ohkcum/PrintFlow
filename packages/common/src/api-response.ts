import { z } from "zod";

export type ApiResponse<T = unknown> = {
  success: true;
  data: T;
  timestamp: string;
} | {
  success: false;
  error: {
    code: string;
    message: string;
    details?: Record<string, unknown>;
  };
  timestamp: string;
};

export function successResponse<T>(data: T): ApiResponse<T> {
  return {
    success: true,
    data,
    timestamp: new Date().toISOString(),
  };
}

export function errorResponse(
  code: string,
  message: string,
  details?: Record<string, unknown>
): ApiResponse<never> {
  return {
    success: false,
    error: { code, message, details },
    timestamp: new Date().toISOString(),
  };
}

export function paginatedResponse<T>(
  data: T[],
  total: number,
  page: number,
  pageSize: number
): ApiResponse<{ items: T[]; total: number; page: number; pageSize: number; totalPages: number }> {
  return {
    success: true,
    data: {
      items: data,
      total,
      page,
      pageSize,
      totalPages: Math.ceil(total / pageSize),
    },
    timestamp: new Date().toISOString(),
  };
}
