package com.gemini.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final ChatClient chatClient;

    // 暫時用 Map 存每個 session 的對話紀錄
    private final Map<String, List<Map<String, String>>> sessions = new ConcurrentHashMap<>();

    // 開始面試
    @PostMapping("/start")
    public String start(@RequestBody String role) {
        String sessionId = java.util.UUID.randomUUID().toString();
        sessions.put(sessionId, new ArrayList<>());

        String prompt = String.format("""
                你是一位嚴格但公正的技術面試官，專門面試 %s 職位。
                
                請開始模擬面試，直接提出第一個技術問題。
                格式如下：
                
                模擬面試開始！職位：%s
                
                Q1：
                [提出一個適合該職位的技術問題]
                
                請等待應試者回答。
                """, role, role);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // 把第一題存進對話紀錄
        sessions.get(sessionId).add(Map.of("role", "assistant", "content", response));

        return sessionId + "|||" + response;
    }

    // 回答問題，取得評分和下一題
    @PostMapping("/answer")
    public String answer(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        String userAnswer = body.get("answer");
        String currentQuestion = body.get("currentQuestion");

        List<Map<String, String>> history = sessions.getOrDefault(sessionId, new ArrayList<>());

        String prompt = String.format("""
                面試官剛才的問題是：
                %s
                
                應試者的回答是：
                %s
                
                請用以下格式評分，然後出下一題：
                
                📊 評分：[分數]/100
                
                ✅ 優點：
                - [列出回答的優點]
                
                ❌ 缺點：
                - [列出需要改進的地方]
                
                💡 參考答案重點：
                - [簡短說明理想答案的關鍵點]
                
                ---
                
                Q[下一題編號]：
                [提出下一個技術問題]
                
                請等待應試者回答。
                """, currentQuestion, userAnswer);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        history.add(Map.of("role", "user", "content", userAnswer));
        history.add(Map.of("role", "assistant", "content", response));

        return response;
    }
}