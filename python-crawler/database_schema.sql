-- ============================================
-- TechBlogHub Database Schema DDL
-- PostgreSQL 전용 스키마 생성 스크립트
-- ============================================

-- 1. ENUM 타입 생성
CREATE TYPE blog_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');
CREATE TYPE blog_type AS ENUM ('WORDPRESS', 'NAVER_D2', 'NHN_CLOUD', 'LYCORP', 'MEDIUM', 'KAKAO', 'TOSS');

-- 2. 블로그 테이블 (기존)
CREATE TABLE IF NOT EXISTS blog (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL UNIQUE,
    company VARCHAR NOT NULL,
    rss_url VARCHAR NOT NULL UNIQUE,
    site_url VARCHAR NOT NULL,
    description TEXT,
    logo_url VARCHAR,
    status blog_status DEFAULT 'ACTIVE',
    blog_type blog_type,
    last_crawled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 블로그 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_blog_name ON blog(name);
CREATE INDEX IF NOT EXISTS idx_blog_status ON blog(status);
CREATE INDEX IF NOT EXISTS idx_blog_type ON blog(blog_type);

-- 3. 포스트 테이블 (기존)
CREATE TABLE IF NOT EXISTS posts (
    id SERIAL PRIMARY KEY,
    title VARCHAR NOT NULL,
    content TEXT,
    original_url VARCHAR(1023) NOT NULL UNIQUE,
    author VARCHAR,
    published_at TIMESTAMP WITH TIME ZONE,
    blog_id INTEGER NOT NULL REFERENCES blog(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 포스트 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_post_published_at ON posts(published_at);
CREATE INDEX IF NOT EXISTS idx_post_blog_id ON posts(blog_id);
CREATE INDEX IF NOT EXISTS idx_post_original_url ON posts(original_url);

-- 4. 태그 마스터 테이블 (신규)
CREATE TABLE IF NOT EXISTS tags (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    tag_group VARCHAR(50),
    color VARCHAR(7),
    usage_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    parent_id INTEGER REFERENCES tags(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_tag_name UNIQUE (name),
    CONSTRAINT uq_tag_slug UNIQUE (slug)
);

-- 태그 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_tag_name ON tags(name);
CREATE INDEX IF NOT EXISTS idx_tag_slug ON tags(slug);
CREATE INDEX IF NOT EXISTS idx_tag_group ON tags(tag_group);
CREATE INDEX IF NOT EXISTS idx_tag_usage_count ON tags(usage_count DESC);
CREATE INDEX IF NOT EXISTS idx_tag_active ON tags(is_active);
CREATE INDEX IF NOT EXISTS idx_tag_parent_id ON tags(parent_id);

-- 5. 카테고리 마스터 테이블 (신규)
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(7), -- Hex color code (#FFFFFF)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_category_name UNIQUE (name)
);

-- 카테고리 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_category_name ON categories(name);

-- 6. 포스트-태그 관계 테이블 (신규)
CREATE TABLE IF NOT EXISTS post_tags (
    id SERIAL PRIMARY KEY,
    post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_post_tag UNIQUE (post_id, tag_id)
);

-- 포스트-태그 관계 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_post_tags_post_id ON post_tags(post_id);
CREATE INDEX IF NOT EXISTS idx_post_tags_tag_id ON post_tags(tag_id);

-- 7. 포스트-카테고리 관계 테이블 (신규)
CREATE TABLE IF NOT EXISTS post_categories (
    id SERIAL PRIMARY KEY,
    post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_post_category UNIQUE (post_id, category_id)
);

-- 포스트-카테고리 관계 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_post_categories_post_id ON post_categories(post_id);
CREATE INDEX IF NOT EXISTS idx_post_categories_category_id ON post_categories(category_id);

-- ============================================
-- 기본 데이터 삽입
-- ============================================

-- 기본 카테고리 데이터 삽입
INSERT INTO categories (name, description, color) VALUES 
    ('Frontend', 'Frontend development and web technologies', '#3B82F6'),
    ('Backend', 'Backend development and server technologies', '#10B981'),
    ('Mobile', 'Mobile app development (iOS, Android)', '#8B5CF6'),
    ('AI/ML', 'Artificial Intelligence and Machine Learning', '#F59E0B'),
    ('Data', 'Data Science, Analytics, and Engineering', '#EF4444'),
    ('DevOps', 'DevOps, CI/CD, and Infrastructure as Code', '#6366F1'),
    ('Infrastructure', 'System Infrastructure and Cloud', '#84CC16'),
    ('Security', 'Cybersecurity and Information Security', '#DC2626'),
    ('Test', 'Software Testing and Quality Assurance', '#059669'),
    ('Architecture', 'Software Architecture and System Design', '#7C3AED'),
    ('Product', 'Product Management and Development', '#DB2777'),
    ('Culture', 'Engineering Culture and Team Management', '#0EA5E9'),
    ('Career', 'Career Development and Professional Growth', '#F97316')
ON CONFLICT (name) DO NOTHING;

-- 기본 태그 데이터 삽입
INSERT INTO tags (name, slug, description, tag_group, color) VALUES 
    -- Programming Languages
    ('JavaScript', 'javascript', 'Dynamic programming language for web development', 'language', '#F7DF1E'),
    ('TypeScript', 'typescript', 'Typed superset of JavaScript', 'language', '#3178C6'),
    ('Python', 'python', 'High-level programming language', 'language', '#3776AB'),
    ('Java', 'java', 'Object-oriented programming language', 'language', '#ED8B00'),
    ('Go', 'go', 'Statically typed compiled language by Google', 'language', '#00ADD8'),
    ('Rust', 'rust', 'Systems programming language', 'language', '#000000'),
    ('Swift', 'swift', 'Programming language for iOS development', 'language', '#FA7343'),
    ('Kotlin', 'kotlin', 'Modern programming language for Android', 'language', '#0095D5'),
    ('C++', 'cpp', 'General-purpose programming language', 'language', '#00599C'),
    ('C#', 'csharp', 'Microsoft object-oriented language', 'language', '#239120'),
    ('PHP', 'php', 'Server-side scripting language', 'language', '#777BB4'),
    ('Ruby', 'ruby', 'Dynamic object-oriented language', 'language', '#CC342D'),
    ('Scala', 'scala', 'Functional and object-oriented language', 'language', '#DC322F'),
    ('Clojure', 'clojure', 'Dynamic Lisp dialect', 'language', '#5881D8'),
    ('Elixir', 'elixir', 'Dynamic functional language', 'language', '#4B275F'),
    ('Haskell', 'haskell', 'Purely functional language', 'language', '#5E5086'),
    ('Dart', 'dart', 'Client-optimized language by Google', 'language', '#0175C2'),
    ('R', 'r', 'Statistical computing language', 'language', '#276DC3'),
    ('Shell', 'shell', 'Command-line scripting', 'language', '#89E051'),
    ('C', 'c', 'Low-level programming language', 'language', '#A8B9CC'),
    
    -- Frontend Frameworks
    ('React', 'react', 'JavaScript library for building user interfaces', 'frontend-framework', '#61DAFB'),
    ('Vue', 'vue', 'Progressive JavaScript framework', 'frontend-framework', '#4FC08D'),
    ('Angular', 'angular', 'TypeScript-based web application framework', 'frontend-framework', '#DD0031'),
    ('Svelte', 'svelte', 'Cybernetically enhanced web apps', 'frontend-framework', '#FF3E00'),
    ('Solid', 'solid', 'Simple and performant reactivity', 'frontend-framework', '#2C4F7C'),
    ('Next.js', 'nextjs', 'React framework for production', 'frontend-framework', '#000000'),
    ('Nuxt.js', 'nuxtjs', 'Vue.js framework', 'frontend-framework', '#00DC82'),
    ('SvelteKit', 'sveltekit', 'Full-stack Svelte framework', 'frontend-framework', '#FF3E00'),
    ('Remix', 'remix', 'Full stack web framework', 'frontend-framework', '#000000'),
    ('Gatsby', 'gatsby', 'Static site generator', 'frontend-framework', '#663399'),
    ('Astro', 'astro', 'Static site builder', 'frontend-framework', '#FF5D01'),
    
    -- Styling & CSS
    ('CSS', 'css', 'Cascading Style Sheets', 'styling', '#1572B6'),
    ('SCSS', 'scss', 'Syntactically Awesome Style Sheets', 'styling', '#CF649A'),
    ('Sass', 'sass', 'CSS extension language', 'styling', '#CF649A'),
    ('Less', 'less', 'CSS preprocessor', 'styling', '#1D365D'),
    ('Tailwind CSS', 'tailwindcss', 'Utility-first CSS framework', 'styling', '#06B6D4'),
    ('Styled Components', 'styled-components', 'CSS-in-JS library', 'styling', '#DB7093'),
    ('Emotion', 'emotion', 'CSS-in-JS library', 'styling', '#D26AC2'),
    ('CSS-in-JS', 'css-in-js', 'CSS styling in JavaScript', 'styling', '#000000'),
    ('Bootstrap', 'bootstrap', 'CSS framework', 'styling', '#7952B3'),
    ('Chakra UI', 'chakra-ui', 'Simple modular accessible component library', 'styling', '#319795'),
    
    -- Build Tools
    ('Webpack', 'webpack', 'Static module bundler', 'build-tool', '#8DD6F9'),
    ('Vite', 'vite', 'Frontend build tool', 'build-tool', '#646CFF'),
    ('Rollup', 'rollup', 'Module bundler', 'build-tool', '#EC4A3F'),
    ('Parcel', 'parcel', 'Zero configuration build tool', 'build-tool', '#E7A93F'),
    ('esbuild', 'esbuild', 'Extremely fast bundler', 'build-tool', '#FFCF00'),
    ('Turbopack', 'turbopack', 'Incremental bundler by Vercel', 'build-tool', '#000000'),
    ('Grunt', 'grunt', 'JavaScript task runner', 'build-tool', '#FBA919'),
    ('Gulp', 'gulp', 'Streaming build system', 'build-tool', '#CF4647'),
    
    -- Backend Frameworks - Node.js
    ('Node.js', 'nodejs', 'JavaScript runtime environment', 'backend-framework', '#339933'),
    ('Express', 'express', 'Fast Node.js web framework', 'backend-framework', '#000000'),
    ('Fastify', 'fastify', 'Fast and low overhead web framework', 'backend-framework', '#000000'),
    ('Koa', 'koa', 'Expressive middleware for Node.js', 'backend-framework', '#000000'),
    ('NestJS', 'nestjs', 'Scalable Node.js framework', 'backend-framework', '#E0234E'),
    ('Hapi', 'hapi', 'Rich framework for building applications', 'backend-framework', '#FF6900'),
    
    -- Backend Frameworks - Python
    ('Django', 'django', 'High-level Python web framework', 'backend-framework', '#092E20'),
    ('FastAPI', 'fastapi', 'Modern Python web framework', 'backend-framework', '#009688'),
    ('Flask', 'flask', 'Lightweight Python web framework', 'backend-framework', '#000000'),
    ('Tornado', 'tornado', 'Python web framework and library', 'backend-framework', '#000000'),
    ('Pyramid', 'pyramid', 'Python web framework', 'backend-framework', '#000000'),
    ('Starlette', 'starlette', 'Lightweight ASGI framework', 'backend-framework', '#000000'),
    
    -- Backend Frameworks - Java
    ('Spring Boot', 'spring-boot', 'Java application framework', 'backend-framework', '#6DB33F'),
    ('Spring', 'spring', 'Java application framework', 'backend-framework', '#6DB33F'),
    ('Quarkus', 'quarkus', 'Supersonic Subatomic Java', 'backend-framework', '#4695EB'),
    ('Micronaut', 'micronaut', 'Modern JVM framework', 'backend-framework', '#01B9CC'),
    
    -- Backend Frameworks - Go
    ('Gin', 'gin', 'HTTP web framework for Go', 'backend-framework', '#00ADD8'),
    ('Echo', 'echo', 'High performance Go web framework', 'backend-framework', '#00ADD8'),
    ('Fiber', 'fiber', 'Express inspired web framework', 'backend-framework', '#00ADD8'),
    ('Chi', 'chi', 'Lightweight HTTP router', 'backend-framework', '#00ADD8'),
    
    -- Backend Frameworks - Others
    ('Laravel', 'laravel', 'PHP web application framework', 'backend-framework', '#FF2D20'),
    ('Rails', 'rails', 'Ruby web application framework', 'backend-framework', '#CC0000'),
    ('ASP.NET', 'aspnet', 'Microsoft web framework', 'backend-framework', '#512BD4'),
    ('Phoenix', 'phoenix', 'Elixir web framework', 'backend-framework', '#FD4F00'),
    
    -- Databases - Relational
    ('PostgreSQL', 'postgresql', 'Advanced open source database', 'database', '#336791'),
    ('MySQL', 'mysql', 'Popular open source database', 'database', '#4479A1'),
    ('MariaDB', 'mariadb', 'MySQL compatible database', 'database', '#003545'),
    ('SQLite', 'sqlite', 'Lightweight database engine', 'database', '#003B57'),
    ('Oracle', 'oracle', 'Enterprise database system', 'database', '#F80000'),
    ('SQL Server', 'sql-server', 'Microsoft database system', 'database', '#CC2927'),
    
    -- Databases - NoSQL
    ('MongoDB', 'mongodb', 'Document-oriented database', 'database', '#47A248'),
    ('Redis', 'redis', 'In-memory data structure store', 'database', '#DC382D'),
    ('Cassandra', 'cassandra', 'Distributed database', 'database', '#1287B1'),
    ('CouchDB', 'couchdb', 'Document database', 'database', '#E42528'),
    ('Neo4j', 'neo4j', 'Graph database', 'database', '#008CC1'),
    ('DynamoDB', 'dynamodb', 'AWS NoSQL database', 'database', '#FF9900'),
    ('InfluxDB', 'influxdb', 'Time series database', 'database', '#22ADF6'),
    
    -- Search Engines
    ('Elasticsearch', 'elasticsearch', 'Distributed search engine', 'search', '#005571'),
    ('Solr', 'solr', 'Enterprise search platform', 'search', '#D9411E'),
    ('Algolia', 'algolia', 'Search-as-a-service platform', 'search', '#003DFF'),
    ('MeiliSearch', 'meilisearch', 'Lightning fast search engine', 'search', '#FF5CAA'),
    
    -- Cloud Platforms
    ('AWS', 'aws', 'Amazon Web Services', 'cloud', '#FF9900'),
    ('Azure', 'azure', 'Microsoft Cloud Platform', 'cloud', '#0078D4'),
    ('GCP', 'gcp', 'Google Cloud Platform', 'cloud', '#4285F4'),
    ('DigitalOcean', 'digitalocean', 'Cloud infrastructure provider', 'cloud', '#0080FF'),
    ('Heroku', 'heroku', 'Cloud platform as a service', 'cloud', '#430098'),
    ('Vercel', 'vercel', 'Frontend cloud platform', 'cloud', '#000000'),
    ('Netlify', 'netlify', 'Web development platform', 'cloud', '#00C7B7'),
    ('Firebase', 'firebase', 'Google app development platform', 'cloud', '#FFCA28'),
    
    -- Containerization
    ('Docker', 'docker', 'Containerization platform', 'container', '#2496ED'),
    ('Kubernetes', 'kubernetes', 'Container orchestration', 'container', '#326CE5'),
    ('Podman', 'podman', 'Daemonless container engine', 'container', '#892CA0'),
    ('Docker Compose', 'docker-compose', 'Multi-container Docker applications', 'container', '#2496ED'),
    ('Helm', 'helm', 'Kubernetes package manager', 'container', '#0F1689'),
    ('Rancher', 'rancher', 'Kubernetes management platform', 'container', '#0075A8'),
    
    -- Infrastructure as Code
    ('Terraform', 'terraform', 'Infrastructure as code tool', 'iac', '#623CE4'),
    ('Pulumi', 'pulumi', 'Modern infrastructure as code', 'iac', '#8A3391'),
    ('CloudFormation', 'cloudformation', 'AWS infrastructure as code', 'iac', '#FF9900'),
    ('CDK', 'cdk', 'AWS Cloud Development Kit', 'iac', '#FF9900'),
    ('Ansible', 'ansible', 'IT automation tool', 'iac', '#EE0000'),
    ('Chef', 'chef', 'Configuration management tool', 'iac', '#F09820'),
    ('Puppet', 'puppet', 'Configuration management', 'iac', '#FFAE1A'),
    
    -- CI/CD Tools
    ('GitHub Actions', 'github-actions', 'CI/CD platform by GitHub', 'cicd', '#2088FF'),
    ('GitLab CI', 'gitlab-ci', 'GitLab continuous integration', 'cicd', '#FC6D26'),
    ('Jenkins', 'jenkins', 'Automation server', 'cicd', '#D33833'),
    ('CircleCI', 'circleci', 'Continuous integration platform', 'cicd', '#343434'),
    ('Travis CI', 'travis-ci', 'Continuous integration service', 'cicd', '#3EAAAF'),
    ('Azure DevOps', 'azure-devops', 'Microsoft DevOps solution', 'cicd', '#0078D4'),
    
    -- Version Control
    ('Git', 'git', 'Distributed version control', 'vcs', '#F05032'),
    ('GitHub', 'github', 'Git repository hosting', 'vcs', '#181717'),
    ('GitLab', 'gitlab', 'Git repository manager', 'vcs', '#FC6D26'),
    ('Bitbucket', 'bitbucket', 'Git repository hosting', 'vcs', '#0052CC')
ON CONFLICT (name) DO NOTHING;

-- Additional tags continued...
INSERT INTO tags (name, slug, description, tag_group, color) VALUES 
    -- Testing Frameworks
    ('Jest', 'jest', 'JavaScript testing framework', 'testing', '#C21325'),
    ('Cypress', 'cypress', 'End-to-end testing framework', 'testing', '#17202C'),
    ('Playwright', 'playwright', 'Cross-browser testing', 'testing', '#2EAD33'),
    ('Selenium', 'selenium', 'Web application testing', 'testing', '#43B02A'),
    ('Testing Library', 'testing-library', 'Testing utilities', 'testing', '#E33332'),
    ('Mocha', 'mocha', 'JavaScript test framework', 'testing', '#8D6748'),
    ('Chai', 'chai', 'BDD/TDD assertion library', 'testing', '#A30701'),
    ('Jasmine', 'jasmine', 'Behavior-driven testing', 'testing', '#8A4182'),
    ('pytest', 'pytest', 'Python testing framework', 'testing', '#0A9EDC'),
    ('JUnit', 'junit', 'Java testing framework', 'testing', '#25A162'),
    ('Vitest', 'vitest', 'Vite native testing framework', 'testing', '#6E9F18'),
    ('Storybook', 'storybook', 'Component development tool', 'testing', '#FF4785'),
    
    -- Mobile Development
    ('iOS', 'ios', 'Apple mobile platform', 'mobile', '#000000'),
    ('Android', 'android', 'Google mobile platform', 'mobile', '#3DDC84'),
    ('React Native', 'react-native', 'Cross-platform mobile framework', 'mobile', '#61DAFB'),
    ('Flutter', 'flutter', 'Google UI toolkit', 'mobile', '#02569B'),
    ('Ionic', 'ionic', 'Cross-platform app framework', 'mobile', '#3880FF'),
    ('Xamarin', 'xamarin', 'Microsoft mobile framework', 'mobile', '#3199DC'),
    ('Cordova', 'cordova', 'Mobile app development framework', 'mobile', '#E8E8E8'),
    ('PWA', 'pwa', 'Progressive Web Apps', 'mobile', '#5A0FC8'),
    
    -- State Management
    ('Redux', 'redux', 'Predictable state container', 'state-management', '#764ABC'),
    ('MobX', 'mobx', 'Simple, scalable state management', 'state-management', '#FF9955'),
    ('Zustand', 'zustand', 'Small, fast state management', 'state-management', '#000000'),
    ('Recoil', 'recoil', 'Experimental state management for React', 'state-management', '#007AF4'),
    ('Jotai', 'jotai', 'Primitive and flexible state management', 'state-management', '#000000'),
    ('Pinia', 'pinia', 'Vue store library', 'state-management', '#FFD859'),
    ('Vuex', 'vuex', 'State management pattern for Vue', 'state-management', '#4FC08D'),
    
    -- API & Communication
    ('REST', 'rest', 'Representational State Transfer', 'api', '#000000'),
    ('GraphQL', 'graphql', 'Query language for APIs', 'api', '#E10098'),
    ('gRPC', 'grpc', 'High performance RPC framework', 'api', '#00ADD8'),
    ('WebSocket', 'websocket', 'Real-time communication protocol', 'api', '#000000'),
    ('tRPC', 'trpc', 'End-to-end typesafe APIs', 'api', '#398CCB'),
    ('OpenAPI', 'openapi', 'API specification', 'api', '#6BA539'),
    ('Swagger', 'swagger', 'API documentation tool', 'api', '#85EA2D'),
    
    -- Architecture Patterns
    ('Microservices', 'microservices', 'Architectural style', 'architecture', '#000000'),
    ('Monolith', 'monolith', 'Single-tiered application', 'architecture', '#000000'),
    ('Serverless', 'serverless', 'Cloud execution model', 'architecture', '#FD5750'),
    ('Event-Driven', 'event-driven', 'Event-driven architecture', 'architecture', '#000000'),
    ('CQRS', 'cqrs', 'Command Query Responsibility Segregation', 'architecture', '#000000'),
    ('DDD', 'ddd', 'Domain-Driven Design', 'architecture', '#000000'),
    ('Clean Architecture', 'clean-architecture', 'Software design philosophy', 'architecture', '#000000'),
    ('Hexagonal Architecture', 'hexagonal-architecture', 'Ports and adapters pattern', 'architecture', '#000000'),
    ('MVC', 'mvc', 'Model-View-Controller', 'architecture', '#000000'),
    ('MVP', 'mvp', 'Model-View-Presenter', 'architecture', '#000000'),
    ('MVVM', 'mvvm', 'Model-View-ViewModel', 'architecture', '#000000'),
    
    -- Data & Analytics
    ('Apache Spark', 'apache-spark', 'Unified analytics engine', 'data', '#E25A1C'),
    ('Hadoop', 'hadoop', 'Distributed storage and processing', 'data', '#66CCFF'),
    ('Kafka', 'kafka', 'Distributed streaming platform', 'data', '#000000'),
    ('Apache Airflow', 'apache-airflow', 'Workflow automation', 'data', '#017CEE'),
    ('dbt', 'dbt', 'Data build tool', 'data', '#FF694B'),
    ('Pandas', 'pandas', 'Data manipulation and analysis', 'data', '#150458'),
    ('NumPy', 'numpy', 'Scientific computing with Python', 'data', '#013243'),
    ('Matplotlib', 'matplotlib', 'Python plotting library', 'data', '#11557C'),
    
    -- Machine Learning & AI
    ('TensorFlow', 'tensorflow', 'Machine learning framework', 'ai-ml', '#FF6F00'),
    ('PyTorch', 'pytorch', 'Machine learning framework', 'ai-ml', '#EE4C2C'),
    ('Scikit-learn', 'scikit-learn', 'Machine learning library', 'ai-ml', '#F7931E'),
    ('Keras', 'keras', 'Deep learning API', 'ai-ml', '#D00000'),
    ('OpenAI', 'openai', 'AI research company', 'ai-ml', '#412991'),
    ('Hugging Face', 'hugging-face', 'ML model hub', 'ai-ml', '#FFD21E'),
    ('LangChain', 'langchain', 'Framework for LLM applications', 'ai-ml', '#1C3C3C'),
    
    -- Security
    ('OAuth', 'oauth', 'Open standard for access delegation', 'security', '#000000'),
    ('JWT', 'jwt', 'JSON Web Tokens', 'security', '#000000'),
    ('SAML', 'saml', 'Security Assertion Markup Language', 'security', '#000000'),
    ('HTTPS', 'https', 'HTTP Secure', 'security', '#000000'),
    ('TLS', 'tls', 'Transport Layer Security', 'security', '#000000'),
    ('CORS', 'cors', 'Cross-Origin Resource Sharing', 'security', '#000000'),
    ('OWASP', 'owasp', 'Open Web Application Security Project', 'security', '#000000'),
    
    -- Performance & Optimization
    ('CDN', 'cdn', 'Content Delivery Network', 'performance', '#000000'),
    ('Caching', 'caching', 'Data storage technique', 'performance', '#000000'),
    ('Code Splitting', 'code-splitting', 'Bundle optimization technique', 'performance', '#000000'),
    ('Lazy Loading', 'lazy-loading', 'Loading technique', 'performance', '#000000'),
    ('Web Vitals', 'web-vitals', 'Quality signals for web', 'performance', '#4285F4'),
    ('Lighthouse', 'lighthouse', 'Web performance tool', 'performance', '#F44B21'),
    ('Service Worker', 'service-worker', 'Background script', 'performance', '#000000'),
    
    -- Development Tools
    ('VS Code', 'vscode', 'Code editor', 'tool', '#007ACC'),
    ('IntelliJ IDEA', 'intellij-idea', 'Integrated development environment', 'tool', '#000000'),
    ('Postman', 'postman', 'API development environment', 'tool', '#FF6C37'),
    ('Figma', 'figma', 'Design tool', 'tool', '#F24E1E'),
    ('Slack', 'slack', 'Team communication', 'tool', '#4A154B'),
    ('Jira', 'jira', 'Issue tracking', 'tool', '#0052CC'),
    ('Notion', 'notion', 'Workspace tool', 'tool', '#000000'),
    
    -- Monitoring & Observability
    ('Prometheus', 'prometheus', 'Monitoring system', 'monitoring', '#E6522C'),
    ('Grafana', 'grafana', 'Analytics and monitoring', 'monitoring', '#F46800'),
    ('Datadog', 'datadog', 'Monitoring and analytics', 'monitoring', '#632CA6'),
    ('New Relic', 'new-relic', 'Application performance monitoring', 'monitoring', '#008C99'),
    ('Sentry', 'sentry', 'Error tracking', 'monitoring', '#362D59'),
    ('Elastic Stack', 'elastic-stack', 'Search and analytics', 'monitoring', '#005571'),
    
    -- Messaging & Queues
    ('RabbitMQ', 'rabbitmq', 'Message broker', 'messaging', '#FF6600'),
    ('Apache Kafka', 'apache-kafka', 'Distributed streaming', 'messaging', '#000000'),
    ('AWS SQS', 'aws-sqs', 'Message queuing service', 'messaging', '#FF9900'),
    ('Redis Pub/Sub', 'redis-pubsub', 'Publish/subscribe messaging', 'messaging', '#DC382D'),
    
    -- DevOps Concepts
    ('CI/CD', 'cicd', 'Continuous Integration/Deployment', 'devops', '#000000'),
    ('Infrastructure as Code', 'iac', 'Managing infrastructure through code', 'devops', '#000000'),
    ('GitOps', 'gitops', 'Git-based deployment', 'devops', '#000000'),
    ('Blue-Green Deployment', 'blue-green-deployment', 'Deployment strategy', 'devops', '#000000'),
    ('Canary Deployment', 'canary-deployment', 'Gradual rollout strategy', 'devops', '#000000'),
    
    -- Soft Skills & Concepts
    ('Agile', 'agile', 'Software development methodology', 'methodology', '#000000'),
    ('Scrum', 'scrum', 'Agile framework', 'methodology', '#000000'),
    ('TDD', 'tdd', 'Test-Driven Development', 'methodology', '#000000'),
    ('BDD', 'bdd', 'Behavior-Driven Development', 'methodology', '#000000'),
    ('Code Review', 'code-review', 'Code examination process', 'methodology', '#000000'),
    ('Pair Programming', 'pair-programming', 'Collaborative programming', 'methodology', '#000000'),
    ('Technical Debt', 'technical-debt', 'Future cost of rework', 'concept', '#000000'),
    ('Refactoring', 'refactoring', 'Code restructuring', 'concept', '#000000')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- 유용한 뷰 생성
-- ============================================

-- 포스트와 태그 정보를 함께 보는 뷰
CREATE OR REPLACE VIEW post_with_tags AS
SELECT 
    p.id,
    p.title,
    p.author,
    p.published_at,
    p.original_url,
    b.name as blog_name,
    b.company,
    ARRAY_AGG(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL) as tags,
    ARRAY_AGG(DISTINCT c.name) FILTER (WHERE c.name IS NOT NULL) as categories
FROM posts p
LEFT JOIN blog b ON p.blog_id = b.id
LEFT JOIN post_tags pt ON p.id = pt.post_id
LEFT JOIN tags t ON pt.tag_id = t.id
LEFT JOIN post_categories pc ON p.id = pc.post_id
LEFT JOIN categories c ON pc.category_id = c.id
GROUP BY p.id, p.title, p.author, p.published_at, p.original_url, b.name, b.company;

-- 태깅 통계 뷰
CREATE OR REPLACE VIEW tagging_stats AS
SELECT 
    COUNT(*) as total_posts,
    COUNT(DISTINCT pt.post_id) as tagged_posts,
    COUNT(DISTINCT pc.post_id) as categorized_posts,
    COUNT(DISTINCT COALESCE(pt.post_id, pc.post_id)) as fully_tagged_posts,
    (COUNT(*) - COUNT(DISTINCT COALESCE(pt.post_id, pc.post_id))) as untagged_posts,
    ROUND(
        COALESCE(COUNT(DISTINCT COALESCE(pt.post_id, pc.post_id))::DECIMAL / NULLIF(COUNT(*), 0), 0) * 100, 2
    ) as tagging_completion_rate
FROM posts p
LEFT JOIN post_tags pt ON p.id = pt.post_id
LEFT JOIN post_categories pc ON p.id = pc.post_id;

-- ============================================
-- 성능 최적화를 위한 추가 인덱스 (선택적)
-- ============================================

-- 복합 인덱스 (검색 성능 향상)
CREATE INDEX IF NOT EXISTS idx_posts_blog_published ON posts(blog_id, published_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_published_desc ON posts(published_at DESC);

-- 태깅 관련 복합 인덱스
CREATE INDEX IF NOT EXISTS idx_post_tags_composite ON post_tags(tag_id, post_id);
CREATE INDEX IF NOT EXISTS idx_post_categories_composite ON post_categories(category_id, post_id);

-- ============================================
-- 권한 설정 (선택적 - 필요에 따라 수정)
-- ============================================

-- 애플리케이션 사용자 권한 부여 (사용자명은 실제 환경에 맞게 수정)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO techbloghub_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO techbloghub_app;

-- ============================================
-- 스키마 검증 쿼리
-- ============================================

-- 테이블 존재 확인
SELECT schemaname, tablename 
FROM pg_tables 
WHERE schemaname = 'public' 
    AND tablename IN ('blog', 'posts', 'tags', 'categories', 'post_tags', 'post_categories')
ORDER BY tablename;

-- 인덱스 확인
SELECT schemaname, tablename, indexname 
FROM pg_indexes 
WHERE schemaname = 'public' 
    AND tablename IN ('blog', 'posts', 'tags', 'categories', 'post_tags', 'post_categories')
ORDER BY tablename, indexname;

-- 외래키 제약조건 확인
SELECT
    tc.table_name, 
    tc.constraint_name, 
    tc.constraint_type,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name 
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY' 
    AND tc.table_schema='public'
    AND tc.table_name IN ('posts', 'post_tags', 'post_categories')
ORDER BY tc.table_name;

COMMENT ON TABLE blog IS '블로그 정보 테이블';
COMMENT ON TABLE posts IS '블로그 포스트 테이블';
COMMENT ON TABLE tags IS '태그 마스터 테이블';
COMMENT ON TABLE categories IS '카테고리 마스터 테이블';
COMMENT ON TABLE post_tags IS '포스트-태그 다대다 관계 테이블';
COMMENT ON TABLE post_categories IS '포스트-카테고리 다대다 관계 테이블';

-- ============================================
-- 거부된 태그 추적 테이블 (신규)
-- ============================================

-- 8. 거부된 태그 추적 테이블
CREATE TABLE IF NOT EXISTS rejected_tags (
    id SERIAL PRIMARY KEY,
    tag_name VARCHAR(100) NOT NULL,
    post_id INTEGER REFERENCES posts(id) ON DELETE CASCADE,
    post_title VARCHAR(500),
    post_url VARCHAR(1023),
    blog_name VARCHAR(255),
    rejected_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    frequency_count INTEGER DEFAULT 1,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, IGNORED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_rejected_tag_status CHECK (status IN ('PENDING', 'APPROVED', 'IGNORED'))
);

-- 거부된 태그 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_rejected_tags_name ON rejected_tags(tag_name);
CREATE INDEX IF NOT EXISTS idx_rejected_tags_frequency ON rejected_tags(frequency_count DESC);
CREATE INDEX IF NOT EXISTS idx_rejected_tags_status ON rejected_tags(status);
CREATE INDEX IF NOT EXISTS idx_rejected_tags_rejected_at ON rejected_tags(rejected_at DESC);
CREATE INDEX IF NOT EXISTS idx_rejected_tags_post_id ON rejected_tags(post_id);

-- 거부된 카테고리 추적 테이블  
CREATE TABLE IF NOT EXISTS rejected_categories (
    id SERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    post_id INTEGER REFERENCES posts(id) ON DELETE CASCADE,
    post_title VARCHAR(500),
    post_url VARCHAR(1023),
    blog_name VARCHAR(255),
    rejected_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    frequency_count INTEGER DEFAULT 1,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, IGNORED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_rejected_category_status CHECK (status IN ('PENDING', 'APPROVED', 'IGNORED'))
);

-- 거부된 카테고리 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_rejected_categories_name ON rejected_categories(category_name);
CREATE INDEX IF NOT EXISTS idx_rejected_categories_frequency ON rejected_categories(frequency_count DESC);
CREATE INDEX IF NOT EXISTS idx_rejected_categories_status ON rejected_categories(status);
CREATE INDEX IF NOT EXISTS idx_rejected_categories_rejected_at ON rejected_categories(rejected_at DESC);

-- ============================================
-- 거부된 태그/카테고리 통계 뷰
-- ============================================

-- 거부된 태그 통계 뷰
CREATE OR REPLACE VIEW rejected_tag_stats AS
SELECT 
    tag_name,
    COUNT(*) as occurrence_count,
    SUM(frequency_count) as total_frequency,
    MIN(rejected_at) as first_seen,
    MAX(rejected_at) as last_seen,
    status,
    ARRAY_AGG(DISTINCT blog_name) FILTER (WHERE blog_name IS NOT NULL) as blogs,
    COUNT(DISTINCT post_id) as unique_posts
FROM rejected_tags
GROUP BY tag_name, status
ORDER BY total_frequency DESC, occurrence_count DESC;

-- 최근 거부된 태그 트렌드 뷰  
CREATE OR REPLACE VIEW recent_rejected_tags AS
SELECT 
    tag_name,
    COUNT(*) as recent_count,
    SUM(frequency_count) as total_frequency,
    MAX(rejected_at) as last_rejected
FROM rejected_tags
WHERE rejected_at >= NOW() - INTERVAL '7 days'
    AND status = 'PENDING'
GROUP BY tag_name
HAVING COUNT(*) >= 2  -- 최근 7일 내 2회 이상 거부
ORDER BY total_frequency DESC, recent_count DESC;

-- 승인 후보 태그 뷰
CREATE OR REPLACE VIEW tag_approval_candidates AS
SELECT 
    tag_name,
    SUM(frequency_count) as total_frequency,
    COUNT(DISTINCT post_id) as unique_posts,
    COUNT(DISTINCT blog_name) as unique_blogs,
    MIN(rejected_at) as first_seen,
    MAX(rejected_at) as last_seen
FROM rejected_tags
WHERE status = 'PENDING'
GROUP BY tag_name
HAVING SUM(frequency_count) >= 5  -- 5회 이상 등장한 태그들
ORDER BY total_frequency DESC;

-- 완료 메시지
SELECT 'TechBlogHub Database Schema with Rejected Tags Tracking Created Successfully!' as status;