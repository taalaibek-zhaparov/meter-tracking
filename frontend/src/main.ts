/**
 * Полифилы ДОЛЖНЫ быть здесь — ДО любых импортов.
 * sockjs-client и @stomp/stompjs написаны под Node.js
 * и используют `global`, `process`, которых нет в браузере.
 */

// Node.js `global` → window
if (typeof (window as any).global === 'undefined') {
  (window as any).global = window;
}

// Node.js `process`
if (typeof (window as any).process === 'undefined') {
  (window as any).process = {
    env: { DEBUG: undefined },
    version: '',
    versions: {},
    browser: true,
    nextTick: (fn: Function, ...args: any[]) => setTimeout(() => fn(...args), 0)
  };
}

// `globalThis` (на случай старых браузеров)
if (typeof globalThis === 'undefined') {
  (window as any).globalThis = window;
}

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';

platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => console.error(err));
