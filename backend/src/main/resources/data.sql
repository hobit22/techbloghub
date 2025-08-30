-- 초기 블로그 데이터 (국내 IT 대기업)
INSERT INTO blog (name, company, rss_url, site_url, description, status, created_at, updated_at) VALUES
('네이버 D2', '네이버', 'https://d2.naver.com/d2.atom', 'https://d2.naver.com', '네이버 개발자들의 기술 경험과 노하우 공유', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('카카오 Tech', '카카오', 'https://tech.kakao.com/feed/', 'https://tech.kakao.com', '카카오의 기술 블로그', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('우아한기술블로그', '우아한형제들', 'https://techblog.woowahan.com/feed/', 'https://techblog.woowahan.com', '우아한형제들 기술블로그', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LY Corporation Tech Blog', '라인', 'https://techblog.lycorp.co.jp/ko/feed/index.xml', 'https://techblog.lycorp.co.jp/ko', 'LINE의 기술과 문화', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NHN Cloud Meetup', 'NHN', 'https://meetup.nhncloud.com/rss', 'https://meetup.nhncloud.com', 'NHN의 기술 경험과 노하우', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('토스 Tech', '토스', 'https://toss.tech/rss.xml', 'https://toss.tech', '토스의 개발 문화와 기술', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('당근 Tech Blog', '당근마켓', 'https://medium.com/feed/daangn', 'https://medium.com/daangn', '당근마켓의 개발 이야기', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SOCAR Tech Blog', '쏘카', 'https://tech.socarcorp.kr/feed.xml', 'https://tech.socarcorp.kr/', '당근마켓의 개발 이야기', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('컬리 기술 블로그', '마켓컬리', 'https://helloworld.kurly.com/feed.xml', 'https://helloworld.kurly.com', '쏘카 기술 블로그', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('카카모빌리티 디벨로퍼스', '카카오모빌리티', 'https://developers.kakaomobility.com/techblogs.xml', 'https://developers.kakaomobility.com/techblogs', '카카오모빌리티 디벨로퍼스 기술블로그', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('카카오페이 기술 블로그', '카카오페이', 'https://tech.kakaopay.com/rss', 'https://tech.kakaopay.com', '기술과 경험을 함께 공유합니다', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);