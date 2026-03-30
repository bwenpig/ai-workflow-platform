package com.ben.workflow.model;

import java.util.List;
import java.util.Map;

public class WorkflowNode {
    private String nodeId;
    private String type;
    private Position position;
    private List<InputPort> inputs;
    private List<OutputPort> outputs;
    private Map<String, Object> config;
    private String modelProvider;
    private NodeStatus status;
    private ExecutionResult result;

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public List<InputPort> getInputs() { return inputs; }
    public void setInputs(List<InputPort> inputs) { this.inputs = inputs; }
    public List<OutputPort> getOutputs() { return outputs; }
    public void setOutputs(List<OutputPort> outputs) { this.outputs = outputs; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }
    public ExecutionResult getResult() { return result; }
    public void setResult(ExecutionResult result) { this.result = result; }

    public enum NodeStatus { PENDING, RUNNING, SUCCESS, FAILED, SKIPPED }

    public static class Position {
        private Double x;
        private Double y;
        public Double getX() { return x; }
        public void setX(Double x) { this.x = x; }
        public Double getY() { return y; }
        public void setY(Double y) { this.y = y; }
    }

    public static class InputPort {
        private String id;
        private String label;
        private String type;
        private Object defaultValue;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
    }

    public static class OutputPort {
        private String id;
        private String label;
        private String type;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class ExecutionResult {
        private String outputUrl;
        private Map<String, Object> metadata;
        private String errorMessage;
        public String getOutputUrl() { return outputUrl; }
        public void setOutputUrl(String outputUrl) { this.outputUrl = outputUrl; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
