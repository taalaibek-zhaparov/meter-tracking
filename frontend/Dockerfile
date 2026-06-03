# Этап 1: сборка Angular
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install --legacy-peer-deps
COPY . .
RUN npm run build -- --configuration production

# Этап 2: nginx отдаёт статику
FROM nginx:alpine
COPY --from=builder /app/dist/meter-tracking-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80 443
