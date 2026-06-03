export interface Plan {
  id: number;
  user: { id: number; username: string; email: string; } | null;
  region: string; tp: string; licevoy: string; tip: string;
  nomerSchetchika: string; pokazaniya: number; data: string; completed: boolean;
  // НОВЫЕ ПОЛЯ из 1С HTTP
  fio?: string;
  adres?: string;
  documentType?: string;
}

export interface PlanRequest {
  userId: number; region: string; tp: string; licevoy: string; tip: string;
  nomerSchetchika: string; pokazaniya: number; data: string;
  // НОВЫЕ ПОЛЯ
  fio?: string;
  adres?: string;
   documentType?: string;
}

export interface CompletedTask {
  id: number;
  plan: Plan;
  user: { id: number; username: string; email: string; } | null;
  region: string; tp: string; licevoy: string; tip: string;
  nomerSchetchika: string; pokazaniya: number; data: string;
  nomerPlomby: string; nomerSimKarty: string; nomerIccid: string;
  oldNomerSchetchika?: string; oldPokazaniya?: number; newPokazaniya?: number;
  plombaGos?: string; naKryshke?: string; naYashike?: string;
  phases?: number | null;      // ← было newPhases
  amperage?: number | null;    // ← было newAmperage
  oldMeterType?: string; newMeterType?: string;
  fio?: string;
  adres?: string;
  createdAt: string; updatedAt: string; updatedBy: number;
  documentNumber?: string;
  znch?: number | null;
  signatureAbonent?: string;
  signatureMaster?: string;
  documentType?: string;
}

export interface CompletedTaskRequest {
  planId: number; region: string; tp: string; licevoy: string; tip: string;
  nomerSchetchika: string; pokazaniya: number; data: string;
  nomerPlomby: string; nomerSimKarty: string; nomerIccid: string;
  oldNomerSchetchika: string; oldPokazaniya: number; newPokazaniya: number;
  plombaGos: string; naKryshke: string; naYashike: string;
  newPhases?: number | null;
  newAmperage?: number | null;
  // НОВЫЕ ПОЛЯ
  fio?: string;
  adres?: string;
  znch?: number | null;
  documentType?: string;
}

export interface UpdateCompletedTaskRequest {
  completedTaskId: number; region: string; tp: string; licevoy: string; tip: string;
  nomerSchetchika: string; pokazaniya: number; data: string;
  nomerPlomby: string; nomerSimKarty: string; nomerIccid: string;
  naKryshke: string; naYashike: string; plombaGos: string;
}

export interface ChangeHistory {
  id: number; completedTask: CompletedTask;
  user: { id: number; username: string; email: string; } | null;
  userName: string; changeTime: string; fieldName: string;
  oldValue: string; newValue: string; actionType: string;
}

export interface OneCMeterReading {
  account: string; tp: string; region: string;
  meterNumber: string; meterType: string; reading: number; date: string;
  // НОВЫЕ ПОЛЯ
  fio?: string;
  adres?: string;
}

export interface MeterDevice {
  id: number; meterNumber: string; meterType: string;
  simCardNumber: string; iccidNumber: string; sealNumber: string;
  phases?: number | null;
  amperage?: number | null;
  available: boolean;
}

export interface User {
  id: number; email: string; username: string; roles: Role[]; region: string;
}
export interface Role { id: number; name: string; }
export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest {
  email: string; password: string; username: string; role: string; region: string;
}
export interface LoginResponse { token: string; }