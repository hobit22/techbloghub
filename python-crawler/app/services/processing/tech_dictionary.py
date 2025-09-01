"""
기술 용어 사전 및 namespace별 분류를 위한 모듈
"""

from typing import Dict, Set, List
from dataclasses import dataclass


@dataclass
class TechTerm:
    """기술 용어 정보"""
    term: str
    aliases: List[str]  # 동의어/변형 (예: js, javascript)
    weight: float = 1.0  # 중요도 가중치


class TechDictionary:
    """기술 용어 사전 관리"""
    
    def __init__(self):
        self._init_dictionaries()
    
    def _init_dictionaries(self):
        """모든 namespace별 사전 초기화"""
        
        # 프로그래밍 언어
        self.languages = {
            "java": TechTerm("java", ["자바"], 1.0),
            "python": TechTerm("python", ["파이썬"], 1.0), 
            "javascript": TechTerm("javascript", ["js", "자바스크립트"], 1.0),
            "typescript": TechTerm("typescript", ["ts", "타입스크립트"], 0.9),
            "go": TechTerm("go", ["golang", "구글"], 0.9),
            "rust": TechTerm("rust", ["러스트"], 0.8),
            "kotlin": TechTerm("kotlin", ["코틀린"], 0.9),
            "swift": TechTerm("swift", ["스위프트"], 0.8),
            "c++": TechTerm("c++", ["cpp", "씨플플"], 0.8),
            "c#": TechTerm("c#", ["csharp", "씨샵"], 0.8),
            "php": TechTerm("php", [], 0.7),
            "ruby": TechTerm("ruby", ["루비"], 0.7),
            "scala": TechTerm("scala", ["스칼라"], 0.6),
            "r": TechTerm("r", [], 0.6),
        }
        
        # 프레임워크/라이브러리
        self.frameworks = {
            "spring": TechTerm("spring", ["스프링", "spring-boot", "springboot"], 1.0),
            "react": TechTerm("react", ["리액트", "reactjs"], 1.0),
            "vue": TechTerm("vue", ["뷰", "vuejs", "vue.js"], 0.9),
            "angular": TechTerm("angular", ["앵귤러", "angularjs"], 0.9),
            "django": TechTerm("django", ["장고"], 0.9),
            "flask": TechTerm("flask", ["플라스크"], 0.8),
            "fastapi": TechTerm("fastapi", ["패스트api"], 0.8),
            "express": TechTerm("express", ["익스프레스", "expressjs"], 0.8),
            "nextjs": TechTerm("nextjs", ["next.js", "넥스트"], 0.9),
            "nuxtjs": TechTerm("nuxtjs", ["nuxt.js", "넉스트"], 0.8),
            "svelte": TechTerm("svelte", ["스벨트"], 0.7),
            "laravel": TechTerm("laravel", ["라라벨"], 0.7),
            "rails": TechTerm("rails", ["ruby-on-rails", "레일즈"], 0.7),
        }
        
        # 데이터베이스
        self.databases = {
            "mysql": TechTerm("mysql", ["마이sql"], 1.0),
            "postgresql": TechTerm("postgresql", ["postgres", "포스트그레"], 1.0),
            "mongodb": TechTerm("mongodb", ["몽고db", "mongo"], 0.9),
            "redis": TechTerm("redis", ["레디스"], 0.9),
            "elasticsearch": TechTerm("elasticsearch", ["엘라스틱서치", "elastic"], 0.8),
            "oracle": TechTerm("oracle", ["오라클"], 0.8),
            "sqlite": TechTerm("sqlite", ["에스큐라이트"], 0.7),
            "cassandra": TechTerm("cassandra", ["카산드라"], 0.6),
        }
        
        # 클라우드/인프라
        self.cloud = {
            "aws": TechTerm("aws", ["amazon-web-services", "아마존"], 1.0),
            "azure": TechTerm("azure", ["애저", "마이크로소프트"], 0.9),
            "gcp": TechTerm("gcp", ["google-cloud", "구글클라우드"], 0.9),
            "docker": TechTerm("docker", ["도커"], 1.0),
            "kubernetes": TechTerm("kubernetes", ["k8s", "쿠버네티스"], 0.9),
            "jenkins": TechTerm("jenkins", ["젠킨스"], 0.8),
            "github": TechTerm("github", ["깃허브"], 0.9),
            "gitlab": TechTerm("gitlab", ["깃랩"], 0.7),
            "terraform": TechTerm("terraform", ["테라폼"], 0.7),
        }
        
        # 주제/분야
        self.topics = {
            "algorithm": TechTerm("algorithm", ["알고리즘", "자료구조"], 1.0),
            "machine-learning": TechTerm("machine-learning", ["머신러닝", "ml", "기계학습"], 1.0),
            "deep-learning": TechTerm("deep-learning", ["딥러닝", "dl", "심층학습"], 0.9),
            "ai": TechTerm("ai", ["artificial-intelligence", "인공지능"], 1.0),
            "web-development": TechTerm("web-development", ["웹개발", "frontend", "backend"], 1.0),
            "mobile-development": TechTerm("mobile-development", ["모바일개발", "앱개발"], 0.9),
            "devops": TechTerm("devops", ["데브옵스", "ci/cd"], 0.9),
            "security": TechTerm("security", ["보안", "사이버보안"], 0.8),
            "blockchain": TechTerm("blockchain", ["블록체인", "암호화폐"], 0.7),
            "game-development": TechTerm("game-development", ["게임개발"], 0.7),
            "data-science": TechTerm("data-science", ["데이터사이언스", "빅데이터"], 0.9),
        }
        
        # 한국 IT 회사
        self.companies = {
            "naver": TechTerm("naver", ["네이버"], 1.0),
            "kakao": TechTerm("kakao", ["카카오"], 1.0),
            "line": TechTerm("line", ["라인"], 0.9),
            "coupang": TechTerm("coupang", ["쿠팡"], 0.9),
            "baemin": TechTerm("baemin", ["배민", "배달의민족"], 0.8),
            "toss": TechTerm("toss", ["토스"], 0.8),
            "ncsoft": TechTerm("ncsoft", ["엔씨소프트"], 0.7),
            "nexon": TechTerm("nexon", ["넥슨"], 0.7),
            "lg": TechTerm("lg", ["엘지"], 0.6),
            "samsung": TechTerm("samsung", ["삼성"], 0.6),
        }
        
        # 레벨/난이도
        self.levels = {
            "beginner": TechTerm("beginner", ["초급", "초보", "입문"], 0.8),
            "intermediate": TechTerm("intermediate", ["중급", "중간"], 0.8),
            "advanced": TechTerm("advanced", ["고급", "상급", "전문"], 0.8),
            "expert": TechTerm("expert", ["전문가", "마스터"], 0.7),
        }
    
    def get_namespace_terms(self, namespace: str) -> Dict[str, TechTerm]:
        """특정 namespace의 용어 사전 반환"""
        namespace_map = {
            "language": self.languages,
            "framework": self.frameworks, 
            "database": self.databases,
            "cloud": self.cloud,
            "topic": self.topics,
            "company": self.companies,
            "level": self.levels,
        }
        return namespace_map.get(namespace, {})
    
    def get_all_terms(self) -> Dict[str, Dict[str, TechTerm]]:
        """모든 namespace의 용어 사전 반환"""
        return {
            "language": self.languages,
            "framework": self.frameworks,
            "database": self.databases, 
            "cloud": self.cloud,
            "topic": self.topics,
            "company": self.companies,
            "level": self.levels,
        }
    
    def find_term_in_text(self, text: str, namespace: str = None) -> List[tuple]:
        """
        텍스트에서 기술 용어를 찾아서 반환
        Returns: List[(namespace, term_key, weight)]
        """
        text_lower = text.lower()
        found_terms = []
        
        namespaces_to_search = [namespace] if namespace else self.get_all_terms().keys()
        
        for ns in namespaces_to_search:
            terms_dict = self.get_namespace_terms(ns)
            
            for term_key, tech_term in terms_dict.items():
                # 메인 용어 검색
                if tech_term.term in text_lower:
                    found_terms.append((ns, term_key, tech_term.weight))
                
                # 동의어/별칭 검색
                for alias in tech_term.aliases:
                    if alias in text_lower:
                        found_terms.append((ns, term_key, tech_term.weight))
                        break  # 하나의 용어당 한 번만 매칭
        
        return found_terms
    
    def get_namespace_list(self) -> List[str]:
        """사용 가능한 namespace 목록 반환"""
        return list(self.get_all_terms().keys())


# 전역 인스턴스
tech_dictionary = TechDictionary()