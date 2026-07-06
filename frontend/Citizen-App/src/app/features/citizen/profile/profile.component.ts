import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { CitizenService, CitizenProfile } from '../../../core/services/citizen.service';
import { PROFILE_STRINGS } from './profile.strings';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, OnDestroy {
  strings = PROFILE_STRINGS['en'];
  
  profile: CitizenProfile | null = null;
  selectedLanguage = 'en';

  languages = [
    { code: 'en', label: 'English' },
    { code: 'hi', label: 'Hindi (हिन्दी)' },
    { code: 'kn', label: 'Kannada (ಕನ್ನಡ)' },
    { code: 'bn', label: 'Bengali (বাংলা)' },
    { code: 'te', label: 'Telugu (తెలుగు)' },
    { code: 'mr', label: 'Marathi (मराठी)' },
    { code: 'ta', label: 'Tamil (தமிழ்)' },
    { code: 'ur', label: 'Urdu (اردو)' },
    { code: 'gu', label: 'Gujarati (ગુજરાતી)' },
    { code: 'ml', label: 'Malayalam (മലയാളം)' },
    { code: 'pa', label: 'Punjabi (ਪੰਜਾਬੀ)' },
    { code: 'or', label: 'Odia (ଓଡ଼િଆ)' },
    { code: 'as', label: 'Assamese (অসমীয়া)' }
  ];

  private subscription = new Subscription();

  constructor(
    private readonly router: Router,
    private readonly citizenService: CitizenService
  ) {}

  ngOnInit(): void {
    this.subscription.add(
      this.citizenService.currentUser$.subscribe({
        next: (profile) => {
          this.profile = profile;
          if (profile) {
            this.selectedLanguage = profile.language;
            this.strings = PROFILE_STRINGS[this.selectedLanguage] || PROFILE_STRINGS['en'];
          }
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  onLanguageChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedLanguage = select.value;
    this.citizenService.updateLanguage(this.selectedLanguage);
  }

  logout(): void {
    this.citizenService.logout();
    this.router.navigate(['/login']);
  }
}
