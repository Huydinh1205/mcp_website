package com.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Server-side OpenAI plumbing. One chat-completions turn with tool calling at
 * temperature 0. The API key stays here (Constitution: keys server-side only).
 * Port of oneTurn() in lib/llm.ts.
 */
@Service
public class AgentTurnService {

  private final AppProps props;
  private final ObjectMapper json = new ObjectMapper();
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public AgentTurnService(AppProps props) {
    this.props = props;
  }

  /** messages/tools are the raw JSON shapes the browser harness sends. */
  public Map<String, Object> oneTurn(List<Map<String, Object>> messages, List<Map<String, Object>> tools)
      throws Exception {
    String key = props.openaiApiKey();
    if (key == null || key.isBlank()) throw new IllegalStateException("OPENAI_API_KEY not set");
    String model = props.openaiModel() == null ? "gpt-4o-mini" : props.openaiModel();

    ObjectNode body = json.createObjectNode();
    body.put("model", model);
    body.put("temperature", 0);
    ArrayNode msgs = body.putArray("messages");
    for (var m : messages) {
      ObjectNode mn = msgs.addObject();
      mn.put("role", String.valueOf(m.getOrDefault("role", "user")));
      mn.put("content", m.get("content") == null ? "" : String.valueOf(m.get("content")));
      Object tcid = m.get("toolCallId");
      if (tcid != null) mn.put("tool_call_id", String.valueOf(tcid));
    }
    if (tools != null && !tools.isEmpty()) {
      ArrayNode ts = body.putArray("tools");
      for (var t : tools) {
        ObjectNode fn = ts.addObject();
        fn.put("type", "function");
        ObjectNode f = fn.putObject("function");
        f.put("name", String.valueOf(t.get("name")));
        f.put("description", String.valueOf(t.getOrDefault("description", "")));
        Object params = t.get("parameters");
        f.set("parameters", params == null
            ? json.createObjectNode().put("type", "object")
            : json.valueToTree(params));
      }
      body.put("tool_choice", "auto");
    }

    JsonNode out = post(key, body);
    JsonNode msg = out.path("choices").path(0).path("message");
    List<Map<String, Object>> toolCalls = new ArrayList<>();
    for (JsonNode tc : msg.path("tool_calls")) {
      if (!"function".equals(tc.path("type").asText("function"))) continue;
      Object args;
      try {
        args = json.readValue(tc.path("function").path("arguments").asText("{}"), Object.class);
      } catch (Exception e) {
        args = Map.of();
      }
      toolCalls.add(Map.of(
          "id", tc.path("id").asText(),
          "name", tc.path("function").path("name").asText(),
          "arguments", args));
    }
    Map<String, Object> result = new java.util.HashMap<>();
    result.put("content", msg.hasNonNull("content") ? msg.get("content").asText() : null);
    result.put("toolCalls", toolCalls);
    return result;
  }

  private JsonNode post(String key, ObjectNode body) throws Exception {
    String base = props.openaiBaseUrl() == null || props.openaiBaseUrl().isBlank()
        ? "https://api.openai.com/v1" : props.openaiBaseUrl().replaceAll("/+$", "");
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(base + "/chat/completions"))
        .timeout(Duration.ofSeconds(45))
        .header("Authorization", "Bearer " + key)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
        .build();
    HttpResponse<String> res;
    try {
      res = http.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() >= 500 || res.statusCode() == 429) throw new RuntimeException("retry");
    } catch (RuntimeException e) {
      res = http.send(req, HttpResponse.BodyHandlers.ofString()); // one retry
    }
    if (res.statusCode() >= 300) {
      throw new RuntimeException("openai " + res.statusCode() + ": " + res.body());
    }
    return json.readTree(res.body());
  }
}
