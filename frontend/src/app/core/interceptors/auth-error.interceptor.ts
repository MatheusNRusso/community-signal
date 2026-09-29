import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if ((err.status === 401 || err.status === 403) && req.url.includes('/api/drafts')) {
        auth.clearInvalidToken();
        router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
      }
      return throwError(() => err);
    })
  );
};
