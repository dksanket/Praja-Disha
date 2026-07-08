/**
 * Domain models for the Team Management feature.
 * These mirror the backend MongoDB schema for departments and officers.
 */

export { Department } from '../department.model';

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
