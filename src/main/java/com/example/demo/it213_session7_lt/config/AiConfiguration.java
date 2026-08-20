package com.example.demo.it213_session7_lt.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

    static final String SYSTEM_PROMPT = """
            Bạn là trợ lý chăm sóc khách hàng của LapTech.
            Chỉ trả lời bằng thông tin có trong ngữ cảnh tài liệu được truy xuất.
            Không dùng kiến thức bên ngoài, không suy đoán và không tự tạo chính sách, giá hay thông tin cửa hàng.
            Nếu ngữ cảnh không chứa đủ dữ liệu để trả lời, hãy nói rõ:
            \"Xin lỗi, tài liệu LapTech hiện chưa có thông tin này.\"
            Trả lời bằng tiếng Việt, ngắn gọn, chính xác; nêu mục chính sách liên quan khi có thể.
            """;

    @Bean
    ChatMemory chatMemory(RagProperties properties) {
        return MessageWindowChatMemory.builder()
                .maxMessages(properties.maxMemoryMessages())
                .build();
    }

    @Bean
    ChatClient lapTechChatClient(ChatClient.Builder builder,
                                 VectorStore vectorStore,
                                 ChatMemory chatMemory,
                                 RagProperties properties) {
        var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        var searchRequest = SearchRequest.builder()
                .topK(properties.topK())
                .similarityThreshold(properties.similarityThreshold())
                .build();
        var questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(memoryAdvisor, questionAnswerAdvisor)
                .build();
    }
}
