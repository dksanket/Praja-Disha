import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { LAYOUT_STRINGS } from './dashboard-layout.strings';
import { AuthService } from '../../core/services/auth.service';

/**
 * DashboardLayoutComponent — persistent shell rendered for all authenticated routes.
 * Provides the fixed sidebar (with org info + nav items) and the top navigation bar.
 * The main content area is projected via <router-outlet>.
 */
@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink, RouterLinkActive],
  templateUrl: './dashboard-layout.component.html',
  styleUrls: ['./dashboard-layout.component.scss'],
})
export class DashboardLayoutComponent {
  readonly strings = LAYOUT_STRINGS;

  /** Name of the logged-in officer */
  officerName = '';

  /** Precomputed initials of the officer name */
  officerInitials = '';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    const profile = this.authService.getCurrentUser();
    if (profile) {
      this.officerName = profile.name;
      this.officerInitials = this.computeInitials(profile.name);
    }
  }

  /** Logs out the user and navigates to the login screen */
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  /** Computes the initials from the user's name */
  private computeInitials(name: string): string {
    if (!name) {
      return '';
    }
    const parts = name.split(/\s+/);
    if (parts.length > 1) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name[0].toUpperCase();
  }
}
