/**
 * 
 */
package com.oneandone.consumer.messageapi.processor;

import com.oneandone.consumer.messageapi.MessageApi;

//TODO Find out why Eclipse builds the SimpleMethod classes for the test code (Maven does not)
@MessageApi
public interface AnnotationProcessorTestApi {
    void simpleMethod1();

    void simpleMethod2(int i);

    void simpleMethod3(int i, String s);
}