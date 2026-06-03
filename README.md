# Meter Tracking

![CI](https://github.com/taalaibek-zhaparov/meter-tracking/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green?logo=springboot)
![Angular](https://img.shields.io/badge/Angular-17-red?logo=angular)
![Docker](https://img.shields.io/badge/Docker-ready-blue?logo=docker)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?logo=postgresql)


A system for managing and tracking electricity meter replacement processes for utility companies.

## Overview

Meter Tracking is designed to automate meter accounting, replacement planning, and work execution tracking.
It replaces manual spreadsheets and fragmented Excel files with a unified digital workflow that provides real-time status tracking, reporting, and integration with internal systems.

## What it solves

- Eliminates manual meter tracking in spreadsheets
- Simplifies replacement planning
- Provides real-time visibility of task execution
- Automates reporting (PDF / Excel)
- Reduces human error
- Improves transparency across the entire network

## Architecture
User
↓
Nginx (HTTPS)
↓
Angular SPA
↓
Spring Boot API
↓
PostgreSQL
Real-time: WebSocket
Monitoring: Prometheus → Grafana

## Tech Stack

### Backend
- Java 17 + Spring Boot 3
- PostgreSQL 17
- JWT authentication
- WebSocket
- Integration with 1C systems

### Frontend
- Angular 17 + TypeScript
- Angular Material
- WebSocket client

### Infrastructure
- Docker + Docker Compose
- Nginx (reverse proxy + HTTPS)
- Prometheus + Grafana
- CI/CD via GitHub Actions

## Features

- Authentication and role-based access (ADMIN, ADMIN_RES, USER)
- Meter replacement request planning
- Work execution tracking with digital signatures
- PDF / Excel report generation
- Real-time notifications via WebSocket
- Integration with 1C
- Meter directory with data import

## Quick Start

### Requirements
- Docker 24+
- Docker Compose v2+

### Run


git clone https://github.com/taalaibek-zhaparov/meter-tracking.git
cd meter-tracking
cp infra/.env.example infra/.env
# configure environment variables
cd infra
docker compose up -d


## Access

| Service | URL |
|---------|-----|
| Application | https://localhost |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9091 |

## DevOps Infrastructure

- Multi-stage Docker builds — backend 180MB, frontend 25MB
- Health checks — automatic service health monitoring
- Resource limits — protection against OOM
- Rate limiting — brute-force protection (5 req/min on /auth/login)
- HTTPS — SSL/TLS via Nginx with HTTP → HTTPS redirect
- Prometheus + Grafana — real-time JVM metrics monitoring
- CI/CD via GitHub Actions — automated build and deployment

## Author

Taalaibek Zhaparov 
[GitHub](https://github.com/taalaibek-zhaparov)
