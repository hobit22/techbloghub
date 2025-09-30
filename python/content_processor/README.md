# Content Processor

자동화된 웹 콘텐츠 추출 및 AI 요약 생성 시스템

## 개요

이 시스템은 데이터베이스의 `posts` 테이블에서 미처리된 URL들을 자동으로 처리하여:
1. 웹 콘텐츠 추출 (`total_content`)
2. AI 기반 요약 생성 (`summary_content`)
3. 결과를 데이터베이스에 저장

## 주요 특징

### 🔄 하이브리드 콘텐츠 추출
- **WebBaseLoader**: 일반적인 웹사이트 처리 (빠름)
- **PlaywrightURLLoader**: SPA 및 JavaScript 필요 사이트 처리 (안정적)
- **자동 Fallback**: WebBaseLoader 실패 시 자동으로 PlaywrightURLLoader 재시도

### 🧠 AI 기반 요약 생성
- OpenAI GPT-4o-mini 모델 사용
- 구조화된 요약 형식 (제목, 주요내용, 작성자, 내용)
- 배치 처리로 효율성 극대화

### 📊 배치 처리
- 10개씩 묶어서 처리 (설정 가능)
- 메모리 효율적 처리
- 실패한 URL이 전체 배치에 영향 없음

### 🛡️ 안정성
- 개별 URL별 에러 격리
- 자동 재시도 로직
- 상세한 로깅 및 모니터링

## 설치 및 설정

### 1. 의존성 설치

```bash
cd /Users/apple/IdeaProjects/techbloghub/python/content_processor
pip install -r requirements.txt

# Playwright 브라우저 설치
playwright install
```

### 2. 환경 변수 설정

```bash
cp .env.example .env
```

`.env` 파일에 OpenAI API 키 설정:
```env
OPENAI_API_KEY=your_openai_api_key_here
```

### 3. 설정 파일 수정

`config/config.yaml`에서 데이터베이스 연결 정보 확인/수정

## 사용법

### 기본 실행
```bash
python main.py
```

### 옵션 실행
```bash
# 배치 크기 변경
python main.py --batch-size 5

# 최대 배치 수 제한
python main.py --max-batches 3

# 테스트 실행 (DB 업데이트 없음)
python main.py --dry-run

# 모든 옵션 조합
python main.py --batch-size 5 --max-batches 2 --dry-run
```

## 아키텍처

### 파일 구조
```
content_processor/
├── main.py                     # 메인 실행 스크립트
├── processors/
│   ├── database_manager.py     # PostgreSQL 연동
│   ├── content_processor.py    # 웹 콘텐츠 추출
│   └── summary_generator.py    # AI 요약 생성
├── config/
│   └── config.yaml            # 설정 파일
├── utils/
│   └── logger.py              # 로깅 설정
├── logs/                      # 로그 파일들
├── requirements.txt           # Python 의존성
└── README.md
```

### 처리 흐름

1. **데이터베이스 조회**
   - `total_content IS NULL OR summary_content IS NULL`인 posts 조회
   - 10개씩 배치 단위로 처리

2. **콘텐츠 추출**
   - WebBaseLoader로 순차 처리 (alazy_load)
   - 실패한 URL은 PlaywrightURLLoader로 재시도
   - 텍스트 분할 및 정리

3. **AI 요약 생성**
   - 추출된 콘텐츠를 OpenAI로 요약
   - 구조화된 형식으로 변환
   - 배치 처리로 API 효율성 최적화

4. **데이터베이스 업데이트**
   - `total_content`, `summary_content` 컬럼 업데이트
   - 트랜잭션 단위로 안전한 처리

### 주요 클래스

#### `DatabaseManager`
- PostgreSQL 연결 및 CRUD 작업
- 배치 업데이트 지원
- 처리 통계 조회

#### `ContentProcessor`
- 하이브리드 웹 스크래핑
- WebBaseLoader + PlaywrightURLLoader
- 에러 복구 및 재시도 로직

#### `SummaryGenerator`
- OpenAI 기반 AI 요약
- LangChain map-reduce 체인
- 구조화된 요약 템플릿

## 모니터링

### 로그 확인
```bash
tail -f logs/content_processor.log
```

### 처리 상태 확인
스크립트 실행 시 실시간으로 다음 정보 출력:
- 배치별 처리 진행 상황
- URL별 성공/실패 상태
- 콘텐츠 추출량 및 요약 품질
- 데이터베이스 업데이트 결과

### 예시 출력
```
2025-01-15 10:30:15 - INFO - Processing batch 1 with 10 posts
2025-01-15 10:30:20 - INFO - WebBase: https://tech.kakao.com/posts/724 (813 chars)
2025-01-15 10:30:22 - INFO - ✅ Success: 1 chunks, 813 total chars
2025-01-15 10:30:25 - INFO - Generating summary for https://tech.kakao.com/posts/724
2025-01-15 10:30:28 - INFO - ✅ Summary generated (245 chars, 2.8s)
2025-01-15 10:30:30 - INFO - Batch update completed: 8/10 successful
```

## 스케줄링

### Cron 예시
```bash
# 매 30분마다 실행
*/30 * * * * cd /path/to/content_processor && python main.py --max-batches 5

# 매일 오전 2시에 실행
0 2 * * * cd /path/to/content_processor && python main.py
```

### Systemd 서비스 예시
```ini
[Unit]
Description=Content Processor
After=network.target

[Service]
Type=oneshot
User=your_user
WorkingDirectory=/path/to/content_processor
ExecStart=/usr/bin/python3 main.py --max-batches 10
Environment=PYTHONPATH=/path/to/content_processor

[Install]
WantedBy=multi-user.target
```

## 문제 해결

### 일반적인 문제

1. **PlaywrightURLLoader 오류**
   ```bash
   playwright install  # 브라우저 재설치
   ```

2. **데이터베이스 연결 실패**
   - `config/config.yaml`의 DB 설정 확인
   - PostgreSQL 서비스 상태 확인

3. **OpenAI API 오류**
   - `.env` 파일의 API 키 확인
   - API 사용량 및 한도 확인

4. **메모리 부족**
   - `--batch-size` 값을 줄여서 실행
   - 시스템 리소스 모니터링

### 디버깅

```bash
# 상세 로그로 실행
LOG_LEVEL=DEBUG python main.py --dry-run --max-batches 1

# 특정 배치만 테스트
python main.py --batch-size 3 --max-batches 1 --dry-run
```

## 성능 튜닝

### 설정 최적화
- `requests_per_second`: 웹 서버 부하 고려하여 조정
- `batch_size`: 시스템 메모리에 맞게 조정
- `delay_between_requests`: API 레이트 리밋 고려

### 효율성 팁
- 실패한 URL들은 별도로 관리하여 재처리
- 피크 시간대 피해서 스케줄링
- 로그 레벨을 INFO로 설정하여 성능 향상