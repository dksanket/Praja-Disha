import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { LOGIN_STRINGS } from './login.strings';

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

  /** Reactive form holding the identifier field and terms checkbox */
  loginForm = new FormGroup({
    countryCode: new FormControl('+91'),
    identifier: new FormControl('', [Validators.required]),
    terms: new FormControl(false, [Validators.requiredTrue]),
  });

  /** Switch between phone and email modes, clearing the identifier field */
  setAuthMethod(method: 'phone' | 'email'): void {
    this.authMethod = method;
    this.loginForm.get('identifier')?.reset('');
  }

  /** Placeholder handler — integrates with the auth service in a later sprint */
  onGoogleLogin(): void {
    // TODO: trigger Google OAuth flow
  }

  /** Send OTP or trigger email link depending on authMethod */
  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    // TODO: call AuthService.sendOtp() / sendEmailLink()
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
