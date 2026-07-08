import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardTask, DashboardStats } from '../models/dashboard.model';
import { environment } from '../../../environments/environment';

/**
 * DashboardService — fetches real task rows and stat counters from the backend.
 */
@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/dashboard`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Loads the live triage task list from the backend.
   * Optional query params for filtering by statusType or priority.
   */
  getTasks(statusType?: string, priority?: string): Observable<DashboardTask[]> {
    const params: Record<string, string> = {};
    if (statusType) params['statusType'] = statusType;
    if (priority) params['priority'] = priority;
    return this.http.get<DashboardTask[]>(`${this.baseUrl}/tasks`, { params });
  }

  /**
   * Fetches the live stats counters for the bento grid.
   */
  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.baseUrl}/stats`);
  }
}
