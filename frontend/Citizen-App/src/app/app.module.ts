import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ShellLayoutComponent } from './features/layout/shell-layout/shell-layout.component';
import { ReportComponent } from './features/citizen/report/report.component';
import { TrackComponent } from './features/citizen/track/track.component';
import { TicketDetailsComponent } from './features/citizen/ticket-details/ticket-details.component';
import { WalletComponent } from './features/citizen/wallet/wallet.component';
import { ProfileComponent } from './features/citizen/profile/profile.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    ShellLayoutComponent,
    ReportComponent,
    TrackComponent,
    TicketDetailsComponent,
    WalletComponent,
    ProfileComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
