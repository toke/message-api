package net.java.messageapi.processor;

import java.io.*;
import java.util.*;

import javax.annotation.Generated;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import javax.xml.bind.annotation.*;

import net.java.messageapi.*;
import net.java.messageapi.pojo.*;
import net.java.messageapi.reflection.ReflectionAdapter;

public class PojoGenerator extends AbstractGenerator {

    private static class MethodAdapter {
        private final ExecutableElement method;
        private final ReflectionAdapter<?> reflection;

        public MethodAdapter(ExecutableElement method) {
            this.method = method;
            this.reflection = ReflectionAdapter.of(method);
        }

        public boolean isUnique() {
            return reflection.isUnique();
        }

        public ExecutableElement getElement() {
            return method;
        }

        public String getPackage() {
            return reflection.getPackage();
        }

        public String getMethodName() {
            return reflection.getMethodName();
        }

        public String getMethodNameAsClassName() {
            return reflection.getMethodNameAsClassName();
        }

        public List<VariableElement> getParameters() {
            @SuppressWarnings("unchecked")
            List<VariableElement> parameters = (List<VariableElement>) method.getParameters();
            return parameters;
        }

        public String getContainingClassName() {
            Element enclosingElement = method.getEnclosingElement();
            if (enclosingElement instanceof TypeElement)
                return ((TypeElement) enclosingElement).getQualifiedName().toString();
            return enclosingElement.getSimpleName().toString();
        }
    }

    private final List<Pojo> generatedPojos = new ArrayList<Pojo>();

    public PojoGenerator(Messager messager, Filer filer, Elements utils) {
        super(messager, filer, utils);
    }

    @Override
    public void process(Element element) {
        TypeElement type = checkType(element);
        if (type != null) {
            processType(type);
        }
    }

    private TypeElement checkType(Element element) {
        if (ElementKind.INTERFACE != element.getKind()) {
            error("The MessageApi annotation can only be put on an interface, not on a " + element.getKind(), element);
            return null;
        }
        if (element.getEnclosedElements().isEmpty()) {
            error("MessageApi must have methods", element);
            return null;
        }
        return (TypeElement) element; // @MessageApi is @Target(TYPE)
    }

    private void processType(TypeElement type) {
        note("Processing MessageApi [" + type.getQualifiedName() + "]");
        for (Element enclosedElement : type.getEnclosedElements()) {
            if (enclosedElement instanceof ExecutableElement) {
                ExecutableElement method = (ExecutableElement) enclosedElement;
                if (checkMethod(method)) {
                    try {
                        generatedPojos.add(createPojoFor(method));
                    } catch (RuntimeException e) {
                        error("can't generate pojo: " + e, method);
                    }
                }
            }
        }
    }

    private boolean checkMethod(ExecutableElement executable) {
        if (TypeKind.VOID != executable.getReturnType().getKind()) {
            error("MessageApi methods must return void; they are asynchronous!", executable);
            return false;
        }
        if (!executable.getThrownTypes().isEmpty()) {
            error("MessageApi methods must not declare an exception; they are asynchronous!", executable);
            return false;
        }
        return true;
    }

    private Pojo createPojoFor(ExecutableElement method) {
        Pojo pojo = createPojo(method);

        note("Writing " + pojo.getName());

        Writer writer = null;
        try {
            JavaFileObject sourceFile = createSourceFile(pojo.getName(), method);
            writer = sourceFile.openWriter();
            pojo.writeTo(writer);
        } catch (IOException e) {
            error("Can't write MessageApi pojo\n" + e, method);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return pojo;
    }

    private Pojo createPojo(ExecutableElement executableElement) {
        MethodAdapter method = new MethodAdapter(executableElement);
        if (!method.isUnique())
            warn("ambiguous method name; it's generally better to use unique names "
                    + "and not rely on the parameter type mangling.", method.getElement());

        String pkg = method.getPackage();
        String container = method.getContainingClassName();
        assert container.startsWith(pkg + ".");
        container = container.substring(pkg.length() + 1);
        container = container.replace('.', '$');
        String className = container + "$" + method.getMethodNameAsClassName();
        Pojo pojo = new Pojo(pkg, className);
        pojo.addInterface(Serializable.class);
        addAnnotations(method, pojo);
        addProperties(method, pojo);
        pojo.addPrivateDefaultConstructor();

        return pojo;
    }

    private void addAnnotations(MethodAdapter method, Pojo pojo) {
        pojo.annotate(Generated.class, mapOf("value", MessageApiAnnotationProcessor.class.getName()));
        pojo.annotate(XmlRootElement.class, mapOf("name", method.getMethodName()));
    }

    private void addProperties(MethodAdapter method, Pojo pojo) {
        List<String> propOrder = new ArrayList<String>();

        for (VariableElement parameter : method.getParameters()) {
            String type = parameter.asType().toString();
            String name = getParameterName(parameter);

            Optional optional = parameter.getAnnotation(Optional.class);
            PojoProperty property = pojo.addProperty(type, name);

            // TODO check required parameters in the constructor
            if (parameter.getAnnotation(JmsProperty.class) != null) {
                property.annotate(JmsProperty.class);
                property.setTransient();
            } else {
                boolean required = (optional == null);
                property.annotate(XmlElement.class, mapOf("required", required));
                propOrder.add(name);
            }
        }

        String[] propOrderArray = propOrder.toArray(new String[propOrder.size()]);
        pojo.annotate(XmlType.class, mapOf("propOrder", propOrderArray));
    }

    private <T> Map<String, T> mapOf(String key, T value) {
        HashMap<String, T> map = new HashMap<String, T>();
        map.put(key, value);
        return map;
    }

    private String getParameterName(VariableElement parameter) {
        return parameter.getSimpleName().toString();
    }

    List<Pojo> getGeneratedPojos() {
        return generatedPojos;
    }
}
