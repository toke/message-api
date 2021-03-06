package net.java.messageapi.reflection;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.*;

public class ParameterMapNameSupplier implements ParameterNameSupplier {

    public static final String SUFFIX = ".parametermap";

    private static final Pattern METHOD_SIGNATURE = Pattern.compile("(\\w*)\\((.*)\\)");

    private final ParameterNameSupplier delegate;

    public ParameterMapNameSupplier(ParameterNameSupplier delegate) {
        this.delegate = delegate;
    }

    @Override
    public String get(Parameter parameter) {
        LineNumberReader content = null;
        try {
            content = getParameterMap(parameter.getMethod());
            if (content != null) {
                for (String line = readLine(content); line != null; line = readLine(content)) {
                    if (line.startsWith("#"))
                        continue;
                    List<String> matchedNames = matchedNames(line, parameter.getMethod());
                    if (matchedNames == null)
                        continue;
                    return matchedNames.get(parameter.getIndex());
                }
            }
        } finally {
            if (content != null) {
                try {
                    content.close();
                } catch (IOException e) {
                    // ignore while closing
                }
            }
        }
        return delegate.get(parameter);
    }

    private LineNumberReader getParameterMap(Method method) {
        Class<?> container = method.getDeclaringClass();
        // TODO use FQN and strip the package instead so nested classes work!
        String mapName = container.getSimpleName() + SUFFIX;
        URL url = container.getResource(mapName);
        if (url == null)
            return null;
        return getContent(url);
    }

    private LineNumberReader getContent(URL url) {
        try {
            InputStream stream = url.openStream();
            return new LineNumberReader(new InputStreamReader(stream, Charset.defaultCharset()));
        } catch (IOException e) {
            throw new RuntimeException("opening " + url, e);
        }
    }

    private String readLine(LineNumberReader content) {
        try {
            return content.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> matchedNames(String line, Method method) {
        Matcher methodMatcher = METHOD_SIGNATURE.matcher(line);
        if (!methodMatcher.matches())
            throw new InvalidParameterMapFileException(method, "[" + line
                    + "] doesn't seem to be a valid method signature.");
        if (!method.getName().equals(methodMatcher.group(1)))
            return null;

        List<String> names = new ArrayList<String>();
        List<ParameterMatch> matches = ParameterMatch.parse(methodMatcher.group(2));
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            Class<?> type = method.getParameterTypes()[i];
            ParameterMatch match = matches.get(i);
            if (!match.matches(type))
                return null;
            names.add(match.getParameterName());
        }

        return names;
    }
}
