package com.techbloghub.output.gpt.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 미리 정의된 태그와 카테고리 정보를 관리하는 클래스
 */
public class TagDefinitions {
    
    // 미리 정의된 카테고리
    public static final List<String> PREDEFINED_CATEGORIES = List.of(
        "Frontend", "Backend", "Mobile", "AI/ML", "Data", "DevOps",
        "Infrastructure", "Security", "Test", "Architecture",
        "Product", "Culture", "Career"
    );
    
    // 미리 정의된 태그 (그룹별)
    public static final Map<String, List<String>> PREDEFINED_TAGS = Map.ofEntries(
        Map.entry("language", List.of(
            "JavaScript", "TypeScript", "Python", "Java", "Go", "Rust", "Swift", "Kotlin",
            "C++", "C#", "PHP", "Ruby", "Scala", "Clojure", "Elixir", "Haskell", "Dart", "R", "Shell", "C"
        )),
        Map.entry("frontend-framework", List.of(
            "React", "Vue", "Angular", "Svelte", "Solid", "Next.js", "Nuxt.js", "SvelteKit",
            "Remix", "Gatsby", "Astro"
        )),
        Map.entry("backend-framework", List.of(
            "Node.js", "Express", "Fastify", "Koa", "NestJS", "Hapi", "Django", "FastAPI",
            "Flask", "Tornado", "Pyramid", "Starlette", "Spring Boot", "Spring", "Quarkus",
            "Micronaut", "Gin", "Echo", "Fiber", "Chi", "Laravel", "Rails", "ASP.NET", "Phoenix"
        )),
        Map.entry("database", List.of(
            "PostgreSQL", "MySQL", "MariaDB", "SQLite", "Oracle", "SQL Server", "MongoDB",
            "Redis", "Cassandra", "CouchDB", "Neo4j", "DynamoDB", "InfluxDB"
        )),
        Map.entry("cloud", List.of(
            "AWS", "Azure", "GCP", "DigitalOcean", "Heroku", "Vercel", "Netlify", "Firebase"
        )),
        Map.entry("container", List.of(
            "Docker", "Kubernetes", "Podman", "Docker Compose", "Helm", "Rancher"
        )),
        Map.entry("testing", List.of(
            "Jest", "Cypress", "Playwright", "Selenium", "Testing Library", "Mocha",
            "Chai", "Jasmine", "pytest", "JUnit", "Vitest", "Storybook"
        )),
        Map.entry("styling", List.of(
            "CSS", "SCSS", "Sass", "Less", "Tailwind CSS", "Styled Components", "Emotion",
            "CSS-in-JS", "Bootstrap", "Chakra UI"
        )),
        Map.entry("build-tool", List.of(
            "Webpack", "Vite", "Rollup", "Parcel", "esbuild", "Turbopack", "Grunt", "Gulp"
        )),
        Map.entry("mobile", List.of(
            "iOS", "Android", "React Native", "Flutter", "Ionic", "Xamarin", "Cordova", "PWA"
        )),
        Map.entry("api", List.of(
            "REST", "GraphQL", "gRPC", "WebSocket", "tRPC", "OpenAPI", "Swagger"
        )),
        Map.entry("architecture", List.of(
            "Microservices", "Monolith", "Serverless", "Event-Driven", "CQRS", "DDD",
            "Clean Architecture", "Hexagonal Architecture", "MVC", "MVP", "MVVM"
        )),
        Map.entry("security", List.of(
            "OAuth", "JWT", "SAML", "HTTPS", "TLS", "CORS", "OWASP"
        )),
        Map.entry("performance", List.of(
            "CDN", "Caching", "Code Splitting", "Lazy Loading", "Web Vitals", "Lighthouse", "Service Worker"
        ))
    );
    
    // 태그 변형 매핑
    public static final Map<String, String> TAG_VARIATIONS = Map.ofEntries(
        Map.entry("js", "JavaScript"),
        Map.entry("javascript", "JavaScript"),
        Map.entry("reactjs", "React"),
        Map.entry("react.js", "React"),
        Map.entry("vuejs", "Vue"),
        Map.entry("vue.js", "Vue"),
        Map.entry("angularjs", "Angular"),
        Map.entry("angular.js", "Angular"),
        Map.entry("nodejs", "Node.js"),
        Map.entry("node", "Node.js"),
        Map.entry("nextjs", "Next.js"),
        Map.entry("nuxtjs", "Nuxt.js"),
        Map.entry("django", "Django"),
        Map.entry("flask", "Flask"),
        Map.entry("fastapi", "FastAPI"),
        Map.entry("express.js", "Express"),
        Map.entry("expressjs", "Express"),
        Map.entry("spring-boot", "Spring Boot"),
        Map.entry("springboot", "Spring Boot"),
        Map.entry("postgres", "PostgreSQL"),
        Map.entry("psql", "PostgreSQL"),
        Map.entry("mysql", "MySQL"),
        Map.entry("mongodb", "MongoDB"),
        Map.entry("mongo", "MongoDB"),
        Map.entry("redis", "Redis"),
        Map.entry("amazon-web-services", "AWS"),
        Map.entry("amazon web services", "AWS"),
        Map.entry("google-cloud", "GCP"),
        Map.entry("google cloud platform", "GCP"),
        Map.entry("microsoft-azure", "Azure"),
        Map.entry("docker", "Docker"),
        Map.entry("k8s", "Kubernetes"),
        Map.entry("kubernetes", "Kubernetes"),
        Map.entry("typescript", "TypeScript"),
        Map.entry("ts", "TypeScript"),
        Map.entry("python", "Python"),
        Map.entry("java", "Java"),
        Map.entry("golang", "Go"),
        Map.entry("go-lang", "Go"),
        Map.entry("c++", "C++"),
        Map.entry("cpp", "C++"),
        Map.entry("c#", "C#"),
        Map.entry("csharp", "C#"),
        Map.entry("c-sharp", "C#")
    );
    
    /**
     * 모든 미리 정의된 태그를 하나의 Set으로 반환
     */
    public static Set<String> getAllPredefinedTags() {
        return PREDEFINED_TAGS.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toSet());
    }
    
    /**
     * 태그 변형을 표준 태그로 매핑
     */
    public static String normalizeTag(String tag) {
        return TAG_VARIATIONS.getOrDefault(tag.toLowerCase(), tag);
    }
    
    /**
     * 태그가 미리 정의된 목록에 있는지 확인
     */
    public static boolean isPredefinedTag(String tag) {
        return getAllPredefinedTags().contains(tag);
    }
    
    /**
     * 카테고리가 미리 정의된 목록에 있는지 확인
     */
    public static boolean isPredefinedCategory(String category) {
        return PREDEFINED_CATEGORIES.contains(category);
    }
}