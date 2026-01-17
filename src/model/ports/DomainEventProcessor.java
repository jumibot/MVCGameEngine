package model.ports;

import java.util.List;

public interface DomainEventProcessor {

    public void notifyNewDynamic(String entityId, String assetId);

    public void notifyNewStatic(String entityId, String assetId);

    public List<ActionDTO> decideActions(List<Event> events);
}
