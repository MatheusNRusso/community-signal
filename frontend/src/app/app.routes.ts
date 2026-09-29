import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'review', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login/login').then(m => m.LoginComponent) },
  { path: 'oauth2/callback', loadComponent: () => import('./features/auth/oauth2-callback/oauth2-callback').then(m => m.OAuth2CallbackComponent) },
  // Public read: anyone can view the queue and details
  { path: 'review', loadComponent: () => import('./features/review/draft-list/draft-list').then(m => m.DraftListComponent) },
  { path: 'review/:id', loadComponent: () => import('./features/review/draft-detail/draft-detail').then(m => m.DraftDetailComponent) },
];
