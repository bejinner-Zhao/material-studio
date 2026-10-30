FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
# 有锁文件时用 npm ci 保证可复现；尚未生成锁文件时回退，避免构建直接失败
RUN if [ -f package-lock.json ]; then npm ci; else npm install --no-audit --no-fund; fi

FROM node:22-alpine AS build
WORKDIR /app
# rewrites 的目标地址在构建时写入产物，因此必须通过构建参数传入
ARG BACKEND_ORIGIN=http://backend:8080
ENV BACKEND_ORIGIN=${BACKEND_ORIGIN}
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:22-alpine AS run
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
EXPOSE 3000
CMD ["node", "server.js"]
