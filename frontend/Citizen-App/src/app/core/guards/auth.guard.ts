import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';

/**
 * AuthGuard - Prevents unauthenticated access to citizen app features.
 * Checks for the presence of citizen_jwt in localStorage.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(private readonly router: Router) {}

  canActivate(): boolean | UrlTree {
    const token = localStorage.getItem('citizen_jwt');
    if (token) {
      return true;
    }
    // Redirect to login if token is missing
    return this.router.parseUrl('/login');
  }
}
