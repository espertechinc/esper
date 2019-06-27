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
package com.espertech.esper.common.internal.event.json.compiletime;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.bean.core.PropertyStem;
import com.espertech.esper.common.internal.event.bean.introspect.PropertyListBuilderPublic;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeFactoryBuiltinClassTyped;
import com.espertech.esper.common.internal.util.ConstructorHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class JsonEventTypeUtilityReflective {

    public static LinkedHashMap<Class, JsonApplicationClassDelegateDesc> computeClassesDeep(Class clazz, String eventTypeName, Annotation[] annotations, StatementCompileTimeServices services)
        throws ExprValidationException {
        LinkedHashMap<Class, List<Field>> deepClassesWFields = new LinkedHashMap<>();
        computeClassesDeep(clazz, deepClassesWFields, new ArrayDeque<>(), annotations, services);
        return assignDelegateClassNames(eventTypeName, deepClassesWFields);
    }


    public static LinkedHashMap<Class, JsonApplicationClassDelegateDesc> computeClassesDeep(Map<String, Object> fields, String eventTypeName, Annotation[] annotations, StatementCompileTimeServices services)
        throws ExprValidationException {
        LinkedHashMap<Class, List<Field>> deepClassesWFields = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (entry.getValue() instanceof Class) {
                Class clazz = (Class) entry.getValue();
                if (isDeepClassEligibleType(clazz, entry.getKey(), null, annotations, services)) {
                    computeClassesDeep(clazz, deepClassesWFields, new ArrayDeque<>(), annotations, services);
                }
            }
        }
        return assignDelegateClassNames(eventTypeName, deepClassesWFields);
    }

    private static LinkedHashMap<Class, JsonApplicationClassDelegateDesc> assignDelegateClassNames(String eventTypeName, LinkedHashMap<Class, List<Field>> deepClassesWFields) {
        LinkedHashMap<Class, JsonApplicationClassDelegateDesc> classes = new LinkedHashMap<>();
        for (Map.Entry<Class, List<Field>> classEntry : deepClassesWFields.entrySet()) {
            String replaced = classEntry.getKey().getName().replaceAll("\\.", "_").replaceAll("\\$", "_");
            String delegateClassName = eventTypeName + "_Delegate_" + replaced;
            String delegateFactoryClassName = eventTypeName + "_Factory_" + replaced;
            classes.put(classEntry.getKey(), new JsonApplicationClassDelegateDesc(delegateClassName, delegateFactoryClassName, classEntry.getValue()));
        }
        return classes;
    }

    private static void computeClassesDeep(Class clazz, Map<Class, List<Field>> deepClasses, Deque<Class> stack, Annotation[] annotations, StatementCompileTimeServices services) throws ExprValidationException {
        if (deepClasses.containsKey(clazz)) {
            return;
        }

        List<Field> fields = resolveFields(clazz);

        // we go deep first
        for (Field field : fields) {
            if (JavaClassHelper.isImplementsInterface(field.getType(), Collection.class)) {
                Class genericType = JavaClassHelper.getGenericFieldType(field, true);
                if (genericType != null && !stack.contains(genericType) && isDeepClassEligibleType(genericType, field.getName(), field, annotations, services) && genericType != Object.class) {
                    stack.add(genericType);
                    computeClassesDeep(genericType, deepClasses, stack, annotations, services);
                    stack.removeLast();
                }
                continue;
            }

            if (field.getType().isArray()) {
                Class arrayType = JavaClassHelper.getArrayComponentTypeInnermost(field.getType());
                if (!stack.contains(arrayType) && isDeepClassEligibleType(arrayType, field.getName(), field, annotations, services) && arrayType != Object.class) {
                    stack.add(arrayType);
                    computeClassesDeep(arrayType, deepClasses, stack, annotations, services);
                    stack.removeLast();
                }
                continue;
            }

            if (!stack.contains(field.getType()) && isDeepClassEligibleType(field.getType(), field.getName(), field, annotations, services)) {
                stack.add(field.getType());
                computeClassesDeep(field.getType(), deepClasses, stack, annotations, services);
                stack.removeLast();
            }
        }

        deepClasses.put(clazz, fields);
    }

    private static boolean isDeepClassEligibleType(Class genericType, String fieldName, Field optionalField, Annotation[] annotations, StatementCompileTimeServices services) throws ExprValidationException {
        if (!ConstructorHelper.hasDefaultConstructor(genericType)) {
            return false;
        }
        try {
            JsonForgeFactoryBuiltinClassTyped.forge(genericType, fieldName, optionalField, Collections.emptyMap(), annotations, services);
            return false;
        } catch (UnsupportedOperationException ex) {
            return true;
        }
    }

    private static List<Field> resolveFields(Class clazz) {
        PropertyListBuilderPublic propertyListBuilder = new PropertyListBuilderPublic(new ConfigurationCommonEventTypeBean());
        List<PropertyStem> properties = propertyListBuilder.assessProperties(clazz);
        List<Field> props = new ArrayList<>();
        for (PropertyStem stem : properties) {
            Field field = stem.getAccessorField();
            if (field == null) {
                continue;
            }
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                props.add(field);
            }
        }
        return props;
    }
}
