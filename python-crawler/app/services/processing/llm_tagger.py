"""LLM-based tagging and categorization service for tech blog posts."""

import json
import os
from typing import Dict, List, Optional
from loguru import logger
from openai import OpenAI

from ...models import PostEntity


class TaggingResult:
    """Result of LLM tagging operation."""
    
    def __init__(self, tags: List[str], categories: List[str]):
        self.tags = tags
        self.categories = categories
    
    def to_dict(self) -> Dict:
        return {
            "tags": self.tags,
            "categories": self.categories
        }


class LLMTagger:
    """LLM-based tagger for extracting tags and categories from tech blog posts."""
    
    def __init__(self, api_key: Optional[str] = None, db_session = None):
        """Initialize LLM tagger.
        
        Args:
            api_key: OpenAI API key. If None, will try to get from environment.
        """
        # Try to get API key from parameter, then environment, then settings
        if api_key:
            self.api_key = api_key
        else:
            # Import here to avoid circular import
            from ...core.config import get_settings
            settings = get_settings()
            self.api_key = settings.openai_api_key or os.getenv("OPENAI_API_KEY")
        
        if not self.api_key or not self.api_key.strip():
            raise ValueError("OpenAI API key is required. Set OPENAI_API_KEY environment variable or configure openai_api_key in settings.")
        
        logger.info(f"Initializing OpenAI client with API key: {self.api_key[:10]}...")
        self.client = OpenAI(api_key=self.api_key)
        self.model = "gpt-4.1"  # Cost-effective model for tagging
        self.db_session = db_session
        
        # Predefined categories for tech blog posts
        self.predefined_categories = [
            "Frontend", "Backend", "Mobile", "AI/ML", "Data", "DevOps", 
            "Infrastructure", "Security", "Test", "Architecture", 
            "Product", "Culture", "Career"
        ]
        
        # Predefined tags organized by groups for better LLM matching
        self.predefined_tags = {
            "language": [
                "JavaScript", "TypeScript", "Python", "Java", "Go", "Rust", "Swift", "Kotlin",
                "C++", "C#", "PHP", "Ruby", "Scala", "Clojure", "Elixir", "Haskell", "Dart", "R", "Shell", "C"
            ],
            "frontend-framework": [
                "React", "Vue", "Angular", "Svelte", "Solid", "Next.js", "Nuxt.js", "SvelteKit",
                "Remix", "Gatsby", "Astro"
            ],
            "backend-framework": [
                "Node.js", "Express", "Fastify", "Koa", "NestJS", "Hapi", "Django", "FastAPI",
                "Flask", "Tornado", "Pyramid", "Starlette", "Spring Boot", "Spring", "Quarkus",
                "Micronaut", "Gin", "Echo", "Fiber", "Chi", "Laravel", "Rails", "ASP.NET", "Phoenix"
            ],
            "database": [
                "PostgreSQL", "MySQL", "MariaDB", "SQLite", "Oracle", "SQL Server", "MongoDB",
                "Redis", "Cassandra", "CouchDB", "Neo4j", "DynamoDB", "InfluxDB"
            ],
            "cloud": [
                "AWS", "Azure", "GCP", "DigitalOcean", "Heroku", "Vercel", "Netlify", "Firebase"
            ],
            "container": [
                "Docker", "Kubernetes", "Podman", "Docker Compose", "Helm", "Rancher"
            ],
            "testing": [
                "Jest", "Cypress", "Playwright", "Selenium", "Testing Library", "Mocha",
                "Chai", "Jasmine", "pytest", "JUnit", "Vitest", "Storybook"
            ],
            "styling": [
                "CSS", "SCSS", "Sass", "Less", "Tailwind CSS", "Styled Components", "Emotion",
                "CSS-in-JS", "Bootstrap", "Chakra UI"
            ],
            "build-tool": [
                "Webpack", "Vite", "Rollup", "Parcel", "esbuild", "Turbopack", "Grunt", "Gulp"
            ],
            "mobile": [
                "iOS", "Android", "React Native", "Flutter", "Ionic", "Xamarin", "Cordova", "PWA"
            ],
            "api": [
                "REST", "GraphQL", "gRPC", "WebSocket", "tRPC", "OpenAPI", "Swagger"
            ],
            "architecture": [
                "Microservices", "Monolith", "Serverless", "Event-Driven", "CQRS", "DDD",
                "Clean Architecture", "Hexagonal Architecture", "MVC", "MVP", "MVVM"
            ],
            "security": [
                "OAuth", "JWT", "SAML", "HTTPS", "TLS", "CORS", "OWASP"
            ],
            "performance": [
                "CDN", "Caching", "Code Splitting", "Lazy Loading", "Web Vitals", "Lighthouse", "Service Worker"
            ]
        }
    
    def tag_post(self, post: PostEntity) -> TaggingResult:
        """Extract tags and categories from a blog post using LLM.
        
        Args:
            post: PostEntity to analyze
            
        Returns:
            TaggingResult containing extracted tags and categories
        """
        try:
            logger.debug(f"Tagging post: {post.id} - {post.title[:50]}...")
            
            # Prepare content for analysis
            title = post.title or ""
            content = post.content or ""
            
            if not title.strip() and not content.strip():
                logger.warning(f"Post {post.id} has no content to analyze")
                return TaggingResult([], [])
            
            # Truncate content if too long (to stay within token limits)
            max_content_length = 3000
            if len(content) > max_content_length:
                content = content[:max_content_length] + "..."
            
            # Create prompt for LLM
            prompt = self._create_tagging_prompt(title, content)
            
            # Call OpenAI API
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {
                        "role": "system",
                        "content": "You are an expert at analyzing tech blog posts and extracting relevant tags and categories."
                    },
                    {
                        "role": "user", 
                        "content": prompt
                    }
                ],
                temperature=0.1,  # Low temperature for consistent results
                max_tokens=1000
            )
            
            # Parse response
            result = self._parse_llm_response(response.choices[0].message.content, post)
            
            logger.debug(f"Extracted {len(result.tags)} tags and {len(result.categories)} categories for post {post.id}")
            return result
            
        except Exception as e:
            logger.error(f"Failed to tag post {post.id}: {e}")
            return TaggingResult([], [])
    
    def _create_tagging_prompt(self, title: str, content: str) -> str:
        """Create prompt for LLM tagging."""
        categories_str = ", ".join(self.predefined_categories)
        
        # Create structured tag groups for comprehensive display
        tag_groups_str = ""
        for group_name, tags in self.predefined_tags.items():
            group_display_name = group_name.replace("-", " ").title()
            tags_str = ", ".join(tags)
            tag_groups_str += f"{group_display_name}: {tags_str}\n"
        
        return f"""Please analyze this tech blog post and extract relevant tags and categories.

Title: {title}

Content: {content}

AVAILABLE TAGS (MUST USE ONLY FROM THIS LIST):
{tag_groups_str}

AVAILABLE CATEGORIES (MUST USE ONLY FROM THIS LIST):
{categories_str}

Please provide your response in the following JSON format:
{{
    "tags": ["tag1", "tag2", "tag3"],
    "categories": ["category1", "category2"]
}}

STRICT GUIDELINES:
- **MANDATORY**: ONLY use tags from the "AVAILABLE TAGS" list above - DO NOT create new tags
- **MANDATORY**: ONLY use categories from the "AVAILABLE CATEGORIES" list above  
- Provide 0-8 most relevant tags that best represent the technical content
- Provide 1-3 most relevant categories that best fit the post
- Use EXACT naming from predefined lists (case-sensitive)
- If the post is in Korean, provide English tags and categories from the lists
- Focus on technical aspects and concrete technologies mentioned in the content
- If you cannot find appropriate tags from the predefined list, use fewer tags rather than creating new ones

Return only the JSON response, no other text."""
    
    def _parse_llm_response(self, response: str, post: PostEntity) -> TaggingResult:
        """Parse LLM response to extract tags and categories."""
        try:
            # Clean up response (sometimes LLM adds extra text)
            response = response.strip()

            logger.info(f"LLM response: {response}")
            
            # Find JSON part in response
            start_idx = response.find('{')
            end_idx = response.rfind('}') + 1
            
            if start_idx == -1 or end_idx == 0:
                logger.warning("No JSON found in LLM response")
                return TaggingResult([], [])
            
            json_str = response[start_idx:end_idx]
            data = json.loads(json_str)
            
            tags = data.get("tags", [])
            categories = data.get("categories", [])
            
            # Create flattened list of all predefined tags for validation
            all_predefined_tags = []
            for tag_group in self.predefined_tags.values():
                all_predefined_tags.extend(tag_group)
            
            # Validate and clean tags - ONLY allow predefined tags
            validated_tags = []
            for tag in tags:
                if isinstance(tag, str) and tag.strip():
                    clean_tag = tag.strip()
                    # Exact match first
                    if clean_tag in all_predefined_tags:
                        validated_tags.append(clean_tag)
                    else:
                        # Try case-insensitive match
                        for predefined_tag in all_predefined_tags:
                            if clean_tag.lower() == predefined_tag.lower():
                                validated_tags.append(predefined_tag)
                                logger.info(f"Auto-corrected tag case: '{clean_tag}' -> '{predefined_tag}'")
                                break
                        else:
                            # Try common variations mapping
                            mapped_tag = self._map_tag_variations(clean_tag, all_predefined_tags)
                            if mapped_tag:
                                validated_tags.append(mapped_tag)
                                logger.info(f"Auto-mapped tag variation: '{clean_tag}' -> '{mapped_tag}'")
                            else:
                                logger.warning(f"Rejected non-predefined tag: '{clean_tag}'")
                                # Save rejected tag if database session is available
                                self._save_rejected_tag(clean_tag, post)
            
            # Limit to 8 tags and remove duplicates
            validated_tags = list(dict.fromkeys(validated_tags))[:8]
            
            # Validate categories - ONLY allow predefined categories  
            validated_categories = []
            for cat in categories:
                if isinstance(cat, str) and cat.strip():
                    clean_cat = cat.strip()
                    if clean_cat in self.predefined_categories:
                        validated_categories.append(clean_cat)
                    else:
                        # Try case-insensitive match
                        for predefined_cat in self.predefined_categories:
                            if clean_cat.lower() == predefined_cat.lower():
                                validated_categories.append(predefined_cat)
                                logger.info(f"Auto-corrected category case: '{clean_cat}' -> '{predefined_cat}'")
                                break
                        else:
                            logger.warning(f"Rejected non-predefined category: '{clean_cat}'")
                            # Save rejected category if database session is available
                            self._save_rejected_category(clean_cat, post)
            
            # Limit to 3 categories and remove duplicates
            validated_categories = list(dict.fromkeys(validated_categories))[:3]
            
            logger.info(f"Final validated tags: {validated_tags}")
            logger.info(f"Final validated categories: {validated_categories}")
            
            return TaggingResult(validated_tags, validated_categories)
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON from LLM response: {e}")
            logger.debug(f"Response was: {response}")
            return TaggingResult([], [])
        except Exception as e:
            logger.error(f"Error parsing LLM response: {e}")
            return TaggingResult([], [])
    
    def _map_tag_variations(self, tag: str, predefined_tags: List[str]) -> Optional[str]:
        """Map common tag variations to predefined tags."""
        tag_lower = tag.lower()
        
        # Common variations mapping
        variations = {
            # JavaScript variations
            'js': 'JavaScript',
            'javascript': 'JavaScript',
            'reactjs': 'React',
            'react.js': 'React',
            'vuejs': 'Vue',
            'vue.js': 'Vue',
            'angularjs': 'Angular',
            'angular.js': 'Angular',
            'nodejs': 'Node.js',
            'node': 'Node.js',
            'nextjs': 'Next.js',
            'nuxtjs': 'Nuxt.js',
            
            # Framework variations
            'django': 'Django',
            'flask': 'Flask',
            'fastapi': 'FastAPI',
            'express.js': 'Express',
            'expressjs': 'Express',
            'spring-boot': 'Spring Boot',
            'springboot': 'Spring Boot',
            
            # Database variations
            'postgres': 'PostgreSQL',
            'psql': 'PostgreSQL',
            'mysql': 'MySQL',
            'mongodb': 'MongoDB',
            'mongo': 'MongoDB',
            'redis': 'Redis',
            
            # Cloud variations
            'amazon-web-services': 'AWS',
            'amazon web services': 'AWS',
            'google-cloud': 'GCP',
            'google cloud platform': 'GCP',
            'microsoft-azure': 'Azure',
            
            # Container variations
            'docker': 'Docker',
            'k8s': 'Kubernetes',
            'kubernetes': 'Kubernetes',
            
            # Other variations
            'typescript': 'TypeScript',
            'ts': 'TypeScript',
            'python': 'Python',
            'java': 'Java',
            'golang': 'Go',
            'go-lang': 'Go',
            'c++': 'C++',
            'cpp': 'C++',
            'c#': 'C#',
            'csharp': 'C#',
            'c-sharp': 'C#',
        }
        
        # Check direct mapping
        if tag_lower in variations:
            return variations[tag_lower]
        
        # Check if variation exists in predefined tags (case-insensitive)
        for predefined_tag in predefined_tags:
            if tag_lower == predefined_tag.lower():
                return predefined_tag
        
        return None
    
    def _save_rejected_tag(self, tag_name: str, post: PostEntity):
        """Save rejected tag to database if session is available."""
        if not self.db_session:
            return
        
        try:
            # Import here to avoid circular imports
            from ..rejected_tag_service import RejectedTagService
            
            rejected_service = RejectedTagService(self.db_session)
            rejected_service.save_rejected_tag(tag_name, post)
            
        except Exception as e:
            logger.error(f"Failed to save rejected tag '{tag_name}': {e}")
    
    def _save_rejected_category(self, category_name: str, post: PostEntity):
        """Save rejected category to database if session is available.""" 
        if not self.db_session:
            return
        
        try:
            # Import here to avoid circular imports
            from ..rejected_tag_service import RejectedTagService
            
            rejected_service = RejectedTagService(self.db_session)
            rejected_service.save_rejected_category(category_name, post)
            
        except Exception as e:
            logger.error(f"Failed to save rejected category '{category_name}': {e}")
    
    def batch_tag_posts(self, posts: List[PostEntity]) -> Dict[int, TaggingResult]:
        """Tag multiple posts in batch.
        
        Args:
            posts: List of PostEntity objects to tag
            
        Returns:
            Dictionary mapping post ID to TaggingResult
        """
        results = {}
        
        for post in posts:
            try:
                result = self.tag_post(post)
                results[post.id] = result
            except Exception as e:
                logger.error(f"Failed to tag post {post.id}: {e}")
                results[post.id] = TaggingResult([], [])
        
        return results