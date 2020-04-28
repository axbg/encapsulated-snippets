import org.atteo.classindex.ClassIndex;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class AnnotationManager {
    private Map<String, Class<?>> injectableTypes;
    private Map<String, Object> managedObjects;
    private static AnnotationManager instance;

    private AnnotationManager() {
        managedObjects = new HashMap<>();
        injectableTypes = new HashMap<>();
        initializeInjectableTypes();
    }

    public static AnnotationManager getInstance() {
        if (instance == null) {
            instance = new AnnotationManager();
        }
        return instance;
    }

    private void initializeInjectableTypes() {
        for (Class<?> clazz : ClassIndex.getAnnotated(ManagedClass.class)) {
            injectableTypes.put(clazz.getName(), clazz);
        }
    }

    private Object getInjectableValue(String className) throws Exception {
        if (!managedObjects.containsKey(className)) {
            Object managedObject = provideManagedObject(injectableTypes.get(className));
            managedObjects.put(className, managedObject);
        }

        return managedObjects.get(className);
    }

    public <T> T provideManagedObject(Class<?> clazz) throws Exception {
        Constructor<?> constructor = clazz.getConstructor();
        Object providedObject = constructor.newInstance();
        for (Field field : clazz.getDeclaredFields()) {
            if (injectableTypes.get(field.getType().getName()) != null) {
                Object injectableValue = getInjectableValue(field.getType().getName());
                field.setAccessible(true);
                field.set(providedObject, injectableValue);
            }
        }
        return (T) providedObject;
    }
}