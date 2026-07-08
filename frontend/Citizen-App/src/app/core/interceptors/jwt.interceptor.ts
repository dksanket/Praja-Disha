import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * JwtInterceptor — attaches the stored JWT token to every outgoing HTTP request.
 * The token is saved to localStorage after a successful login or register call.
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = localStorage.getItem('citizen_jwt');
    if (token) {
      // Clone the request and attach the Authorization header
      request = request.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
    return next.handle(request);
  }
}
