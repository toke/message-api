package net.java.messageapi;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Place this parameter of a MessageApi method into the JMS message header instead of the payload.
 * If you annotate some indirect field of such a parameter, it will be in the header, too, but it
 * will still be part of the payload. To remove it from the payload, annotate that field
 * additionally as {@link javax.xml.bind.annotation.XmlTransient XmlTransient}.
 */
@Qualifier
@Target({ PARAMETER, FIELD })
@Retention(RUNTIME)
public @interface JmsProperty {
    // intentionally empty
}
