package com.ben.workflow.engine;

import com.ben.workflow.model.WorkflowNode;
import java.time.Instant;
import java.util.Map;

public class ExecutionState {
    private String instanceId;
    private String workflowId;
    private Status status;
    private Instant startedAt;
    private Instant endedAt;
    private Map<String, NodeState> nodeStates;
    private Map<String, Object> outputs;
    private String errorMessage;

    public enum Status { PENDING, RUNNING, SUCCESS, FAILED, CANCELLED }

    public static class NodeState {
        private String nodeId;
        private String status;
        private Object result;
        private String errorMessage;
        private Instant startedAt;
        private Instant endedAt;

        public NodeState() {}
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Instant getStartedAt() { return startedAt; }
        public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
        public Instant getEndedAt() { return endedAt; }
        public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    }

    public ExecutionState() {}
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public Map<String, NodeState> getNodeStates() { return nodeStates; }
    public void setNodeStates(Map<String, NodeState> nodeStates) { this.nodeStates = nodeStates; }
    public Map<String, Object> getOutputs() { return outputs; }
    public void setOutputs(Map<String, Object> outputs) { this.outputs = outputs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private ExecutionState instance = new ExecutionState();
        public Builder instanceId(String val) { instance.setInstanceId(val); return this; }
        public Builder workflowId(String val) { instance.setWorkflowId(val); return this; }
        public Builder status(Status val) { instance.setStatus(val); return this; }
        public Builder startedAt(Instant val) { instance.setStartedAt(val); return this; }
        public Builder endedAt(Instant val) { instance.setEndedAt(val); return this; }
        public Builder nodeStates(Map<String, NodeState> val) { instance.setNodeStates(val); return this; }
        public Builder outputs(Map<String, Object> val) { instance.setOutputs(val); return this; }
        public Builder errorMessage(String val) { instance.setErrorMessage(val); return this; }
        public ExecutionState build() { return instance; }
    }
}
