package com.marketplace.web;

import com.marketplace.AgentTurnService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {

  private static final Logger log = LoggerFactory.getLogger(AgentController.class);

  private final AgentTurnService agent;

  public AgentController(AgentTurnService agent) {
    this.agent = agent;
  }

  @SuppressWarnings("unchecked")
  @PostMapping("/api/agent/turn")
  public ResponseEntity<?> turn(@RequestBody Map<String, Object> body) {
    try {
      var messages = (List<Map<String, Object>>) body.getOrDefault("messages", List.of());
      var tools = (List<Map<String, Object>>) body.getOrDefault("tools", List.of());
      return ResponseEntity.ok(agent.oneTurn(messages, tools));
    } catch (Exception e) {
      log.warn("agent turn failed: {}", e.getMessage());
      return ResponseEntity.status(503).body(Map.of("error", "agent_upstream"));
    }
  }
}
