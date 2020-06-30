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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.bean.core.PropertyStem;
import com.espertech.esper.common.internal.event.bean.introspect.PropertyListBuilderPublic;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeFactoryBuiltinClassTyped;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
import com.espertech.esper.common.internal.util.ConstructorHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class JsonEventTypeUtilityReflective {

    public static LinkedHashMap<EPTypeClass, JsonApplicationClassDelegateDesc> computeClassesDeep(EPTypeClass clazz, String eventTypeName, Annotation[] annotations, StatementCompileTimeServices services)
        throws ExprValidationException {
        LinkedHashMap<EPTypeClass, List<Field>> deepClassesWFields = new LinkedHashMap<>();
        computeClassesDeep(clazz, deepClassesWFields, new ArrayDeque<>(), annotations, services);
        return assignDelegateClassNames(eventTypeName, deepClassesWFields);
    }


    public static LinkedHashMap<EPTypeClass, JsonApplicationClassDelegateDesc> computeClassesDeep(Map<String, Object> fields, String eventTypeName, Annotation[] annotations, StatementCompileTimeServices services)
        throws ExprValidationException {
        LinkedHashMap<EPTypeClass, List<Field>> deepClassesWFields = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (entry.getValue() instanceof EPTypeClass) {
                EPTypeClass clazz = (EPTypeClass) entry.getValue();
                if (isDeepClassEligibleType(clazz, entry.getKey(), null, annotations, services)) {
                    computeClassesDeep(clazz, deepClassesWFields, new ArrayDeque<>(), annotations, services);
                }
            }
        }
        return assignDelegateClassNames(eventTypeName, deepClassesWFields);
    }

    private static LinkedHashMap<EPTypeClass, JsonApplicationClassDelegateDesc> assignDelegateClassNames(String eventTypeName, LinkedHashMap<EPTypeClass, List<Field>> deepClassesWFields) {
        LinkedHashMap<EPTypeClass, JsonApplicationClassDelegateDesc> classes = new LinkedHashMap<>();
        for (Map.Entry<EPTypeClass, List<Field>> classEntry : deepClassesWFields.entrySet()) {
            String replaced = classEntry.getKey().getTypeName().replaceAll("\\.", "_").replaceAll("\\$", "_");
            String delegateClassName = eventTypeName + "_Delegate_" + replaced;
            String delegateFactoryClassName = eventTypeName + "_Factory_" + replaced;
            classes.put(classEntry.getKey(), new JsonApplicationClassDelegateDesc(delegateClassName, delegateFactoryClassName, classEntry.getValue()));
        }
        return classes;
    }

    private static void computeClassesDeep(EPTypeClass clazz, Map<EPTypeClass, List<Field>> deepClasses, Deque<EPTypeClass> stack, Annotation[] annotations, StatementCompileTimeServices services) throws ExprValidationException {
        if (deepClasses.containsKey(clazz)) {
            return;
        }

        List<Field> fields = resolveFields(clazz);

        // we go deep first
        for (Field field : fields) {
            EPTypeClass fieldType = ClassHelperGenericType.getFieldEPType(field);

            if (JavaClassHelper.isImplementsInterface(fieldType, Collection.class)) {
                EPTypeClass parameter = JavaClassHelper.getSingleParameterTypeOrObject(fieldType);
                if (parameter != null && !stack.contains(parameter) && isDeepClassEligibleType(parameter, field.getName(), field, annotations, services) && parameter.getType() != Object.class) {
                    stack.add(parameter);
                    computeClassesDeep(parameter, deepClasses, stack, annotations, services);
                    stack.removeLast();
                }
                continue;
            }

            if (field.getType().isArray()) {
                EPTypeClass arrayType = JavaClassHelper.getArrayComponentTypeInnermost(fieldType);
                if (!stack.contains(arrayType) && isDeepClassEligibleType(arrayType, field.getName(), field, annotations, services) && arrayType.getType() != Object.class) {
                    stack.add(arrayType);
                    computeClassesDeep(arrayType, deepClasses, stack, annotations, services);
                    stack.removeLast();
                }
                continue;
            }

            if (!stack.contains(fieldType) && isDeepClassEligibleType(fieldType, field.getName(), field, annotations, services)) {
                stack.add(fieldType);
                computeClassesDeep(fieldType, deepClasses, stack, annotations, services);
                stack.removeLast();
            }
        }

        deepClasses.put(clazz, fields);
    }

    private static boolean isDeepClassEligibleType(EPTypeClass genericType, String fieldName, Field optionalField, Annotation[] annotations, StatementCompileTimeServices services) throws ExprValidationException {
        if (!ConstructorHelper.hasDefaultConstructor(genericType.getType())) {
            return false;
        }
        try {
            JsonForgeFactoryBuiltinClassTyped.forge(genericType, fieldName, optionalField, Collections.emptyMap(), annotations, services);
            return false;
        } catch (UnsupportedOperationException ex) {
            return true;
        }
    }

    private static List<Field> resolveFields(EPTypeClass clazz) {
        PropertyListBuilderPublic propertyListBuilder = new PropertyListBuilderPublic(new ConfigurationCommonEventTypeBean());
        List<PropertyStem> properties = propertyListBuilder.assessProperties(clazz.getType());
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
