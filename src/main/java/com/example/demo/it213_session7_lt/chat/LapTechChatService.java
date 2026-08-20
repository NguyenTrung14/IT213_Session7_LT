package com.example.demo.it213_session7_lt.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class LapTechChatService {

    private final ChatClient chatClient;

    public LapTechChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResponse chat(ChatRequest request) {
        String answer = chatClient.prompt()
                .user(request.message().strip())
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID, request.sessionId().strip()))
                .call()
                .content();

        if (answer == null || answer.isBlank()) {
            answer = "Xin lỗi, tài liệu LapTech hiện chưa có thông tin này.";
        }
        return new ChatResponse(request.sessionId().strip(), answer);
    }
}
