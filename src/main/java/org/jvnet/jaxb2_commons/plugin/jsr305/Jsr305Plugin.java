package org.jvnet.jaxb2_commons.plugin.jsr305;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JAnnotationValue;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.bind.api.impl.NameConverter;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

public class Jsr305Plugin extends Plugin {

    private static final Collection<String> PRIMITIVE_TYPES = new HashSet<String>(Arrays.asList(
            "void",
            "boolean",
            "char",
            "byte", "short", "int", "long",
            "float", "double"
    ));

    @Override
    public String getOptionName() {
        return "Xjsr305";
    }

    @Override
    public String getUsage() {
        return "-Xjsr305 : activate jsr305 annotations generation";
    }

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
        for (ClassOutline classOutline : outline.getClasses()) {
            for (JFieldVar field : classOutline.implClass.fields().values()) {
                PropertyInfo propertyInfo = extractPropertyInfo(field);
                String propertyName = getPropertyName(propertyInfo.name);

                for (JMethod method : classOutline.implClass.methods()) {
                    if (isGetter(method, propertyName)) {
                        if (isListGetter(method)) {
                            annotateList(method);
                        } else {
                            annotateGetter(method, propertyInfo.required);
                        }
                    } else if (isSetter(method, propertyName)) {
                        annotateSetter(method, propertyInfo.required);
                    }
                }
            }
        }
        return true;
    }

    private PropertyInfo extractPropertyInfo(JFieldVar field) {
        String name = field.name();
        boolean required = false;

        for (JAnnotationUse annotation : field.annotations()) {
            if ("javax.xml.bind.annotation.XmlElement".equals(annotation.getAnnotationClass().fullName())) {
                name = getStringValue(annotation.getAnnotationMembers().get("name"), name);
                required = getBooleanValue(annotation.getAnnotationMembers().get("required"), false) &&
                        !getBooleanValue(annotation.getAnnotationMembers().get("nillable"), false);
            } else if ("javax.xml.bind.annotation.XmlAttribute".equals(annotation.getAnnotationClass().fullName())) {
                name = getStringValue(annotation.getAnnotationMembers().get("name"), name);
                required = getBooleanValue(annotation.getAnnotationMembers().get("required"), false);
            } else if ("javax.xml.bind.annotation.XmlElementRef".equals(annotation.getAnnotationClass().fullName())) {
                name = getStringValue(annotation.getAnnotationMembers().get("name"), name);
                required = getBooleanValue(annotation.getAnnotationMembers().get("required"), true);
            }
        }

        return new PropertyInfo(name, required);
    }

    private String getStringValue(JAnnotationValue annotation, String defaultValue) {
        if (annotation == null) {
            return defaultValue;
        }
        StringWriter writer = new StringWriter();
        annotation.generate(new JFormatter(writer));
        return dequotify(writer.toString());
    }

    private String dequotify(String s) {
        if (s.startsWith("\"")) {
            s = s.substring(1);
        }
        if (s.endsWith("\"")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private boolean getBooleanValue(JAnnotationValue annotation, boolean defaultValue) {
        if (annotation == null) {
            return defaultValue;
        }
        StringWriter writer = new StringWriter(5);
        annotation.generate(new JFormatter(writer));
        return Boolean.valueOf(writer.toString());
    }

    private String getPropertyName(String name) {
        return NameConverter.standard.toPropertyName(name);
    }

    private boolean isGetter(JMethod method, String propertyName) {
        return method.name().matches("(get|is)" + propertyName) &&
                method.params().isEmpty();
    }

    private boolean isListGetter(JMethod method) {
        return method.type().fullName().matches("java\\.util\\.List<.*>");
    }

    private boolean isSetter(JMethod method, String propertyName) {
        return method.name().equals("set" + propertyName) &&
                "void".equals(method.type().name()) &&
                method.params().size() == 1;
    }

    private void annotateList(JMethod method) {
        method.annotate(Nonnull.class);
    }

    private void annotateGetter(JMethod method, boolean required) {
        if (PRIMITIVE_TYPES.contains(method.type().fullName())) {
            return;
        }

        method.annotate(required ? Nonnull.class : CheckForNull.class);
    }

    private void annotateSetter(JMethod method, boolean required) {
        JVar parameter = method.params().get(0);
        String parameterTypeName = parameter.type().name();

        if (PRIMITIVE_TYPES.contains(parameterTypeName)) {
            return;
        }

        parameter.annotate(required ? Nonnull.class : Nullable.class);
    }

    private static class PropertyInfo {
        private final String name;
        private final boolean required;

        PropertyInfo(String name, boolean required) {
            this.name = name;
            this.required = required;
        }
    }
}
