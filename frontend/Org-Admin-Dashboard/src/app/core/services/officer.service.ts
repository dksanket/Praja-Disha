import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Officer } from '../models/officer.model';
import { environment } from '../../../environments/environment';

/**
 * OfficerService — handles API calls related to officers.
 * All calls go to the live backend.
 */
@Injectable({
  providedIn: 'root',
})
export class OfficerService {
  private readonly realUrl = `${environment.apiBaseUrl}/api/officers`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Fetches the list of all officers.
   */
  getOfficers(): Observable<Officer[]> {
    return this.http.get<Officer[]>(this.realUrl);
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
    return this.http.post<Officer>(this.realUrl, officer);
  }

  /**
   * Updates an existing officer by ID.
   */
  updateOfficer(id: string, updatedOfficer: Officer): Observable<Officer> {
    return this.http.put<Officer>(`${this.realUrl}/${id}`, updatedOfficer);
  }
}
