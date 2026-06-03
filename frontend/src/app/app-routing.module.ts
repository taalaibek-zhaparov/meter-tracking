import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/components/login.component';
import { DashboardComponent } from './user/components/dashboard.component';
import { AdminDashboardComponent } from './admin/components/admin-dashboard.component';
import { MeterDevicesComponent } from './admin/components/meter-devices.component';
import { MeterCatalogComponent } from './admin/components/meter-catalog.component';
import { ReportsComponent } from './admin/components/reports.component';
import { AdminResDashboardComponent } from './admin-res/components/admin-res-dashboard.component';
import { AuthGuard } from './shared/guards/auth.guard';

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'admin', component: AdminDashboardComponent, canActivate: [AuthGuard] },
  { path: 'admin/meters', component: MeterDevicesComponent, canActivate: [AuthGuard] },
  { path: 'admin/catalog', component: MeterCatalogComponent, canActivate: [AuthGuard] },
  { path: 'admin/reports', component: ReportsComponent, canActivate: [AuthGuard] },
  { path: 'admin-res', component: AdminResDashboardComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
