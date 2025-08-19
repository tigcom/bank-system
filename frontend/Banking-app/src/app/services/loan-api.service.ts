import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
}

export interface LoanRequest {
  loanId?: number;
  disbursementAccountNumber: string;
  repaymentAccountNumber: string;
  amount: number;
  interestRate: number;
  termMonths: number;
  status?: string;
  loanType?: string;
  customerId?: number;
  approvedAt?: string;
  createdAt?: string;
  paidAmount?: number;
}

@Injectable({ providedIn: 'root' })
export class LoanApiService {
  private token$ = new BehaviorSubject<string | null>(null);

  constructor(private http: HttpClient) {
    const saved = localStorage.getItem('auth_token');
    if (saved) this.token$.next(saved);
  }

  setToken(token: string) {
    this.token$.next(token);
    localStorage.setItem('auth_token', token);
  }

  clearToken() {
    this.token$.next(null);
    localStorage.removeItem('auth_token');
  }

  private authHeaders(): HttpHeaders {
    const t = this.token$.value;
    let headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    if (t) headers = headers.set('Authorization', `Bearer ${t}`);
    return headers;
  }

  createLoan(body: LoanRequest): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>('/api/loans', body, { headers: this.authHeaders() });
  }

  updateLoan(body: LoanRequest): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>('/api/loans', body, { headers: this.authHeaders() });
  }

  approveLoan(loanId: number): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`/api/loans/${loanId}/approve`, {}, { headers: this.authHeaders() });
  }

  // Actuator metrics/health
  getCircuitBreakerHealth(): Observable<any> {
    return this.http.get('/actuator/health/circuitbreakers', { headers: this.authHeaders() });
  }
  getCircuitBreakerMetrics(): Observable<any> {
    return this.http.get('/actuator/metrics/resilience4j.circuitbreaker.calls', { headers: this.authHeaders() });
  }
  getRetryMetrics(): Observable<any> {
    return this.http.get('/actuator/metrics/resilience4j.retry.calls', { headers: this.authHeaders() });
  }
  getRateLimiterMetrics(): Observable<any> {
    return this.http.get('/actuator/metrics/resilience4j.ratelimiter.available.permissions', { headers: this.authHeaders() });
  }
  getBulkheadMetrics(): Observable<any> {
    return this.http.get('/actuator/metrics/resilience4j.bulkhead.available.concurrent.calls', { headers: this.authHeaders() });
  }

  // New test endpoints
  testUnstable(fail: boolean, delay: number): Observable<ApiResponse<string>> {
    return this.http.get<ApiResponse<string>>(`/api/resilience-test/unstable?fail=${fail}&delay=${delay}`, { headers: this.authHeaders() });
  }
  testRateLimiter(note: string): Observable<ApiResponse<string>> {
    return this.http.get<ApiResponse<string>>(`/api/resilience-test/ratelimiter?note=${encodeURIComponent(note)}`, { headers: this.authHeaders() });
  }
  testBulkhead(delay: number): Observable<ApiResponse<string>> {
    return this.http.get<ApiResponse<string>>(`/api/resilience-test/bulkhead?delay=${delay}`, { headers: this.authHeaders() });
  }
  testTimeLimiter(delay: number): Observable<ApiResponse<string>> {
    return this.http.get<ApiResponse<string>>(`/api/resilience-test/timelimiter?delay=${delay}`, { headers: this.authHeaders() });
  }
} 