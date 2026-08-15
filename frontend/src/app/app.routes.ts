import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './core/services/auth.service';

const authGuard = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  return auth.getToken() ? true : router.navigate(['/login']);
};

export const routes: Routes = [
  { path: '', redirectTo: 'review', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then(m => m.LoginComponent)
  },
  {
    path: 'oauth2/callback',
    loadComponent: () => import('./features/auth/oauth2-callback/oauth2-callback').then(m => m.OAuth2CallbackComponent)
  },
  {
    path: 'review',
    canActivate: [authGuard],
    loadComponent: () => import('./features/review/draft-list/draft-list').then(m => m.DraftListComponent)
  },
  {
    path: 'review/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/review/draft-detail/draft-detail').then(m => m.DraftDetailComponent)
  }
];
