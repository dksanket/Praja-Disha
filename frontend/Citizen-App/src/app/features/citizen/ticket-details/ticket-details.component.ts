import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { CitizenService, Ticket } from '../../../core/services/citizen.service';
import { TICKET_DETAILS_STRINGS } from './ticket-details.strings';

@Component({
  selector: 'app-ticket-details',
  templateUrl: './ticket-details.component.html',
  styleUrls: ['./ticket-details.component.scss']
})
export class TicketDetailsComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('chatScrollContainer') private chatScrollContainer!: ElementRef;

  strings = TICKET_DETAILS_STRINGS['en'];
  
  ticket: Ticket | undefined;
  rating = 0;
  feedbackComment = '';
  feedbackSubmitted = false;
  
  // Stepper pre-computed styles
  step1Class = 'bg-primary text-on-primary';
  step2Class = 'bg-surface-container-high text-on-surface-variant';
  step3Class = 'bg-surface-container-high text-on-surface-variant';
  
  line1Style = 'width: 0%';
  line2Style = 'width: 0%';

  private subscription = new Subscription();
  private needScroll = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly citizenService: CitizenService
  ) {}

  ngOnInit(): void {
    this.subscription.add(
      this.route.paramMap.subscribe({
        next: (params) => {
          const id = params.get('id');
          if (id) {
            this.loadTicketDetails(id);
          }
        }
      })
    );

    this.subscription.add(
      this.citizenService.currentUser$.subscribe({
        next: (profile) => {
          if (profile) {
            const lang = profile.language || 'en';
            this.strings = TICKET_DETAILS_STRINGS[lang] || TICKET_DETAILS_STRINGS['en'];
          }
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  ngAfterViewChecked(): void {
    if (this.needScroll) {
      this.scrollToBottom();
      this.needScroll = false;
    }
  }

  private loadTicketDetails(id: string): void {
    this.subscription.add(
      this.citizenService.tickets$.subscribe({
        next: (tickets) => {
          this.ticket = tickets.find(t => t.id === id);
          if (this.ticket) {
            this.preCalculateStepperStyles(this.ticket.status);
            this.needScroll = true;
          }
        }
      })
    );
  }

  private preCalculateStepperStyles(status: string): void {
    const doneClass = 'bg-primary text-on-primary';
    const pendingClass = 'bg-surface-container-high text-on-surface-variant';

    if (status === 'Submitted') {
      this.step1Class = doneClass;
      this.step2Class = pendingClass;
      this.step3Class = pendingClass;
      this.line1Style = 'width: 0%';
      this.line2Style = 'width: 0%';
    } else if (status === 'AI-Assigned' || status === 'In Progress') {
      this.step1Class = doneClass;
      this.step2Class = doneClass;
      this.step3Class = pendingClass;
      this.line1Style = 'width: 100%';
      this.line2Style = 'width: 0%';
    } else if (status === 'Resolved') {
      this.step1Class = doneClass;
      this.step2Class = doneClass;
      this.step3Class = doneClass;
      this.line1Style = 'width: 100%';
      this.line2Style = 'width: 100%';
    }
  }

  selectRating(stars: number): void {
    if (this.ticket?.rating) {
      return; // Already rated
    }
    this.rating = stars;
  }

  submitFeedback(): void {
    if (!this.ticket || this.rating === 0) {
      return;
    }

    this.citizenService.submitFeedback(this.ticket.id, this.rating, this.feedbackComment);
    this.feedbackSubmitted = true;
    setTimeout(() => {
      this.feedbackSubmitted = false;
      this.rating = 0;
      this.feedbackComment = '';
    }, 3000);
  }

  reopenTicket(): void {
    if (!this.ticket) {
      return;
    }
    this.citizenService.reopenTicket(this.ticket.id);
    this.needScroll = true;
  }

  goBack(): void {
    this.router.navigate(['/app/track']);
  }

  private scrollToBottom(): void {
    try {
      this.chatScrollContainer.nativeElement.scrollTop = this.chatScrollContainer.nativeElement.scrollHeight;
    } catch (err) {
      // Container not ready or scrolled
    }
  }
}
