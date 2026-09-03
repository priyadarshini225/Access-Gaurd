package compensation_engine.exception;

public class OllamaUnavailableException extends RuntimeException {

    public OllamaUnavailableException() {
        super("Ollama is not reachable. Please start Ollama and ensure " +
              "the qwen2.5:1.5b model is available. Run: ollama serve");
    }

    public OllamaUnavailableException(String detail) {
        super("Ollama is not reachable: " + detail);
    }
}
