package com.example.webchatserver;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.annotation.WebServlet;

/**
 * This is a class that has services.
 * In our case, we are using this to generate unique room IDs.
 */
@WebServlet(name = "chat-servlet", value = "/chat-servlet/*")
public class ChatServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Set<String> rooms = new HashSet<>();
    private static final SecureRandom random = new SecureRandom();

    /**
     * Method generates unique room codes.
     */
    private static String generateRandomRoomCode() {
        byte[] bytes = new byte[3]; // 3 bytes = 4 characters
        random.nextBytes(bytes);
        String code = bytesToHexString(bytes).toUpperCase();
        while (rooms.contains(code)) {
            random.nextBytes(bytes);
            code = bytesToHexString(bytes).toUpperCase();
        }
        rooms.add(code);
        return code;
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUrl = request.getRequestURI();
        if (requestUrl.endsWith("/rooms")) {
            response.setContentType("application/json");
            response.getWriter().write(new Gson().toJson(rooms));
        } else {
            response.setContentType("text/plain");
            response.getWriter().write(generateRandomRoomCode());
        }
    }
}
