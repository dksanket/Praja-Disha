import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MANAGE_TEAM_STRINGS } from './manage-team.strings';
import { ManageDepartmentsComponent } from './components/manage-departments/manage-departments.component';
import { ManageOfficersComponent } from './components/manage-officers/manage-officers.component';

/**
 * ManageTeamComponent — top-level shell for the Team Management page.
 * Orchestrates subcomponents ManageDepartmentsComponent and ManageOfficersComponent.
 */
@Component({
  selector: 'app-manage-team',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ManageDepartmentsComponent,
    ManageOfficersComponent,
  ],
  templateUrl: './manage-team.component.html',
  styleUrls: ['./manage-team.component.scss'],
})
export class ManageTeamComponent {
  readonly strings = MANAGE_TEAM_STRINGS;

  /** Currently active tab */
  activeTab: 'officers' | 'departments' = 'departments';

  setTab(tab: 'officers' | 'departments'): void {
    this.activeTab = tab;
  }
}
