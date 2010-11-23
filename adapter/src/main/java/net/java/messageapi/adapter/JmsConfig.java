package net.java.messageapi.adapter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import javax.naming.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;

/**
 * The configuration container for the {@link AbstractJmsSenderFactory}. Although this class
 * <b>can</b> be instantiated directly, most commonly a factory like {@link DefaultJmsConfigFactory}
 * is used.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class JmsConfig {

    /**
     * Load a {@link JmsConfig} from a file named like that interface plus "-jmsconfig.xml"
     */
    public static JmsConfig getConfigFor(Class<?> api) {
        Reader reader = getReaderFor(api);
        return readConfigFrom(reader);
    }

    private static Reader getReaderFor(Class<?> api) {
        String fileName = api.getSimpleName() + "-jmsconfig.xml";
        InputStream stream = api.getResourceAsStream(fileName);
        if (stream == null)
            throw new RuntimeException("file not found: " + fileName);
        return new InputStreamReader(stream, Charset.forName("utf-8"));
    }

    public static JmsConfig readConfigFrom(Reader reader) {
        try {
            JAXBContext context = getJaxbContext();
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (JmsConfig) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeConfigTo(Writer writer) {
        try {
            JAXBContext context = getJaxbContext();
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(this, writer);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private static JAXBContext getJaxbContext() throws JAXBException {
        return JAXBContext.newInstance(XmlJmsConfig.class, MapJmsConfig.class);
    }

    public static JmsConfig getJmsConfig(String factoryName, String queueName, String user,
            String pass, boolean transacted, Supplier<Properties> contextPropertiesSupplier,
            Supplier<Map<String, Object>> headerSupplier, JmsSenderFactoryType type) {
        // FIXME
        if (type == JmsSenderFactoryType.XML) {
            return new XmlJmsConfig(factoryName, queueName, user, pass, transacted,
                    contextPropertiesSupplier, headerSupplier);
        } else if (type == JmsSenderFactoryType.MAP) {
            return new MapJmsConfig(factoryName, queueName, user, pass, transacted,
                    contextPropertiesSupplier, headerSupplier);
        } else {
            throw new UnsupportedOperationException("unknown type: " + type);
        }
    }

    @XmlElement(name = "factory")
    private final String factoryName;
    @XmlElement(name = "destination")
    private final String destinationName;
    private final String user;
    private final String pass;
    private final boolean transacted;

    // FIXME these should not be transient
    @XmlTransient
    private final Supplier<Properties> contextPropertiesSupplier;
    @XmlTransient
    private final Supplier<Map<String, Object>> headerSupplier;

    // just to satisfy JAXB
    protected JmsConfig() {
        this.factoryName = null;
        this.destinationName = null;
        this.user = null;
        this.pass = null;
        this.transacted = true;
        this.contextPropertiesSupplier = null;
        this.headerSupplier = null;
    }

    public JmsConfig(String factoryName, String destinationName, String user, String pass,
            boolean transacted, Supplier<Properties> contextPropertiesSupplier,
            Supplier<Map<String, Object>> headerSupplier) {
        this.factoryName = factoryName;
        this.destinationName = destinationName;
        this.user = user;
        this.pass = pass;
        this.transacted = transacted;
        this.contextPropertiesSupplier = contextPropertiesSupplier;
        this.headerSupplier = headerSupplier;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public boolean isTransacted() {
        return transacted;
    }

    public Context getContext() throws NamingException {
        Properties context = (contextPropertiesSupplier == null) ? new Properties()
                : contextPropertiesSupplier.get();
        return new InitialContext(context);
    }

    public Map<String, Object> getAdditionalProperties() {
        if (headerSupplier == null)
            return ImmutableMap.of();
        return headerSupplier.get();
    }

    public <T> T createProxy(Class<T> api) {
        return createFactory(api).get();
    }

    public abstract <T> AbstractJmsSenderFactory<T, ?> createFactory(Class<T> api);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((contextPropertiesSupplier == null) ? 0 : contextPropertiesSupplier.hashCode());
        result = prime * result + ((destinationName == null) ? 0 : destinationName.hashCode());
        result = prime * result + ((factoryName == null) ? 0 : factoryName.hashCode());
        result = prime * result + ((headerSupplier == null) ? 0 : headerSupplier.hashCode());
        result = prime * result + ((pass == null) ? 0 : pass.hashCode());
        result = prime * result + (transacted ? 1231 : 1237);
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JmsConfig other = (JmsConfig) obj;
        if (contextPropertiesSupplier == null) {
            if (other.contextPropertiesSupplier != null) {
                return false;
            }
        } else if (!contextPropertiesSupplier.equals(other.contextPropertiesSupplier)) {
            return false;
        }
        if (destinationName == null) {
            if (other.destinationName != null) {
                return false;
            }
        } else if (!destinationName.equals(other.destinationName)) {
            return false;
        }
        if (factoryName == null) {
            if (other.factoryName != null) {
                return false;
            }
        } else if (!factoryName.equals(other.factoryName)) {
            return false;
        }
        if (headerSupplier == null) {
            if (other.headerSupplier != null) {
                return false;
            }
        } else if (!headerSupplier.equals(other.headerSupplier)) {
            return false;
        }
        if (pass == null) {
            if (other.pass != null) {
                return false;
            }
        } else if (!pass.equals(other.pass)) {
            return false;
        }
        if (transacted != other.transacted) {
            return false;
        }
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "JmsConfig ["
                + (contextPropertiesSupplier != null ? "contextPropertiesSupplier="
                        + contextPropertiesSupplier + ", " : "")
                + (destinationName != null ? "destinationName=" + destinationName + ", " : "")
                + (factoryName != null ? "factoryName=" + factoryName + ", " : "")
                + (headerSupplier != null ? "headerSupplier=" + headerSupplier + ", " : "")
                + (pass != null ? "pass=" + pass + ", " : "") + "transacted=" + transacted + ", "
                + (user != null ? "user=" + user : "") + "]";
    }
}
