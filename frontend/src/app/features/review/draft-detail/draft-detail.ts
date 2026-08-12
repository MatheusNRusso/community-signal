import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ReplacePipe } from '../../../shared/replace.pipe';
import { DraftService } from '../../../core/services/draft';
import { AuthService } from '../../../core/services/auth.service';
import { Draft } from '../../../core/models/draft.model';

@Component({
  selector: 'app-draft-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ReplacePipe],
  templateUrl: './draft-detail.html',
  styleUrl: './draft-detail.scss',
})
export class DraftDetailComponent implements OnInit {
  draft!: Draft;
  loading = false;
  submitting = false;
  note = '';
  reviewerId = '';
  showConfirmModal = false;
  pendingAction: 'approve' | 'reject' | null = null;

  readonly RING_CIRC = 2 * Math.PI * 54;

  constructor(
    private draftService: DraftService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.reviewerId = this.authService.getUser()?.username ?? '';
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.loading = true;
    this.draftService.getDraft(id).subscribe({
      next: d => {
        if (!d) { this.router.navigate(['/review']); return; }
        this.draft = d;
        this.reviewerId = d.reviewerId ?? this.authService.getUser()?.username ?? '';
        this.note = d.reviewerNote ?? '';
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.loading = false; this.router.navigate(['/review']); }
    });
  }

  get ringDash(): string {
    const filled = ((this.draft?.guardrailScore ?? 0) / 100) * this.RING_CIRC;
    return `${filled} ${this.RING_CIRC}`;
  }

  get guardrailColor(): string {
    const s = this.draft?.guardrailScore;
    if (!s) return '#9e9e9e';
    return s >= 80 ? '#2e7d32' : s >= 60 ? '#f57f17' : '#c62828';
  }

  channelIcon(channel: string): string {
    return ({ LINKEDIN: '💼', TWITTER: '🐦', NEWSLETTER: '📧' } as any)[channel] ?? '📄';
  }

  scoreColor(score: number): string {
    if (score >= 80) return '#2e7d32';
    if (score >= 60) return '#f57f17';
    return '#c62828';
  }

  startReview(): void {
    this.submitting = true;
    this.draftService.startReview(this.draft.id, this.reviewerId).subscribe({
      next: updated => {
        this.draft = updated;
        this.submitting = false;
        this.cdr.detectChanges();
      }
    });
  }

  confirmAction(action: 'approve' | 'reject'): void {
    if (action === 'reject' && !this.note.trim()) return;
    this.pendingAction = action;
    this.showConfirmModal = true;
  }

  cancelAction(): void {
    this.showConfirmModal = false;
    this.pendingAction = null;
  }

  executeAction(): void {
    if (!this.pendingAction) return;
    this.submitting = true;
    const call = this.pendingAction === 'approve'
      ? this.draftService.approve(this.draft.id, { reviewerId: this.reviewerId, note: this.note })
      : this.draftService.reject(this.draft.id, { reviewerId: this.reviewerId, note: this.note });
    call.subscribe(() => {
      this.submitting = false;
      this.showConfirmModal = false;
      this.pendingAction = null;
      this.router.navigate(['/review']);
    });
  }
}
