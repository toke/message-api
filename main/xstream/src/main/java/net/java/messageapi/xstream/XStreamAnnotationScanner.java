package net.java.messageapi.xstream;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;

import com.thoughtworks.xstream.XStream;

/**
 * Scans the JAXB annotations of the classes in the given contextPath (colon
 * separated path of package names). The acquired info is then added to XStream.
 */
class XStreamAnnotationScanner {
	private final XStream xStream;

	private final Set<Class<?>> visited = new HashSet<Class<?>>();

	public XStreamAnnotationScanner(XStream xStream) {
		this.xStream = xStream;
	}

	public void scanPath(String contextPath,
			ClassLoader classLoader) {
		try {
			scanPathOrThrow(contextPath, classLoader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void scanTypes(Class<?>[] classes) {
		for (Class<?> type : classes) {
			scanType(type);
		}
	}

	private void scanPathOrThrow(String contextPath, ClassLoader classLoader) throws IOException, ClassNotFoundException {
		for (String pathElement : contextPath.split(":")) {
			scanDirectory(pathElement, classLoader);
		}
	}

	private void scanDirectory(String pathElement, ClassLoader classLoader) throws IOException,
			ClassNotFoundException {
		String jaxbIndexFileName = pathElement.replace('.', File.separatorChar)
				+ "/jaxb.index";
		URL jaxbIndexUrl = classLoader.getResource(jaxbIndexFileName);
		if (jaxbIndexUrl != null) {
			scanIndexFile(jaxbIndexUrl, pathElement, classLoader);
		}
	}

	private void scanIndexFile(URL jaxbIndex, String prefix, ClassLoader classLoader)
			throws IOException, ClassNotFoundException {
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(
				jaxbIndex.openStream()));
		for (String line = reader.readLine(); line != null; line = reader
				.readLine()) {
			String fullClassName = prefix + "." + line;
			Class<?> type = Class.forName(fullClassName, true, classLoader);
			scanType(type);
		}
	}

	private void scanType(Class<?> type) {
		for (Field field : type.getDeclaredFields()) {
			if (field.isAnnotationPresent(XmlAttribute.class)) {
				XmlAttribute attribute = field
						.getAnnotation(XmlAttribute.class);
				xStream.useAttributeFor(type, field.getName());
				if (!"##default".equals(attribute.name())) {
					xStream.aliasField(field.getName(), type, attribute.name());
				}
			}
			Class<?> subType = field.getType();
			if (visit(subType)) {
				scanType(subType);
			}
		}
	}

	private boolean visit(Class<?> type) {
		return !type.isPrimitive() && !visited.add(type);
	}
}