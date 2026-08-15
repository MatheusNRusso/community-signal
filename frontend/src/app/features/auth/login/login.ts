import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;
  error = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  onSubmit(): void {
    if (!this.username.trim() || !this.password.trim()) {
      this.error = 'Please enter both your username and password.';
      return;
    }
    this.loading = true;
    this.error = '';

    this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/review']);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.error = err.status === 401
          ? 'Invalid username or password.'
          : 'Login failed. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  loginWithGithub(): void {
    // Redirect to backend OAuth2 authorization endpoint
    // The backend will redirect to GitHub, then back to /login/oauth2/code/github,
    // then redirect to frontend /oauth2/callback#token=...
    window.location.href = `${environment.apiUrl}/oauth2/authorization/github`;
  }
}
