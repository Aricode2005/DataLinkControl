package com.network.web;

import com.network.web.SimulationWebSocketHandler;
import com.network.util.logger.Logger;

public class WebSocketLogger implements Logger {
    private final int sessionCode;
    private final boolean isSender;

    public WebSocketLogger(int sessionCode, boolean isSender) {
        this.sessionCode = sessionCode;
        this.isSender = isSender;
    }

    @Override
    public void log(String message) {
        System.out.println(message); // Still print to server console for debugging
        if (isSender) {
            SimulationWebSocketHandler.sendToSender(sessionCode, message);
        } else {
            SimulationWebSocketHandler.sendToReceiver(sessionCode, message);
        }
    }
}