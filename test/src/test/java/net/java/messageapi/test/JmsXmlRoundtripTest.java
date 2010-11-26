package net.java.messageapi.test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import net.java.messageapi.adapter.JmsConfig;
import net.java.messageapi.adapter.XmlJmsConfig;
import net.java.messageapi.adapter.xml.*;
import net.java.messageapi.adapter.xml.JaxbProvider.JaxbProviderMemento;
import net.java.messageapi.test.defaultjaxb.JodaTimeApi;
import net.sf.twip.*;

import org.joda.time.Instant;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockejb.jms.TextMessageImpl;

@RunWith(TwiP.class)
public class JmsXmlRoundtripTest extends AbstractJmsSenderFactoryTest {

    private final JaxbProviderMemento memento;

    // TODO support: ECLIPSE_LINK
    public JmsXmlRoundtripTest(
            @NotNull @Assume("!= XSTREAM & != ECLIPSE_LINK") JaxbProvider jaxbProvider) {
        this.memento = jaxbProvider.setUp();
    }

    @After
    public void after() {
        memento.restore();
    }

    @Override
    protected JmsConfig createConfig() {
        return new XmlJmsConfig(FACTORY, QUEUE, QUEUE_USER, QUEUE_PASS, true, new Properties(),
                Collections.<String, Object> emptyMap());
    }

    private String instantCallXml(Instant now) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<ns2:instantCall xmlns:ns2=\"http://messageapi.java.net\">\n"
                + "    <instantName>" + now + "</instantName>\n" //
                + "</ns2:instantCall>\n";
    }

    private String getMessagePayload() throws JMSException {
        return ((TextMessage) captureMessage()).getText();
    }

    @Test
    public void shouldCallServiceWhenSendingAsXmlMessage() throws JMSException {
        // TODO split into send and receive using MockEJB
        // Given
        TestApi service = CONFIG.createProxy(TestApi.class);

        // When
        service.multiCall("a", "b");

        // Then
        String xml = getMessagePayload();
        TestApi serviceImpl = mock(TestApi.class);
        XmlStringDecoder.create(TestApi.class, serviceImpl).decode(xml);
        verify(serviceImpl).multiCall("a", "b");
    }

    @Test
    public void shouldSendUsingImplicitConversion() throws Exception {
        // Given
        Instant now = new Instant();
        JodaTimeApi service = CONFIG.createProxy(JodaTimeApi.class);

        // When
        service.instantCall(now);

        assertEquals(instantCallXml(now), getMessagePayload());
    }

    @Test
    public void shouldReceiveUsingImplicitConversion() throws Exception {
        // Given
        Instant now = new Instant();
        String xml = instantCallXml(now);
        TextMessage textMessage = new TextMessageImpl(xml);
        JodaTimeApi serviceImpl = mock(JodaTimeApi.class);
        XmlMessageDecoder<JodaTimeApi> decoder = XmlMessageDecoder.create(JodaTimeApi.class,
                serviceImpl);

        // When
        decoder.onMessage(textMessage);

        // Then
        verify(serviceImpl).instantCall(now);
    }
}
