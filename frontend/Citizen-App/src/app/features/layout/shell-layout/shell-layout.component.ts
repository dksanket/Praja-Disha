import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { CitizenService } from '../../../core/services/citizen.service';
import { SHELL_LAYOUT_STRINGS } from './shell-layout.strings';

@Component({
  selector: 'app-shell-layout',
  templateUrl: './shell-layout.component.html',
  styleUrls: ['./shell-layout.component.scss']
})
export class ShellLayoutComponent implements OnInit, OnDestroy {
  strings = SHELL_LAYOUT_STRINGS['en'];
  
  currentUrl = '';
  avatarUrl = 'https://lh3.googleusercontent.com/aida-public/AB6AXuD2yyEKiYBHfV1ER2JdZ8jX65zyPXhS7AcQIz0oMAEMkt4R1fPvhtxLo_Jrvf4xjhtpW8QoYQoKdrl68TQxUdOVroPaUgjCFwwPRZ13ZoiD1bvTMM0slweoGEJJkMx6nZoRHZSPSYoyF8nCy3dXU_rYMAaNzWjxnQkqZxldfPmXPg3ZWHcpn1Uh-dsxUA59s2a3NihHccE9qHO62ZwtcB-gWPiSJNPbO18nGyz3VTFtri25S2vqv9PkeIPNCtdC34YG5ts2wY_l7Zs';

  private subscription = new Subscription();

  constructor(
    private readonly router: Router,
    private readonly citizenService: CitizenService
  ) {
    this.currentUrl = this.router.url;
  }

  ngOnInit(): void {
    // Track URL shifts to toggle navbar highlights
    this.subscription.add(
      this.router.events.pipe(
        filter(event => event instanceof NavigationEnd)
      ).subscribe({
        next: (event) => {
          this.currentUrl = (event as NavigationEnd).urlAfterRedirects;
        }
      })
    );

    // Dynamic language localization
    this.subscription.add(
      this.citizenService.currentUser$.subscribe({
        next: (profile) => {
          if (profile) {
            const lang = profile.language || 'en';
            this.strings = SHELL_LAYOUT_STRINGS[lang] || SHELL_LAYOUT_STRINGS['en'];
          }
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  // Pre-calculated template values (no function calls in HTML)
  get isReportActive(): boolean {
    return this.currentUrl.includes('/report');
  }

  get isTrackActive(): boolean {
    return this.currentUrl.includes('/track');
  }

  get isWalletActive(): boolean {
    return this.currentUrl.includes('/wallet');
  }

  get isProfileActive(): boolean {
    return this.currentUrl.includes('/profile');
  }

  navigateTo(path: string): void {
    this.router.navigate([`/app/${path}`]);
  }
}
