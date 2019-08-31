/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.common.internal.epl.annotation;

import com.espertech.esper.common.client.annotation.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage1.spec.AnnotationDesc;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.type.*;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleTypeCaster;
import com.espertech.esper.common.internal.util.SimpleTypeCasterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Utility to handle EPL statement annotations.
 */
public class AnnotationUtil {
    private static final Logger log = LoggerFactory.getLogger(AnnotationUtil.class);

    public static Map<String, List<AnnotationDesc>> mapByNameLowerCase(List<AnnotationDesc> annotations) {
        Map<String, List<AnnotationDesc>> map = new HashMap<String, List<AnnotationDesc>>();
        for (AnnotationDesc desc : annotations) {
            String key = desc.getName().toLowerCase(Locale.ENGLISH);

            if (map.containsKey(key)) {
                map.get(key).add(desc);
                continue;
            }

            List<AnnotationDesc> annos = new ArrayList<AnnotationDesc>(2);
            annos.add(desc);
            map.put(key, annos);
        }
        return map;
    }

    public static Object getValue(AnnotationDesc desc) {
        for (Pair<String, Object> pair : desc.getAttributes()) {
            if (pair.getFirst().toLowerCase(Locale.ENGLISH).equals("value")) {
                return pair.getSecond();
            }
        }
        return null;
    }

    /**
     * Compile annotation objects from descriptors.
     *
     * @param annotationSpec         spec for annotations
     * @param classpathImportService imports
     * @param compilable             statement expression
     * @return annotations
     * @throws StatementSpecCompileException compile exception
     */
    public static Annotation[] compileAnnotations(List<AnnotationDesc> annotationSpec, ClasspathImportServiceCompileTime classpathImportService, Compilable compilable) throws StatementSpecCompileException {
        Annotation[] annotations;
        try {
            annotations = AnnotationUtil.compileAnnotations(annotationSpec, classpathImportService);
        } catch (AnnotationException e) {
            throw new StatementSpecCompileException("Failed to process statement annotations: " + e.getMessage(), e, compilable.toEPL());
        } catch (RuntimeException ex) {
            String message = "Unexpected exception compiling annotations in statement, please consult the log file and report the exception: " + ex.getMessage();
            log.error(message, ex);
            throw new StatementSpecCompileException(message, ex, compilable.toEPL());
        }
        return annotations;
    }

    public static CodegenMethod makeAnnotations(Class arrayType, Annotation[] annotations, CodegenMethod parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(arrayType, AnnotationUtil.class, classScope);
        method.getBlock().declareVar(arrayType, "annotations", newArrayByLength(arrayType.getComponentType(), constant(annotations.length)));
        for (int i = 0; i < annotations.length; i++) {
            method.getBlock().assignArrayElement("annotations", constant(i), makeAnnotation(annotations[i], parent, classScope));
        }
        method.getBlock().methodReturn(ref("annotations"));
        return method;
    }

    /**
     * Compiles annotations to an annotation array.
     *
     * @param desc                   a list of descriptors
     * @param classpathImportService for resolving the annotation class
     * @return annotations or empty array if none
     * @throws AnnotationException if annotations could not be created
     */
    private static Annotation[] compileAnnotations(List<AnnotationDesc> desc, ClasspathImportServiceCompileTime classpathImportService)
        throws AnnotationException {
        Annotation[] annotations = new Annotation[desc.size()];
        for (int i = 0; i < desc.size(); i++) {
            annotations[i] = createProxy(desc.get(i), classpathImportService);
            if (annotations[i] instanceof Hint) {
                HintEnum.validateGetListed(annotations[i]);
            }
        }

        return annotations;
    }

    private static Annotation createProxy(AnnotationDesc desc, ClasspathImportServiceCompileTime classpathImportService)
        throws AnnotationException {
        // resolve class
        final Class annotationClass;
        try {
            annotationClass = classpathImportService.resolveAnnotation(desc.getName());
        } catch (ClasspathImportException e) {
            throw new AnnotationException("Failed to resolve @-annotation class: " + e.getMessage());
        }

        // obtain Annotation class properties
        List<AnnotationAttribute> annotationAttributeLists = getAttributes(annotationClass);
        Set<String> allAttributes = new HashSet<String>();
        Set<String> requiredAttributes = new LinkedHashSet<String>();
        for (AnnotationAttribute annotationAttribute : annotationAttributeLists) {
            allAttributes.add(annotationAttribute.getName());
            if (annotationAttribute.getDefaultValue() != null) {
                requiredAttributes.add(annotationAttribute.getName());
            }
        }

        // get attribute values
        List<String> providedValues = new ArrayList<String>();
        for (Pair<String, Object> annotationValuePair : desc.getAttributes()) {
            providedValues.add(annotationValuePair.getFirst());
        }

        // for all attributes determine value
        final Map<String, Object> properties = new LinkedHashMap<String, Object>();
        for (AnnotationAttribute annotationAttribute : annotationAttributeLists) {
            // find value pair for this attribute
            String attributeName = annotationAttribute.getName();
            Pair<String, Object> pairFound = null;
            for (Pair<String, Object> annotationValuePair : desc.getAttributes()) {
                if (annotationValuePair.getFirst().equals(attributeName)) {
                    pairFound = annotationValuePair;
                }
            }

            Object valueProvided = pairFound == null ? null : pairFound.getSecond();
            Object value = getFinalValue(annotationClass, annotationAttribute, valueProvided, classpathImportService);
            properties.put(attributeName, value);
            providedValues.remove(attributeName);
            requiredAttributes.remove(attributeName);
        }

        if (requiredAttributes.size() > 0) {
            List<String> required = new ArrayList<String>(requiredAttributes);
            Collections.sort(required);
            throw new AnnotationException("Annotation '" + annotationClass.getSimpleName() + "' requires a value for attribute '" + required.iterator().next() + "'");
        }

        if (providedValues.size() > 0) {
            List<String> provided = new ArrayList<String>(providedValues);
            Collections.sort(provided);
            if (allAttributes.contains(provided.get(0))) {
                throw new AnnotationException("Annotation '" + annotationClass.getSimpleName() + "' has duplicate attribute values for attribute '" + provided.get(0) + "'");
            } else {
                throw new AnnotationException("Annotation '" + annotationClass.getSimpleName() + "' does not have an attribute '" + provided.get(0) + "'");
            }
        }

        // return handler
        InvocationHandler handler = new EPLAnnotationInvocationHandler(annotationClass, properties);
        return (Annotation) Proxy.newProxyInstance(classpathImportService.getClassLoader(), new Class[]{annotationClass}, handler);
    }

    private static Object getFinalValue(Class annotationClass, AnnotationAttribute annotationAttribute, Object value, ClasspathImportServiceCompileTime classpathImportService) throws AnnotationException {
        if (value == null) {
            if (annotationAttribute.getDefaultValue() == null) {
                throw new AnnotationException("Annotation '" + annotationClass.getSimpleName() + "' requires a value for attribute '" + annotationAttribute.getName() + "'");
            }
            return annotationAttribute.getDefaultValue();
        }

        // handle non-array
        if (!annotationAttribute.getType().isArray()) {
            // handle primitive value
            if (!annotationAttribute.getType().isAnnotation()) {
                // if expecting an enumeration type, allow string value
                if (annotationAttribute.getType().isEnum() && JavaClassHelper.isImplementsInterface(value.getClass(), CharSequence.class)) {
                    String valueString = value.toString().trim();

                    // find case-sensitive exact match first
                    for (Object constant : annotationAttribute.getType().getEnumConstants()) {
                        Enum e = (Enum) constant;
                        if (e.name().equals(valueString)) {
                            return constant;
                        }
                    }

                    // find case-insensitive match
                    String valueUppercase = valueString.toUpperCase(Locale.ENGLISH);
                    for (Object constant : annotationAttribute.getType().getEnumConstants()) {
                        Enum e = (Enum) constant;
                        if (e.name().toUpperCase(Locale.ENGLISH).equals(valueUppercase)) {
                            return constant;
                        }
                    }

                    throw new AnnotationException("Annotation '" + annotationClass.getSimpleName() + "' requires an enum-value '" +
                        annotationAttribute.getType().getSimpleName() + "' for attribute '" + annotationAttribute.getName() +
                        "' but received '" + value + "' which is not one of the enum choices");
                }

                // cast as required
                SimpleTypeCaster caster = SimpleTypeCasterFactory.getCaster(value.getClass(), annotationAttribute.getType());
                Object finalValue = caster.cast(value);
                if (finalValue == null) {
                    throw new AnnotationException("Annotation '" + annotationClass.getSimpleName() + "' requires a " +
                        annotationAttribute.getType().getSimpleName() + "-typed value for attribute '" + annotationAttribute.getName() + "' but received " +
                        "a " + value.getClass().getSimpleName() + "-typed value");
                }
                return finalValue;
            } else {
                // nested annotation
                if (!(value instanceof AnnotationDesc)) {
                    throw new AnnotationException("Annotation '" + annotationClass.getSimpleName() + "' requires a " +
                        annotationAttribute.getType().getSimpleName() + "-typed value for attribute '" + annotationAttribute.getName() + "' but received " +
                        "a " + value.getClass().getSimpleName() + "-typed value");
                }
                return createProxy((AnnotationDesc) value, classpathImportService);
            }
        }

        if (!value.getClass().isArray()) {
            throw new AnnotationException("Annotation '" + annotationClass.getSimpleName() + "' requires a " +
                annotationAttribute.getType().getSimpleName() + "-typed value for attribute '" + annotationAttribute.getName() + "' but received " +
                "a " + value.getClass().getSimpleName() + "-typed value");
        }

        Class componentType = annotationAttribute.getType().getComponentType();
        Object array = Array.newInstance(componentType, Array.getLength(value));

        for (int i = 0; i < Array.getLength(value); i++) {
            Object arrayValue = Array.get(value, i);
            if (arrayValue == null) {
                throw new AnnotationException("Annotation '" + annotationClass.getSimpleName() + "' requires a " +
                    "non-null value for array elements for attribute '" + annotationAttribute.getName() + "'");
            }

            Object finalValue;
            if (arrayValue instanceof AnnotationDesc) {
                Annotation inner = createProxy((AnnotationDesc) arrayValue, classpathImportService);
                if (inner.annotationType() != componentType) {
                    throw makeArrayMismatchException(annotationClass, componentType, annotationAttribute.getName(), inner.annotationType());
                }
                finalValue = inner;
            } else {
                SimpleTypeCaster caster = SimpleTypeCasterFactory.getCaster(arrayValue.getClass(), annotationAttribute.getType().getComponentType());
                finalValue = caster.cast(arrayValue);
                if (finalValue == null) {
                    throw makeArrayMismatchException(annotationClass, componentType, annotationAttribute.getName(), arrayValue.getClass());
                }
            }
            Array.set(array, i, finalValue);
        }
        return array;
    }

    private static List<AnnotationAttribute> getAttributes(Class annotationClass) {
        List<AnnotationAttribute> props = new ArrayList<AnnotationAttribute>();
        Method[] methods = annotationClass.getMethods();
        if (methods == null) {
            return Collections.EMPTY_LIST;
        }

        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getReturnType() == void.class) {
                continue;
            }
            if (methods[i].getParameterTypes().length > 0) {
                continue;
            }
            if ((methods[i].getName().equals("class")) ||
                (methods[i].getName().equals("getClass")) ||
                (methods[i].getName().equals("toString")) ||
                (methods[i].getName().equals("annotationType")) ||
                (methods[i].getName().equals("hashCode"))) {
                continue;
            }

            props.add(new AnnotationAttribute(methods[i].getName(), methods[i].getReturnType(), methods[i].getDefaultValue()));
        }

        Collections.sort(props, new Comparator<AnnotationAttribute>() {
            public int compare(AnnotationAttribute o1, AnnotationAttribute o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return props;
    }

    public static boolean hasAnnotation(Annotation[] annotations, Class<? extends Annotation> annotationClass) {
        if (!annotationClass.isAnnotation()) {
            throw new IllegalArgumentException("Class " + annotationClass.getName() + " is not an annotation class");
        }
        if (annotations == null || annotations.length == 0) {
            return false;
        }
        for (Annotation anno : annotations) {
            if (JavaClassHelper.isImplementsInterface(anno.getClass(), annotationClass)) {
                return true;
            }

            // also check the annotations of the annotation, recursively
            Annotation[] declared = anno.annotationType().getDeclaredAnnotations();
            if (declared != null && declared.length > 0 && hasAnnotationDeclared(declared, annotationClass)) {
                return true;
            }
        }

        return false;
    }

    public static Annotation findAnnotation(Annotation[] annotations, Class annotationClass) {
        if (!annotationClass.isAnnotation()) {
            throw new IllegalArgumentException("Class " + annotationClass.getName() + " is not an annotation class");
        }
        if (annotations == null || annotations.length == 0) {
            return null;
        }
        for (Annotation anno : annotations) {
            if (JavaClassHelper.isImplementsInterface(anno.getClass(), annotationClass)) {
                return anno;
            }
        }

        return null;
    }

    private static boolean hasAnnotationDeclared(final Annotation[] annotations, final Class<? extends Annotation> target) {
        return hasAnnotationDeclaredRecursive(annotations, target, new HashSet<>());
    }

    private static boolean hasAnnotationDeclaredRecursive(final Annotation[] annotations, final Class<? extends Annotation> target,
                                                          final Set<Annotation> visited) {
        if (annotations == null) {
            return false;
        }
        for (Annotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType == target) {
                return true;
            }
            if (!visited.add(annotation)) {
                return false;
            }
            Annotation[] declared = annotationType.getDeclaredAnnotations();
            if (hasAnnotationDeclaredRecursive(declared, target, visited)) {
                return true;
            }
        }
        return false;
    }

    public static List<Annotation> findAnnotations(Annotation[] annotations, Class annotationClass) {
        if (!annotationClass.isAnnotation()) {
            throw new IllegalArgumentException("Class " + annotationClass.getName() + " is not an annotation class");
        }
        if (annotations == null || annotations.length == 0) {
            return null;
        }
        List<Annotation> annotationsList = new ArrayList<Annotation>();
        for (Annotation anno : annotations) {
            if (JavaClassHelper.isImplementsInterface(anno.getClass(), annotationClass)) {
                annotationsList.add(anno);
            }
        }
        return annotationsList;
    }

    public static Annotation[] mergeAnnotations(Annotation[] first, Annotation[] second) {
        return (Annotation[]) CollectionUtil.addArrays(first, second);
    }

    public static String getExpectSingleStringValue(String msgPrefix, List<AnnotationDesc> annotationsSameName) throws ExprValidationException {
        if (annotationsSameName.size() > 1) {
            throw new ExprValidationException(msgPrefix + " multiple annotations provided named '" + annotationsSameName.get(0).getName() + "'");
        }
        AnnotationDesc annotation = annotationsSameName.get(0);
        Object value = AnnotationUtil.getValue(annotation);
        if (value == null) {
            throw new ExprValidationException(msgPrefix + " no value provided for annotation '" + annotation.getName() + "', expected a value");
        }
        if (!(value instanceof String)) {
            throw new ExprValidationException(msgPrefix + " string value expected for annotation '" + annotation.getName() + "'");
        }
        return (String) value;
    }

    private static AnnotationException makeArrayMismatchException(Class annotationClass, Class componentType, String attributeName, Class unexpected) {
        return new AnnotationException("Annotation '" + annotationClass.getSimpleName() + "' requires a " +
            componentType.getSimpleName() + "-typed value for array elements for attribute '" + attributeName + "' but received " +
            "a " + unexpected.getSimpleName() + "-typed value");
    }

    private static CodegenExpression makeAnnotation(Annotation annotation, CodegenMethod parent, CodegenClassScope codegenClassScope) {
        if (annotation == null) {
            return constantNull();
        } else if (annotation instanceof Name) {
            return newInstance(AnnotationName.class, constant(((Name) annotation).value()));
        } else if (annotation instanceof Priority) {
            return newInstance(AnnotationPriority.class, constant(((Priority) annotation).value()));
        } else if (annotation instanceof Tag) {
            Tag tag = (Tag) annotation;
            return newInstance(AnnotationTag.class, constant(tag.name()), constant(tag.value()));
        } else if (annotation instanceof Drop) {
            return newInstance(AnnotationDrop.class);
        } else if (annotation instanceof Description) {
            return newInstance(AnnotationDescription.class, constant(((Description) annotation).value()));
        } else if (annotation instanceof Hint) {
            Hint hint = (Hint) annotation;
            return newInstance(AnnotationHint.class, constant(hint.value()), constant(hint.applies()), constant(hint.model()));
        } else if (annotation instanceof NoLock) {
            return newInstance(AnnotationNoLock.class);
        } else if (annotation instanceof Audit) {
            Audit hint = (Audit) annotation;
            return newInstance(AnnotationAudit.class, constant(hint.value()));
        } else if (annotation instanceof EventRepresentation) {
            EventRepresentation anno = (EventRepresentation) annotation;
            return newInstance(AnnotationEventRepresentation.class, enumValue(anno.value().getClass(), anno.value().name()));
        } else if (annotation instanceof IterableUnbound) {
            return newInstance(AnnotationIterableUnbound.class);
        } else if (annotation instanceof Hook) {
            Hook hook = (Hook) annotation;
            return newInstance(AnnotationHook.class, enumValue(HookType.class, hook.type().name()), constant(hook.hook()));
        } else if (annotation instanceof AvroSchemaField) {
            AvroSchemaField field = (AvroSchemaField) annotation;
            return newInstance(AvroSchemaFieldHook.class, constant(field.name()), constant(field.schema()));
        } else if (annotation instanceof Private) {
            return newInstance(AnnotationPrivate.class);
        } else if (annotation instanceof Protected) {
            return newInstance(AnnotationProtected.class);
        } else if (annotation instanceof Public) {
            return newInstance(AnnotationPublic.class);
        } else if (annotation instanceof BusEventType) {
            return newInstance(AnnotationBusEventType.class);
        } else if (annotation instanceof JsonSchema) {
            JsonSchema jsonSchema = (JsonSchema) annotation;
            return newInstance(AnnotationJsonSchema.class, constant(jsonSchema.dynamic()), constant(jsonSchema.className()));
        } else if (annotation instanceof JsonSchemaField) {
            JsonSchemaField field = (JsonSchemaField) annotation;
            return newInstance(AnnotationJsonSchemaField.class, constant(field.name()), constant(field.adapter()));
        } else if (annotation instanceof XMLSchema) {
            XMLSchema xmlSchema = (XMLSchema) annotation;
            return AnnotationXMLSchema.toExpression(xmlSchema, parent, codegenClassScope);
        } else if (annotation instanceof XMLSchemaNamespacePrefix) {
            XMLSchemaNamespacePrefix prefix = (XMLSchemaNamespacePrefix) annotation;
            return AnnotationXMLSchemaNamespacePrefix.toExpression(prefix, parent, codegenClassScope);
        } else if (annotation instanceof XMLSchemaField) {
            XMLSchemaField field = (XMLSchemaField) annotation;
            return AnnotationXMLSchemaField.toExpression(field, parent, codegenClassScope);
        } else if (annotation.annotationType().getPackage().equals(Name.class.getPackage())) {
            throw new IllegalStateException("Unrecognized annotation residing in the '" + Name.class.getPackage() + " package having type" + annotation.annotationType().getName());
        } else {
            // application-provided annotation
            EPLAnnotationInvocationHandler innerProxy = (EPLAnnotationInvocationHandler) Proxy.getInvocationHandler(annotation);
            CodegenMethod methodNode = parent.makeChild(Annotation.class, AnnotationUtil.class, codegenClassScope);
            CodegenExpressionNewAnonymousClass clazz = newAnonymousClass(methodNode.getBlock(), annotation.annotationType());

            CodegenMethod annotationType = CodegenMethod.makeParentNode(Class.class, AnnotationUtil.class, codegenClassScope);
            clazz.addMethod("annotationType", annotationType);
            annotationType.getBlock().methodReturn(clazz(innerProxy.getAnnotationClass()));

            for (Method method : innerProxy.getAnnotationClass().getMethods()) {
                if (method.getName().equals("equals") || method.getName().equals("hashCode") || method.getName().equals("toString") || method.getName().equals("annotationType")) {
                    continue;
                }

                CodegenMethod annotationValue = CodegenMethod.makeParentNode(method.getReturnType(), AnnotationUtil.class, codegenClassScope);
                Object value = innerProxy.getAttributes().get(method.getName());
                clazz.addMethod(method.getName(), annotationValue);

                CodegenExpression valueExpression;
                if (value == null) {
                    valueExpression = constantNull();
                } else if (method.getReturnType() == Class.class) {
                    valueExpression = clazz((Class) value);
                } else if (method.getReturnType().isArray() && method.getReturnType().getComponentType().isAnnotation()) {
                    valueExpression = localMethod(makeAnnotations(method.getReturnType(), (Annotation[]) value, methodNode, codegenClassScope));
                } else if (!method.getReturnType().isAnnotation()) {
                    valueExpression = constant(value);
                } else {
                    valueExpression = cast(method.getReturnType(), makeAnnotation((Annotation) value, methodNode, codegenClassScope));
                }
                annotationValue.getBlock().methodReturn(valueExpression);
            }

            methodNode.getBlock().methodReturn(clazz);
            return localMethod(methodNode);
        }
    }
}
