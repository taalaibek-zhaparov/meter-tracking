import { Component, OnInit, OnDestroy } from '@angular/core';
import { WebSocketService } from './shared/services/websocket.service';
import { AuthService } from './shared/services/auth.service';

@Component({
  standalone: false,
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Meter Tracking System';

  constructor(
    private webSocketService: WebSocketService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Подключаем WebSocket если пользователь залогинен
    if (this.authService.isLoggedIn()) {
      console.log('Connecting WebSocket...');
      this.webSocketService.connect();
    }

    // Подписываемся на изменения токена
    this.authService.token$.subscribe(token => {
      if (token && !this.webSocketService.isConnected()) {
        console.log('User logged in, connecting WebSocket...');
        this.webSocketService.connect();
      } else if (!token && this.webSocketService.isConnected()) {
        console.log('User logged out, disconnecting WebSocket...');
        this.webSocketService.disconnect();
      }
    });
  }

  ngOnDestroy(): void {
    this.webSocketService.disconnect();
  }
}
