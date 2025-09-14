package com.techbloghub.output.gpt.config;

import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static io.micrometer.observation.ObservationRegistry.NOOP;
import static org.springframework.ai.retry.RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;
import static org.springframework.ai.retry.RetryUtils.DEFAULT_RETRY_TEMPLATE;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

@Configuration
public class OpenAiConfig {

    private static final String BASE_URL = "https://api.openai.com";
    private static final String COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String EMBEDDINGS_PATH = "/v1/embeddings";

    @Value("${gpt.openai.api}")
    private String openAiApiKey;
    
    @Bean
    OpenAiApi openAiApi() {
        ApiKey apiKey = new SimpleApiKey(openAiApiKey);
        return new OpenAiApi(
                BASE_URL,
                apiKey, 
                toMultiValueMap(Map.of()),
                COMPLETIONS_PATH,
                EMBEDDINGS_PATH,
                RestClient.builder(),
                WebClient.builder(),
                DEFAULT_RESPONSE_ERROR_HANDLER);
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.1)
                .build();

        return new OpenAiChatModel(
                openAiApi,
                options,
                DefaultToolCallingManager.builder().build(),
                DEFAULT_RETRY_TEMPLATE,
                NOOP);
    }
}