package net.java.messageapi.adapter;

import java.lang.reflect.Method;

import javax.jms.*;
import javax.xml.bind.annotation.XmlSeeAlso;


@XmlSeeAlso({ XmlJmsPayloadHandler.class, MapJmsPayloadHandler.class })
public abstract class JmsPayloadHandler {

    public abstract Object toPayload(Class<?> api, Method method, Object pojo);

    public abstract Message createJmsMessage(Object payload, Session session) throws JMSException;
}
