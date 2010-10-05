package com.oneandone.consumer.messageapi.processor.pojo;

import java.io.IOException;
import java.io.Writer;

/**
 * @see PojoProperty
 */
class ArrayPojoProperty extends NullablePojoProperty {

    public ArrayPojoProperty(String type, String name) {
        super(type, name);
    }

    @Override
    protected void writeHashCodeCallTo(Writer writer) throws IOException {
        writer.append("java.util.Arrays.hashCode(").append(name).append(")");
    }

    @Override
    protected void writeEqualsCompareTo(Writer writer) throws IOException {
        writer.append("java.util.Arrays.equals(").append(name);
        writer.append(", other.").append(name).append(")");
    }

    @Override
    public void writeToStringTo(Writer writer) throws IOException {
        writer.append("java.util.Arrays.toString(").append(name).append(")");
    }
}
