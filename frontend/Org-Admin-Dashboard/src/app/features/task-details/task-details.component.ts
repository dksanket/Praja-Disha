import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { TASK_DETAILS_STRINGS } from './task-details.strings';
import { TaskDetailsService } from '../../core/services/task-details.service';
import { TaskDetailPayload } from '../../core/models/task-detail.model';
import { DepartmentService } from '../../core/services/department.service';
import { OfficerService } from '../../core/services/officer.service';
import { Department } from '../../core/models/department.model';
import { Officer } from '../../core/models/officer.model';
import { environment } from '../../../environments/environment';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

/**
 * TaskDetailsComponent — handles the Task Deep-Dive detail dashboard,
 * including mock audio simulation, tab switching, and local comment/note storage.
 */
@Component({
  selector: 'app-task-details',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './task-details.component.html',
  styleUrls: ['./task-details.component.scss']
})
export class TaskDetailsComponent implements OnInit, OnDestroy {
  readonly strings = TASK_DETAILS_STRINGS;

  task: TaskDetailPayload | null = null;
  activeTab: 'comments' | 'notes' | 'activity' = 'comments';
  
  // Input fields
  newCommentText = '';
  newNoteText = '';

  // Master Lists
  departmentsList: Department[] = [];
  officersList: Officer[] = [];
  filteredOfficersList: Officer[] = [];

  // Modal Visibility States
  showUpdateStatusModal = false;
  showEditAssigneeModal = false;
  showAddSubtaskModal = false;

  // Update Status Form Inputs
  selectedStatus: 'Submitted' | 'AI-Assigned' | 'In Progress' | 'Resolved' | 'Rejected' = 'In Progress';
  statusRemarks = '';

  // Edit Assignee Form Inputs
  selectedDeptId = '';
  selectedOfficerId = '';

  // Add Subtask Form Inputs
  subtaskTitle = '';
  subtaskDescription = '';
  subtaskPriority: 'P0' | 'P1' | 'P2' | 'P3' = 'P2';
  subtaskDeptId = '';
  subtaskOfficerId = '';

  // Audio simulation state
  isAudioPlaying = false;
  audioPercent = 0;
  audioDurationSeconds = 42; // default mock
  currentAudioSeconds = 0;
  formattedAudioTime = '0:00';

  private readonly subscription = new Subscription();
  private audio: HTMLAudioElement | null = null;

  sanitizedMapUrl: SafeResourceUrl | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly location: Location,
    private readonly taskDetailsService: TaskDetailsService,
    private readonly departmentService: DepartmentService,
    private readonly officerService: OfficerService,
    private readonly sanitizer: DomSanitizer,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Listen to route params to load task details
    this.subscription.add(
      this.route.paramMap.subscribe(params => {
        const id = params.get('id');
        if (id) {
          this.loadTaskDetails(id);
        }
      })
    );

    // Fetch lists of departments and officers
    this.loadDepartmentsAndOfficers();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
    this.cleanupAudio();
  }

  /**
   * Resolves relative media URLs to absolute URLs using apiBaseUrl.
   */
  private resolveUrl(url: string | undefined): string {
    if (!url) return '';
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    }
    // Prepend backend apiBaseUrl to relative paths (e.g. /files/images/...)
    return `${environment.apiBaseUrl}${url}`;
  }

  /**
   * Processes the task details payload after fetching/updating, reversing
   * the activities array (newest first) and resolving relative media URLs.
   */
  private processTaskDetails(data: TaskDetailPayload): TaskDetailPayload {
    if (data) {
      if (data.activities) {
        data.activities = [...data.activities].reverse();
      }
      if (data.imageUrl) {
        data.imageUrl = this.resolveUrl(data.imageUrl);
      }
      if (data.mediaUrls) {
        data.mediaUrls = data.mediaUrls.map(url => this.resolveUrl(url));
      }
      if (data.voiceUrl) {
        data.voiceUrl = this.resolveUrl(data.voiceUrl);
      }
      // Generate a secure Google Maps embed URL using the task location coordinate points
      if (data.location && data.location.lat && data.location.lng) {
        const lat = data.location.lat;
        const lng = data.location.lng;
        const url = `https://maps.google.com/maps?q=${lat},${lng}&t=&z=15&ie=UTF8&iwloc=&output=embed`;
        this.sanitizedMapUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
      } else {
        this.sanitizedMapUrl = null;
      }
    }
    return data;
  }

  /**
   * Loads task details from service.
   */
  private loadTaskDetails(id: string): void {
    this.subscription.add(
      this.taskDetailsService.getTaskDetails(id).subscribe({
        next: (data) => {
          this.cleanupAudio();
          this.task = this.processTaskDetails(data);
          this.resetAudioPlayer(data.voiceDuration);
        },
        error: (err) => console.error('Error fetching task details:', err)
      })
    );
  }

  /**
   * Resets the audio player state.
   */
  private resetAudioPlayer(durationStr: string): void {
    this.cleanupAudio();
    this.isAudioPlaying = false;
    this.audioPercent = 0;
    this.currentAudioSeconds = 0;
    this.formattedAudioTime = '0:00';

    // Parse duration "0:42" -> 42, "0:30" -> 30
    if (durationStr) {
      const parts = durationStr.split(':');
      if (parts.length === 2) {
        const mins = parseInt(parts[0], 10);
        const secs = parseInt(parts[1], 10);
        this.audioDurationSeconds = (mins * 60) + secs;
      }
    }
  }

  /**
   * Toggles the audio play/pause state.
   */
  toggleAudio(): void {
    if (!this.task || !this.task.voiceUrl) return;

    if (this.isAudioPlaying) {
      this.pauseAudio();
    } else {
      this.playAudio();
    }
  }

  private playAudio(): void {
    if (!this.audio) {
      this.audio = new Audio(this.task!.voiceUrl);

      this.audio.addEventListener('timeupdate', () => {
        if (this.audio) {
          this.currentAudioSeconds = Math.floor(this.audio.currentTime);
          const duration = this.audio.duration || this.audioDurationSeconds;
          this.audioPercent = duration > 0 ? (this.audio.currentTime / duration) * 100 : 0;
          this.formattedAudioTime = this.formatTime(this.currentAudioSeconds);
          this.cdr.detectChanges();
        }
      });

      this.audio.addEventListener('ended', () => {
        this.resetAudioState();
        this.cdr.detectChanges();
      });

      this.audio.addEventListener('loadedmetadata', () => {
        if (this.audio && this.audio.duration) {
          this.audioDurationSeconds = Math.floor(this.audio.duration);
        }
      });
    }

    this.audio.play().then(() => {
      this.isAudioPlaying = true;
      this.cdr.detectChanges();
    }).catch(err => {
      console.error('Failed to play audio:', err);
      this.isAudioPlaying = false;
      this.cdr.detectChanges();
    });
  }

  private pauseAudio(): void {
    if (this.audio) {
      this.audio.pause();
    }
    this.isAudioPlaying = false;
    this.cdr.detectChanges();
  }

  private resetAudioState(): void {
    this.isAudioPlaying = false;
    this.audioPercent = 0;
    this.currentAudioSeconds = 0;
    this.formattedAudioTime = '0:00';
  }

  private cleanupAudio(): void {
    this.resetAudioState();
    if (this.audio) {
      this.audio.pause();
      this.audio = null;
    }
  }

  /**
   * Formats seconds into a M:SS representation.
   */
  private formatTime(totalSeconds: number): string {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    const secsStr = secs < 10 ? '0' + secs : secs.toString();
    return `${mins}:${secsStr}`;
  }

  /**
   * Navigates back, using Location history, or falling back to dashboard.
   */
  goBack(): void {
    // Standard Angular approach to go back
    const state = this.location.getState() as any;
    if (state && state.navigationId > 1) {
      this.location.back();
    } else {
      // Fallback
      window.location.href = '/dashboard';
    }
  }

  /**
   * Switches the active detail tab.
   */
  setActiveTab(tab: 'comments' | 'notes' | 'activity'): void {
    this.activeTab = tab;
  }

  /**
   * Submits a public comment and updates task model.
   */
  submitComment(): void {
    const text = this.newCommentText.trim();
    if (!text || !this.task) return;

    this.subscription.add(
      this.taskDetailsService.addComment(
        this.task.id,
        text,
        true, // Officer flag (since this is the Org Admin Dashboard)
        'Suresh M.' // Logged in officer name (mocked)
      ).subscribe({
        next: (updatedTask) => {
          this.task = this.processTaskDetails(updatedTask);
          this.newCommentText = '';
        },
        error: (err) => console.error('Error submitting comment:', err)
      })
    );
  }

  /**
   * Submits an internal note and updates task model.
   */
  submitNote(): void {
    const text = this.newNoteText.trim();
    if (!text || !this.task) return;

    this.subscription.add(
      this.taskDetailsService.addNote(
        this.task.id,
        text,
        'Suresh M.' // Logged in officer name (mocked)
      ).subscribe({
        next: (updatedTask) => {
          this.task = this.processTaskDetails(updatedTask);
          this.newNoteText = '';
        },
        error: (err) => console.error('Error saving note:', err)
      })
    );
  }

  private loadDepartmentsAndOfficers(): void {
    this.subscription.add(
      this.departmentService.getDepartments().subscribe({
        next: (depts) => {
          this.departmentsList = depts;
        },
        error: (err) => console.error('Error fetching departments:', err)
      })
    );

    this.subscription.add(
      this.officerService.getOfficers().subscribe({
        next: (off) => {
          this.officersList = off;
        },
        error: (err) => console.error('Error fetching officers:', err)
      })
    );
  }

  onAssigneeDeptChange(): void {
    if (!this.selectedDeptId) {
      this.filteredOfficersList = [];
    } else {
      this.filteredOfficersList = this.officersList.filter(o => 
        o.departmentIds && o.departmentIds.includes(this.selectedDeptId)
      );
    }
    this.selectedOfficerId = '';
  }

  onSubtaskDeptChange(): void {
    if (!this.subtaskDeptId) {
      this.filteredOfficersList = [];
    } else {
      this.filteredOfficersList = this.officersList.filter(o => 
        o.departmentIds && o.departmentIds.includes(this.subtaskDeptId)
      );
    }
    this.subtaskOfficerId = '';
  }

  // ── Update Status Modal Handlers ───────────────────────────────────────────
  openUpdateStatusModal(): void {
    if (this.task) {
      this.selectedStatus = (this.task.globalStatus as any) || 'In Progress';
      this.statusRemarks = '';
      this.showUpdateStatusModal = true;
    }
  }

  closeUpdateStatusModal(): void {
    this.showUpdateStatusModal = false;
  }

  submitStatus(): void {
    if (!this.task) return;

    this.subscription.add(
      this.taskDetailsService.updateStatus(
        this.task.id,
        this.selectedStatus,
        this.statusRemarks
      ).subscribe({
        next: (updatedTask) => {
          this.task = this.processTaskDetails(updatedTask);
          this.closeUpdateStatusModal();
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Error updating status:', err)
      })
    );
  }

  // ── Edit Assignee Modal Handlers ────────────────────────────────────────────
  openEditAssigneeModal(): void {
    if (this.task) {
      this.selectedDeptId = '';
      this.selectedOfficerId = '';
      
      if (this.task.subTasks && this.task.subTasks.length > 0) {
        const rootNode = this.task.subTasks[0];
        const dept = this.departmentsList.find(d => d.name === rootNode.department);
        if (dept) {
          this.selectedDeptId = dept.id;
          this.onAssigneeDeptChange();
          const officer = this.officersList.find(o => o.name === rootNode.assignee);
          if (officer) {
            this.selectedOfficerId = officer.id;
          }
        }
      }
      
      this.showEditAssigneeModal = true;
    }
  }

  closeEditAssigneeModal(): void {
    this.showEditAssigneeModal = false;
  }

  submitAssignee(): void {
    if (!this.task) return;

    this.subscription.add(
      this.taskDetailsService.updateAssignee(
        this.task.id,
        this.selectedDeptId || null,
        this.selectedOfficerId || null
      ).subscribe({
        next: (updatedTask) => {
          this.task = this.processTaskDetails(updatedTask);
          this.closeEditAssigneeModal();
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Error updating assignee:', err)
      })
    );
  }

  // ── Add Subtask Modal Handlers ──────────────────────────────────────────────
  openAddSubtaskModal(): void {
    if (this.task) {
      this.subtaskTitle = '';
      this.subtaskDescription = '';
      this.subtaskPriority = 'P2';
      this.subtaskDeptId = '';
      this.subtaskOfficerId = '';
      this.filteredOfficersList = [];
      this.showAddSubtaskModal = true;
    }
  }

  closeAddSubtaskModal(): void {
    this.showAddSubtaskModal = false;
  }

  submitSubtask(): void {
    if (!this.task || !this.subtaskTitle.trim()) return;

    this.subscription.add(
      this.taskDetailsService.createSubTask(
        this.task.id,
        this.subtaskTitle.trim(),
        this.subtaskDescription.trim(),
        this.subtaskPriority,
        this.task.category,
        this.subtaskDeptId || null,
        this.subtaskOfficerId || null
      ).subscribe({
        next: () => {
          this.loadTaskDetails(this.task!.id);
          this.closeAddSubtaskModal();
        },
        error: (err) => console.error('Error creating subtask:', err)
      })
    );
  }
}
