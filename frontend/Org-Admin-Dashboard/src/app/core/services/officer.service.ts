import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Officer } from '../models/officer.model';

/**
 * OfficerService — handles API calls related to officers.
 * Mimics backend endpoints but currently returns mock data from JSON asset with session caching.
 */
@Injectable({
  providedIn: 'root',
})
export class OfficerService {
  private readonly useRealApi = false;

  private readonly mockUrl = 'assets/mock-data/officers.json';
  private readonly realUrl = '/api/officers';

  // Session-level cache to hold changes in-memory during testing
  private officersCache: Officer[] | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Fetches the list of officers.
   * Returns an Observable of Officer array.
   */
  getOfficers(): Observable<Officer[]> {
    if (this.useRealApi) {
      return this.http.get<Officer[]>(this.realUrl);
    }
    if (this.officersCache) {
      return of(this.officersCache);
    }
    return this.http.get<Officer[]>(this.mockUrl).pipe(
      tap((data) => {
        this.officersCache = data;
      })
    );
  }

  /**
   * Retrieves a single officer by ID.
   */
  getOfficerById(id: string): Observable<Officer | undefined> {
    return this.getOfficers().pipe(
      map((officers) => officers.find((o) => o.id === id))
    );
  }

  /**
   * Creates a new officer.
   */
  createOfficer(officer: Officer): Observable<Officer> {
    if (this.useRealApi) {
      return this.http.post<Officer>(this.realUrl, officer);
    }
    if (!this.officersCache) {
      return this.getOfficers().pipe(
        map(() => {
          this.addCachedOfficer(officer);
          return officer;
        })
      );
    }
    this.addCachedOfficer(officer);
    return of(officer);
  }

  /**
   * Updates an existing officer by ID.
   */
  updateOfficer(id: string, updatedOfficer: Officer): Observable<Officer> {
    if (this.useRealApi) {
      return this.http.put<Officer>(`${this.realUrl}/${id}`, updatedOfficer);
    }
    if (!this.officersCache) {
      return this.getOfficers().pipe(
        map(() => {
          this.updateCachedOfficer(id, updatedOfficer);
          return updatedOfficer;
        })
      );
    }
    this.updateCachedOfficer(id, updatedOfficer);
    return of(updatedOfficer);
  }

  private addCachedOfficer(officer: Officer): void {
    if (this.officersCache) {
      this.officersCache = [...this.officersCache, officer];
    }
  }

  private updateCachedOfficer(id: string, updatedOfficer: Officer): void {
    if (this.officersCache) {
      this.officersCache = this.officersCache.map((o) =>
        o.id === id ? updatedOfficer : o
      );
    }
  }
}
