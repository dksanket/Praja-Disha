import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Department } from '../models/department.model';
import { environment } from '../../../environments/environment';

/**
 * DepartmentService — handles API calls related to departments.
 * All calls go to the live backend.
 */
@Injectable({
  providedIn: 'root',
})
export class DepartmentService {
  private readonly realUrl = `${environment.apiBaseUrl}/api/departments`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Fetches the list of all departments.
   */
  getDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>(this.realUrl);
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
    return this.http.post<Department>(this.realUrl, dept);
  }

  /**
   * Updates an existing department by its ID.
   */
  updateDepartment(id: string, updatedDept: Department): Observable<Department> {
    return this.http.put<Department>(`${this.realUrl}/${id}`, updatedDept);
  }
}
