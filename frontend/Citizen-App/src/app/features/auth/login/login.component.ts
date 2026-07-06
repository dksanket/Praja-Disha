import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { CitizenService } from '../../../core/services/citizen.service';
import { LOGIN_STRINGS } from './login.strings';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {
  strings = LOGIN_STRINGS['en'];
  
  identifier = LOGIN_STRINGS['en'].dummyPhone;
  otp = LOGIN_STRINGS['en'].dummyOtp;
  selectedLanguage = 'en';
  isSending = false;
  isSent = false;

  languages = [
    { code: 'en', label: 'English' },
    { code: 'hi', label: 'हिन्दी (Hindi)' },
    { code: 'kn', label: 'ಕನ್ನಡ (Kannada)' },
    { code: 'bn', label: 'বাংলা (Bengali)' },
    { code: 'ta', label: 'தமிழ் (Tamil)' },
    { code: 'te', label: 'తెలుగు (Telugu)' },
    { code: 'mr', label: 'मराठी (Marathi)' },
    { code: 'gu', label: 'ગુજરાતી (Gujarati)' },
    { code: 'ml', label: 'മലയാളം (Malayalam)' },
    { code: 'pa', label: 'ਪੰਜਾਬੀ (Punjabi)' },
    { code: 'or', label: 'ଓଡ଼ಿଆ (Odiya)' },
    { code: 'as', label: 'অসমীয়া (Assamese)' }
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
          if (profile) {
            this.selectedLanguage = profile.language || 'en';
            this.strings = LOGIN_STRINGS[this.selectedLanguage] || LOGIN_STRINGS['en'];
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

  onSubmit(event: Event): void {
    event.preventDefault();
    if (!this.identifier.trim() || !this.otp.trim()) {
      return;
    }

    this.isSending = true;

    // Simulate OTP sending delay
    setTimeout(() => {
      this.isSending = false;
      this.citizenService.login(this.identifier).subscribe(() => {
        // After logging in, take user to the report issue page
        this.router.navigate(['/app/report']);
      });
    }, 800);
  }
}
