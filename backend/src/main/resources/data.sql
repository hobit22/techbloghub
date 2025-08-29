-- H2 초기 데이터 (애플리케이션 시작 시 자동 실행)

-- 초기 카테고리 데이터
INSERT INTO categories (name, description, color, created_at, updated_at) VALUES
('Frontend', '프론트엔드 개발', '#FF6B6B', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Backend', '백엔드 개발', '#4ECDC4', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DevOps', '데브옵스 및 인프라', '#45B7D1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Data', '데이터 및 AI/ML', '#96CEB4', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Mobile', '모바일 개발', '#FECA57', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Security', '보안', '#FF9FF3', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Performance', '성능 최적화', '#54A0FF', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Architecture', '아키텍처 및 설계', '#5F27CD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 초기 태그 데이터
INSERT INTO tag (name, description, created_at, updated_at) VALUES
('Java', 'Java 프로그래밍', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Spring', 'Spring Framework', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('JavaScript', 'JavaScript', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('React', 'React.js', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Python', 'Python 프로그래밍', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Docker', 'Docker 컨테이너', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Kubernetes', 'Kubernetes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('AWS', 'Amazon Web Services', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('개발', '개발 관련', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('데이터', '데이터 관련', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 초기 블로그 데이터 (국내 IT 대기업)
INSERT INTO blog (name, company, rss_url, site_url, description, status, created_at, updated_at) VALUES
('네이버 D2', '네이버', 'https://d2.naver.com/d2.atom', 'https://d2.naver.com', '네이버 개발자들의 기술 경험과 노하우 공유', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('카카오 Tech', '카카오', 'https://tech.kakao.com/feed/', 'https://tech.kakao.com', '카카오의 기술 블로그', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('우아한기술블로그', '우아한형제들', 'https://techblog.woowahan.com/feed/', 'https://techblog.woowahan.com', '우아한형제들 기술블로그', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LY Corporation Tech Blog', '라인', 'https://techblog.lycorp.co.jp/ko/feed/index.xml', 'https://techblog.lycorp.co.jp/ko', 'LINE의 기술과 문화', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NHN Cloud Meetup', 'NHN', 'https://meetup.nhncloud.com/rss', 'https://meetup.nhncloud.com', 'NHN의 기술 경험과 노하우', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('토스 Tech', '토스', 'https://toss.tech/rss.xml', 'https://toss.tech', '토스의 개발 문화와 기술', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('당근 Tech Blog', '당근마켓', 'https://medium.com/feed/daangn', 'https://medium.com/daangn', '당근마켓의 개발 이야기', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('컬리 기술 블로그', '마켓컬리', 'https://helloworld.kurly.com/feed.xml', 'https://helloworld.kurly.com', '컬리의 기술적 도전과 성장', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);