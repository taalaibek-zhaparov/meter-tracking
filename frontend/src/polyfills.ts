/**
 * Полифилы для Angular 20 + sockjs-client + @stomp/stompjs.
 *
 * В Angular 17+ (esbuild) этот файл подключается как отдельный chunk.
 * Основные полифилы перенесены в main.ts (до импортов).
 */

// zone.js обязателен для Angular
import 'zone.js';
