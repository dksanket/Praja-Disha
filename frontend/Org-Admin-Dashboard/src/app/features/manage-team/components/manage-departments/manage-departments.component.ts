import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Table, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { MANAGE_TEAM_STRINGS } from '../../manage-team.strings';
import { Department } from '../../../../core/models/manage-team/manage-team.domain-models';
import { DepartmentService } from '../../../../core/services/department.service';

/** Interface extending the department model to add computed UI fields */
interface DepartmentUi extends Department {
  headOfficerInitials: string;
}

/**
 * ManageDepartmentsComponent — Sub-component for managing organizational departments.
 * Uses PrimeNG p-table for sorting, built-in column filtering, and pagination.
 */
@Component({
  selector: 'app-manage-departments',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    TableModule,
    InputTextModule,
    ButtonModule,
    DropdownModule,
  ],
  templateUrl: './manage-departments.component.html',
  styleUrls: ['./manage-departments.component.scss'],
})
export class ManageDepartmentsComponent implements OnInit {
  readonly strings = MANAGE_TEAM_STRINGS;

  /** Reference to the p-table instance — used to drive the global filter input */
  @ViewChild('dt') table!: Table;

  /** Fields searched by the global filter input */
  readonly globalFilterFields: (keyof DepartmentUi)[] = [
    'id',
    'name',
    'parentDepartmentName',
    'headOfficerName',
  ];

  /** Rows per page for the PrimeNG paginator */
  readonly rows = 4;

  /** Options shown in the rows-per-page dropdown */
  readonly rowsPerPageOptions = [4, 8, 12, 24];

  departments: DepartmentUi[] = [];

  constructor(private departmentService: DepartmentService) {}

  ngOnInit(): void {
    this.departmentService.getDepartments().subscribe({
      next: (data) => {
        // Pre-compute initials to prevent function calls in template expressions (Angular performance best practice)
        this.departments = data.map((dept) => ({
          ...dept,
          headOfficerInitials: dept.headOfficerName ? this.computeInitials(dept.headOfficerName) : '',
        }));
      },
      error: (err) => {
        console.error('Error fetching departments:', err);
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

  /** Helper to pre-compute name initials - e.g. "Sarah Jenkins" -> "SJ" */
  private computeInitials(name: string): string {
    return name
      .split(' ')
      .map((w) => w[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }
}
