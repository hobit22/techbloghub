# Backend CI/CD and EC2 Deployment

이 문서는 `fastapi/` 백엔드용 GitHub Actions 설정값과 EC2 배포 절차를 정리합니다.

## 대상 워크플로우

- `.github/workflows/backend-ci.yml`
  - `fastapi/**` 변경 시 Python dependency install, 소스 compile, Docker build, `/health` smoke check 수행
- `.github/workflows/backend-release-deploy.yml`
  - 수동 실행(`workflow_dispatch`)으로 Docker 이미지를 ECR에 push
  - 선택적으로 EC2에 접속해 새 이미지를 pull/run 하고 health check 후 실패 시 이전 이미지로 롤백

## GitHub 설정값

### Repository Variables

`Settings > Secrets and variables > Actions > Variables` 에 아래 값을 추가합니다.

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `AWS_REGION` | 예 | AWS 리전. 예: `ap-northeast-2` |
| `ECR_REPOSITORY` | 예 | ECR repository 이름. 예: `techbloghub-fastapi` |
| `AWS_ROLE_TO_ASSUME` | 권장 | GitHub OIDC로 Assume 할 IAM Role ARN |
| `EC2_PORT` | 선택 | SSH 포트. 기본값 `22` |
| `EC2_CONTAINER_NAME` | 예 | EC2에서 실행할 Docker container 이름 |
| `EC2_CONTAINER_PORT` | 예 | 컨테이너 내부 포트. 현재 앱은 `8000` |
| `EC2_HOST_PORT` | 예 | EC2 host에 바인딩할 포트. 예: `8000` |
| `EC2_ENV_FILE_PATH` | 예 | EC2에 이미 존재하는 env 파일 경로. 예: `/opt/techbloghub/fastapi.env` |
| `EC2_HEALTHCHECK_URL` | 예 | 배포 후 확인할 헬스체크 URL. 예: `http://127.0.0.1:8000/health` |

### Repository Secrets

`Settings > Secrets and variables > Actions > Secrets` 에 아래 값을 추가합니다.

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `EC2_HOST` | 예 | 배포 대상 EC2 public IP 또는 DNS |
| `EC2_USER` | 예 | SSH 사용자. 예: `ec2-user`, `ubuntu` |
| `EC2_SSH_PRIVATE_KEY` | 예 | EC2 접속용 private key |
| `AWS_ACCESS_KEY_ID` | OIDC 미사용 시 | 장기 키 방식 사용할 때만 필요 |
| `AWS_SECRET_ACCESS_KEY` | OIDC 미사용 시 | 장기 키 방식 사용할 때만 필요 |

## 권장 인증 방식

### 1. 권장: OIDC

가장 권장되는 방식은 `AWS_ROLE_TO_ASSUME` 를 사용해 GitHub Actions가 AWS IAM Role을 Assume 하도록 구성하는 것입니다.

- 장기 AWS key를 GitHub에 저장하지 않아도 됨
- ECR push 권한을 workflow 단위로 제한하기 쉬움
- backend release workflow가 이미 OIDC를 우선 사용하도록 작성되어 있음

### 2. 차선: Access Key

OIDC를 아직 연결하지 않았다면 아래 secret을 사용합니다.

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

이 방식은 동작은 단순하지만 장기 credential 관리 부담이 있습니다.

## EC2 사전 준비

배포 workflow를 실행하기 전에 EC2에 아래가 준비되어 있어야 합니다.

1. Docker 설치
2. AWS CLI 설치
3. `EC2_ENV_FILE_PATH` 에 지정한 env 파일 생성
4. host port가 security group 및 reverse proxy 설정과 일치하도록 열려 있어야 함

예시 env 파일:

```env
APP_NAME=TechBlog Hub
APP_VERSION=1.0.0
DEBUG=False
DATABASE_URL=postgresql+asyncpg://...
ALLOWED_ORIGINS=["https://teckbloghub.kr","https://www.teckbloghub.kr"]
ADMIN_USERNAME=...
ADMIN_PASSWORD=...
OPENAI_API_KEY=...
OPENAI_MODEL=gpt-4o-mini
OPENAI_MAX_TOKENS=10000
RSS_PROXY_URL=https://rss-proxy.<subdomain>.workers.dev/?url=
DISCORD_WEBHOOK_ENABLED=true
DISCORD_WEBHOOK_URL=...
```

## Backend CI 동작

`backend-ci.yml` 은 별도 GitHub secret 없이 실행됩니다.

- `pip install -r fastapi/requirements.txt`
- `python -m compileall app main.py`
- `fastapi/Dockerfile` 로 이미지 build
- dummy env 값으로 컨테이너 실행
- `GET /health` 확인

주의:

- smoke test용 `DATABASE_URL`, `OPENAI_API_KEY` 등은 실제 외부 연결을 위한 값이 아니라 import/startup 검증용 placeholder 입니다.
- backend 앱이 `.env`의 잔여 키를 무시하도록 되어 있으므로, 오래된 env 파일이 있어도 startup 자체는 막지 않습니다.

## Release / Deploy 실행 절차

GitHub에서 `Actions > Backend Release Deploy` 로 이동한 뒤 수동 실행합니다.

입력값:

- `git_ref`: 빌드할 ref. 보통 `main`
- `image_tag`: 비워두면 `git-<shortsha>` 자동 생성
- `deploy_to_ec2`: `true` 면 EC2까지 배포, `false` 면 ECR push만 수행

### ECR push만 할 때

1. `git_ref=main`
2. `image_tag` 비워두기 또는 원하는 tag 입력
3. `deploy_to_ec2=false`
4. workflow 완료 후 summary에 출력된 `image_uri` 확인

### EC2까지 배포할 때

1. `git_ref=main`
2. 필요하면 `image_tag` 지정
3. `deploy_to_ec2=true`
4. `production` environment 승인
5. workflow가 EC2에서 아래 순서로 실행됨
   - ECR login
   - 새 이미지 pull
   - 기존 컨테이너 image 확인
   - 기존 컨테이너 stop/rm
   - 새 이미지로 `docker run`
   - `EC2_HEALTHCHECK_URL` polling
   - 실패 시 이전 이미지로 rollback

## 롤백 방식

현재 workflow는 새 이미지가 health check를 통과하지 못하면 바로 이전 image reference로 컨테이너를 다시 띄웁니다.

제약:

- 이전 컨테이너가 정상적으로 떠 있었던 경우에만 이전 image를 자동 복구할 수 있습니다.
- env 파일 문제나 host-level reverse proxy 문제까지 자동으로 복구하지는 않습니다.

## 권장 운영 순서

1. PR에서 `backend-ci.yml` 통과 확인
2. `main` merge
3. `Backend Release Deploy` 를 `deploy_to_ec2=false` 로 먼저 실행해 ECR image 생성 확인
4. 같은 tag로 `deploy_to_ec2=true` 실행
5. EC2 health check 및 실제 서비스 URL 확인

## 현재 범위 밖

이 문서와 workflow는 아래까지는 자동화하지 않습니다.

- DB migration 자동 실행
- nginx/reverse proxy 설정 변경
- multi-instance rolling deploy
- Cloudflare Worker 배포

이 단계들은 별도 운영 절차 또는 후속 workflow로 분리하는 것이 안전합니다.
