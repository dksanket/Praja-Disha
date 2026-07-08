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

  /**
   * Updates task assignee (department and officer) and returns the updated task payload.
   */
  updateAssignee(taskId: string, departmentId: string | null, officerId: string | null): Observable<TaskDetailPayload> {
    return this.http.put<TaskDetailPayload>(`${this.baseUrl}/${taskId}/assignee`, {
      departmentId,
      officerId
    });
  }

  /**
   * Updates task status and returns the updated task payload.
   */
  updateStatus(taskId: string, status: string, remarks: string | null): Observable<TaskDetailPayload> {
    return this.http.put<TaskDetailPayload>(`${this.baseUrl}/${taskId}/status`, {
      status,
      remarks
    });
  }

  /**
   * Creates a subtask under a parent task and returns the created subtask.
   */
  createSubTask(
    parentId: string,
    title: string,
    description: string,
    priority: string,
    category: string,
    departmentId: string | null,
    officerId: string | null
  ): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${parentId}/subtasks`, {
      title,
      description,
      priority,
      category,
      departmentId,
      officerId
    });
  }
}
