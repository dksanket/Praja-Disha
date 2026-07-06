import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ShellLayoutComponent } from './features/layout/shell-layout/shell-layout.component';
import { ReportComponent } from './features/citizen/report/report.component';
import { TrackComponent } from './features/citizen/track/track.component';
import { TicketDetailsComponent } from './features/citizen/ticket-details/ticket-details.component';
import { WalletComponent } from './features/citizen/wallet/wallet.component';
import { ProfileComponent } from './features/citizen/profile/profile.component';

const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'app',
    component: ShellLayoutComponent,
    children: [
      { path: '', redirectTo: 'track', pathMatch: 'full' },
      { path: 'report', component: ReportComponent },
      { path: 'track', component: TrackComponent },
      { path: 'track/:id', component: TicketDetailsComponent },
      { path: 'wallet', component: WalletComponent },
      { path: 'profile', component: ProfileComponent }
    ]
  },
  { path: '**', redirectTo: 'login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
