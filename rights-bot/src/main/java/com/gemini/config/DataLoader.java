package com.gemini.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final VectorStore vectorStore;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(
            new ClassPathResource("interview-questions.json").getInputStream()
        );

        List<Document> documents = new ArrayList<>();

        for (JsonNode node : rootNode) {
            String role = node.get("role").asText();
            String category = node.get("category").asText();
            String question = node.get("question").asText();
            String keyPoints = node.get("keyPoints").asText();

            String content = String.format(
                "職位：%s\n分類：%s\n面試題目：%s\n關鍵答案重點：%s",
                role, category, question, keyPoints
            );

            Document doc = new Document(
                content,
                Map.of("role", role, "category", category)
            );
            documents.add(doc);
        }

        vectorStore.add(documents);
        log.info("已載入 {} 筆面試題目進向量資料庫", documents.size());
    }
}