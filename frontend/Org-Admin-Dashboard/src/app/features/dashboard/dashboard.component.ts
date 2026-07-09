import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { DASHBOARD_STRINGS } from './dashboard.strings';
import { DashboardTask, DashboardStats } from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { DepartmentService } from '../../core/services/department.service';
import { Department } from '../../core/models/department.model';

export interface DropdownDepartment extends Department {
  selected: boolean;
}

/**
 * DashboardComponent — Command Center Dashboard feature component.
 * Visualizes operational metrics via a Bento grid and triage tasks via a table.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  readonly strings = DASHBOARD_STRINGS;

  // Raw data from service
  allTasks: DashboardTask[] = [];
  stats: DashboardStats = {
    awaitingAiReviewCount: 0,
    dueTodayCount: 0,
    myDeptCount: 0,
  };

  // Filter & Search states
  filterTaskId = '';
  filterTitle = '';
  filterPriority = 'all';
  filterDueDate = '';
  filterStatus = 'all';
  selectedFilter: 'all' | 'ai-pending' | 'due-today' = 'all';
  selectedGroupId: string | null = null;
  showFutureFeatureModal = false;

  // Department dropdown states
  hierarchicalDepartments: DropdownDepartment[] = [];
  filteredDropdownDepts: DropdownDepartment[] = [];
  selectedDeptNames: string[] = [];
  selectedDeptNamesSummary = '';
  dropdownOpen = false;
  deptSearchQuery = '';

  // Pagination states
  currentPage = 1;
  readonly pageSize = 7;

  // Pre-calculated template values (no function calls in HTML)
  filteredTasks: DashboardTask[] = [];
  paginatedTasks: DashboardTask[] = [];
  totalItems = 0;
  startIndex = 0;
  endIndex = 0;
  totalPages = 1;
  hasPreviousPage = false;
  hasNextPage = false;

  private readonly subscription = new Subscription();

  constructor(
    private readonly dashboardService: DashboardService,
    private readonly departmentService: DepartmentService
  ) {}

  ngOnInit(): void {
    this.selectedDeptNamesSummary = this.strings.triageTable.filterPlaceholders.assignment;

    // Fetches live statistics
    this.subscription.add(
      this.dashboardService.getStats().subscribe({
        next: (liveStats) => {
          this.stats = liveStats;
        },
        error: (err) => console.error('Error fetching dashboard statistics:', err),
      })
    );

    // Fetches live tasks list
    this.subscription.add(
      this.dashboardService.getTasks().subscribe({
        next: (tasks) => {
          this.allTasks = this.markDuplicateGroups(tasks);
          this.applyFiltersAndPagination();
        },
        error: (err) => console.error('Error fetching triage tasks:', err),
      })
    );

    // Fetches departments
    this.subscription.add(
      this.departmentService.getDepartments().subscribe({
        next: (depts) => {
          this.buildHierarchicalDepartments(depts);
        },
        error: (err) => console.error('Error fetching departments:', err),
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Updates column filters state and recalculates views.
   */
  onFilterChange(): void {
    this.currentPage = 1; // Resets pagination to first page
    this.applyFiltersAndPagination();
  }

  /**
   * Sets active category filter based on Bento card clicks.
   */
  setCategoryFilter(filter: 'all' | 'ai-pending' | 'due-today'): void {
    // Toggles filter back to 'all' if selected again
    this.selectedFilter = this.selectedFilter === filter ? 'all' : filter;
    this.currentPage = 1;
    this.applyFiltersAndPagination();
  }

  /**
   * Shifts table page backwards.
   */
  prevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.applyFiltersAndPagination();
    }
  }

  /**
   * Shifts table page forwards.
   */
  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.applyFiltersAndPagination();
    }
  }

  /**
   * Central filter & pagination engine.
   * Updates bound variables to guarantee 0 function calls inside HTML.
   */
  private applyFiltersAndPagination(): void {
    // 1. Apply category, group, and column filters
    this.filteredTasks = this.allTasks.filter((task) => {
      // Group ID filter check
      if (this.selectedGroupId && task.groupId !== this.selectedGroupId) {
        return false;
      }

      // Category filter check
      if (this.selectedFilter === 'ai-pending' && task.statusType !== 'ai-pending') {
        return false;
      }
      if (this.selectedFilter === 'due-today' && !task.dueDateCritical) {
        return false;
      }

      // Column Filters
      // 1. Task ID filter (case-insensitive partial match)
      if (this.filterTaskId) {
        const idSearch = this.filterTaskId.trim().toLowerCase();
        if (!task.id.toLowerCase().includes(idSearch)) {
          return false;
        }
      }

      // 2. Title filter (case-insensitive partial match)
      if (this.filterTitle) {
        const titleSearch = this.filterTitle.trim().toLowerCase();
        if (!task.title.toLowerCase().includes(titleSearch)) {
          return false;
        }
      }

      // 3. Priority filter (exact match if not 'all')
      if (this.filterPriority && this.filterPriority !== 'all') {
        if (task.priority !== this.filterPriority) {
          return false;
        }
      }

      // 4. Assignment filter (checks if task assignments match any of the selected department names)
      if (this.selectedDeptNames.length > 0) {
        const hasAssignMatch = task.assignments.some((assign) =>
          this.selectedDeptNames.includes(assign.label)
        );
        if (!hasAssignMatch) {
          return false;
        }
      }

      // 5. Due Date filter (case-insensitive partial match)
      if (this.filterDueDate) {
        const dueSearch = this.filterDueDate.trim().toLowerCase();
        if (!task.dueDate.toLowerCase().includes(dueSearch)) {
          return false;
        }
      }

      // 6. Status filter (exact match on statusType if not 'all')
      if (this.filterStatus && this.filterStatus !== 'all') {
        if (task.statusType !== this.filterStatus) {
          return false;
        }
      }

      return true;
    });

    // 2. Perform pagination calculations
    this.totalItems = this.filteredTasks.length;
    this.totalPages = Math.max(1, Math.ceil(this.totalItems / this.pageSize));

    // Boundary check for current page
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages;
    }

    this.startIndex = this.totalItems === 0 ? 0 : (this.currentPage - 1) * this.pageSize + 1;
    const computedEnd = this.currentPage * this.pageSize;
    this.endIndex = Math.min(computedEnd, this.totalItems);

    // Slice array for view list
    const startIdx = (this.currentPage - 1) * this.pageSize;
    this.paginatedTasks = this.filteredTasks.slice(startIdx, startIdx + this.pageSize);

    // Calculate button availability
    this.hasPreviousPage = this.currentPage > 1;
    this.hasNextPage = this.currentPage < this.totalPages;
  }

  /**
   * Helper to mark tasks as being part of a duplicate group if their groupId
   * is shared with other tasks in the active list.
   */
  private markDuplicateGroups(tasks: DashboardTask[]): DashboardTask[] {
    const groupCounts = new Map<string, number>();

    // Count occurrences of each groupId
    for (const t of tasks) {
      if (t.groupId) {
        groupCounts.set(t.groupId, (groupCounts.get(t.groupId) || 0) + 1);
      }
    }

    // Mark tasks with isDuplicateGroup = true if the groupId occurs more than once
    return tasks.map((t) => ({
      ...t,
      isDuplicateGroup: t.groupId ? (groupCounts.get(t.groupId) || 0) > 1 : false,
    }));
  }

  /**
   * Set the active group ID filter.
   */
  selectGroupId(groupId: string): void {
    this.selectedGroupId = this.selectedGroupId === groupId ? null : groupId;
    this.currentPage = 1;
    this.applyFiltersAndPagination();
  }

  /**
   * Clear the active group ID filter.
   */
  clearGroupFilter(): void {
    this.selectedGroupId = null;
    this.currentPage = 1;
    this.applyFiltersAndPagination();
  }

  /**
   * Opens the future feature notice modal.
   */
  openFutureFeatureModal(): void {
    this.showFutureFeatureModal = true;
  }

  /**
   * Closes the future feature notice modal.
   */
  closeFutureFeatureModal(): void {
    this.showFutureFeatureModal = false;
  }

  /**
   * Builds the sorted hierarchical department options.
   */
  private buildHierarchicalDepartments(depts: Department[]): void {
    const rootDepts = depts.filter((d) => !d.parentDepartmentId);
    const result: DropdownDepartment[] = [];

    const traverse = (dept: Department) => {
      result.push({
        ...dept,
        selected: false,
      });
      const children = depts.filter((d) => d.parentDepartmentId === dept.id);
      children.sort((a, b) => a.name.localeCompare(b.name));
      children.forEach(traverse);
    };

    rootDepts.sort((a, b) => a.name.localeCompare(b.name));
    rootDepts.forEach(traverse);

    // Fallback: append any orphaned departments
    depts.forEach((d) => {
      if (!result.some((r) => r.id === d.id)) {
        result.push({
          ...d,
          selected: false,
        });
      }
    });

    this.hierarchicalDepartments = result;
    this.filteredDropdownDepts = result;
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
    if (this.dropdownOpen) {
      this.deptSearchQuery = '';
      this.filteredDropdownDepts = this.hierarchicalDepartments;
    }
  }

  toggleDeptSelection(deptName: string): void {
    const dept = this.hierarchicalDepartments.find((d) => d.name === deptName);
    if (dept) {
      dept.selected = !dept.selected;
      this.updateSelectionAndFilter();
    }
  }

  selectAllDepts(): void {
    this.filteredDropdownDepts.forEach((d) => {
      d.selected = true;
    });
    this.updateSelectionAndFilter();
  }

  clearAllDepts(): void {
    this.filteredDropdownDepts.forEach((d) => {
      d.selected = false;
    });
    this.updateSelectionAndFilter();
  }

  onDeptSearchChange(): void {
    const query = this.deptSearchQuery.toLowerCase().trim();
    if (!query) {
      this.filteredDropdownDepts = this.hierarchicalDepartments;
    } else {
      this.filteredDropdownDepts = this.hierarchicalDepartments.filter((d) =>
        d.name.toLowerCase().includes(query)
      );
    }
  }

  private updateSelectionAndFilter(): void {
    this.selectedDeptNames = this.hierarchicalDepartments
      .filter((d) => d.selected)
      .map((d) => d.name);

    if (this.selectedDeptNames.length === 0) {
      this.selectedDeptNamesSummary = this.strings.triageTable.filterPlaceholders.assignment;
    } else if (this.selectedDeptNames.length === 1) {
      this.selectedDeptNamesSummary = this.selectedDeptNames[0];
    } else {
      this.selectedDeptNamesSummary = `${this.selectedDeptNames.length} ${this.strings.triageTable.filterPlaceholders.assignmentSelected}`;
    }

    this.onFilterChange();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.dept-dropdown-container')) {
      this.dropdownOpen = false;
    }
  }
}
