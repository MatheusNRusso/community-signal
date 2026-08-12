export type DraftStatus = 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED' | 'REVISED' | 'PUBLISHED';
export type Channel = 'LINKEDIN' | 'TWITTER' | 'NEWSLETTER';

export interface Draft {
  id: string;
  windowId: string;
  clusterId: string;
  channel: Channel;
  content: string;
  status: DraftStatus;
  guardrailScore?: number;
  guardrailPassed?: boolean;
  guardrailReasons?: string[];
  llmModel?: string;
  reviewerId?: string;
  reviewerNote?: string;
  reviewedAt?: string;
  generatedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DraftPage {
  content: Draft[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  page?: {
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  };
}

export interface ReviewRequest {
  reviewerId: string;
  note?: string;
}

export interface DraftStats {
  pending: number;
  in_review: number;
  approved: number;
  rejected: number;
}
