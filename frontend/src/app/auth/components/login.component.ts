import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../shared/services/auth.service';
import { LoginRequest } from '../../shared/models/user.model';

@Component({
  standalone: false,
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  loginData: LoginRequest = { email: '', password: '' };
  errorMessage = '';
  loading = false;
  showPassword = false;

  constructor(private authService: AuthService, private router: Router) {}

  togglePasswordVisibility(): void { this.showPassword = !this.showPassword; }

  onSubmit(): void {
    this.errorMessage = '';
    this.loading = true;

    this.authService.login(this.loginData).subscribe({
      next: () => {
        this.loading = false;
        const role = this.authService.getUserRole();
        if      (role === 'ADMIN')     this.router.navigate(['/admin']);
        else if (role === 'ADMIN_RES') this.router.navigate(['/admin-res']);
        else                           this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.error || 'Ошибка входа. Проверьте email и пароль.';
      }
    });
  }
}
