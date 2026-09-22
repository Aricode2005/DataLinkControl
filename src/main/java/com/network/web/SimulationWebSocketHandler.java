package com.network.web;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimulationWebSocketHandler extends TextWebSocketHandler {
    // Maps a SessionCode (Integer) -> Sender Session, Receiver Session
    public static final Map<Integer, WebSocketSession> senderSessions = new ConcurrentHashMap<>();
    public static final Map<Integer, WebSocketSession> receiverSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // We will register them when they send their first message indicating role and sessionCode
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        // payload format: "REGISTER:ROLE:SESSIONCODE"
        if (payload.startsWith("REGISTER:SENDER:")) {
            int code = Integer.parseInt(payload.split(":")[2]);
            senderSessions.put(code, session);
        } else if (payload.startsWith("REGISTER:RECEIVER:")) {
            int code = Integer.parseInt(payload.split(":")[2]);
            receiverSessions.put(code, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        senderSessions.values().remove(session);
        receiverSessions.values().remove(session);
    }

    public static void sendToSender(int sessionCode, String message) {
        WebSocketSession session = senderSessions.get(sessionCode);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {}
        }
    }

    public static void sendToReceiver(int sessionCode, String message) {
        WebSocketSession session = receiverSessions.get(sessionCode);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {}
        }
    }
}