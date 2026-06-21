FROM node:22-alpine AS build
WORKDIR /workspace
COPY app-web/package*.json ./
RUN npm ci
COPY app-web/ .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
COPY deploy/nginx/app.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
