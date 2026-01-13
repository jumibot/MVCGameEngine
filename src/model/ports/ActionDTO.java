package model.ports;

public class ActionDTO {
    final public String entityId;
    final public ActionType type;
    final public ActionExecutor executor;
    final public ActionPriority priority;

    public ActionDTO(String entityId, ActionType type, ActionExecutor executor, ActionPriority priority) {
        this.entityId = entityId;
        this.type = type;
        this.executor = executor;
        this.priority = priority;
    }
}