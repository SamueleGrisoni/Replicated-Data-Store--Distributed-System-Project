package click.replicatedDataStore;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class InjectionUtils {
    private static LinkedList<Boolean> oldDebug = new LinkedList<>();

    /**
     * @param debug the new value of the debug global parameter
     */
    public static void setDebug(boolean debug) {
            if(debug != ClientGlobalParameters.debug) {
                oldDebug.add(ClientGlobalParameters.debug);
                try {
                    Field debugField = ClientGlobalParameters.class.getDeclaredField("debug");
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(debugField, debugField.getModifiers() & ~Modifier.FINAL);
                    debugField.set(null, debug);
                    modifiersField.setInt(debugField, debugField.getModifiers() & Modifier.FINAL);
                    modifiersField.setAccessible(false);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
    }

    /**
     * Reset the debug global parameter to its original value
     */
    public static void resetDebug() {
        boolean last = false;
        try {
             last = oldDebug.getLast();
            oldDebug.removeLast();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        setDebug(last);
    }

    public static void resetDebugAll() {
        while(!oldDebug.isEmpty()) {
            oldDebug.clear();
        }
    }

    /**
     * @param obj the object to check
     * @param fieldName the name of the field to check
     * @param value the value to check against
     * @return
     */
    public static boolean assertPrivateFieldEquals(Object obj, String fieldName, Object value) {
        boolean result = false;
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            makeFieldPublic(field);
            Object actualValue = field.get(obj);
            result = actualValue.equals(value);
            resetFieldAccess(field);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Object getPrivateField(Object obj, String fieldName){
        Object result = null;
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            makeFieldPublic(field);
            result = field.get(obj);
            resetFieldAccess(field);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void makeFieldPublic(Field field) throws NoSuchFieldException {
        field.setAccessible(true);
    }

    private static void resetFieldAccess(Field field) {
        field.setAccessible(false);
    }

}
