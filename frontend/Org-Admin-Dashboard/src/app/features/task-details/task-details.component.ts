import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { TASK_DETAILS_STRINGS } from './task-details.strings';
import { TaskDetailsService, TaskDetailPayload } from '../../core/services/task-details.service';

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

  // Audio simulation state
  isAudioPlaying = false;
  audioPercent = 0;
  audioDurationSeconds = 42; // default mock
  currentAudioSeconds = 0;
  formattedAudioTime = '0:00';

  private readonly subscription = new Subscription();
  private audioTimerSubscription: Subscription | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly location: Location,
    private readonly taskDetailsService: TaskDetailsService
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
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
    this.stopAudioTimer();
  }

  /**
   * Loads task details from service.
   */
  private loadTaskDetails(id: string): void {
    this.subscription.add(
      this.taskDetailsService.getTaskDetails(id).subscribe({
        next: (data) => {
          this.task = data;
          this.resetAudioPlayer(data.voiceDuration);
        },
        error: (err) => console.error('Error fetching task details:', err)
      })
    );
  }

  /**
   * Resets the mock audio player state.
   */
  private resetAudioPlayer(durationStr: string): void {
    this.stopAudioTimer();
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
   * Toggles the audio simulation play/pause state.
   */
  toggleAudio(): void {
    this.isAudioPlaying = !this.isAudioPlaying;
    if (this.isAudioPlaying) {
      this.startAudioTimer();
    } else {
      this.stopAudioTimer();
    }
  }

  /**
   * Starts the mock timer ticking every second.
   */
  private startAudioTimer(): void {
    this.stopAudioTimer(); // safeguard
    this.audioTimerSubscription = interval(1000).subscribe(() => {
      this.currentAudioSeconds++;
      if (this.currentAudioSeconds >= this.audioDurationSeconds) {
        this.currentAudioSeconds = this.audioDurationSeconds;
        this.stopAudioTimer();
        this.isAudioPlaying = false;
      }
      this.audioPercent = (this.currentAudioSeconds / this.audioDurationSeconds) * 100;
      this.formattedAudioTime = this.formatTime(this.currentAudioSeconds);
    });
  }

  /**
   * Stops the active audio simulation subscription.
   */
  private stopAudioTimer(): void {
    if (this.audioTimerSubscription) {
      this.audioTimerSubscription.unsubscribe();
      this.audioTimerSubscription = null;
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
          this.task = updatedTask;
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
          this.task = updatedTask;
          this.newNoteText = '';
        },
        error: (err) => console.error('Error saving note:', err)
      })
    );
  }
}
