import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TaskDetailPayload } from '../models/task-detail.model';

/**
 * TaskDetailsService — loads task details and posts comments/notes to the backend.
 * The server is the single source of truth; no local in-memory cache is used.
 */
@Injectable({
  providedIn: 'root'
})
export class TaskDetailsService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/tasks`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Retrieves the full task detail payload for the given task ID.
   */
  getTaskDetails(id: string): Observable<TaskDetailPayload> {
    return this.http.get<TaskDetailPayload>(`${this.baseUrl}/${id}/details`);
  }

  /**
   * Appends a comment to a task and returns the updated task payload.
   */
  addComment(taskId: string, commentText: string, isOfficer: boolean, userName: string): Observable<TaskDetailPayload> {
    return this.http.post<TaskDetailPayload>(`${this.baseUrl}/${taskId}/comments`, {
      text: commentText,
      isOfficer,
      userName
    });
  }

  /**
   * Appends an internal officer note to a task and returns the updated task payload.
   */
  addNote(taskId: string, noteText: string, userName: string): Observable<TaskDetailPayload> {
    return this.http.post<TaskDetailPayload>(`${this.baseUrl}/${taskId}/notes`, {
      text: noteText,
      userName
    });
  }
}
