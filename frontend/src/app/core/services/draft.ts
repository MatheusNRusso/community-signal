import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Draft, DraftPage, DraftStats, ReviewRequest } from '../models/draft.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DraftService {
  private readonly base = `${environment.apiUrl}/api/drafts`;

  constructor(private http: HttpClient) {}

  getDrafts(status = 'PENDING', page = 0, size = 20): Observable<DraftPage> {
    const params = new HttpParams()
      .set('status', status)
      .set('page', page)
      .set('size', size);
    return this.http.get<DraftPage>(this.base, { params });
  }

  getDraft(id: string): Observable<Draft> {
    return this.http.get<Draft>(`${this.base}/${id}`);
  }

  startReview(id: string, reviewerId: string): Observable<Draft> {
    return this.http.post<Draft>(`${this.base}/${id}/review`, { reviewerId });
  }

  approve(id: string, req: ReviewRequest): Observable<Draft> {
    return this.http.post<Draft>(`${this.base}/${id}/approve`, req);
  }

  reject(id: string, req: ReviewRequest): Observable<Draft> {
    return this.http.post<Draft>(`${this.base}/${id}/reject`, req);
  }

  getStats(): Observable<DraftStats> {
    return this.http.get<DraftStats>(`${this.base}/stats`);
  }
}
