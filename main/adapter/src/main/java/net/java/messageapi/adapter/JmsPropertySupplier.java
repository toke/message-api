package net.java.messageapi.adapter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import javax.ejb.EJBException;
import javax.jms.JMSException;
import javax.jms.Message;

import net.java.messageapi.JmsProperty;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Provides the header fields that are annotated as {@link JmsProperty}
 */
class JmsPropertySupplier implements JmsHeaderSupplier {

    private static class FieldScanner {

        private final Message message;
        private final List<Object> visited = Lists.newArrayList();

        public FieldScanner(Message message) {
            this.message = message;
        }

        public void scan(Object pojo) throws JMSException {
            scan(pojo, "", false);
        }

        private void scan(Object pojo, String prefix, boolean doAdd) throws JMSException {
            for (Field field : pojo.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()))
                    continue;
                Object value = getField(pojo, field);
                if (value == null)
                    continue;
                boolean alreadSeen = !visit(value);
                if (alreadSeen)
                    continue;
                boolean nestedDoAdd = doAdd || field.isAnnotationPresent(JmsProperty.class);
                String fieldName = prefix + field.getName();
                if (nestedDoAdd) {
                    setProperty(fieldName, value);
                }
            }
        }

        private void scan(Collection<?> collection, String prefix, boolean doAdd)
                throws JMSException {
            int i = 0;
            for (Iterator<?> iterator = collection.iterator(); iterator.hasNext(); i++) {
                Object element = iterator.next();
                setProperty(prefix + "[" + i + "]", element);
            }
        }

        private void scan(Object[] list, String prefix, boolean doAdd) throws JMSException {
            for (int i = 0; i < list.length; i++) {
                Object element = list[i];
                setProperty(prefix + "[" + i + "]", element);
            }
        }

        private Object getField(Object pojo, Field field) {
            try {
                field.setAccessible(true);
                return field.get(pojo);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private boolean visit(Object value) {
            // IdentitySet would be nicer
            for (Object element : visited) {
                if (value == element) {
                    return false;
                }
            }
            visited.add(value);
            return true;
        }

        private void setProperty(String name, Object value) throws JMSException {
            if (value instanceof String) {
                message.setStringProperty(name, (String) value);
            } else if (value instanceof Boolean) {
                message.setBooleanProperty(name, (Boolean) value);
            } else if (value instanceof Byte) {
                message.setByteProperty(name, (Byte) value);
            } else if (value instanceof Character) {
                message.setStringProperty(name, ((Character) value).toString());
            } else if (value instanceof Short) {
                message.setShortProperty(name, (Short) value);
            } else if (value instanceof Integer) {
                message.setIntProperty(name, (Integer) value);
            } else if (value instanceof Long) {
                message.setLongProperty(name, (Long) value);
            } else if (value instanceof Float) {
                message.setFloatProperty(name, (Float) value);
            } else if (value instanceof Double) {
                message.setDoubleProperty(name, (Double) value);
            } else if (value instanceof Collection) {
                scan((Collection<?>) value, name, true);
            } else if (value.getClass().isArray()) {
                scan((Object[]) value, name, true);
            } else if (!isPrimitive(value.getClass())) {
                scan(value, name + "/", true);
            }
        }

        private static final Set<Class<?>> PRIMITIVE_TYPES = ImmutableSet.<Class<?>> of(
                Boolean.class, Byte.class, Character.class, Short.class, Long.class, Float.class,
                Double.class, String.class);

        private boolean isPrimitive(Class<?> type) {
            return type.isPrimitive() || PRIMITIVE_TYPES.contains(type);
        }
    }

    @Override
    public void addTo(Message message, Object pojo) throws JMSException {
        FieldScanner scanner = new FieldScanner(message);
        try {
            scanner.scan(pojo);
        } catch (EJBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other != null && other.getClass().equals(this.getClass());
    }
}