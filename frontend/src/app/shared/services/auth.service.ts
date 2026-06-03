import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, RegisterRequest } from '../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = environment.apiUrl;
  private tokenSubject = new BehaviorSubject<string | null>(this.getToken());
  public token$ = this.tokenSubject.asObservable();

  constructor(private http: HttpClient) {}

  register(request: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, request);
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, request).pipe(
      tap(response => {
        this.setToken(response.token);
        this.tokenSubject.next(response.token);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    this.tokenSubject.next(null);
  }

  getToken(): string | null { return localStorage.getItem('token'); }

  private setToken(token: string): void {
    localStorage.setItem('token', token);
    this.tokenSubject.next(token);
  }

  private decodeToken(): any {
    const token = this.getToken();
    if (!token) return null;
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const json = decodeURIComponent(
        atob(base64).split('').map(c =>
          '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        ).join('')
      );
      return JSON.parse(json);
    } catch { return null; }
  }

  isLoggedIn(): boolean {
    const payload = this.decodeToken();
    if (!payload) return false;
    if (Date.now() >= payload.exp * 1000) { this.logout(); return false; }
    return true;
  }

  getRoles(): string[] {
    const payload = this.decodeToken();
    return payload?.roles || [];
  }

  getUserRole(): string | null {
    const roles = this.getRoles();
    if (roles.some((r: string) => r === 'ROLE_ADMIN'     || r === 'ADMIN'))     return 'ADMIN';
    if (roles.some((r: string) => r === 'ROLE_ADMIN_RES' || r === 'ADMIN_RES')) return 'ADMIN_RES';
    if (roles.some((r: string) => r === 'ROLE_USER'      || r === 'USER'))      return 'USER';
    return null;
  }

  getCurrentUsername(): string {
    const payload = this.decodeToken();
    return payload?.username || payload?.sub || '';
  }

  isAdmin():    boolean { return this.getUserRole() === 'ADMIN'; }
  isAdminRes(): boolean { return this.getUserRole() === 'ADMIN_RES'; }
  isUser():     boolean { return this.getUserRole() === 'USER'; }
}
