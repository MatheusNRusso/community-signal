import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DraftStatus } from '../../../core/models/draft.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="badge" [ngClass]="statusClass">{{ status }}</span>
  `,
  styles: [`
    .badge {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .pending    { background: #fff3e0; color: #e65100; }
    .in-review  { background: #e3f2fd; color: #1565c0; }
    .approved   { background: #e8f5e9; color: #2e7d32; }
    .rejected   { background: #ffebee; color: #c62828; }
    .revised    { background: #f3e5f5; color: #6a1b9a; }
    .published  { background: #e0f2f1; color: #00695c; }
  `]
})
export class StatusBadgeComponent {
  @Input() status!: DraftStatus;
  get statusClass(): string {
    return this.status?.toLowerCase().replace('_', '-') ?? '';
  }
}
