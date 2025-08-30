"""
텍스트 전처리 및 분석을 위한 서비스
"""

import re
import string
from typing import List, Set, Optional
from loguru import logger

try:
    from konlpy.tag import Okt
    KONLPY_AVAILABLE = True
except ImportError:
    logger.warning("KoNLPy not available. Korean text processing will be limited.")
    KONLPY_AVAILABLE = False

import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize


class TextProcessor:
    """텍스트 전처리 및 토큰화 서비스"""
    
    def __init__(self):
        self._init_nltk_data()
        if KONLPY_AVAILABLE:
            self.okt = Okt()
        else:
            self.okt = None
        
        # 한국어 불용어
        self.korean_stopwords = {
            '이', '그', '저', '것', '들', '등', '및', '또는', '그리고', '하지만', '그러나', '때문에',
            '그래서', '따라서', '즉', '또한', '예를 들어', '같은', '다른', '새로운', '기존', '최근',
            '이번', '다음', '지난', '오늘', '어제', '내일', '년', '월', '일', '시간', '분', '초',
            '개발자', '프로그래머', '엔지니어', '회사', '팀', '프로젝트', '서비스', '시스템',
            '방법', '기술', '도구', '라이브러리', '프레임워크', '언어', '코드', '프로그램',
            '사용', '이용', '적용', '개발', '구현', '설계', '설정', '설치', '배포', '운영',
            '문제', '해결', '개선', '최적화', '성능', '효율', '품질', '안정성', '보안'
        }
        
        # 영어 불용어
        self.english_stopwords = set(stopwords.words('english')) if self._check_nltk_data('stopwords') else set()
        
        # 기술 관련 일반적인 단어들 (너무 일반적이어서 태그로 의미없는 것들)
        self.tech_common_words = {
            'api', 'app', 'web', 'server', 'client', 'user', 'data', 'file', 'code', 'test',
            'build', 'run', 'start', 'stop', 'create', 'delete', 'update', 'get', 'set',
            'function', 'method', 'class', 'object', 'variable', 'parameter', 'return',
            'error', 'exception', 'bug', 'fix', 'issue', 'version', 'release', 'update'
        }
    
    def _init_nltk_data(self):
        """NLTK 데이터 초기화"""
        try:
            nltk.data.find('tokenizers/punkt')
        except LookupError:
            logger.info("Downloading NLTK punkt tokenizer...")
            nltk.download('punkt', quiet=True)
        
        try:
            nltk.data.find('corpora/stopwords')
        except LookupError:
            logger.info("Downloading NLTK stopwords...")
            nltk.download('stopwords', quiet=True)
    
    def _check_nltk_data(self, dataset: str) -> bool:
        """NLTK 데이터 존재 여부 확인"""
        try:
            if dataset == 'stopwords':
                nltk.data.find('corpora/stopwords')
            elif dataset == 'punkt':
                nltk.data.find('tokenizers/punkt')
            return True
        except LookupError:
            return False
    
    def clean_html(self, text: str) -> str:
        """HTML 태그 제거"""
        if not text:
            return ""
        
        # HTML 태그 제거
        clean_text = re.sub(r'<[^>]+>', '', text)
        
        # HTML 엔티티 디코딩
        html_entities = {
            '&amp;': '&',
            '&lt;': '<',
            '&gt;': '>',
            '&quot;': '"',
            '&#39;': "'",
            '&nbsp;': ' '
        }
        
        for entity, char in html_entities.items():
            clean_text = clean_text.replace(entity, char)
        
        return clean_text
    
    def normalize_text(self, text: str) -> str:
        """텍스트 정규화"""
        if not text:
            return ""
        
        # HTML 태그 제거
        text = self.clean_html(text)
        
        # 연속된 공백을 하나로
        text = re.sub(r'\s+', ' ', text)
        
        # 특수문자 정리 (일부 유지)
        text = re.sub(r'[^\w\s\-\.#@]', ' ', text)
        
        # 연속된 공백 다시 정리
        text = re.sub(r'\s+', ' ', text).strip()
        
        return text
    
    def extract_keywords_korean(self, text: str) -> List[str]:
        """한국어 텍스트에서 키워드 추출"""
        if not KONLPY_AVAILABLE or not self.okt:
            # KoNLPy가 없는 경우 간단한 분할
            return self._simple_korean_tokenize(text)
        
        try:
            # 형태소 분석으로 명사, 알파벳, 영어 추출
            morphs = self.okt.pos(text, stem=True)
            
            keywords = []
            for word, pos in morphs:
                word = word.lower().strip()
                
                # 2글자 이상, 20글자 이하
                if len(word) < 2 or len(word) > 20:
                    continue
                
                # 명사, 영어, 숫자 조합만 추출
                if pos in ['Noun', 'Alpha', 'Number'] or re.match(r'^[a-z][a-z0-9\-_]*$', word):
                    if word not in self.korean_stopwords:
                        keywords.append(word)
            
            return keywords
            
        except Exception as e:
            logger.warning(f"Korean morphological analysis failed: {e}")
            return self._simple_korean_tokenize(text)
    
    def _simple_korean_tokenize(self, text: str) -> List[str]:
        """간단한 한국어 토큰화 (KoNLPy 없이)"""
        # 영어 단어와 숫자 조합 추출
        english_pattern = re.findall(r'\b[a-zA-Z][a-zA-Z0-9\-_]*\b', text)
        
        # 한글 키워드 추출 (간단한 방법)
        korean_words = re.findall(r'[가-힣]{2,}', text)
        
        keywords = []
        
        # 영어 키워드
        for word in english_pattern:
            word = word.lower()
            if len(word) >= 2 and len(word) <= 20:
                keywords.append(word)
        
        # 한글 키워드 (불용어 제거)
        for word in korean_words:
            if word not in self.korean_stopwords and len(word) >= 2:
                keywords.append(word)
        
        return keywords
    
    def extract_keywords_english(self, text: str) -> List[str]:
        """영어 텍스트에서 키워드 추출"""
        try:
            # NLTK 토큰화
            if self._check_nltk_data('punkt'):
                tokens = word_tokenize(text.lower())
            else:
                # 간단한 분할
                tokens = re.findall(r'\b[a-zA-Z][a-zA-Z0-9\-_]*\b', text.lower())
            
            keywords = []
            for token in tokens:
                token = token.strip()
                
                # 길이 제한
                if len(token) < 2 or len(token) > 20:
                    continue
                
                # 문자와 숫자만 (하이픈, 언더스코어 허용)
                if not re.match(r'^[a-z][a-z0-9\-_]*$', token):
                    continue
                
                # 불용어 제거
                if token in self.english_stopwords or token in self.tech_common_words:
                    continue
                
                keywords.append(token)
            
            return keywords
            
        except Exception as e:
            logger.warning(f"English tokenization failed: {e}")
            # 실패시 간단한 정규표현식 사용
            return re.findall(r'\b[a-zA-Z][a-zA-Z0-9\-_]+\b', text.lower())
    
    def extract_hashtags(self, text: str) -> List[str]:
        """해시태그 추출"""
        hashtags = re.findall(r'#([a-zA-Z0-9가-힣_]+)', text)
        return [tag.lower() for tag in hashtags if len(tag) >= 2]
    
    def extract_all_keywords(self, text: str) -> List[str]:
        """텍스트에서 모든 키워드 추출 (한국어 + 영어)"""
        if not text or not text.strip():
            return []
        
        # 텍스트 정규화
        normalized_text = self.normalize_text(text)
        
        # 키워드 추출
        keywords = []
        
        # 해시태그 추출
        hashtags = self.extract_hashtags(normalized_text)
        keywords.extend(hashtags)
        
        # 한국어 키워드
        korean_keywords = self.extract_keywords_korean(normalized_text)
        keywords.extend(korean_keywords)
        
        # 영어 키워드  
        english_keywords = self.extract_keywords_english(normalized_text)
        keywords.extend(english_keywords)
        
        # 중복 제거 및 정렬
        unique_keywords = list(set(keywords))
        unique_keywords.sort()
        
        return unique_keywords
    
    def get_processing_stats(self) -> dict:
        """텍스트 프로세서 상태 정보"""
        return {
            "konlpy_available": KONLPY_AVAILABLE,
            "nltk_punkt_available": self._check_nltk_data('punkt'),
            "nltk_stopwords_available": self._check_nltk_data('stopwords'),
            "korean_stopwords_count": len(self.korean_stopwords),
            "english_stopwords_count": len(self.english_stopwords),
        }


# 전역 인스턴스
text_processor = TextProcessor()