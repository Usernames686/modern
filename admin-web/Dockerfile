FROM node:22-alpine AS build
WORKDIR /workspace
COPY admin-web/package*.json ./
RUN npm ci
COPY admin-web/ .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
COPY deploy/nginx/admin.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
