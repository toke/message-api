package net.java.messageapi.adapter;

import java.lang.reflect.*;
import java.util.*;

import javax.jms.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import net.java.messageapi.JmsProperty;

/**
 * A {@link JmsPayloadHandler} that serializes the payload as map message.
 */
@XmlRootElement
public class MapJmsPayloadHandler extends JmsPayloadHandler {

    @XmlElement
    @XmlJavaTypeAdapter(JmsMappingAdapter.class)
    public final Mapping mapping;

    public MapJmsPayloadHandler() {
        this(MappingBuilder.DEFAULT);
    }

    public MapJmsPayloadHandler(Mapping mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object toPayload(Object pojo) {
        Map<String, Object> result = new HashMap<String, Object>();
        addOperation(pojo, result);
        result.putAll(readFields(pojo));
        return result;
    }

    public void addOperation(Object pojo, Map<String, Object> result) {
        String operationName = mapping.getOperationForMethod(getSimpleTypeName(pojo));
        String operationField = mapping.getOperationMessageAttibute();
        result.put(operationField, operationName);
    }

    private String getSimpleTypeName(Object pojo) {
        String simpleName = pojo.getClass().getSimpleName();
        if (simpleName.contains("$"))
            simpleName = simpleName.substring(simpleName.indexOf('$') + 1);
        simpleName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        return simpleName;
    }

    private Map<String, Object> readFields(Object pojo) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Field field : pojo.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(JmsProperty.class) || Modifier.isStatic(field.getModifiers()))
                continue;
            String fieldName = field.getName();
            @SuppressWarnings("unchecked")
            FieldMapping<Object> fieldMapping = (FieldMapping<Object>) mapping.getMappingForField(fieldName);
            Object value = getField(pojo, field);
            if (value != null) {
                result.put(fieldMapping.getAttributeName(), fieldMapping.marshal(value));
            }
        }
        return result;
    }

    private Object getField(Object pojo, Field field) {
        try {
            field.setAccessible(true);
            return field.get(pojo);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Message createJmsMessage(Object payload, Session session) throws JMSException {
        MapMessage message = session.createMapMessage();
        populateBody(message, (Map<String, Object>) payload);
        return message;
    }

    private void populateBody(MapMessage message, Map<String, Object> body) throws JMSException {
        for (Map.Entry<String, Object> e : body.entrySet()) {
            message.setObject(e.getKey(), e.getValue());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((mapping == null) ? 0 : mapping.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MapJmsPayloadHandler other = (MapJmsPayloadHandler) obj;
        if (mapping == null) {
            if (other.mapping != null) {
                return false;
            }
        } else if (!mapping.equals(other.mapping)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MapJmsPayloadHandler [" + mapping + "]";
    }

    @Override
    public String getName() {
        return "mapped";
    }
}
