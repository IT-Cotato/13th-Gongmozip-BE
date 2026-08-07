package org.cotato.gongmozip.global.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.test.annotation.DirtiesContext
class AiGatewayClientIntegrationTest {

    static {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("build"));
            try (PrintWriter writer = new PrintWriter(new FileWriter("build/env_debug.txt", true))) {
                writer.println("=== .env Loading Debug Start ===");
                String userDir = System.getProperty("user.dir");
                writer.println("user.dir: " + userDir);

                java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
                writer.println("Checking .env path: " + envPath.toAbsolutePath());

                if (!java.nio.file.Files.exists(envPath)) {
                    envPath = java.nio.file.Paths.get("../.env");
                    writer.println("Checking parent .env path: " + envPath.toAbsolutePath());
                }

                if (java.nio.file.Files.exists(envPath)) {
                    writer.println(".env file FOUND!");
                    List<String> lines = java.nio.file.Files.readAllLines(envPath);
                    for (String line : lines) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                            int index = line.indexOf("=");
                            String key = line.substring(0, index).trim();
                            String value = line.substring(index + 1).trim();
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            } else if (value.startsWith("'") && value.endsWith("'")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            System.setProperty(key, value);
                            writer.println("Loaded System Property: " + key);
                        }
                    }
                } else {
                    writer.println(".env file NOT FOUND anywhere!");
                }
                writer.println("=== .env Loading Debug End ===");
            }
        } catch (Exception e) {
            System.err.println("통합 테스트에서 .env 디버그 기록 에러: " + e.getMessage());
        }
    }

    @Autowired
    private AiGatewayClient aiGatewayClient;

    @DisplayName("환경변수/System Property에 API 키가 설정되어 있으면 실제 팩트챗 API Gateway를 호출해 응답을 검증한다.")
    @Test
    void testRealGatewayCall() {
        if (!aiGatewayClient.isEnabled()) {
            System.out.println("\n=======================================================");
            System.out.println(" [경고] FACTCHAT_API_KEY가 설정되지 않아 실제 호출 테스트를 건너뜁니다.");
            System.out.println("=======================================================\n");
            return;
        }

        System.out.println("\n=======================================================");
        System.out.println(" 팩트챗 API Gateway 실제 호출 테스트를 시작합니다...");
        System.out.println("=======================================================");

        String prompt = "너는 아주 친절하고 공손하게 딱 한 문장으로 대답하는 로봇이야. '공모전 파이팅'이라고 힘차게 격려해줘.";
        String response = aiGatewayClient.generateContent(prompt);

        System.out.println("\n[실제 AI Gateway 응답 결과]");
        System.out.println("-> " + response);
        System.out.println("=======================================================\n");

        assertThat(response).isNotBlank();
    }
}
