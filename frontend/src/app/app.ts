import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule],
  template: `
    <nav class="navbar">
      <div class="brand">📡 Community Signal</div>
      <div class="nav-links">
        <a routerLink="/review" routerLinkActive="active">Draft Review</a>
      </div>
    </nav>
    <router-outlet/>
  `,
  styles: [`
    .navbar {
      background: #1976d2; color: white; padding: 0 24px;
      height: 56px; display: flex; align-items: center; justify-content: space-between;
      box-shadow: 0 2px 4px rgba(0,0,0,.2);
    }
    .brand { font-size: 18px; font-weight: 700; }
    .nav-links a {
      color: rgba(255,255,255,.85); text-decoration: none; padding: 8px 12px;
      border-radius: 4px; font-size: 14px;
      &.active, &:hover { color: white; background: rgba(255,255,255,.15); }
    }
  `]
})
export class AppComponent {}
