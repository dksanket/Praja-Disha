import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { CitizenService, Ticket } from '../../../core/services/citizen.service';
import { TRACK_STRINGS } from './track.strings';

interface DisplayTicket extends Ticket {
  isStep1Active: boolean;
  isStep2Active: boolean;
  isStep3Active: boolean;
  isStep4Active: boolean;
  
  step1Class: string;
  step2Class: string;
  step3Class: string;
  step4Class: string;
  
  line1Class: string;
  line2Class: string;
  line3Class: string;
}

@Component({
  selector: 'app-track',
  templateUrl: './track.component.html',
  styleUrls: ['./track.component.scss']
})
export class TrackComponent implements OnInit, OnDestroy {
  strings = TRACK_STRINGS['en'];
  
  displayTickets: DisplayTicket[] = [];
  isRefreshing = false;

  // Pull to refresh state
  pullStartY = 0;
  pullDistance = 0;
  readonly pullThreshold = 70; // Threshold distance in pixels to trigger refresh
  private readonly maxPull = 120; // Cap pull distance to prevent excessive drawing
  
  private subscription = new Subscription();

  constructor(
    private readonly router: Router,
    private readonly citizenService: CitizenService
  ) {}

  private isAtTop(): boolean {
    return (window.pageYOffset || document.documentElement.scrollTop || window.scrollY || 0) === 0;
  }

  onTouchStart(event: TouchEvent): void {
    if (this.isAtTop()) {
      this.pullStartY = event.touches[0].clientY;
    }
  }

  onTouchMove(event: TouchEvent): void {
    if (this.isAtTop() && this.pullStartY > 0 && !this.isRefreshing) {
      const currentY = event.touches[0].clientY;
      const diff = currentY - this.pullStartY;
      
      if (diff > 0) {
        // Apply scaling resistance
        this.pullDistance = Math.min(diff * 0.4, this.maxPull);
        // Prevent default overscroll pull on mobile browsers if pullDistance is positive
        if (event.cancelable) {
          event.preventDefault();
        }
      }
    }
  }

  onTouchEnd(): void {
    if (this.pullDistance >= this.pullThreshold && !this.isRefreshing) {
      this.pullDistance = 50; // Hold at loading state height
      this.refreshTickets();
    } else if (!this.isRefreshing) {
      this.resetPullDistance();
    }
    this.pullStartY = 0;
  }

  refreshTickets(): void {
    this.isRefreshing = true;
    this.citizenService.loadTickets().subscribe({
      next: () => {
        // Visual delay to ensure the spinner is visible to the user
        setTimeout(() => {
          this.isRefreshing = false;
          this.resetPullDistance();
        }, 800);
      },
      error: (err) => {
        console.error('Error refreshing tickets:', err);
        this.isRefreshing = false;
        this.resetPullDistance();
      }
    });
  }

  private resetPullDistance(): void {
    const step = this.pullDistance / 8;
    const animate = () => {
      if (this.pullDistance > 0) {
        this.pullDistance = Math.max(0, this.pullDistance - step);
        requestAnimationFrame(animate);
      }
    };
    requestAnimationFrame(animate);
  }

  ngOnInit(): void {
    // Load tickets from backend on initialization
    this.citizenService.loadTickets().subscribe();

    this.subscription.add(
      this.citizenService.tickets$.subscribe({
        next: (tickets) => {
          this.displayTickets = tickets.map(t => this.mapToDisplayTicket(t));
        }
      })
    );
    
    this.subscription.add(
      this.citizenService.currentUser$.subscribe({
        next: (profile) => {
          if (profile) {
            const lang = profile.language || 'en';
            this.strings = TRACK_STRINGS[lang] || TRACK_STRINGS['en'];
          }
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  // Pre-calculate all stepper visual states
  private mapToDisplayTicket(ticket: Ticket): DisplayTicket {
    const status = ticket.status;
    
    // Check points
    const step1 = true; // Always submitted
    const step2 = status === 'AI-Assigned' || status === 'In Progress' || status === 'Resolved';
    const step3 = status === 'In Progress' || status === 'Resolved';
    const step4 = status === 'Resolved';

    // Tailwind styles mapping
    const activeClass = 'bg-primary text-on-primary ring-4 ring-primary-container/20';
    const doneClass = 'bg-primary-container text-on-primary-container';
    const pendingClass = 'bg-surface-container-high text-on-surface-variant';

    let s1 = doneClass;
    let s2 = pendingClass;
    let s3 = pendingClass;
    let s4 = pendingClass;

    let l1 = 'bg-outline-variant';
    let l2 = 'bg-outline-variant';
    let l3 = 'bg-outline-variant';

    if (status === 'Submitted') {
      s1 = activeClass;
    } else if (status === 'AI-Assigned') {
      s1 = doneClass;
      s2 = activeClass;
      l1 = 'bg-primary-container';
    } else if (status === 'In Progress') {
      s1 = doneClass;
      s2 = doneClass;
      s3 = activeClass;
      l1 = 'bg-primary-container';
      l2 = 'bg-primary-container';
    } else if (status === 'Resolved') {
      s1 = doneClass;
      s2 = doneClass;
      s3 = doneClass;
      s4 = doneClass;
      l1 = 'bg-primary-container';
      l2 = 'bg-primary-container';
      l3 = 'bg-primary-container';
    }

    return {
      ...ticket,
      isStep1Active: step1,
      isStep2Active: step2,
      isStep3Active: step3,
      isStep4Active: step4,
      step1Class: s1,
      step2Class: s2,
      step3Class: s3,
      step4Class: s4,
      line1Class: l1,
      line2Class: l2,
      line3Class: l3
    };
  }

  viewDetails(id: string): void {
    this.router.navigate([`/app/track/${id}`]);
  }

  goToReport(): void {
    this.router.navigate(['/app/report']);
  }
}
