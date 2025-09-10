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

# 빌드 시 ARG로 API URL을 받아서 환경변수로 설정
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

RUN npm run build

EXPOSE 3000

CMD ["npm", "start"]