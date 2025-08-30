from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    # Database
    database_url: str = "postgresql://user:password@localhost:5432/techbloghub"
    
    # API
    api_title: str = "TechBlogHub RSS Crawler"
    api_version: str = "1.0.0"
    api_description: str = "RSS Crawling and NLP Tagging Service"
    
    # Logging
    log_level: str = "INFO"
    
    # Crawler settings
    crawler_user_agent: str = "TechBlogHub-Crawler/1.0"
    crawler_timeout: int = 30
    crawler_max_posts_per_feed: int = 50
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()