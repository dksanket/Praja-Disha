import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Table, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MANAGE_TEAM_STRINGS } from '../../manage-team.strings';
import { Officer } from '../../../../core/models/officer.model';
import { OfficerService } from '../../../../core/services/officer.service';

/** Interface extending the officer model to add computed UI fields */
interface OfficerUi extends Officer {
  initials: string;
  departmentNames: string[];
}

/**
 * ManageOfficersComponent — Sub-component for managing officers and their department roles.
 * Displays officer name, email, phone, list of departments, and active status.
 */
@Component({
  selector: 'app-manage-officers',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    TableModule,
    InputTextModule,
    ButtonModule,
  ],
  templateUrl: './manage-officers.component.html',
  styleUrls: ['./manage-officers.component.scss'],
})
export class ManageOfficersComponent implements OnInit {
  readonly strings = MANAGE_TEAM_STRINGS;

  /** Reference to the p-table instance — used to drive the global filter input */
  @ViewChild('dt') table!: Table;

  /** Fields searched by the global filter input */
  readonly globalFilterFields: (keyof OfficerUi)[] = [
    'id',
    'name',
    'officerUserName',
    'email',
    'phone',
    'departmentNames',
  ];

  /** Rows per page for the PrimeNG paginator */
  readonly rows = 4;

  officers: OfficerUi[] = [];

  constructor(private officerService: OfficerService) {}

  ngOnInit(): void {
    this.officerService.getOfficers().subscribe({
      next: (data) => {
        // Pre-compute initials and department lists in the TS layer to optimize performance
        // and respect the rule against method invocations inside HTML templates.
        this.officers = data.map((off) => ({
          ...off,
          initials: this.computeInitials(off.name),
          departmentNames: (off as any).departmentNames || [],
        }));
      },
      error: (err) => {
        console.error('Error fetching officers:', err);
      },
    });
  }

  /**
   * Drives the PrimeNG global filter from our custom search input.
   * Called on every keystroke via (input) binding in the template.
   */
  applyGlobalFilter(value: string): void {
    this.table.filterGlobal(value, 'contains');
  }

  /** Helper to pre-compute name initials - e.g. "Kiran Kumar" -> "KK" */
  private computeInitials(name: string): string {
    return name
      .split(' ')
      .map((w) => w[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }
}
