import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink, RouterLinkActive } from '@angular/router';
import { LAYOUT_STRINGS } from './dashboard-layout.strings';

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
}
