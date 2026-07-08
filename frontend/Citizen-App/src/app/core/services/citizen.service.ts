import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { map, tap, catchError, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// ---- Domain types -------------------------------------------------------

export interface CitizenProfile {
  id?: string;
  username: string;
  name: string;
  email: string;
  phone: string;
  points: number;
  tier: string;
  language: string;
}

export interface ChatComment {
  userName: string;
  initials: string;
  isOfficer: boolean;
  text: string;
  timestamp: string;
  imageUrl?: string;
  department?: string;
}

export interface Ticket {
  id: string;
  category: string;
  title: string;
  description: string;
  date: string;
  status: 'Submitted' | 'AI-Assigned' | 'In Progress' | 'Resolved';
  lastUpdate: string;
  location: string;
  imageUrl?: string;
  comments: ChatComment[];
  rating?: number;
  feedbackComment?: string;
}

export interface TransitPass {
  id: string;
  title: string;
  pointsCost: number;
  expiresAt: string;
  fareType: string;
  qrCodeData: string;
  isActive: boolean;
}

export interface PointActivity {
  id: string;
  title: string;
  source: string;
  date: string;
  points: number;
}

// ---- Backend response shapes --------------------------------------------

interface LoginResponse {
  exists: boolean;
  profile: CitizenProfile | null;
  token: string | null;
}

interface TicketRow {
  id: string;
  category: string;
  title: string;
  description: string;
  date: string;
  status: string;
  lastUpdate: string;
  location: string;
  imageUrl?: string;
}

interface CreateTicketResponse {
  id: string;
}

interface FeedbackResponse {
  message: string;
  awardedPoints: number;
  updatedPoints: number;
  tier: string;
}

interface RedeemResponse {
  success: boolean;
  updatedPoints: number;
  pass: TransitPass;
}

@Injectable({
  providedIn: 'root'
})
export class CitizenService {

  private readonly base = environment.apiBaseUrl + '/api/citizen';

  /** Currently authenticated citizen profile (null when logged out). */
  private readonly currentUserSubject = new BehaviorSubject<CitizenProfile | null>(null);
  readonly currentUser$ = this.currentUserSubject.asObservable();

  /** Cached tickets from the backend. */
  private readonly ticketsSubject = new BehaviorSubject<Ticket[]>([]);
  readonly tickets$ = this.ticketsSubject.asObservable();

  /** Cached transit passes from the backend. */
  private readonly passesSubject = new BehaviorSubject<TransitPass[]>([]);
  readonly passes$ = this.passesSubject.asObservable();

  /** Cached point activities from the backend. */
  private readonly pointActivitiesSubject = new BehaviorSubject<PointActivity[]>([]);
  readonly pointActivities$ = this.pointActivitiesSubject.asObservable();

  constructor(private readonly http: HttpClient) {
    // Restore session from localStorage on app start
    this.restoreSession();
  }

  // ------------------------------------------------------------------ session

  /**
   * Restores the logged-in citizen profile from localStorage (JWT is sent
   * automatically by the JwtInterceptor, so no re-login needed on page refresh).
   */
  private restoreSession(): void {
    const saved = localStorage.getItem('citizen_profile');
    if (saved) {
      try {
        this.currentUserSubject.next(JSON.parse(saved));
      } catch {
        // Corrupted data — clear it
        localStorage.removeItem('citizen_profile');
      }
    }
  }

  // ------------------------------------------------------------------ auth

  /**
   * Attempts login with phone/email/username.
   * Returns true if the citizen exists, false if they need to register.
   */
  login(identifier: string): Observable<boolean> {
    return this.http.post<LoginResponse>(`${this.base}/auth/login`, { identifier }).pipe(
      tap(res => {
        if (res.exists && res.profile && res.token) {
          this.saveSession(res.profile, res.token);
        }
      }),
      map(res => res.exists),
      catchError(() => of(false))
    );
  }

  /**
   * Registers a new citizen and returns the created profile.
   */
  register(name: string, phone?: string, email?: string, language = 'en'): Observable<CitizenProfile> {
    return this.http.post<LoginResponse>(`${this.base}/auth/register`, { name, phone, email, language }).pipe(
      tap(res => {
        if (res.profile && res.token) {
          this.saveSession(res.profile, res.token);
        }
      }),
      map(res => res.profile as CitizenProfile)
    );
  }

  private saveSession(profile: CitizenProfile, token: string): void {
    localStorage.setItem('citizen_jwt', token);
    localStorage.setItem('citizen_profile', JSON.stringify(profile));
    this.currentUserSubject.next(profile);
  }

  // ------------------------------------------------------------------ tickets

  /** Fetches all tickets for the current citizen from the backend. */
  loadTickets(): Observable<Ticket[]> {
    return this.http.get<TicketRow[]>(`${this.base}/tickets`).pipe(
      map(rows => rows.map(r => this.rowToTicket(r))),
      tap(tickets => this.ticketsSubject.next(tickets)),
      catchError(() => {
        // Return cached tickets on error
        return of(this.ticketsSubject.value);
      })
    );
  }

  /** Submits a new ticket and refreshes the tickets list. */
  submitTicket(
    title: string,
    description: string,
    location: string,
    latitude?: number | null,
    longitude?: number | null,
    imageUrl?: string,
    voiceUrl?: string,
    voiceDuration?: string,
    mediaUrls?: string[],
    language?: string
  ): Observable<string> {
    return this.http.post<CreateTicketResponse>(`${this.base}/tickets`, {
      title,
      description,
      location,
      latitude,
      longitude,
      imageUrl,
      voiceUrl,
      voiceDuration,
      mediaUrls,
      language
    }).pipe(
      tap(() => this.loadTickets().subscribe()),
      map(res => res.id)
    );
  }

  /** Uploads a file (photo, video, or voice recording) to the server. */
  uploadFile(file: File | Blob, filename: string): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file, filename);
    return this.http.post<{ url: string }>(`${environment.apiBaseUrl}/api/files/upload`, formData);
  }

  /** Submits feedback/rating for a resolved ticket and refreshes data. */
  submitFeedback(id: string, rating: number, comment: string): Observable<FeedbackResponse> {
    return this.http.post<FeedbackResponse>(`${this.base}/tickets/${id}/feedback`, {
      rating, comment
    }).pipe(
      tap(res => {
        // Update cached profile points and tier
        const current = this.currentUserSubject.value;
        if (current) {
          const updated = { ...current, points: res.updatedPoints, tier: res.tier };
          this.currentUserSubject.next(updated);
          localStorage.setItem('citizen_profile', JSON.stringify(updated));
        }
        this.loadTickets().subscribe();
      })
    );
  }

  /** Reopens a resolved/closed ticket. */
  reopenTicket(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/tickets/${id}/reopen`, {}).pipe(
      tap(() => this.loadTickets().subscribe())
    );
  }

  /** Fetches full ticket details including comments/chat log. */
  loadTicketComments(id: string): Observable<ChatComment[]> {
    return this.http.get<any>(`${environment.apiBaseUrl}/api/tasks/${id}/details`).pipe(
      map(res => {
        const comments: ChatComment[] = (res.comments || []).map((c: any) => ({
          userName: c.userName,
          initials: c.initials || '',
          isOfficer: c.isSelf,
          text: c.text,
          timestamp: c.timestamp,
          imageUrl: c.imageUrl,
          department: c.department
        }));
        
        // Update comments in the cached ticket
        const currentTickets = this.ticketsSubject.value;
        const ticket = currentTickets.find(t => t.id === id);
        if (ticket) {
          ticket.comments = comments;
          this.ticketsSubject.next([...currentTickets]);
        }
        return comments;
      }),
      catchError(() => of([]))
    );
  }

  // ----------------------------------------------------------------- profile

  /** Updates the citizen's preferred language. */
  updateLanguage(lang: string): Observable<void> {
    return this.http.put<{ success: boolean; language: string }>(`${this.base}/profile/language`, {
      language: lang
    }).pipe(
      tap(() => {
        const current = this.currentUserSubject.value;
        if (current) {
          const updated = { ...current, language: lang };
          this.currentUserSubject.next(updated);
          localStorage.setItem('citizen_profile', JSON.stringify(updated));
        }
      }),
      map(() => undefined)
    );
  }

  // ------------------------------------------------------------------ wallet

  /** Fetches transit passes and point activities from the backend. */
  loadWalletData(): Observable<{ passes: TransitPass[]; activities: PointActivity[] }> {
    const passes$ = this.http.get<TransitPass[]>(`${this.base}/wallet/passes`).pipe(
      tap(p => this.passesSubject.next(p))
    );
    const activities$ = this.http.get<PointActivity[]>(`${this.base}/wallet/activities`).pipe(
      tap(a => this.pointActivitiesSubject.next(a))
    );
    return passes$.pipe(
      switchMap(passes => activities$.pipe(
        map(activities => ({ passes, activities }))
      )),
      catchError(() => of({ passes: this.passesSubject.value, activities: this.pointActivitiesSubject.value }))
    );
  }

  /** Redeems a transit pass using points and refreshes wallet data. */
  redeemPass(pointsCost: number, title: string): Observable<RedeemResponse> {
    return this.http.post<RedeemResponse>(`${this.base}/wallet/redeem`, { pointsCost, title }).pipe(
      tap(res => {
        // Update cached profile points
        const current = this.currentUserSubject.value;
        if (current) {
          const updated = { ...current, points: res.updatedPoints };
          this.currentUserSubject.next(updated);
          localStorage.setItem('citizen_profile', JSON.stringify(updated));
        }
        this.loadWalletData().subscribe();
      })
    );
  }

  // ------------------------------------------------------------------- auth

  logout(): void {
    localStorage.removeItem('citizen_jwt');
    localStorage.removeItem('citizen_profile');
    this.currentUserSubject.next(null);
    this.ticketsSubject.next([]);
    this.passesSubject.next([]);
    this.pointActivitiesSubject.next([]);
  }

  // ----------------------------------------------------------------- helpers

  /**
   * Maps a backend TicketRow to the richer frontend Ticket model.
   * Comments are not included in the list view — they are loaded on the detail page.
   */
  private rowToTicket(row: TicketRow): Ticket {
    return {
      id: row.id,
      category: row.category || 'Grievance',
      title: row.title,
      description: row.description || '',
      date: row.date,
      status: this.mapStatus(row.status),
      lastUpdate: row.lastUpdate,
      location: row.location || '',
      imageUrl: row.imageUrl,
      comments: []
    };
  }

  /** Normalizes backend status strings to the frontend Ticket status union type. */
  private mapStatus(backendStatus: string): Ticket['status'] {
    const s = (backendStatus || '').toLowerCase();
    if (s.includes('resolved')) return 'Resolved';
    if (s.includes('progress')) return 'In Progress';
    if (s.includes('ai') || s.includes('assigned')) return 'AI-Assigned';
    return 'Submitted';
  }
}
