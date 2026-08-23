package compensation_engine.saga;

public class SagaStep {

    private final String name;
    private final Runnable action;
    private final Compensator compensator;

    public SagaStep(String name, Runnable action, Compensator compensator) {
        this.name = name;
        this.action = action;
        this.compensator = compensator;
    }

    public String getName() {
        return name;
    }

    public Runnable getAction() {
        return action;
    }

    public Compensator getCompensator() {
        return compensator;
    }
}