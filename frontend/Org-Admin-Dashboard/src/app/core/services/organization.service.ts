import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Organization } from '../models/organization.model';

/**
 * OrganizationService — handles fetching organization details.
 * Currently reads mock organization data with session caching.
 */
@Injectable({
  providedIn: 'root',
})
export class OrganizationService {
  private readonly mockUrl = 'assets/mock-data/organization.json';
  private activeOrgCache: Organization | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Fetches active organization details.
   */
  getActiveOrganization(): Observable<Organization> {
    if (this.activeOrgCache) {
      return of(this.activeOrgCache);
    }
    return this.http.get<Organization>(this.mockUrl).pipe(
      tap((org) => {
        this.activeOrgCache = org;
      })
    );
  }
}
