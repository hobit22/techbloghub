"""
토스 크롤러 테스트 스크립트
"""
import asyncio
from app.services.crawling.toss_crawler import TossCrawler
from app.models.blog import BlogEntity, BlogType


async def test_toss_crawler():
    """토스 크롤러 테스트"""
    print("토스 크롤러 테스트 시작...")
    
    # 테스트용 블로그 엔티티 생성
    test_blog = BlogEntity(
        id=998,  # 테스트용 임시 ID
        name="토스 기술블로그",
        company="토스",
        rss_url="https://toss.tech/",
        site_url="https://toss.tech/",
        blog_type=BlogType.TOSS
    )
    
    crawler = TossCrawler()
    
    try:
        # 첫 번째 페이지만 테스트
        print("API에서 첫 번째 페이지 가져오기...")
        posts_data = await crawler._fetch_posts_page(1)
        
        if posts_data:
            success_data = posts_data.get('success', {})
            print(f"API 응답 성공! 페이지 정보:")
            print(f"- 현재 페이지: {success_data.get('page', 1)}")
            print(f"- 페이지 크기: {success_data.get('pageSize', 0)}")
            print(f"- 총 포스트 수: {success_data.get('count', 0)}")
            print(f"- 다음 페이지 있음: {bool(success_data.get('next'))}")
            print(f"- 이번 페이지 결과 수: {len(success_data.get('results', []))}")
            
            # 전체 응답 구조 확인
            print(f"\n전체 응답 키들: {list(posts_data.keys())}")
            if success_data and 'results' in success_data and success_data['results']:
                print(f"success 키들: {list(success_data.keys())}")
                print(f"첫 번째 결과 키들: {list(success_data['results'][0].keys())}")
            
            # 포스트 엔티티 생성 테스트
            posts = crawler._extract_posts_from_page(posts_data, test_blog)
            print(f"- 추출된 포스트 수: {len(posts)}")
            
            # 첫 번째 포스트 정보 출력
            if posts:
                first_post = posts[0]
                print(f"\n첫 번째 포스트 정보:")
                print(f"- 제목: {first_post.title}")
                print(f"- 작성자: {first_post.author}")
                print(f"- 발행일: {first_post.published_at}")
                print(f"- URL: {first_post.original_url}")
                print(f"- 내용 길이: {len(first_post.content or '') if first_post.content else 0} 글자")
        else:
            print("API 응답이 None입니다.")
            
    except Exception as e:
        print(f"테스트 중 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
    finally:
        await crawler.close()
    
    print("\n토스 크롤러 테스트 완료")


if __name__ == "__main__":
    asyncio.run(test_toss_crawler())