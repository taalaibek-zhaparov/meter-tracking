import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Plan, CompletedTask, CompletedTaskRequest, UpdateCompletedTaskRequest, ChangeHistory } from '../models/plan.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/user`;

  constructor(private http: HttpClient) {}

  getMyTasks(): Observable<Plan[]> {
    return this.http.get<Plan[]>(`${this.apiUrl}/my-tasks`);
  }

  // Новый метод: получить ВСЕ планы (выполненные и невыполненные)
  getMyAllPlans(): Observable<Plan[]> {
    return this.http.get<Plan[]>(`${this.apiUrl}/my-all-plans`);
  }

  completeTask(request: CompletedTaskRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/complete-task`, request);
  }
  
  updateTask(request: UpdateCompletedTaskRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/update-task`, request);
  }

  getMyCompletedTasks(): Observable<CompletedTask[]> {
    return this.http.get<CompletedTask[]>(`${this.apiUrl}/my-completed-tasks`);
  }
  
  getTaskHistory(completedTaskId: number): Observable<ChangeHistory[]> {
    return this.http.get<ChangeHistory[]>(`${this.apiUrl}/task-history/${completedTaskId}`);
  }

  searchMeters(query: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/meters/search`, { params: { query } });
  }

  getMeterByNumber(meterNumber: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/meters/${meterNumber}`);
  }

  exportMyPlans(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/my-plans`, { responseType: 'blob' });
  }

  exportMyCompletedTasks(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/my-completed-tasks`, { responseType: 'blob' });
  }
  getCurrentUsername(): string {
  const token = localStorage.getItem('token');
  if (!token) return '';
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64).split('').map(c =>
        '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
      ).join('')
    );
    const payload = JSON.parse(json);
    return payload.username || payload.sub || '';
  } catch { return ''; }
}
  markMeterBusy(meterNumber: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/meters/${meterNumber}/mark-busy`, {});
  }

}
