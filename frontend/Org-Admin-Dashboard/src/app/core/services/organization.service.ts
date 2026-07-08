import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Organization } from '../models/organization.model';
import { environment } from '../../../environments/environment';

/**
 * OrganizationService — fetches active organization details from the backend.
 */
@Injectable({
  providedIn: 'root',
})
export class OrganizationService {
  private readonly activeUrl = `${environment.apiBaseUrl}/api/organizations/active`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Fetches the active organization's details.
   */
  getActiveOrganization(): Observable<Organization> {
    return this.http.get<Organization>(this.activeUrl);
  }
}
