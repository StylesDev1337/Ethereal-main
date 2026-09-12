package cn.ethereal.event.events;

import cn.ethereal.event.Event;

public class UpdateEvent extends Event {
    public enum Phase {
        PRE,
        POST,
    }

    private final UpdateEvent.Phase phase;

    public UpdateEvent(UpdateEvent.Phase phase) {
        this.phase = phase;
    }

    public UpdateEvent.Phase getPhase() {
        return phase;
    }
}
