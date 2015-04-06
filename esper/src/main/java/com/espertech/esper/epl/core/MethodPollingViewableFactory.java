/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.core;

import com.espertech.esper.client.*;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.epl.db.DataCache;
import com.espertech.esper.epl.db.DataCacheFactory;
import com.espertech.esper.epl.db.PollExecStrategy;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.MethodStreamSpec;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.schedule.ScheduleBucket;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.HistoricalEventViewable;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory for method-invocation data provider streams.
 */
public class MethodPollingViewableFactory
{
    /**
     * Creates a method-invocation polling view for use as a stream that calls a method, or pulls results from cache.
     * @param streamNumber the stream number
     * @param methodStreamSpec defines the class and method to call
     * @param eventAdapterService for creating event types and events
     * @param epStatementAgentInstanceHandle for time-based callbacks
     * @param methodResolutionService for resolving classes and imports
     * @param engineImportService for resolving configurations
     * @param schedulingService for scheduling callbacks in expiry-time based caches
     * @param scheduleBucket for schedules within the statement
     * @param exprEvaluatorContext expression evaluation context
     * @return pollable view
     * @throws ExprValidationException if the expressions cannot be validated or the method descriptor
     * has incorrect class and method names, or parameter number and types don't match
     */
    public static HistoricalEventViewable createPollMethodView(int streamNumber,
                                                               MethodStreamSpec methodStreamSpec,
                                                               EventAdapterService eventAdapterService,
                                                               EPStatementAgentInstanceHandle epStatementAgentInstanceHandle,
                                                               MethodResolutionService methodResolutionService,
                                                               EngineImportService engineImportService,
                                                               SchedulingService schedulingService,
                                                               ScheduleBucket scheduleBucket,
                                                               ExprEvaluatorContext exprEvaluatorContext,
                                                               VariableService variableService,
                                                               String contextName)
            throws ExprValidationException
    {
        VariableMetaData variableMetaData = variableService.getVariableMetaData(methodStreamSpec.getClassName());
        MethodPollingExecStrategyEnum strategy;
        VariableReader variableReader;
        String variableName;

        // Try to resolve the method
        Method methodReflection;
        FastMethod methodFastClass;
        Class declaringClass;
        Object invocationTarget;
        try
		{
            if (variableMetaData != null) {
                variableName = variableMetaData.getVariableName();
                if (variableMetaData.getContextPartitionName() != null) {
                    if (contextName == null || !contextName.equals(variableMetaData.getContextPartitionName())) {
                        throw new ExprValidationException("Variable by name '" + variableMetaData.getVariableName() + "' has been declared for context '" + variableMetaData.getContextPartitionName() + "' and can only be used within the same context");
                    }
                    strategy = MethodPollingExecStrategyEnum.TARGET_VAR_CONTEXT;
                    variableReader = null;
                    invocationTarget = null;
                }
                else {
                    variableReader = variableService.getReader(methodStreamSpec.getClassName(), VariableService.NOCONTEXT_AGENTINSTANCEID);
                    if (variableMetaData.isConstant()) {
                        invocationTarget = variableReader.getValue();
                        if (invocationTarget instanceof EventBean) {
                            invocationTarget = ((EventBean) invocationTarget).getUnderlying();
                        }
                        strategy = MethodPollingExecStrategyEnum.TARGET_CONST;
                    }
                    else {
                        invocationTarget = null;
                        strategy = MethodPollingExecStrategyEnum.TARGET_VAR;
                    }
                }
                methodReflection = methodResolutionService.resolveNonStaticMethod(variableMetaData.getType(), methodStreamSpec.getMethodName());
            }
            else {
                methodReflection = methodResolutionService.resolveMethod(methodStreamSpec.getClassName(), methodStreamSpec.getMethodName());
                invocationTarget = null;
                variableReader = null;
                variableName = null;
                strategy = MethodPollingExecStrategyEnum.TARGET_CONST;
            }
            declaringClass = methodReflection.getDeclaringClass();
            FastClass declaringFastClass = FastClass.create(Thread.currentThread().getContextClassLoader(), methodReflection.getDeclaringClass());
			methodFastClass = declaringFastClass.getMethod(methodReflection);
		}
        catch(ExprValidationException e)
        {
            throw e;
        }
		catch(Exception e)
		{
			throw new ExprValidationException(e.getMessage(), e);
		}

        // Determine object type returned by method
        Class beanClass = methodFastClass.getReturnType();
        if ((beanClass == void.class) || (beanClass == Void.class) || (JavaClassHelper.isJavaBuiltinDataType(beanClass)))
        {
            throw new ExprValidationException("Invalid return type for static method '" + methodFastClass.getName() + "' of class '" + methodStreamSpec.getClassName() + "', expecting a Java class");
        }
        if (methodFastClass.getReturnType().isArray())
        {
            beanClass = methodFastClass.getReturnType().getComponentType();
        }

        boolean isCollection = JavaClassHelper.isImplementsInterface(beanClass, Collection.class);
        Class collectionClass = null;
        if (isCollection) {
            collectionClass = JavaClassHelper.getGenericReturnType(methodReflection, true);
            beanClass = collectionClass;
        }

        boolean isIterator = JavaClassHelper.isImplementsInterface(beanClass, Iterator.class);
        Class iteratorClass = null;
        if (isIterator) {
            iteratorClass = JavaClassHelper.getGenericReturnType(methodReflection, true);
            beanClass = iteratorClass;
        }

        // If the method returns a Map, look up the map type
        Map<String, Object> mapType = null;
        String mapTypeName = null;
        if ( (JavaClassHelper.isImplementsInterface(methodFastClass.getReturnType(), Map.class)) ||
             (methodFastClass.getReturnType().isArray() && JavaClassHelper.isImplementsInterface(methodFastClass.getReturnType().getComponentType(), Map.class)) ||
              (isCollection && JavaClassHelper.isImplementsInterface(collectionClass, Map.class)) ||
              (isIterator && JavaClassHelper.isImplementsInterface(iteratorClass, Map.class)))
        {
            MethodMetadataDesc metadata = getCheckMetadata(methodStreamSpec.getMethodName(), methodStreamSpec.getClassName(), methodResolutionService, Map.class);
            mapTypeName = metadata.getTypeName();
            mapType = (Map<String, Object>) metadata.getTypeMetadata();
        }

        // If the method returns an Object[] or Object[][], look up the type information
        LinkedHashMap<String, Object> oaType = null;
        String oaTypeName = null;
        if (methodFastClass.getReturnType() == Object[].class ||
            methodFastClass.getReturnType() == Object[][].class ||
            (isCollection && collectionClass == Object[].class) ||
            (isIterator && iteratorClass == Object[].class))
        {
            MethodMetadataDesc metadata = getCheckMetadata(methodStreamSpec.getMethodName(), methodStreamSpec.getClassName(), methodResolutionService, LinkedHashMap.class);
            oaTypeName = metadata.getTypeName();
            oaType = (LinkedHashMap<String, Object>) metadata.getTypeMetadata();
        }

        // Determine event type from class and method name
        EventType eventType;
        if (mapType != null) {
            eventType = eventAdapterService.addNestableMapType(mapTypeName, mapType, null, false, true, true, false, false);
        }
        else if (oaType != null) {
            eventType = eventAdapterService.addNestableObjectArrayType(oaTypeName, oaType, null, false, true, true, false, false, false, null);
        }
        else {
            eventType = eventAdapterService.addBeanType(beanClass.getName(), beanClass, false, true, true);
        }

        // Construct polling strategy as a method invocation
        ConfigurationMethodRef configCache = engineImportService.getConfigurationMethodRef(declaringClass.getName());
        if (configCache == null)
        {
            configCache = engineImportService.getConfigurationMethodRef(declaringClass.getSimpleName());
        }
        ConfigurationDataCache dataCacheDesc = (configCache != null) ? configCache.getDataCacheDesc() : null;
        DataCache dataCache = DataCacheFactory.getDataCache(dataCacheDesc, epStatementAgentInstanceHandle, schedulingService, scheduleBucket);
        PollExecStrategy methodPollStrategy;
        if (mapType != null) {
            if (methodFastClass.getReturnType().isArray()) {
                methodPollStrategy = new MethodPollingExecStrategyMapArray(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
            else if (isCollection) {
                methodPollStrategy = new MethodPollingExecStrategyMapCollection(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
            else if (isIterator) {
                methodPollStrategy = new MethodPollingExecStrategyMapIterator(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
            else {
                methodPollStrategy = new MethodPollingExecStrategyMapPlain(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
        }
        else if (oaType != null) {
            if (methodFastClass.getReturnType() == Object[][].class) {
                methodPollStrategy = new MethodPollingExecStrategyOAArray(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
            else if (isCollection) {
                methodPollStrategy = new MethodPollingExecStrategyOACollection(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
            else if (isIterator) {
                methodPollStrategy = new MethodPollingExecStrategyOAIterator(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
            else {
                methodPollStrategy = new MethodPollingExecStrategyOAPlain(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
        }
        else {
            if (methodFastClass.getReturnType().isArray()) {
                methodPollStrategy = new MethodPollingExecStrategyPOJOArray(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
            else if (isCollection) {
                methodPollStrategy = new MethodPollingExecStrategyPOJOCollection(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
            else if (isIterator) {
                methodPollStrategy = new MethodPollingExecStrategyPOJOIterator(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
            else {
                methodPollStrategy = new MethodPollingExecStrategyPOJOPlain(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            }
        }

        return new MethodPollingViewable(variableMetaData == null, methodReflection.getDeclaringClass(), methodStreamSpec, streamNumber, methodStreamSpec.getExpressions(), methodPollStrategy, dataCache, eventType, exprEvaluatorContext);
    }

    private static MethodMetadataDesc getCheckMetadata(String methodName, String className, MethodResolutionService methodResolutionService, Class metadataClass) {
        Method typeGetterMethod;
        String getterMethodName = methodName + "Metadata";
        try {
            typeGetterMethod = methodResolutionService.resolveMethod(className, getterMethodName, new Class[0], new boolean[0], new boolean[0]);
        }
        catch(Exception e) {
            throw new EPException("Could not find getter method for method invocation, expected a method by name '" + getterMethodName + "' accepting no parameters");
        }

        boolean fail = false;
        if (metadataClass.isInterface()) {
            fail = !JavaClassHelper.isImplementsInterface(typeGetterMethod.getReturnType(), metadataClass);
        }
        else {
            fail = typeGetterMethod.getReturnType() != metadataClass;
        }
        if (fail) {
            throw new EPException("Getter method '" + getterMethodName + "' does not return " + JavaClassHelper.getClassNameFullyQualPretty(metadataClass));
        }

        Object resultType;
        try {
            resultType = typeGetterMethod.invoke(null);
        }
        catch (Exception e) {
            throw new EPException("Error invoking metadata getter method for method invocation, for method by name '" + getterMethodName + "' accepting no parameters: " + e.getMessage(), e);
        }
        if (resultType == null) {
            throw new EPException("Error invoking metadata getter method for method invocation, method returned a null value");
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
