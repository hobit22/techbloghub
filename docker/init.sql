-- 초기 카테고리 데이터
INSERT INTO categories (name, description, color, created_at, updated_at) VALUES
('Frontend', '프론트엔드 개발', '#FF6B6B', NOW(), NOW()),
('Backend', '백엔드 개발', '#4ECDC4', NOW(), NOW()),
('DevOps', '데브옵스 및 인프라', '#45B7D1', NOW(), NOW()),
('Data', '데이터 및 AI/ML', '#96CEB4', NOW(), NOW()),
('Mobile', '모바일 개발', '#FECA57', NOW(), NOW()),
('Security', '보안', '#FF9FF3', NOW(), NOW()),
('Performance', '성능 최적화', '#54A0FF', NOW(), NOW()),
('Architecture', '아키텍처 및 설계', '#5F27CD', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- 초기 태그 데이터
INSERT INTO tags (name, description, created_at, updated_at) VALUES
('Java', 'Java 프로그래밍', NOW(), NOW()),
('Spring', 'Spring Framework', NOW(), NOW()),
('JavaScript', 'JavaScript', NOW(), NOW()),
('React', 'React.js', NOW(), NOW()),
('Python', 'Python 프로그래밍', NOW(), NOW()),
('Docker', 'Docker 컨테이너', NOW(), NOW()),
('Kubernetes', 'Kubernetes', NOW(), NOW()),
('AWS', 'Amazon Web Services', NOW(), NOW()),
('개발', '개발 관련', NOW(), NOW()),
('데이터', '데이터 관련', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- 초기 블로그 데이터 (국내 IT 대기업)
INSERT INTO blogs (name, company, rss_url, site_url, description, status, created_at, updated_at) VALUES
('네이버 D2', '네이버', 'https://d2.naver.com/d2.atom', 'https://d2.naver.com', '네이버 개발자들의 기술 경험과 노하우 공유', 'ACTIVE', NOW(), NOW()),
('카카오 Tech', '카카오', 'https://tech.kakao.com/feed/', 'https://tech.kakao.com', '카카오의 기술 블로그', 'ACTIVE', NOW(), NOW()),
('우아한기술블로그', '우아한형제들', 'https://techblog.woowahan.com/feed/', 'https://techblog.woowahan.com', '우아한형제들 기술블로그', 'ACTIVE', NOW(), NOW()),
('LINE Engineering', '라인', 'https://engineering.linecorp.com/ko/rss/', 'https://engineering.linecorp.com/ko/', 'LINE의 기술과 문화', 'ACTIVE', NOW(), NOW()),
('NHN Cloud Meetup', 'NHN', 'https://meetup.nhncloud.com/rss', 'https://meetup.nhncloud.com', 'NHN의 기술 경험과 노하우', 'ACTIVE', NOW(), NOW()),
('토스 Tech', '토스', 'https://toss.tech/rss.xml', 'https://toss.tech', '토스의 개발 문화와 기술', 'ACTIVE', NOW(), NOW()),
('당근 Tech Blog', '당근마켓', 'https://medium.com/feed/daangn', 'https://medium.com/daangn', '당근마켓의 개발 이야기', 'ACTIVE', NOW(), NOW()),
('컬리 기술 블로그', '마켓컬리', 'https://helloworld.kurly.com/feed.xml', 'https://helloworld.kurly.com', '컬리의 기술적 도전과 성장', 'ACTIVE', NOW(), NOW())
ON CONFLICT (rss_url) DO NOTHING;