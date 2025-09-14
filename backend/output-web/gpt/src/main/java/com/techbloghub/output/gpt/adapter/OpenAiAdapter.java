package com.techbloghub.output.gpt.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techbloghub.domain.model.TaggingResult;
import com.techbloghub.domain.port.out.LlmTaggerPort;
import com.techbloghub.output.gpt.template.PromptTemplate;
import com.techbloghub.output.gpt.template.SchemeTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * OpenAI API를 사용한 LLM 태깅 어댑터
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiAdapter implements LlmTaggerPort {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Override
    public TaggingResult tagContent(String title, String content) {

        // 빈 콘텐츠 검증
        if ((title == null || title.trim().isEmpty()) &&
                (content == null || content.trim().isEmpty())) {
            log.warn("No content to analyze");
            return new TaggingResult(List.of(), List.of(), List.of(), List.of());
        }

        String prompt = PromptTemplate.getTaggingPrompt(title, content);
        String scheme = SchemeTemplate.getTaggingScheme();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, scheme))
                .build();

        String output = Optional.of(new Prompt(prompt, options))
                .map(chatModel::call)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AbstractMessage::getText)
                .orElse(null);

        try {
            return objectMapper.readValue(output, TaggingResult.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}