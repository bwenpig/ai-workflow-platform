package com.ben.workflow.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * 消息发送控制器
 * 供 WxPushExecutor 调用，间接使用 OpenClaw 发送微信消息
 */
@RestController
@RequestMapping("/api/v1/message")
public class MessageController {

    private final WebClient webClient;

    public MessageController() {
        this.webClient = WebClient.builder()
                .baseUrl("http://127.0.0.1:18789")
                .build();
    }

    // 当前用户微信 ID（从之前测试获取）
    private static final String CURRENT_USER_WECHAT_ID = "o9cq80xfLmqZmyjlJhAhj9dExpOM@im.wechat";
    
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        String to = (String) request.getOrDefault("to", "current_user");
        
        // 解析实际的接收者
        String target = "current_user".equals(to) ? CURRENT_USER_WECHAT_ID : to;
        
        try {
            // 使用 curl 调用 OpenClaw 的 WebSocket 消息发送
            // 通过 exec 方式调用 message tool
            ProcessBuilder pb = new ProcessBuilder("openclaw", "message", "send",
                "--channel", "openclaw-weixin",
                "--target", target,
                "--message", content);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            int exitCode = p.waitFor();
            String output = new String(p.getInputStream().readAllBytes());
            
            if (exitCode == 0 || output.contains("messageId")) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "channel", "openclaw-weixin",
                    "messageId", "msg_" + System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", output
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}