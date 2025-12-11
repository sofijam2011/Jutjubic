package com.example.jutjubic.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class HelloController {

    private List<String> messages = new ArrayList<>();

    // GET - Vrati sve poruke
    @GetMapping("/messages")
    public List<String> getMessages() {
        return messages;
    }

    // POST - Dodaj novu poruku
    @PostMapping("/messages")
    public Map<String, String> addMessage(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        messages.add(message);
        return Map.of("status", "success", "message", message);
    }

    // GET - Test endpoint
    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hello from Spring Boot!");
    }
}