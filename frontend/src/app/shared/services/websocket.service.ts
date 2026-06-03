import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Client, Message } from '@stomp/stompjs';
import { environment } from '../../../environments/environment';
import SockJS from 'sockjs-client';

export enum WebSocketEvent {
  PLAN_CREATED = 'PLAN_CREATED',
  PLAN_UPDATED = 'PLAN_UPDATED',
  PLAN_DELETED = 'PLAN_DELETED',
  TASK_COMPLETED = 'TASK_COMPLETED',
  TASK_UPDATED = 'TASK_UPDATED',
  USER_CREATED = 'USER_CREATED',
  USER_DELETED = 'USER_DELETED'
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client | null = null;
  private connected = false;

  public planUpdates$ = new Subject<WebSocketEvent>();
  public taskUpdates$ = new Subject<WebSocketEvent>();
  public userUpdates$ = new Subject<WebSocketEvent>();
  public connectionStatus$ = new Subject<boolean>();

  constructor() {}

  connect(): void {
    if (this.connected || this.stompClient?.active) {
      console.log('WebSocket already connected');
      return;
    }

    const self = this;

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(`${environment.apiUrl.replace('/api', '')}/ws`) as any,

      // Токен обновляется перед КАЖДЫМ переподключением (в т.ч. автоматическим)
      beforeConnect: async () => {
        const token = localStorage.getItem('token');
        if (self.stompClient) {
          self.stompClient.connectHeaders = token
            ? { Authorization: `Bearer ${token}` }
            : {};
        }
      },

      reconnectDelay: 10000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      debug: (str) => console.log('STOMP: ' + str)
    });

    this.stompClient.onConnect = (frame) => {
      console.log('WebSocket Connected:', frame);
      this.connected = true;
      this.connectionStatus$.next(true);

      this.stompClient?.subscribe('/topic/plans', (message: Message) => {
        this.planUpdates$.next(message.body as WebSocketEvent);
      });

      this.stompClient?.subscribe('/topic/tasks', (message: Message) => {
        this.taskUpdates$.next(message.body as WebSocketEvent);
      });

      this.stompClient?.subscribe('/topic/users', (message: Message) => {
        this.userUpdates$.next(message.body as WebSocketEvent);
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      this.connected = false;
      this.connectionStatus$.next(false);
    };

    // Убрано ручное переподключение — STOMP сам переподключается через reconnectDelay
    this.stompClient.onWebSocketClose = () => {
      console.log('WebSocket closed');
      this.connected = false;
      this.connectionStatus$.next(false);
    };

    this.stompClient.activate();
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      this.connected = false;
      this.connectionStatus$.next(false);
    }
  }

  isConnected(): boolean {
    return this.connected;
  }
}