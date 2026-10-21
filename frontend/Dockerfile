# Build stage: install the exact versions from package-lock.json and build the static files.
FROM node:25-alpine AS build
WORKDIR /app

# Dependencies are copied first so they stay cached until the lockfile changes.
# --ignore-scripts: no package may run its own install script during the build.
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts

COPY . .
RUN npm run build

# Run stage: only nginx and the built files, running as a non-root user.
FROM nginxinc/nginx-unprivileged:1.30-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
