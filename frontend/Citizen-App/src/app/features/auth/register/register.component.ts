import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { CitizenService, CitizenProfile } from '../../../core/services/citizen.service';
import { REGISTER_STRINGS } from './register.strings';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit, OnDestroy {
  strings = REGISTER_STRINGS['en'];
  
  fullName = '';
  verifiedContact = '';
  termsAccepted = false;
  
  private subscription = new Subscription();

  constructor(
    private readonly router: Router,
    private readonly citizenService: CitizenService
  ) {}

  ngOnInit(): void {
    // Retrieve verified contact info and update translations dynamically
    this.subscription.add(
      this.citizenService.currentUser$.subscribe({
        next: (profile) => {
          if (profile) {
            this.verifiedContact = profile.email || profile.phone || 'jane.doe@example.com';
            const lang = profile.language || 'en';
            this.strings = REGISTER_STRINGS[lang] || REGISTER_STRINGS['en'];
          }
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    if (!this.fullName.trim() || !this.termsAccepted) {
      return;
    }

    this.citizenService.register(this.fullName);
    this.router.navigate(['/app/track']);
  }

  navigateToLogin(): void {
    this.router.navigate(['/login']);
  }
}
