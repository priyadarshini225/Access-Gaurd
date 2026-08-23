package compensation_engine.saga;

public enum WorkflowPolicy {

    ROLLBACK_ON_FAILURE,

    KEEP_PARTIAL_COMPLETION
}