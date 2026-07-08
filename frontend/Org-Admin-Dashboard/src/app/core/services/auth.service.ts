import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';
import { Officer } from '../models/officer.model';
import { environment } from '../../../environments/environment';

interface LoginResponse {
  exists: boolean;
  profile: Officer | null;
  token: string | null;
}

/**
 * AuthService — Handles user authentication, token storage, and session state.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly base = environment.apiBaseUrl;
  private readonly currentUserSubject = new BehaviorSubject<Officer | null>(null);
  readonly currentUser$ = this.currentUserSubject.asObservable();

  constructor(private readonly http: HttpClient) {
    this.restoreSession();
  }

  /**
   * Restores session details from local storage if they exist.
   */
  private restoreSession(): void {
    const saved = localStorage.getItem('officer_profile');
    if (saved) {
      try {
        this.currentUserSubject.next(JSON.parse(saved));
      } catch {
        localStorage.removeItem('officer_profile');
        localStorage.removeItem('officer_jwt');
      }
    }
  }

  /**
   * Attempts login with the provided identifier.
   */
  login(identifier: string): Observable<boolean> {
    return this.http.post<LoginResponse>(`${this.base}/api/officers/auth/login`, { identifier }).pipe(
      tap((res) => {
        if (res.exists && res.profile && res.token) {
          localStorage.setItem('officer_jwt', res.token);
          localStorage.setItem('officer_profile', JSON.stringify(res.profile));
          this.currentUserSubject.next(res.profile);
        }
      }),
      map((res) => res.exists),
      catchError(() => of(false))
    );
  }

  /**
   * Logs out the current officer, clearing local storage.
   */
  logout(): void {
    localStorage.removeItem('officer_jwt');
    localStorage.removeItem('officer_profile');
    this.currentUserSubject.next(null);
  }

  /**
   * Returns true if the user is authenticated.
   */
  isLoggedIn(): boolean {
    return localStorage.getItem('officer_jwt') !== null;
  }

  /**
   * Returns the current logged-in officer profile.
   */
  getCurrentUser(): Officer | null {
    return this.currentUserSubject.value;
  }
}
