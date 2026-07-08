import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

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
import { JwtInterceptor } from './core/interceptors/jwt.interceptor';

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
    HttpClientModule,
    AppRoutingModule
  ],
  providers: [
    // Attach JWT Bearer token to every outgoing API request
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
