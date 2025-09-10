FROM node:18-alpine

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Next.js 빌드를 위한 환경 변수
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_OPTIONS="--max-old-space-size=2048"

COPY package*.json ./

# 개발 의존성도 설치 (빌드에 필요)
RUN npm ci

COPY . .

# 환경에 따른 .env 파일 복사
ARG ENV=production
COPY .env.${ENV} .env

RUN npm run build

EXPOSE 3000

CMD ["npm", "start"]