# Vercel Frontend 배포 가이드

## 1. Vercel 프로젝트 생성

### 1.1 Vercel CLI 설치 (선택사항)
```bash
npm i -g vercel
```

### 1.2 Vercel 웹사이트에서 배포
1. https://vercel.com 접속 및 로그인
2. "Add New Project" 클릭
3. GitHub 저장소 연결 (techbloghub)
4. Root Directory: `frontend` 선택
5. Framework Preset: `Next.js` 자동 감지 확인

## 2. 환경변수 설정

Vercel 프로젝트 설정에서 다음 환경변수 추가:

### Production 환경변수
```
NEXT_PUBLIC_API_URL=https://api.teckbloghub.kr
NEXT_PUBLIC_GA_ID=G-4SVGK166NK
```

## 3. 도메인 설정

### 3.1 Vercel에서 도메인 추가
1. Vercel 프로젝트 > Settings > Domains
2. 다음 도메인 추가:
   - `teckbloghub.kr`
   - `www.teckbloghub.kr`

### 3.2 Vercel이 제공하는 DNS 레코드 확인
Vercel이 제공하는 CNAME 또는 A 레코드 값을 메모 (다음 단계에서 사용)

## 4. Route53 레코드 업데이트 필요

### 현재 설정 (AWS ALB 연결)
```
teckbloghub.kr       -> ALB (Frontend + Backend)
www.teckbloghub.kr   -> ALB (Frontend + Backend)
api.teckbloghub.kr   -> ALB (Backend only)
```

### 변경될 설정 (Vercel + AWS 분리)
```
teckbloghub.kr       -> Vercel (Frontend)
www.teckbloghub.kr   -> Vercel (Frontend)
api.teckbloghub.kr   -> ALB (Backend only)
```

## 5. 배포 확인

### 5.1 자동 배포
- `develop` 브랜치 push 시 자동 배포
- Production: main/master 브랜치

### 5.2 수동 배포 (CLI)
```bash
cd frontend
vercel --prod
```

## 6. 배포 후 검증

### 6.1 Frontend 접속 확인
- https://teckbloghub.kr
- https://www.teckbloghub.kr

### 6.2 API 연결 확인
- Frontend에서 Backend API 호출 테스트
- CORS 설정 확인

## 7. 트러블슈팅

### API 연결 실패 시
1. `.env.production` 확인: `NEXT_PUBLIC_API_URL=https://api.teckbloghub.kr`
2. Vercel 환경변수 설정 확인
3. Backend CORS 설정 확인 (Vercel 도메인 허용 필요)

### 도메인 연결 실패 시
1. Route53 레코드가 Vercel 레코드로 업데이트되었는지 확인
2. DNS 전파 대기 (최대 24-48시간)
3. `dig teckbloghub.kr` 명령으로 DNS 확인

## 8. 비용

Vercel Free Tier:
- 100 GB 대역폭/월
- 무제한 요청
- 자동 SSL
- Edge Network CDN

Hobby 플랜 ($20/월):
- 1 TB 대역폭/월
- 우선 빌드
- 분석 도구
