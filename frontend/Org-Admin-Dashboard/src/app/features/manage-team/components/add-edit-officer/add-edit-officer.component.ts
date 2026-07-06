import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MANAGE_TEAM_STRINGS } from '../../manage-team.strings';
import { Department } from '../../../../core/models/manage-team/manage-team.domain-models';
import { Officer } from '../../../../core/models/officer.model';
import { DepartmentService } from '../../../../core/services/department.service';
import { OfficerService } from '../../../../core/services/officer.service';

/** UI definition for officer details sidebar representation */
interface ProfileNode {
  label: string;
  value: string;
  icon: string;
}

@Component({
  selector: 'app-add-edit-officer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
  ],
  templateUrl: './add-edit-officer.component.html',
  styleUrls: ['./add-edit-officer.component.scss'],
})
export class AddEditOfficerComponent implements OnInit {
  readonly strings = MANAGE_TEAM_STRINGS;

  isEditMode = false;
  officerId: string | null = null;

  // Form Fields
  name = '';
  officerUserName = '';
  email = '';
  phoneLocal = '';
  isActive = true;

  // Static options loaded from services
  allDepartments: Department[] = [];
  allOfficers: Officer[] = [];

  // Selected lists
  assignedDepartments: Department[] = [];
  assignedManagers: Officer[] = [];

  // Search dropdown overlays
  deptSearchQuery = '';
  showDeptDropdown = false;
  filteredDepartments: Department[] = [];

  managerSearchQuery = '';
  showManagerDropdown = false;
  filteredManagers: Officer[] = [];

  // Profile details representation for sidebar
  profileSidebarList: ProfileNode[] = [];
  isFormSubmitted = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private departmentService: DepartmentService,
    private officerService: OfficerService
  ) {}

  ngOnInit(): void {
    this.officerId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.officerId;

    // Load static lists
    this.departmentService.getDepartments().subscribe({
      next: (depts) => {
        this.allDepartments = depts;
        this.loadOfficerData();
      },
      error: (err) => console.error('Error fetching departments:', err),
    });
  }

  private loadOfficerData(): void {
    this.officerService.getOfficers().subscribe({
      next: (officers) => {
        this.allOfficers = officers;

        if (this.isEditMode && this.officerId) {
          this.officerService.getOfficerById(this.officerId).subscribe((officer) => {
            if (officer) {
              this.name = officer.name;
              this.officerUserName = officer.officerUserName;
              this.email = officer.email;
              this.isActive = officer.isActive;

              // Parse local 10-digit phone number if prefix exists
              if (officer.phone) {
                if (officer.phone.startsWith(this.strings.addEditOfficer.fields.phonePrefix + ' ')) {
                  this.phoneLocal = officer.phone.substring(this.strings.addEditOfficer.fields.phonePrefix.length + 1);
                } else if (officer.phone.startsWith(this.strings.addEditOfficer.fields.phonePrefix)) {
                  this.phoneLocal = officer.phone.substring(this.strings.addEditOfficer.fields.phonePrefix.length);
                } else {
                  this.phoneLocal = officer.phone;
                }
              }

              // Resolve department entities
              this.assignedDepartments = this.allDepartments.filter((d) =>
                officer.departmentIds.includes(d.id)
              );

              // Resolve manager entities
              if (officer.managerUserNames) {
                this.assignedManagers = this.allOfficers.filter((o) =>
                  officer.managerUserNames?.includes(o.officerUserName)
                );
              }
            }
            this.updateProfileSidebar();
          });
        } else {
          this.updateProfileSidebar();
        }
      },
      error: (err) => console.error('Error fetching officers:', err),
    });
  }

  /**
   * Updates the profile details preview list on the right column
   */
  updateProfileSidebar(): void {
    const list: ProfileNode[] = [];

    list.push({
      label: this.strings.addEditOfficer.fields.fullName,
      value: this.name.trim() ? this.name.trim() : `[${this.isEditMode ? 'Editing Profile' : 'New Officer'}]`,
      icon: 'person',
    });

    list.push({
      label: this.strings.addEditOfficer.fields.username,
      value: this.officerUserName.trim() ? '@' + this.officerUserName.trim() : '—',
      icon: 'badge',
    });

    list.push({
      label: this.strings.addEditOfficer.fields.email,
      value: this.email.trim() ? this.email.trim() : '—',
      icon: 'mail',
    });

    const prefix = this.strings.addEditOfficer.fields.phonePrefix;
    list.push({
      label: this.strings.addEditOfficer.fields.phone,
      value: this.phoneLocal.trim() ? `${prefix} ${this.phoneLocal.trim()}` : '—',
      icon: 'phone',
    });

    list.push({
      label: this.strings.addEditOfficer.fields.status,
      value: this.isActive ? this.strings.addEditOfficer.fields.statusActive : this.strings.addEditOfficer.fields.statusInactive,
      icon: this.isActive ? 'check_circle' : 'block',
    });

    this.profileSidebarList = list;
  }

  /**
   * Department Search Autocomplete logic
   */
  onDeptSearchChange(): void {
    const query = this.deptSearchQuery.trim().toLowerCase();
    if (!query) {
      this.filteredDepartments = [];
      this.showDeptDropdown = false;
      return;
    }

    const assignedIds = new Set(this.assignedDepartments.map((d) => d.id));
    this.filteredDepartments = this.allDepartments.filter(
      (d) =>
        (d.name.toLowerCase().includes(query) || d.id.toLowerCase().includes(query)) &&
        !assignedIds.has(d.id)
    );
    this.showDeptDropdown = this.filteredDepartments.length > 0;
  }

  selectDepartment(dept: Department): void {
    this.assignedDepartments = [...this.assignedDepartments, dept];
    this.deptSearchQuery = '';
    this.showDeptDropdown = false;
    this.updateProfileSidebar();
  }

  removeDepartment(dept: Department): void {
    this.assignedDepartments = this.assignedDepartments.filter((d) => d.id !== dept.id);
    this.updateProfileSidebar();
  }

  /**
   * Manager Search Autocomplete logic
   */
  onManagerSearchChange(): void {
    const query = this.managerSearchQuery.trim().toLowerCase();
    if (!query) {
      this.filteredManagers = [];
      this.showManagerDropdown = false;
      return;
    }

    const assignedUserNames = new Set(this.assignedManagers.map((m) => m.officerUserName));
    this.filteredManagers = this.allOfficers.filter(
      (o) =>
        (o.name.toLowerCase().includes(query) || o.officerUserName.toLowerCase().includes(query)) &&
        !assignedUserNames.has(o.officerUserName) &&
        // Prevent assigning oneself as manager
        (!this.isEditMode || o.id !== this.officerId)
    );
    this.showManagerDropdown = this.filteredManagers.length > 0;
  }

  selectManager(officer: Officer): void {
    this.assignedManagers = [...this.assignedManagers, officer];
    this.managerSearchQuery = '';
    this.showManagerDropdown = false;
    this.updateProfileSidebar();
  }

  removeManager(officer: Officer): void {
    this.assignedManagers = this.assignedManagers.filter((m) => m.id !== officer.id);
    this.updateProfileSidebar();
  }

  /**
   * Generate next mock ID for Officer of type OFF-XXX
   */
  private generateNextId(officers: Officer[]): string {
    const ids = officers
      .map((o) => {
        const match = o.id.match(/OFF-(\d+)/);
        return match ? parseInt(match[1], 10) : 0;
      })
      .filter((n) => n > 0);

    const max = ids.length > 0 ? Math.max(...ids) : 0;
    const nextNum = max + 1;
    return `OFF-${nextNum.toString().padStart(3, '0')}`;
  }

  /**
   * Validates and saves the officer data
   */
  save(): void {
    this.isFormSubmitted = true;
    this.errorMessage = '';

    // Validate Name
    if (!this.name.trim()) {
      this.errorMessage = this.strings.addEditOfficer.validation.nameRequired;
      return;
    }

    // Validate Username
    if (!this.officerUserName.trim()) {
      this.errorMessage = this.strings.addEditOfficer.validation.usernameRequired;
      return;
    }

    const usernamePattern = /^[a-zA-Z0-9_]+$/;
    if (!usernamePattern.test(this.officerUserName.trim())) {
      this.errorMessage = this.strings.addEditOfficer.validation.usernamePattern;
      return;
    }

    if (this.officerUserName.trim().length < 3) {
      this.errorMessage = this.strings.addEditOfficer.validation.usernameLength;
      return;
    }

    // Check for duplicate username
    const duplicate = this.allOfficers.some(
      (o) =>
        o.officerUserName.toLowerCase() === this.officerUserName.trim().toLowerCase() &&
        (!this.isEditMode || o.id !== this.officerId)
    );
    if (duplicate) {
      this.errorMessage = this.strings.addEditOfficer.validation.duplicateUsername;
      return;
    }

    // Validate Email if entered
    if (this.email.trim()) {
      const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailPattern.test(this.email.trim())) {
        this.errorMessage = this.strings.addEditOfficer.validation.emailPattern;
        return;
      }
    }

    // Validate Phone if entered
    if (this.phoneLocal.trim()) {
      const phoneDigits = this.phoneLocal.trim().replace(/\s+/g, '');
      if (phoneDigits.length !== 10 || isNaN(Number(phoneDigits))) {
        this.errorMessage = this.strings.addEditOfficer.validation.phonePattern;
        return;
      }
    }

    // Build Payload
    const phoneFull = this.phoneLocal.trim()
      ? `${this.strings.addEditOfficer.fields.phonePrefix} ${this.phoneLocal.trim()}`
      : '';

    const officerPayload: Officer = {
      id: this.isEditMode && this.officerId ? this.officerId : this.generateNextId(this.allOfficers),
      orgIds: ['ORG-001'],
      officerUserName: this.officerUserName.trim(),
      name: this.name.trim(),
      email: this.email.trim(),
      phone: phoneFull,
      departmentIds: this.assignedDepartments.map((d) => d.id),
      isActive: this.isActive,
      managerUserNames: this.assignedManagers.map((m) => m.officerUserName),
      createdAt: this.isEditMode ? 0 : Date.now(), // Will be resolved correctly
    };

    if (this.isEditMode && this.officerId) {
      this.officerService.updateOfficer(this.officerId, officerPayload).subscribe({
        next: () => this.router.navigate(['/manage-team']),
        error: (err) => {
          this.errorMessage = this.strings.addEditOfficer.validation.saveFailed;
          console.error(err);
        },
      });
    } else {
      this.officerService.createOfficer(officerPayload).subscribe({
        next: () => this.router.navigate(['/manage-team']),
        error: (err) => {
          this.errorMessage = this.strings.addEditOfficer.validation.createFailed;
          console.error(err);
        },
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/manage-team']);
  }
}
