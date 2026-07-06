/**
 * Domain models for the Team Management feature.
 * These mirror the backend MongoDB schema for departments and officers.
 */

/** Represents a single department in the organisational hierarchy */
export interface Department {
  id: string;                        // e.g. "DPT-001"
  name: string;
  parentDepartmentId: string | null; // null for root departments
  parentDepartmentName: string | null;
  headOfficerId: string | null;      // null when position is vacant
  headOfficerName: string | null;
  headOfficerAvatarUrl: string | null;
  officerCount: number;
  /** Depth level in the hierarchy tree (0 = root) */
  depth: number;
}

/** Abbreviated officer reference used in table cells */
export interface OfficerRef {
  id: string;
  name: string;
  avatarUrl: string | null;
  initials: string;                  // Fallback when no avatar available
}

/** Pagination metadata returned by the API */
export interface PaginatedResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}
