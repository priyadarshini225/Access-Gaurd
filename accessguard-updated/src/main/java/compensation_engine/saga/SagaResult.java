package compensation_engine.saga;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SagaResult {
    private String status;
    private String message;
    private String failedStep;
    private String residual;
    private final List<Map<String,String>> steps = new ArrayList<>();
    private final List<Map<String,String>> compensation = new ArrayList<>();

    public String getStatus(){ return status; }
    public void setStatus(String status){ this.status=status; }
    public String getMessage(){ return message; }
    public void setMessage(String message){ this.message=message; }
    public String getFailedStep(){ return failedStep; }
    public void setFailedStep(String failedStep){ this.failedStep=failedStep; }
    public String getResidual(){ return residual; }
    public void setResidual(String residual){ this.residual=residual; }
    public List<Map<String,String>> getSteps(){ return steps; }
    public List<Map<String,String>> getCompensation(){ return compensation; }
}
