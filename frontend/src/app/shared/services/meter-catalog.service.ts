import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MeterSpecDTO {
  id: number;
  amp: string;
  digits: number;
  phase: number;
  voltage: string;
}

export interface MeterModelDTO {
  id: number;
  code: string;
  name: string;
  specs: MeterSpecDTO[];
}

export interface MeterImportResponse {
  totalReceived: number;
  modelsCreated: number;
  modelsSkipped: number;
  specsCreated: number;
  specsSkipped: number;
  specsInvalid: number;
}

@Injectable({ providedIn: 'root' })
export class MeterCatalogService {

  private adminUrl = `${environment.apiUrl}/admin/catalog`;
  private userUrl  = `${environment.apiUrl}/user/catalog`;

  constructor(private http: HttpClient) {}

  // ── Импорт из 1С (только ADMIN) ──────────────────────────────────────

  importFromOnec(payload: { items: any[] }): Observable<MeterImportResponse> {
    return this.http.post<MeterImportResponse>(`${this.adminUrl}/import`, payload);
  }

  // ── Справочник (ADMIN) ───────────────────────────────────────────────

  getAllModels(): Observable<MeterModelDTO[]> {
    return this.http.get<MeterModelDTO[]>(`${this.adminUrl}/models`);
  }

  searchModelsAdmin(q: string): Observable<MeterModelDTO[]> {
    return this.http.get<MeterModelDTO[]>(`${this.adminUrl}/models/search`, { params: { q } });
  }

  getModelByCode(code: string): Observable<MeterModelDTO> {
    return this.http.get<MeterModelDTO>(`${this.adminUrl}/models/${code}`);
  }

  linkDeviceToModel(deviceId: number, modelCode: string, specId?: number): Observable<any> {
    const body: any = { modelCode };
    if (specId != null) body['specId'] = specId;
    return this.http.post(`${this.adminUrl}/devices/${deviceId}/link`, body);
  }

  // ── Справочник (USER/монтажник) ──────────────────────────────────────

  searchModels(q: string): Observable<MeterModelDTO[]> {
    return this.http.get<MeterModelDTO[]>(`${this.userUrl}/models/search`, { params: { q } });
  }
}
