import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { CitizenService } from '../../../core/services/citizen.service';
import { REPORT_STRINGS } from './report.strings';

@Component({
  selector: 'app-report',
  templateUrl: './report.component.html',
  styleUrls: ['./report.component.scss']
})
export class ReportComponent implements OnInit, OnDestroy {
  strings = REPORT_STRINGS['en'];
  
  description = '';
  location = '';
  imageUrl = '';
  
  isListening = false;
  isDetectingLocation = false;
  isSubmitting = false;
  
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
            const lang = profile.language || 'en';
            this.strings = REPORT_STRINGS[lang] || REPORT_STRINGS['en'];
          }
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  toggleMic(): void {
    if (this.isListening) {
      this.isListening = false;
      return;
    }

    this.isListening = true;
    
    // Simulate speech-to-text after 3 seconds
    setTimeout(() => {
      if (this.isListening) {
        this.isListening = false;
        const speechText = 'Deep pothole on the middle of 4th cross road, causing traffic issues and waterlogging.';
        
        // Typewriter effect simulation
        let i = 0;
        const interval = setInterval(() => {
          this.description += speechText.charAt(i);
          i++;
          if (i >= speechText.length) {
            clearInterval(interval);
          }
        }, 30);
      }
    }, 3000);
  }

  detectLocation(): void {
    if (this.isDetectingLocation) {
      return;
    }

    this.isDetectingLocation = true;
    
    // Simulate GPS fetch
    setTimeout(() => {
      this.isDetectingLocation = false;
      this.location = '4th Cross Road, Ward 150, near Government Hospital';
    }, 1500);
  }

  selectMockImage(): void {
    // Toggles a preselected mock image
    if (this.imageUrl) {
      this.imageUrl = '';
    } else {
      this.imageUrl = 'https://lh3.googleusercontent.com/aida-public/AB6AXuACadJZV3i-Z5jNXSi7RKxw_dL8jQHCW4BtE4nfkZ0eY_wHbnqHZyn896YHWvp9CMT2NNiyvOnlCe8TaZgfiCz2ox2aYMobDdZ5kxBmKYZjjbDKxAUXLehriZaUEPH7FmwhKPL2pbzarU2CAcpYB0RRmUlyvLTzQ0PiBv5rIf9ejHSdN8w6vP5NKcPSfMwulHkppuC1b4qNet3xgv7w7e5vULsiCK7ajsv0A06YnLStgb0EPekpKMojky_qvCu6r4fT64KbQhwOAkk';
    }
  }

  onSubmit(): void {
    if (!this.description.trim()) {
      return;
    }

    this.isSubmitting = true;

    // Determine category and title from text
    let title = 'Road Repair Concern';
    const text = this.description.toLowerCase();
    if (text.includes('light') || text.includes('lamp') || text.includes('bulb')) {
      title = 'Broken Streetlight';
    } else if (text.includes('pothole') || text.includes('road') || text.includes('street')) {
      title = 'Pothole on Road';
    } else if (text.includes('garbage') || text.includes('waste') || text.includes('dump')) {
      title = 'Illegal Garbage Dumping';
    } else if (text.includes('water') || text.includes('leak') || text.includes('pipe')) {
      title = 'Water Pipeline Leakage';
    }

    // Submit ticket
    setTimeout(() => {
      this.citizenService.submitTicket(title, this.description, this.location, this.imageUrl);
      this.isSubmitting = false;
      this.router.navigate(['/app/track']);
    }, 1200);
  }
}
