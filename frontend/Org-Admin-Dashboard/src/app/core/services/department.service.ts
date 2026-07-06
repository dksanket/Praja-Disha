import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Department } from '../models/manage-team/manage-team.domain-models';

/**
 * DepartmentService — handles API calls related to departments.
 * Mimics backend endpoints but currently returns mock data from JSON asset with session caching.
 */
@Injectable({
  providedIn: 'root',
})
export class DepartmentService {
  // Toggle to true when backend endpoint is ready
  private readonly useRealApi = false;

  private readonly mockUrl = 'assets/mock-data/departments.json';
  private readonly realUrl = '/api/departments';

  // Session-level cache to hold changes in-memory during testing
  private departmentsCache: Department[] | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Fetches the list of departments.
   * Returns an Observable of Department array.
   */
  getDepartments(): Observable<Department[]> {
    if (this.useRealApi) {
      return this.http.get<Department[]>(this.realUrl);
    }
    if (this.departmentsCache) {
      return of(this.departmentsCache);
    }
    return this.http.get<Department[]>(this.mockUrl).pipe(
      tap((data) => {
        this.departmentsCache = data;
      })
    );
  }

  /**
   * Retrieves a single department by its ID.
   */
  getDepartmentById(id: string): Observable<Department | undefined> {
    return this.getDepartments().pipe(
      map((depts) => depts.find((d) => d.id === id))
    );
  }

  /**
   * Creates a new department.
   */
  createDepartment(dept: Department): Observable<Department> {
    if (this.useRealApi) {
      return this.http.post<Department>(this.realUrl, dept);
    }
    if (!this.departmentsCache) {
      return this.getDepartments().pipe(
        map(() => {
          this.addCachedDept(dept);
          return dept;
        })
      );
    }
    this.addCachedDept(dept);
    return of(dept);
  }

  /**
   * Updates an existing department by its ID.
   */
  updateDepartment(id: string, updatedDept: Department): Observable<Department> {
    if (this.useRealApi) {
      return this.http.put<Department>(`${this.realUrl}/${id}`, updatedDept);
    }
    if (!this.departmentsCache) {
      return this.getDepartments().pipe(
        map(() => {
          this.updateCachedDept(id, updatedDept);
          return updatedDept;
        })
      );
    }
    this.updateCachedDept(id, updatedDept);
    return of(updatedDept);
  }

  private addCachedDept(dept: Department): void {
    if (this.departmentsCache) {
      this.departmentsCache = [...this.departmentsCache, dept];
    }
  }

  private updateCachedDept(id: string, updatedDept: Department): void {
    if (this.departmentsCache) {
      this.departmentsCache = this.departmentsCache.map((d) =>
        d.id === id ? updatedDept : d
      );
    }
  }
}
