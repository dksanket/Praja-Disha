import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { DASHBOARD_STRINGS } from './dashboard.strings';
import { DashboardTask, DashboardStats } from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';

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
  searchTerm = '';
  selectedFilter: 'all' | 'ai-pending' | 'due-today' = 'all';

  // Pagination states
  currentPage = 1;
  readonly pageSize = 3;

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

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
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
          this.allTasks = tasks;
          this.applyFiltersAndPagination();
        },
        error: (err) => console.error('Error fetching triage tasks:', err),
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Updates search filter state and recalculates views.
   */
  onSearchChange(): void {
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
    // 1. Apply category and text search filters
    const search = this.searchTerm.trim().toLowerCase();
    this.filteredTasks = this.allTasks.filter((task) => {
      // Category filter check
      if (this.selectedFilter === 'ai-pending' && task.statusType !== 'ai-pending') {
        return false;
      }
      if (this.selectedFilter === 'due-today' && !task.dueDateCritical) {
        return false;
      }

      // Text search match check (IDs or Titles)
      if (search) {
        const idMatch = task.id.toLowerCase().includes(search);
        const titleMatch = task.title.toLowerCase().includes(search);
        const priorityMatch = task.priority.toLowerCase().includes(search);
        return idMatch || titleMatch || priorityMatch;
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
}
