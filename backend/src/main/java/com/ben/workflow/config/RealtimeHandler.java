package com.ben.workflow.config;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * WebSocket 实时消息处理器 (WebFlux 版本)
 * 广播所有收到的消息到所有连接的客户端
 */
public class RealtimeHandler implements WebSocketHandler {

    private final Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 接收客户端消息并广播
        Mono<Void> input = session.receive()
                .map(msg -> msg.getPayloadAsText())
                .doOnNext(text -> sink.tryEmitNext(text))
                .then();

        // 将广播的消息发送给客户端
        Mono<Void> output = session.send(
                sink.asFlux().map(session::textMessage)
        );

        return Mono.zip(input, output).then();
    }
}
