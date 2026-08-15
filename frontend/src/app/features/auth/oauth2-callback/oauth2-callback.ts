import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-oauth2-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="oauth-callback">
      <div class="spinner-large"></div>
      <p>{{ message }}</p>
    </div>
  `,
  styles: [`
    .oauth-callback {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: #0a0a0a;
      color: #fff;
    }
    .spinner-large {
      width: 48px;
      height: 48px;
      border: 4px solid rgba(255,255,255,0.1);
      border-top-color: #00d4ff;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-bottom: 1rem;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class OAuth2CallbackComponent implements OnInit {
  message = 'Completing sign in...';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Extract token from URL fragment (#token=...)
    const hash = window.location.hash;
    if (!hash || !hash.includes('token=')) {
      this.message = 'Authentication failed. Redirecting...';
      setTimeout(() => this.router.navigate(['/login']), 2000);
      return;
    }

    const tokenMatch = hash.match(/token=([^&]+)/);
    if (!tokenMatch) {
      this.message = 'Invalid token. Redirecting...';
      setTimeout(() => this.router.navigate(['/login']), 2000);
      return;
    }

    const token = tokenMatch[1];
    this.authService.handleOAuthCallback(token);
    
    // Clear URL fragment for security
    window.history.replaceState(null, '', window.location.pathname);
    
    this.message = 'Sign in successful! Redirecting...';
    setTimeout(() => this.router.navigate(['/review']), 500);
  }
}
