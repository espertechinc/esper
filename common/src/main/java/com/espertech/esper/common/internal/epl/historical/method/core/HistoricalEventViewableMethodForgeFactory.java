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
package com.espertech.esper.common.internal.epl.historical.method.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.compile.stage1.spec.MethodStreamSpec;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.historical.method.poll.MethodPollingExecStrategyEnum;
import com.espertech.esper.common.internal.epl.script.core.ExprNodeScript;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.settings.ClasspathImportSingleRowDesc;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class HistoricalEventViewableMethodForgeFactory {
    public static HistoricalEventViewableMethodForge createMethodStatementView(int stream, MethodStreamSpec methodStreamSpec, StatementBaseInfo base, StatementCompileTimeServices services)
            throws ExprValidationException {
        VariableMetaData variableMetaData = services.getVariableCompileTimeResolver().resolve(methodStreamSpec.getClassName());
        MethodPollingExecStrategyEnum strategy;
        Method methodReflection = null;
        String eventTypeNameProvidedUDFOrScript = null;
        String contextName = base.getStatementSpec().getRaw().getOptionalContextName();
        ClasspathImportServiceCompileTime classpathImportService = services.getClasspathImportServiceCompileTime();

        // see if this is a script in the from-clause
        ExprNodeScript scriptExpression = null;
        if (methodStreamSpec.getClassName() == null && methodStreamSpec.getMethodName() != null) {
            ExpressionScriptProvided script = services.getScriptCompileTimeResolver().resolve(methodStreamSpec.getMethodName(), methodStreamSpec.getExpressions().size());
            if (script != null) {
                scriptExpression = new ExprNodeScript(services.getConfiguration().getCompiler().getScripts().getDefaultDialect(), script, methodStreamSpec.getExpressions());
            }
        }

        try {
            if (scriptExpression != null) {
                eventTypeNameProvidedUDFOrScript = scriptExpression.getEventTypeNameAnnotation();
                strategy = MethodPollingExecStrategyEnum.TARGET_SCRIPT;
                EPLValidationUtil.validateSimpleGetSubtree(ExprNodeOrigin.METHODINVJOIN, scriptExpression, null, false, base.getStatementRawInfo(), services);
            } else if (variableMetaData != null) {
                String variableName = variableMetaData.getVariableName();
                if (variableMetaData.getOptionalContextName() != null) {
                    if (contextName == null || !contextName.equals(variableMetaData.getOptionalContextName())) {
                        throw new ExprValidationException("Variable by name '" + variableMetaData.getVariableName() + "' has been declared for context '" + variableMetaData.getOptionalContextName() + "' and can only be used within the same context");
                    }
                    strategy = MethodPollingExecStrategyEnum.TARGET_VAR_CONTEXT;
                } else {
                    if (variableMetaData.isConstant()) {
                        strategy = MethodPollingExecStrategyEnum.TARGET_CONST;
                    } else {
                        strategy = MethodPollingExecStrategyEnum.TARGET_VAR;
                    }
                }
                methodReflection = classpathImportService.resolveNonStaticMethodOverloadChecked(variableMetaData.getType(), methodStreamSpec.getMethodName());
            } else if (methodStreamSpec.getClassName() == null) { // must be either UDF or script
                Pair<Class, ClasspathImportSingleRowDesc> udf;
                try {
                    udf = classpathImportService.resolveSingleRow(methodStreamSpec.getMethodName());
                } catch (ClasspathImportException ex) {
                    throw new ExprValidationException("Failed to find user-defined function '" + methodStreamSpec.getMethodName() + "': " + ex.getMessage(), ex);
                }
                methodReflection = classpathImportService.resolveMethodOverloadChecked(udf.getFirst(), methodStreamSpec.getMethodName());
                eventTypeNameProvidedUDFOrScript = udf.getSecond().getOptionalEventTypeName();
                strategy = MethodPollingExecStrategyEnum.TARGET_CONST;
            } else {
                methodReflection = classpathImportService.resolveMethodOverloadChecked(methodStreamSpec.getClassName(), methodStreamSpec.getMethodName());
                strategy = MethodPollingExecStrategyEnum.TARGET_CONST;
            }
        } catch (ExprValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ExprValidationException(e.getMessage(), e);
        }

        Class methodProviderClass = null;
        Class beanClass;
        LinkedHashMap<String, Object> oaType = null;
        Map<String, Object> mapType = null;
        boolean isCollection = false;
        boolean isIterator = false;
        EventType eventType;
        EventType eventTypeWhenMethodReturnsEventBeans = null;
        boolean isStaticMethod = false;

        if (methodReflection != null) {
            methodProviderClass = methodReflection.getDeclaringClass();
            isStaticMethod = variableMetaData == null;

            // Determine object type returned by method
            beanClass = methodReflection.getReturnType();
            if ((beanClass == void.class) || (beanClass == Void.class) || (JavaClassHelper.isJavaBuiltinDataType(beanClass))) {
                throw new ExprValidationException("Invalid return type for static method '" + methodReflection.getName() + "' of class '" + methodStreamSpec.getClassName() + "', expecting a Java class");
            }
            if (methodReflection.getReturnType().isArray() && methodReflection.getReturnType().getComponentType() != EventBean.class) {
                beanClass = methodReflection.getReturnType().getComponentType();
            }

            isCollection = JavaClassHelper.isImplementsInterface(beanClass, Collection.class);
            Class collectionClass = null;
            if (isCollection) {
                collectionClass = JavaClassHelper.getGenericReturnType(methodReflection, true);
                beanClass = collectionClass;
            }

            isIterator = JavaClassHelper.isImplementsInterface(beanClass, Iterator.class);
            Class iteratorClass = null;
            if (isIterator) {
                iteratorClass = JavaClassHelper.getGenericReturnType(methodReflection, true);
                beanClass = iteratorClass;
            }

            // If the method returns a Map, look up the map type
            String mapTypeName = null;
            if ((JavaClassHelper.isImplementsInterface(methodReflection.getReturnType(), Map.class)) ||
                    (methodReflection.getReturnType().isArray() && JavaClassHelper.isImplementsInterface(methodReflection.getReturnType().getComponentType(), Map.class)) ||
                    (isCollection && JavaClassHelper.isImplementsInterface(collectionClass, Map.class)) ||
                    (isIterator && JavaClassHelper.isImplementsInterface(iteratorClass, Map.class))) {
                MethodMetadataDesc metadata;
                if (variableMetaData != null) {
                    metadata = getCheckMetadataVariable(methodStreamSpec.getMethodName(), variableMetaData, classpathImportService, Map.class);
                } else {
                    metadata = getCheckMetadataNonVariable(methodStreamSpec.getMethodName(), methodStreamSpec.getClassName(), classpathImportService, Map.class);
                }
                mapTypeName = metadata.getTypeName();
                mapType = (Map<String, Object>) metadata.getTypeMetadata();
            }

            // If the method returns an Object[] or Object[][], look up the type information
            String oaTypeName = null;
            if (methodReflection.getReturnType() == Object[].class ||
                    methodReflection.getReturnType() == Object[][].class ||
                    (isCollection && collectionClass == Object[].class) ||
                    (isIterator && iteratorClass == Object[].class)) {
                MethodMetadataDesc metadata;
                if (variableMetaData != null) {
                    metadata = getCheckMetadataVariable(methodStreamSpec.getMethodName(), variableMetaData, classpathImportService, LinkedHashMap.class);
                } else {
                    metadata = getCheckMetadataNonVariable(methodStreamSpec.getMethodName(), methodStreamSpec.getClassName(), classpathImportService, LinkedHashMap.class);
                }
                oaTypeName = metadata.getTypeName();
                oaType = (LinkedHashMap<String, Object>) metadata.getTypeMetadata();
            }

            // Determine event type from class and method name
            // If the method returns EventBean[], require the event type
            Function<EventTypeApplicationType, EventTypeMetadata> metadata = apptype -> {
                String eventTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousMethodHistorical(stream);
                return new EventTypeMetadata(eventTypeName, base.getModuleName(), EventTypeTypeClass.METHODPOLLDERIVED, apptype, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
            };
            if ((methodReflection.getReturnType().isArray() && methodReflection.getReturnType().getComponentType() == EventBean.class) ||
                    (isCollection && collectionClass == EventBean.class) ||
                    (isIterator && iteratorClass == EventBean.class)) {
                String typeName = methodStreamSpec.getEventTypeName() == null ? eventTypeNameProvidedUDFOrScript : methodStreamSpec.getEventTypeName();
                eventType = EventTypeUtility.requireEventType("Method", methodReflection.getName(), typeName, services.getEventTypeCompileTimeResolver());
                eventTypeWhenMethodReturnsEventBeans = eventType;
            } else if (mapType != null) {
                eventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata.apply(EventTypeApplicationType.MAP), mapType, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
                services.getEventTypeCompileTimeRegistry().newType(eventType);
            } else if (oaType != null) {
                eventType = BaseNestableEventUtil.makeOATypeCompileTime(metadata.apply(EventTypeApplicationType.OBJECTARR), oaType, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
                services.getEventTypeCompileTimeRegistry().newType(eventType);
            } else {
                BeanEventTypeStem stem = services.getBeanEventTypeStemService().getCreateStem(beanClass, null);
                eventType = new BeanEventType(stem, metadata.apply(EventTypeApplicationType.CLASS), services.getBeanEventTypeFactoryPrivate(), null, null, null, null);
                services.getEventTypeCompileTimeRegistry().newType(eventType);
            }

            // the @type is only allowed in conjunction with EventBean return types
            if (methodStreamSpec.getEventTypeName() != null && eventTypeWhenMethodReturnsEventBeans == null) {
                throw new ExprValidationException(EventTypeUtility.disallowedAtTypeMessage());
            }
        } else {
            String eventTypeName = methodStreamSpec.getEventTypeName() == null ? scriptExpression.getEventTypeNameAnnotation() : methodStreamSpec.getEventTypeName();
            eventType = EventTypeUtility.requireEventType("Script", scriptExpression.getScript().getName(), eventTypeName, services.getEventTypeCompileTimeResolver());
        }

        // metadata
        MethodPollingViewableMeta meta = new MethodPollingViewableMeta(methodProviderClass, isStaticMethod, mapType, oaType, strategy, isCollection, isIterator, variableMetaData, eventTypeWhenMethodReturnsEventBeans, scriptExpression);
        return new HistoricalEventViewableMethodForge(stream, eventType, methodStreamSpec, meta);
    }

    private static MethodMetadataDesc getCheckMetadataVariable(String methodName, VariableMetaData variableMetaData, ClasspathImportServiceCompileTime classpathImportService, Class metadataClass)
            throws ExprValidationException {
        Method typeGetterMethod = getRequiredTypeGetterMethodCanNonStatic(methodName, null, variableMetaData.getType(), classpathImportService, metadataClass);

        if (Modifier.isStatic(typeGetterMethod.getModifiers())) {
            return invokeMetadataMethod(null, variableMetaData.getClass().getSimpleName(), typeGetterMethod);
        }

        // if the metadata is not a static method and we don't have an instance this is a problem
        String messagePrefix = "Failed to access variable method invocation metadata: ";
        Object value = variableMetaData.getValueWhenAvailable();
        if (value == null) {
            throw new ExprValidationException(messagePrefix + "The variable value is null and the metadata method is an instance method");
        }

        if (value instanceof EventBean) {
            value = ((EventBean) value).getUnderlying();
        }
        return invokeMetadataMethod(value, variableMetaData.getClass().getSimpleName(), typeGetterMethod);
    }

    private static MethodMetadataDesc getCheckMetadataNonVariable(String methodName, String className, ClasspathImportServiceCompileTime classpathImportService, Class metadataClass) throws ExprValidationException {
        Method typeGetterMethod = getRequiredTypeGetterMethodCanNonStatic(methodName, className, null, classpathImportService, metadataClass);
        return invokeMetadataMethod(null, className, typeGetterMethod);
    }

    private static Method getRequiredTypeGetterMethodCanNonStatic(String methodName, String classNameWhenNoClass, Class clazzWhenAvailable, ClasspathImportServiceCompileTime classpathImportService, Class metadataClass)
            throws ExprValidationException {
        Method typeGetterMethod;
        String getterMethodName = methodName + "Metadata";
        try {
            if (clazzWhenAvailable != null) {
                typeGetterMethod = classpathImportService.resolveMethod(clazzWhenAvailable, getterMethodName, new Class[0], new boolean[0]);
            } else {
                typeGetterMethod = classpathImportService.resolveMethodOverloadChecked(classNameWhenNoClass, getterMethodName, new Class[0], new boolean[0], new boolean[0]);
            }
        } catch (Exception e) {
            throw new ExprValidationException("Could not find getter method for method invocation, expected a method by name '" + getterMethodName + "' accepting no parameters");
        }

        boolean fail;
        if (metadataClass.isInterface()) {
            fail = !JavaClassHelper.isImplementsInterface(typeGetterMethod.getReturnType(), metadataClass);
        } else {
            fail = typeGetterMethod.getReturnType() != metadataClass;
        }
        if (fail) {
            throw new ExprValidationException("Getter method '" + typeGetterMethod.getName() + "' does not return " + JavaClassHelper.getClassNameFullyQualPretty(metadataClass));
        }

        return typeGetterMethod;
    }

    private static MethodMetadataDesc invokeMetadataMethod(Object target, String className, Method typeGetterMethod)
            throws ExprValidationException {
        Object resultType;
        try {
            resultType = typeGetterMethod.invoke(target);
        } catch (Exception e) {
            throw new ExprValidationException("Error invoking metadata getter method for method invocation, for method by name '" + typeGetterMethod.getName() + "' accepting no parameters: " + e.getMessage(), e);
        }
        if (resultType == null) {
            throw new ExprValidationException("Error invoking metadata getter method for method invocation, method returned a null value");
        }

        return new MethodMetadataDesc(className + "." + typeGetterMethod.getName(), resultType);
    }

    public static class MethodMetadataDesc {
        private final String typeName;
        private final Object typeMetadata;

        public MethodMetadataDesc(String typeName, Object typeMetadata) {
            this.typeName = typeName;
            this.typeMetadata = typeMetadata;
        }

        public String getTypeName() {
            return typeName;
        }

        public Object getTypeMetadata() {
            return typeMetadata;
        }
    }
}
