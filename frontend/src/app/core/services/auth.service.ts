import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LoginRequest { username: string; password: string; }
export interface TokenResponse { token: string; username: string; role: string; expiresIn: number; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly base = `${environment.apiUrl}/api/auth`;
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY  = 'auth_user';
  private loggedIn$ = new BehaviorSubject<boolean>(this.isTokenValid());

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.base}/login`, request).pipe(
      tap(res => this.storeToken(res.token, res.username, res.role))
    );
  }

  handleOAuthCallback(token: string): void {
    const payload = this.decodeJwt(token);
    const username = payload?.sub || 'github-user';
    const roles = payload?.roles || [];
    const role = Array.isArray(roles) && roles.length ? roles[0] : 'ROLE_REVIEWER';
    this.storeToken(token, username, role);
  }

  isTokenValid(): boolean {
    const token = this.getToken();
    if (!token) return false;
    const payload = this.decodeJwt(token);
    if (!payload?.exp) return false;
    return payload.exp * 1000 > Date.now();
  }

  clearInvalidToken(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.loggedIn$.next(false);
  }

  private storeToken(token: string, username: string, role: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.USER_KEY, JSON.stringify({ username, role }));
    this.loggedIn$.next(true);
  }

  private decodeJwt(token: string): any {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(decodeURIComponent(
        atob(base64).split('').map(c =>
          '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        ).join('')
      ));
    } catch { return null; }
  }

  logout(): void {
    this.clearInvalidToken();
    this.router.navigate(['/login']);
  }

  getToken(): string | null { return localStorage.getItem(this.TOKEN_KEY); }
  getUser(): { username: string; role: string } | null {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
  isLoggedIn(): Observable<boolean> { return this.loggedIn$.asObservable(); }
  isAdmin(): boolean { return this.getUser()?.role === 'ROLE_ADMIN'; }
}
