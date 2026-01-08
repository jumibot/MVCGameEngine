package model.ports;

import java.util.List;

public interface DomainEventProcessor {

    public void notifyNewProjectileFired(String entityId, String assetId);

    public List<ActionDTO> decideActions(List<Event> events);
}
