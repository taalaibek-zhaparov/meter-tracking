import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

// Auth
import { LoginComponent } from './auth/components/login.component';

// User
import { DashboardComponent } from './user/components/dashboard.component';
import { MyTasksComponent } from './user/components/my-tasks.component';
import { CompletedTasksComponent } from './user/components/completed-tasks.component';

// Admin
import { AdminDashboardComponent } from './admin/components/admin-dashboard.component';
import { MeterDevicesComponent } from './admin/components/meter-devices.component';
import { MeterCatalogComponent } from './admin/components/meter-catalog.component';
import { ReportsComponent } from './admin/components/reports.component';
import { AdminResDashboardComponent } from './admin-res/components/admin-res-dashboard.component';

// Shared
import { SignaturePadComponent } from './shared/components/signature-pad.component';

// Guards & Interceptors
import { AuthGuard } from './shared/guards/auth.guard';
import { JwtInterceptor } from './shared/guards/jwt.interceptor';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    DashboardComponent,
    MyTasksComponent,
    CompletedTasksComponent,
    AdminDashboardComponent,
    MeterDevicesComponent,
    MeterCatalogComponent,
    ReportsComponent,
    AdminResDashboardComponent,
    SignaturePadComponent
  ],
  imports: [
    BrowserModule,
    CommonModule,
    FormsModule,
    AppRoutingModule,
    RouterModule,
    HttpClientModule
  ],
  providers: [
    AuthGuard,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: JwtInterceptor,
      multi: true
    },
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
