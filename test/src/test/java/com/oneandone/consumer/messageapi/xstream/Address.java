/**
 * 
 */
package com.oneandone.consumer.messageapi.xstream;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Address {
    @XmlAttribute
    long id;
    String street;
}