package com.gemini.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    private final Map<String, List<Map<String, String>>> sessions = new ConcurrentHashMap<>();

    @PostMapping("/start")
    public String start(@RequestBody String role) {
        String sessionId = java.util.UUID.randomUUID().toString();
        sessions.put(sessionId, new ArrayList<>());

        // 從題庫搜尋這個職位的相關題目
        List<Document> relatedQuestions = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(role + " 面試題目")
                .topK(5)
                .build()
        );

        String questionBank = relatedQuestions.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n"));

        String prompt = String.format("""
                你是一位嚴格但公正的技術面試官，專門面試 %s 職位。
                
                以下是題庫中的參考題目，請參考這些題目的風格和難度出題，
                但不要直接複製，可以調整或延伸：
                
                %s
                
                請開始模擬面試，直接提出第一個技術問題。
                格式如下：
                
                模擬面試開始！職位：%s
                
                Q1：
                [提出一個適合該職位的技術問題]
                
                請等待應試者回答。
                """, role, questionBank, role);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        sessions.get(sessionId).add(Map.of("role", "assistant", "content", response));

        return sessionId + "|||" + response;
    }

    @PostMapping("/answer")
    public String answer(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        String userAnswer = body.get("answer");
        String currentQuestion = body.get("currentQuestion");

        List<Map<String, String>> history = sessions.getOrDefault(sessionId, new ArrayList<>());

        // 從題庫搜尋這題的相關答案重點
        List<Document> relatedDocs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(currentQuestion)
                .topK(2)
                .build()
        );

        String referencePoints = relatedDocs.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n"));

        String prompt = String.format("""
                面試官剛才的問題是：
                %s
                
                應試者的回答是：
                %s
                
                以下是題庫中的參考答案重點，請根據這些評分：
                %s
                
                請用以下格式評分，然後出下一題：
                
                📊 評分：[分數]/100
                
                ✅ 優點：
                - [列出回答的優點]
                
                ❌ 缺點：
                - [列出需要改進的地方]
                
                💡 參考答案重點：
                - [根據題庫整理理想答案的關鍵點]
                
                ---
                
                Q[下一題編號]：
                [提出下一個技術問題]
                
                請等待應試者回答。
                """, currentQuestion, userAnswer, referencePoints);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        history.add(Map.of("role", "user", "content", userAnswer));
        history.add(Map.of("role", "assistant", "content", response));

        return response;
    }
}