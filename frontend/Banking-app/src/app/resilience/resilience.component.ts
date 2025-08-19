import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LoanApiService, LoanRequest } from '../services/loan-api.service';

@Component({
  selector: 'app-resilience',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './resilience.component.html',
  styleUrl: './resilience.component.scss'
})
export class ResilienceComponent {
  token = '';

  createPayload: LoanRequest = {
    disbursementAccountNumber: '100000001',
    repaymentAccountNumber: '100000002',
    amount: 1000000,
    interestRate: 6.5,
    termMonths: 12,
    loanType: 'HOME',
    status: 'PENDING'
  };

  updatePayload: LoanRequest = {
    loanId: 1,
    disbursementAccountNumber: '100000001',
    repaymentAccountNumber: '100000002',
    amount: 1200000,
    interestRate: 6.2,
    termMonths: 12,
    loanType: 'HOME',
    status: 'APPROVED'
  };

  approveLoanId = 1;

  unstableFail = true;
  unstableDelay = 0;
  rateNote = 'ping';
  bulkheadDelay = 10000;
  timeDelay = 35000;
  spamCount = 20;

  lastResponse: any = null;
  metrics: { [k: string]: any } = {};
  loading = false;
  error: string | null = null;

  spamResults: { ok: number, fail: number } = { ok: 0, fail: 0 };

  constructor(private api: LoanApiService) {}

  saveToken() {
    this.api.setToken(this.token.trim());
  }
  clearToken() {
    this.token = '';
    this.api.clearToken();
  }

  callCreateLoan() { this.exec(this.api.createLoan(this.createPayload)); }
  callUpdateLoan() { this.exec(this.api.updateLoan(this.updatePayload)); }
  callApproveLoan() { this.exec(this.api.approveLoan(this.approveLoanId)); }

  // New test calls
  callUnstable() { this.exec(this.api.testUnstable(this.unstableFail, this.unstableDelay)); }
  callRateLimiter() { this.exec(this.api.testRateLimiter(this.rateNote)); }
  callBulkhead() { this.exec(this.api.testBulkhead(this.bulkheadDelay)); }
  callTimeLimiter() { this.exec(this.api.testTimeLimiter(this.timeDelay)); }

  spamRateLimiter() {
    this.spamResults = { ok: 0, fail: 0 };
    for (let i = 0; i < this.spamCount; i++) {
      this.api.testRateLimiter(this.rateNote + '_' + i).subscribe({
        next: _ => this.spamResults.ok++,
        error: _ => this.spamResults.fail++
      });
    }
  }

  spamBulkhead() {
    this.spamResults = { ok: 0, fail: 0 };
    for (let i = 0; i < this.spamCount; i++) {
      this.api.testBulkhead(this.bulkheadDelay).subscribe({
        next: _ => this.spamResults.ok++,
        error: _ => this.spamResults.fail++
      });
    }
  }

  refreshMetrics() {
    this.api.getCircuitBreakerHealth().subscribe(v => this.metrics['cbHealth'] = v);
    this.api.getCircuitBreakerMetrics().subscribe(v => this.metrics['cbMetrics'] = v);
    this.api.getRetryMetrics().subscribe(v => this.metrics['retry'] = v);
    this.api.getRateLimiterMetrics().subscribe(v => this.metrics['rateLimiter'] = v);
    this.api.getBulkheadMetrics().subscribe(v => this.metrics['bulkhead'] = v);
  }

  private exec(obs: any) {
    this.loading = true;
    this.error = null;
    this.lastResponse = null;
    obs.subscribe({
      next: (res: any) => { this.lastResponse = res; this.loading = false; },
      error: (err: any) => { this.error = (err?.error?.message || err?.message || 'Request failed'); this.loading = false; }
    });
  }
} 