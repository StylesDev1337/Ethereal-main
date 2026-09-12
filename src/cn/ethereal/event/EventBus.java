package cn.ethereal.event;

import cn.ethereal.event.annotation.EventListener;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventBus {
    private static final Map<Class<? extends Event>, List<EventHandler>> listeners = new ConcurrentHashMap<>();
    private static final List<Object> registeredObjects = new ArrayList<>();
    public static final EventBus INSTANCE = new EventBus();

    // 注册一个对象的所有 @EventListener 方法
    public static void register(Object object) {
        if (registeredObjects.contains(object)) return;
        registeredObjects.add(object);

        Class<?> clazz = object.getClass();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            EventListener annotation = method.getAnnotation(EventListener.class);
            if (annotation != null) {
                // 检查方法签名：必须有一个 Event 参数
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && Event.class.isAssignableFrom(params[0])) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Event> eventClass = (Class<? extends Event>) params[0];
                    EventHandler handler = new EventHandler(object, method, eventClass, annotation.priority());

                    listeners.computeIfAbsent(eventClass, k -> new ArrayList<>())
                            .add(handler);

                    // 按优先级排序
                    listeners.get(eventClass).sort((a, b) -> b.getPriority() - a.getPriority());
                }
            }
        }
    }

    // 注销对象的所有监听器
    public static void unregister(Object object) {
        registeredObjects.remove(object);
        listeners.values().forEach(list -> list.removeIf(handler -> handler.getInstance() == object));
    }

    // 触发事件
    public static <T extends Event> T post(T event) {
        List<EventHandler> list = listeners.get(event.getClass());
        if (list != null) {
            // 使用副本避免并发修改
            List<EventHandler> copyList = new ArrayList<>(list);
            for (EventHandler handler : copyList) {
                // 如果事件被取消，停止传播
                if (event.isCancelled()) {
                    break;
                }
                handler.invoke(event);
            }
        }
        return event;
    }

    // 清空所有监听器
    public static void clear() {
        listeners.clear();
        registeredObjects.clear();
    }
}