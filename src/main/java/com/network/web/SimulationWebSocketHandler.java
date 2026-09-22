package com.network.web;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimulationWebSocketHandler extends TextWebSocketHandler {
    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private static boolean intercepted = false;

    public SimulationWebSocketHandler() {
        if (!intercepted) {
            intercepted = true;
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(originalOut) {
                @Override
                public void println(String x) {
                    super.println(x);
                    broadcast(x);
                }
                @Override
                public void print(String x) {
                    super.print(x);
                    broadcast(x);
                }
            });
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session);
    }

    public static void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}