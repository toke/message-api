package com.oneandone.consumer.messageapi.processor.pojo;

import java.io.IOException;
import java.io.Writer;

/**
 * @see PojoProperty
 */
class ObjectPojoProperty extends NullablePojoProperty {

    public ObjectPojoProperty(String type, String name) {
        super(type, name);
    }

    @Override
    protected void writeHashCodeCallTo(Writer writer) throws IOException {
        writer.append("((").append(name).append(" == null) ? 0 : ");
        writer.append(name).append(".hashCode())");
    }

    @Override
    protected void writeEqualsCompareTo(Writer writer) throws IOException {
        writer.append(name).append(".equals(other.").append(name).append(")");
    }
}
