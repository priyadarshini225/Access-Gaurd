package compensation_engine.saga;

@FunctionalInterface
public interface Compensator {

    void compensate();
}