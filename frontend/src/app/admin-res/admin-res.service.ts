import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AdminResService {
  private api = `${environment.apiUrl}/admin-res`;

  constructor(private http: HttpClient) {}

  getMe(): Observable<any> {
    return this.http.get(`${this.api}/me`);
  }

  getPlans(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/plans`);
  }

  createPlan(request: any): Observable<any> {
    return this.http.post(`${this.api}/plans`, request);
  }

  deletePlan(planId: number): Observable<any> {
    return this.http.delete(`${this.api}/plans/${planId}`);
  }

  getCompletedTasks(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/completed-tasks`);
  }

  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/users`);
  }

  // 1С данные (через общий admin эндпоинт — доступен ADMIN_RES тоже)
  getMeterFromOnec(licevoy: string): Observable<any> {
    // Используем собственный эндпоинт admin-res — не требует роли ADMIN
    return this.http.get(`${this.api}/onec/meter/${licevoy}`);
  }
}
