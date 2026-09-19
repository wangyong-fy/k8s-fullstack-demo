package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class MessageController {

    private final MessageRepository repository;

    @Value("${HOSTNAME:unknown}")
    private String hostname;

    public MessageController(MessageRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/messages")
    public List<Message> list() {
        return repository.findAllByOrderByIdDesc();
    }

    @PostMapping("/messages")
    public Message create(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("content 不能为空");
        }
        return repository.save(new Message(content));
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "UP");
        m.put("pod", hostname);
        m.put("count", repository.count());
        return m;
    }
}
