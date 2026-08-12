import { forkJoin } from 'rxjs';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DraftService } from '../../../core/services/draft';
import { Draft, DraftStatus, DraftStats } from '../../../core/models/draft.model';
import { ReplacePipe } from '../../../shared/replace.pipe';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-draft-list',
  standalone: true,
  imports: [CommonModule, RouterModule, ReplacePipe],
  templateUrl: './draft-list.html',
  styleUrl: './draft-list.scss',
})
export class DraftListComponent implements OnInit {
  drafts: Draft[] = [];
  stats: DraftStats = { pending: 0, in_review: 0, approved: 0, rejected: 0 };
  selectedStatus: DraftStatus | 'ALL' = 'ALL';
  loading = false;
  error = '';
  page = 0;
  totalPages = 0;
  totalElements = 0;
  currentUser: { username: string; role: string } | null = null;

  get statusTabs(): { value: DraftStatus | 'ALL'; label: string; count: number }[] {
    const total = this.stats.pending + this.stats.in_review + this.stats.approved + this.stats.rejected;
    return [
      { value: 'ALL',       label: 'All',       count: total },
      { value: 'PENDING',   label: 'Pending',   count: this.stats.pending },
      { value: 'IN_REVIEW', label: 'In Review', count: this.stats.in_review },
      { value: 'APPROVED',  label: 'Approved',  count: this.stats.approved },
      { value: 'REJECTED',  label: 'Rejected',  count: this.stats.rejected },
    ];
  }

  constructor(private draftService: DraftService, private authService: AuthService, private cdr: ChangeDetectorRef) {
    this.currentUser = this.authService.getUser();
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadDrafts();
  }

  loadStats(): void {
    this.draftService.getStats().subscribe({
      next: s => { this.stats = s; this.cdr.detectChanges(); },
      error: err => console.error('stats error', err)
    });
  }

  loadDrafts(): void {
    this.loading = true;
    this.error = '';

    if (this.selectedStatus === 'ALL') {
      forkJoin({
        pending:  this.draftService.getDrafts('PENDING',   0, 100),
        inReview: this.draftService.getDrafts('IN_REVIEW', 0, 100),
        approved: this.draftService.getDrafts('APPROVED',  0, 100),
        rejected: this.draftService.getDrafts('REJECTED',  0, 100),
      }).subscribe({
        next: (res) => {
          this.drafts = [
            ...(res.pending.content  ?? []),
            ...(res.inReview.content ?? []),
            ...(res.approved.content ?? []),
            ...(res.rejected.content ?? []),
          ];
          this.totalElements = this.drafts.length;
          this.totalPages = 1;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: () => { this.loading = false; this.cdr.detectChanges(); }
      });
    } else {
      this.draftService.getDrafts(this.selectedStatus, this.page).subscribe({
        next: (p: any) => {
          this.drafts = p.content ?? [];
          this.totalElements = p.page?.totalElements ?? p.totalElements ?? 0;
          this.totalPages    = p.page?.totalPages    ?? p.totalPages    ?? 0;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: () => { this.loading = false; this.cdr.detectChanges(); }
      });
    }
  }

  onStatusChange(status: DraftStatus | 'ALL'): void {
    this.selectedStatus = status;
    this.page = 0;
    this.loadDrafts();
  }

  nextPage(): void { if (this.page < this.totalPages - 1) { this.page++; this.loadDrafts(); } }
  prevPage(): void { if (this.page > 0) { this.page--; this.loadDrafts(); } }

  channelIcon(channel: string): string {
    return ({ LINKEDIN: '💼', TWITTER: '🐦', NEWSLETTER: '📧' } as any)[channel] ?? '📄';
  }

  logout(): void { this.authService.logout(); }

  scoreColor(score: number): string {
    if (score >= 80) return '#2e7d32';
    if (score >= 60) return '#f57f17';
    return '#c62828';
  }
}
