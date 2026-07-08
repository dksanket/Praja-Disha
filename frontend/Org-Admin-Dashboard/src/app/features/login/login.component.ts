import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LOGIN_STRINGS } from './login.strings';
import { AuthService } from '../../core/services/auth.service';

/**
 * LoginComponent — handles OTP-based and Google authentication entry point.
 * Supports toggling between phone and email auth methods.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  readonly strings = LOGIN_STRINGS;

  /** Currently selected auth method */
  authMethod: 'phone' | 'email' = 'phone';

  /** Current step in authentication flow */
  step: 'login' | 'otp' = 'login';

  /** Flag to toggle display of the demo credentials warning popup */
  showPopup = false;

  /** Holds login error message */
  errorMsg: string | null = null;

  /** Reactive form holding the identifier field, terms checkbox, and otp */
  loginForm = new FormGroup({
    countryCode: new FormControl('+91'),
    identifier: new FormControl('9999988888', [Validators.required]),
    terms: new FormControl(true, [Validators.requiredTrue]),
    otp: new FormControl('123456', [Validators.required]),
  });

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  /** Switch between phone and email modes, clearing the identifier field */
  setAuthMethod(method: 'phone' | 'email'): void {
    if (method === 'email') {
      this.showPopup = true;
      return;
    }
    this.authMethod = method;
    this.loginForm.get('identifier')?.reset('');
    this.errorMsg = null;
  }

  /** Close the demo credentials warning popup */
  closeWarningPopup(): void {
    this.showPopup = false;
  }

  /** Return back to the phone input step */
  goBack(): void {
    this.step = 'login';
  }

  /** Google SSO handler — triggers warning popup for the demo */
  onGoogleLogin(): void {
    this.showPopup = true;
  }

  /** Send OTP or verify OTP depending on current step */
  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    this.errorMsg = null;
    const identifier = this.loginForm.get('identifier')?.value || '';

    // Enforce demo phone number restriction
    if (identifier !== '9999988888') {
      this.showPopup = true;
      return;
    }

    if (this.step === 'login') {
      // Advance to the OTP verification page step
      this.step = 'otp';
      return;
    }

    // On OTP step: proceed to dashboard with any OTP, bypassing validation
    this.authService.login(identifier).subscribe((success) => {
      if (success) {
        this.router.navigate(['/dashboard']);
      } else {
        this.errorMsg = this.strings.errorInvalidOrNotAllowed;
      }
    });
  }

  /**
   * Adds the CSS press-scale class to the button element.
   * Called from template on mousedown to avoid EventTarget typing issues.
   */
  pressButton(event: MouseEvent): void {
    (event.currentTarget as HTMLButtonElement).classList.add('btn--pressed');
  }

  /** Removes the CSS press-scale class on mouseup / mouseleave. */
  releaseButton(event: MouseEvent): void {
    (event.currentTarget as HTMLButtonElement).classList.remove('btn--pressed');
  }
}
