package cn.ethereal.event;

import cn.ethereal.event.annotation.EventListener;
import java.lang.reflect.Method;

public class EventHandler {
    private final Object instance;
    private final Method method;
    private final int priority;
    private final Class<? extends Event> eventClass;

    public EventHandler(Object instance, Method method, Class<? extends Event> eventClass, int priority) {
        this.instance = instance;
        this.method = method;
        this.eventClass = eventClass;
        this.priority = priority;
        this.method.setAccessible(true);
    }

    public void invoke(Event event) {
        try {
            method.invoke(instance, event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPriority() { return priority; }
    public Class<? extends Event> getEventClass() { return eventClass; }
    public Object getInstance() { return instance; }
}