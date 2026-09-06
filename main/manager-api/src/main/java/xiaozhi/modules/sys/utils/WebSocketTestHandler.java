package xiaozhi.modules.sys.utils;

import java.util.concurrent.CompletableFuture;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public class WebSocketTestHandler implements WebSocketHandler {
    private final CompletableFuture<Boolean> future;

    public WebSocketTestHandler(CompletableFuture<Boolean> future) {
        this.future = future;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        future.complete(true);
        try {
            session.close();
        } catch (Exception e) {
            // Игнорировать исключение закрытия
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        // Обработка сообщений не требуется
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        future.complete(false);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        // Ничего не делать, когда соединение закрыто
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}