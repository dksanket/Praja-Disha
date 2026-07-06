import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { CitizenService, CitizenProfile, PointActivity, TransitPass } from '../../../core/services/citizen.service';
import { WALLET_STRINGS } from './wallet.strings';

@Component({
  selector: 'app-wallet',
  templateUrl: './wallet.component.html',
  styleUrls: ['./wallet.component.scss']
})
export class WalletComponent implements OnInit, OnDestroy {
  strings = WALLET_STRINGS['en'];
  
  profile: CitizenProfile | null = null;
  activities: PointActivity[] = [];
  activePasses: TransitPass[] = [];

  private subscription = new Subscription();

  constructor(private readonly citizenService: CitizenService) {}

  ngOnInit(): void {
    // Current profile and language strings update
    this.subscription.add(
      this.citizenService.currentUser$.subscribe({
        next: (profile) => {
          this.profile = profile;
          if (profile) {
            const lang = profile.language || 'en';
            this.strings = WALLET_STRINGS[lang] || WALLET_STRINGS['en'];
          }
        }
      })
    );

    // Recent activity log
    this.subscription.add(
      this.citizenService.pointActivities$.subscribe({
        next: (activities) => this.activities = activities
      })
    );

    // Passes
    this.subscription.add(
      this.citizenService.passes$.subscribe({
        next: (passes) => this.activePasses = passes.filter(p => p.isActive)
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  redeem(cost: number, title: string): void {
    const success = this.citizenService.redeemPass(cost, title);
    if (success) {
      alert(this.strings.redeemSuccess);
    } else {
      alert(this.strings.redeemFail);
    }
  }
}
