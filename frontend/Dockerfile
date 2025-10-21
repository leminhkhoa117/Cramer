# Multi-stage Dockerfile for frontend (Vite + React)
FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json* ./
COPY src ./src
COPY index.html ./
COPY vite.config.js ./

RUN npm ci
RUN npm run build

FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 5173
CMD ["nginx", "-g", "daemon off;"]
