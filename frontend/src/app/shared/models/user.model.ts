export interface User {
  id: number;
  email: string;
  username: string;
  roles: Role[];
  region: string;  // обязательное — без ?
}

export interface Role {
  id: number;
  name: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  username: string;
  role: string;
  region: string;  // обязательное — без ?
}

export interface LoginResponse {
  token: string;
}