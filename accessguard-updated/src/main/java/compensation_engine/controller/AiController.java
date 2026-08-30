package compensation_engine.controller;

import compensation_engine.ai.OllamaService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiController {

    private final OllamaService ai;

    public AiController(OllamaService ai) {
        this.ai = ai;
    }

    @PostMapping("/plan")
    public Map<String, String> plan(
            @RequestBody Map<String, String> request) {

        return ai.plan(
                request.getOrDefault(
                        "message",
                        ""
                )
        );
    }

    @PostMapping("/chat")
    public Map<String, String> chat(
            @RequestBody Map<String, String> request) {

        return Map.of(
                "response",
                ai.ask(
                        request.getOrDefault(
                                "message",
                                ""
                        )
                )
        );
    }
}