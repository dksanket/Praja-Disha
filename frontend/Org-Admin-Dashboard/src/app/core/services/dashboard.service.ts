import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DashboardTask, DashboardStats } from '../models/dashboard.model';

/**
 * DashboardService — loads and structures metric counts and task lists
 * for the Executive Command Center dashboard view.
 */
@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly tasksUrl = 'assets/mock-data/tasks.json';

  constructor(private readonly http: HttpClient) {}

  /**
   * Loads the list of triage tasks from the mock database.
   */
  getTasks(): Observable<DashboardTask[]> {
    return this.http.get<DashboardTask[]>(this.tasksUrl);
  }

  /**
   * Compiles live stats derived from the current task checklist.
   */
  getStats(): Observable<DashboardStats> {
    return this.getTasks().pipe(
      map((tasks) => {
        // Derives stats directly from the task attributes
        const awaitingAiReviewCount = tasks.filter(
          (t) => t.statusType === 'ai-pending'
        ).length;
        const dueTodayCount = tasks.filter(
          (t) => t.dueDateCritical
        ).length;
        const myDeptCount = tasks.length; // Active workload

        return {
          awaitingAiReviewCount,
          dueTodayCount,
          myDeptCount,
        };
      })
    );
  }
}
