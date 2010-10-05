package com.oneandone.consumer.messageapi.test;

import com.oneandone.consumer.messageapi.MessageApi;

@MessageApi
public interface BoxedPrimitivesTestApi {

    public void boxedBooleanCall(Boolean b);

    public void boxedByteCall(Byte b);

    public void boxedCharCall(Character c);

    public void boxedShortCall(Short s);

    public void boxedIntCall(Integer i);

    public void boxedLongCall(Long l);

    public void boxedFloatCall(Float f);

    public void boxedDoubleCall(Double d);
}
