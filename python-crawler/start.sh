#!/bin/bash

# 가상환경 활성화
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python -m venv venv
fi

echo "Activating virtual environment..."
source venv/bin/activate

# 의존성 설치
echo "Installing dependencies..."
pip install -r requirements.txt

# 환경변수 파일 확인
if [ ! -f ".env" ]; then
    echo "Creating .env file from example..."
    cp .env.example .env
    echo "Please edit .env file with your database configuration"
fi

# 애플리케이션 실행
echo "Starting FastAPI application..."
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload