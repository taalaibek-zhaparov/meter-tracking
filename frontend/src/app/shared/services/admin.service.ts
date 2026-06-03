import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Plan, PlanRequest, CompletedTask, UpdateCompletedTaskRequest, ChangeHistory } from '../models/plan.model';
import { User, RegisterRequest } from '../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  createPlan(request: PlanRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/plans`, request);
  }
  
  updatePlan(planId: number, request: PlanRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/plans/${planId}`, request);
  }
  
  deletePlan(planId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/plans/${planId}`);
  }

  getAllPlans(): Observable<Plan[]> {
    return this.http.get<Plan[]>(`${this.apiUrl}/plans`);
  }

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users`);
  }
  
  createUser(request: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/users`, request);
  }
  
  deleteUser(userId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/users/${userId}`);
  }

  getAllCompletedTasks(): Observable<CompletedTask[]> {
    return this.http.get<CompletedTask[]>(`${this.apiUrl}/completed-tasks`);
  }
  
  updateCompletedTask(taskId: number, request: UpdateCompletedTaskRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/completed-tasks/${taskId}`, request);
  }
  
  getAllHistory(): Observable<ChangeHistory[]> {
    return this.http.get<ChangeHistory[]>(`${this.apiUrl}/history`);
  }
  
  getTaskHistory(completedTaskId: number): Observable<ChangeHistory[]> {
    return this.http.get<ChangeHistory[]>(`${this.apiUrl}/history/${completedTaskId}`);
  }

  getMeterFromOnec(licevoy: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/onec/meter/${licevoy}`);
  }

  getOnecStatus(): Observable<any> {
    return this.http.get(`${this.apiUrl}/onec/status`);
  }

  getAllMeterDevices(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/meter-devices`);
  }

  createMeterDevice(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/meter-devices`, data);
  }

  updateMeterDevice(id: number, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/meter-devices/${id}`, data);
  }

  deleteMeterDevice(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/meter-devices/${id}`);
  }

  exportPlans(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/plans`, { responseType: 'blob' });
  }

  exportCompletedTasks(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/completed-tasks`, { responseType: 'blob' });
  }
 generateProtocol(data: any): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/protocol/generate`, data,
      { responseType: 'blob' });
  }

  updateUser(userId: number, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/${userId}`, data);
  }
  
  // ── ОТЧЁТЫ ───────────────────────────────────────────────────────
  getDashboard(): Observable<any> {
    return this.http.get(`${this.apiUrl}/reports/dashboard`);
  }
}
