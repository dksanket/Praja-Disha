import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { map, tap, switchMap } from 'rxjs/operators';

export interface SubTask {
  id: string;
  parentId?: string;
  title: string;
  role: string;
  department?: string;
  icon: string;
  status: string;
  statusClass: string;
  assignee: string;
  timestamp: string;
}

export interface DetailedComment {
  userName: string;
  initials: string;
  timestamp: string;
  text: string;
  isSelf: boolean;
}

export interface DetailedNote {
  userName: string;
  timestamp: string;
  text: string;
}

export interface DetailedActivity {
  timestamp: string;
  action: string;
  performedBy: string;
  remarks: string;
}

export interface TaskDetailPayload {
  id: string;
  title: string;
  priority: string;
  groupId: string;
  parentTaskId: string | null;
  orgId: string;
  createdAt: string;
  reportedBy: string;
  reporterType: string;
  description: string;
  voiceUrl: string;
  voiceDuration: string;
  category: string;
  language: string;
  location: {
    address: string;
    lat: string;
    lng: string;
  };
  imageUrl: string;
  mapUrl: string;
  subTasks: SubTask[];
  comments: DetailedComment[];
  notes: DetailedNote[];
  activities: DetailedActivity[];
}

/**
 * TaskDetailsService — loads and manages deep dive task details.
 * Supports local modifications (session level persistence) for comments, notes, and activity logs.
 */
@Injectable({
  providedIn: 'root'
})
export class TaskDetailsService {
  private readonly jsonUrl = 'assets/mock-data/task-details.json';
  
  // In-memory cache for session lifetime modifications
  private readonly detailsCache$ = new BehaviorSubject<Record<string, TaskDetailPayload> | null>(null);

  constructor(private readonly http: HttpClient) {}

  /**
   * Loads task details dictionary. Utilizes caching for session level updates.
   */
  private loadDetails(): Observable<Record<string, TaskDetailPayload>> {
    if (this.detailsCache$.value) {
      return of(this.detailsCache$.value);
    }
    return this.http.get<Record<string, TaskDetailPayload>>(this.jsonUrl).pipe(
      tap(data => this.detailsCache$.next(data))
    );
  }

  /**
   * Retrieves full task details for a given task ID.
   * If the ID is not found in mock data, generates a clean default fallback.
   */
  getTaskDetails(id: string): Observable<TaskDetailPayload> {
    return this.loadDetails().pipe(
      map(cache => {
        if (cache[id]) {
          return cache[id];
        }
        // Generate realistic fallback data for other tasks
        const fallback: TaskDetailPayload = {
          id: id,
          title: 'Auto-Triage Assignment context',
          priority: 'P2',
          groupId: 'G-' + id.replace(/\D/g, ''),
          parentTaskId: null,
          orgId: 'ORG-GENERAL',
          createdAt: 'Oct 24, 2026 10:00 IST',
          reportedBy: 'Citizen Reporter',
          reporterType: 'Citizen',
          description: 'No detailed voice transcription available. The standard report has been registered and is awaiting officer triage.',
          voiceUrl: '',
          voiceDuration: '0:00',
          category: 'General Administration',
          language: 'English',
          location: {
            address: 'Main Secretariat Road, Bengaluru Central',
            lat: '12.9716° N',
            lng: '77.5946° E'
          },
          imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBg0ILinaoHUST1gzmjbg3DicaBTDW0RxRsTva5aeFO-aVGAfl-yQHTlxnKrCHUwcrmypjdhvA0XFDvRXlZVZ8KE1gg4zUtRvznIOqJ7mfVl74gfVcWp6CaY-eqy4fLjqcDx1WfAuql1bF08DYQD3MefXxrh8H6scylxhfDoFYvzshDFk7PepJ4Zz9lxvUQvDgoXvR8XAWstnsmwY-92yY47XAYf7sUtJu3Mu88fax8zECN8PJLTXWsClM-hdgZwNfV3UHC-_TzkYU',
          mapUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBftfd0fD5bkIjlgGJPd8xvMqIc2so9g_CG7hXtEty5seUntRKkyCs0ksESaL_iyc6ZHJ6A-s0Th1V0cxiMBMPDneeFh5MLgU_TLC2zj3ZBnfpLrbMzKlPwgq-uQwY-C5zKd5SBnKiHn21JWUkHQLr7yx5yfBZsOHK4qBYnv8jKxJf9IQyjoGNDZem1KbTk4ssnJoAhSOcRvm3L9SJ9JqcEu6Cv0ozrQtU52R2xQi01T8IsmgpsXhoNwwRKkkczdquAwX1pjPozNME',
          subTasks: [
            {
              id: 'SUB-GEN-001',
              title: 'Default Task Context',
              role: 'Root Task',
              department: 'General Admin Dept',
              icon: 'account_tree',
              status: 'ACTIVE',
              statusClass: 'bg-primary-container text-on-primary-container',
              assignee: '',
              timestamp: ''
            }
          ],
          comments: [
            {
              userName: 'System AI',
              initials: 'AI',
              timestamp: '10:00',
              text: 'Auto-ingested ticket and initialized primary category analysis.',
              isSelf: false
            }
          ],
          notes: [],
          activities: [
            {
              timestamp: 'Oct 24, 10:00',
              action: 'AI_ASSIGNED',
              performedBy: 'system_ai',
              remarks: 'Initial ingestion completed.'
            }
          ]
        };

        // Cache the generated fallback so subsequent edits persist for it too
        cache[id] = fallback;
        this.detailsCache$.next(cache);
        return fallback;
      })
    );
  }

  /**
   * Appends a comment to a task's details.
   */
  addComment(taskId: string, commentText: string, isOfficer: boolean, userName: string): Observable<TaskDetailPayload> {
    return this.loadDetails().pipe(
      map(cache => {
        const detail = cache[taskId];
        if (detail) {
          const now = new Date();
          const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
          const initials = userName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();

          detail.comments.unshift({
            userName,
            initials: initials || 'ME',
            timestamp: timeStr,
            text: commentText,
            isSelf: isOfficer
          });

          // Log to activities
          detail.activities.unshift({
            timestamp: now.toLocaleDateString([], { month: 'short', day: '2-digit' }) + ', ' + timeStr,
            action: 'COMMENT_ADDED',
            performedBy: userName,
            remarks: 'Added a public comment.'
          });

          cache[taskId] = { ...detail };
          this.detailsCache$.next(cache);
          return cache[taskId];
        }
        throw new Error('Task detail payload not found for comment.');
      })
    );
  }

  /**
   * Appends an internal note to a task's details.
   */
  addNote(taskId: string, noteText: string, userName: string): Observable<TaskDetailPayload> {
    return this.loadDetails().pipe(
      map(cache => {
        const detail = cache[taskId];
        if (detail) {
          const now = new Date();
          const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });

          detail.notes.unshift({
            userName,
            timestamp: timeStr,
            text: noteText
          });

          // Log to activities
          detail.activities.unshift({
            timestamp: now.toLocaleDateString([], { month: 'short', day: '2-digit' }) + ', ' + timeStr,
            action: 'INTERNAL_NOTE_ADDED',
            performedBy: userName,
            remarks: 'Recorded an internal officer note.'
          });

          cache[taskId] = { ...detail };
          this.detailsCache$.next(cache);
          return cache[taskId];
        }
        throw new Error('Task detail payload not found for note.');
      })
    );
  }
}
