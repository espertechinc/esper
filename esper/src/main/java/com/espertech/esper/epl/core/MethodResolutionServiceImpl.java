/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.core;

import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.agg.aggregator.*;
import com.espertech.esper.epl.agg.service.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.agg.service.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlan;
import com.espertech.esper.epl.approx.CountMinSketchAggState;
import com.espertech.esper.epl.approx.CountMinSketchSpec;
import com.espertech.esper.epl.approx.CountMinSketchState;
import com.espertech.esper.epl.agg.factory.AggregationFactoryFactoryDefault;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.agg.factory.AggregationFactoryFactory;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateFactory;
import com.espertech.esper.schedule.TimeProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Implements method resolution.
 */
public class MethodResolutionServiceImpl implements MethodResolutionService
{
	private final EngineImportService engineImportService;
    private final TimeProvider timeProvider;

    /**
     * Ctor.
     * @param engineImportService is the engine imports
     * @param timeProvider returns time
     */
    public MethodResolutionServiceImpl(EngineImportService engineImportService,
                                       TimeProvider timeProvider)
	{
        this.engineImportService = engineImportService;
        this.timeProvider = timeProvider;
    }

    public boolean isUdfCache() {
        return engineImportService.isUdfCache();
    }

    public boolean isDuckType() {
        return engineImportService.isDuckType();
    }

    public boolean isSortUsingCollator() {
        return engineImportService.isSortUsingCollator();
    }

    public Method resolveMethod(String className, String methodName, Class[] paramTypes, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType)
			throws EngineImportException
    {
        return engineImportService.resolveMethod(className, methodName, paramTypes, allowEventBeanType, allowEventBeanCollType);
	}

    public Method resolveMethod(String className, String methodName)
			throws EngineImportException
    {
        return engineImportService.resolveMethod(className, methodName);
	}

    public Method resolveNonStaticMethod(Class clazz, String methodName) throws EngineImportException
    {
        return engineImportService.resolveNonStaticMethod(clazz, methodName);
    }

    public Constructor resolveCtor(Class clazz, Class[] paramTypes) throws EngineImportException {
        return engineImportService.resolveCtor(clazz, paramTypes);
    }

    public Class resolveClass(String className, boolean forAnnotation) throws EngineImportException {
        return engineImportService.resolveClass(className, forAnnotation);
	}

    public Method resolveMethod(Class clazz, String methodName, Class[] paramTypes, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType) throws EngineImportException
    {
        return engineImportService.resolveMethod(clazz, methodName, paramTypes, allowEventBeanType, allowEventBeanCollType);
    }

    public AggregationFunctionFactory resolveAggregationFactory(String functionName) throws EngineImportUndefinedException, EngineImportException
    {
        return engineImportService.resolveAggregationFactory(functionName);
    }

    public Pair<Class, EngineImportSingleRowDesc> resolveSingleRow(String functionName) throws EngineImportUndefinedException, EngineImportException
    {
        return engineImportService.resolveSingleRow(functionName);
    }

    public AggregationMethod[] newAggregators(AggregationMethodFactory[] prototypes, int agentInstanceId) {
        return newAggregatorsInternal(prototypes, agentInstanceId);
    }

    public AggregationMethod[] newAggregators(AggregationMethodFactory[] prototypes, int agentInstanceId, Object groupKey, Object groupKeyBinding, AggregationGroupByRollupLevel groupByRollupLevel) {
        return newAggregatorsInternal(prototypes, agentInstanceId);
    }

    public AggregationMethod[] newAggregatorsInternal(AggregationMethodFactory[] prototypes, int agentInstanceId) {
        AggregationMethod row[] = new AggregationMethod[prototypes.length];
        for (int i = 0; i < prototypes.length; i++)
        {
            row[i] = prototypes[i].make(this, agentInstanceId, -1, i);
        }
        return row;
    }

    public long getCurrentRowCount(AggregationMethod[] aggregators, AggregationState[] groupStates)
    {
        return 0;   // since the aggregators are always fresh ones 
    }

    public void removeAggregators(int agentInstanceId, Object groupKey, Object groupKeyBinding, AggregationGroupByRollupLevel level)
    {
        // To be overridden by implementations that care when aggregators get removed
    }

    public AggregationState[] newAccesses(int agentInstanceId, boolean isJoin, AggregationStateFactory[] accessAggSpecs, AggregationServicePassThru passThru) {
        return newAccessInternal(agentInstanceId, accessAggSpecs, isJoin, null, passThru);
    }

    public AggregationState[] newAccesses(int agentInstanceId, boolean isJoin, AggregationStateFactory[] accessAggSpecs, Object groupKey, Object groupKeyBinding, AggregationGroupByRollupLevel groupByRollupLevel, AggregationServicePassThru passThru) {
        return newAccessInternal(agentInstanceId, accessAggSpecs, isJoin, groupKey, passThru);
    }

    private AggregationState[] newAccessInternal(int agentInstanceId, AggregationStateFactory[] accessAggSpecs, boolean isJoin, Object groupKey, AggregationServicePassThru passThru) {
        AggregationState[] row = new AggregationState[accessAggSpecs.length];
        int i = 0;
        for (AggregationStateFactory spec : accessAggSpecs) {
            row[i] = spec.createAccess(this, agentInstanceId, 0, i, isJoin, groupKey, passThru);   // no group id assigned
            i++;
        }
        return row;
    }

    public AggregationState makeAccessAggLinearNonJoin(int agentInstanceId, int groupId, int aggregationId, int streamNum, AggregationServicePassThru passThru) {
        return new AggregationStateImpl(streamNum);
    }

    public AggregationState makeAccessAggLinearJoin(int agentInstanceId, int groupId, int aggregationId, int streamNum, AggregationServicePassThru passThru) {
        return new AggregationStateJoinImpl(streamNum);
    }

    public AggregationState makeAccessAggSortedNonJoin(int agentInstanceId, int groupId, int aggregationId, AggregationStateSortedSpec spec, AggregationServicePassThru passThru) {
        return new AggregationStateSortedImpl(spec);
    }

    public AggregationState makeAccessAggSortedJoin(int agentInstanceId, int groupId, int aggregationId, AggregationStateSortedSpec spec, AggregationServicePassThru passThru) {
        return new AggregationStateSortedJoin(spec);
    }

    public AggregationState makeAccessAggMinMaxEver(int agentInstanceId, int groupId, int aggregationId, AggregationStateMinMaxByEverSpec spec, AggregationServicePassThru passThru) {
        return new AggregationStateMinMaxByEver(spec);
    }

    public AggregationState makeAccessAggPlugin(int agentInstanceId, int groupId, int aggregationId, boolean join, PlugInAggregationMultiFunctionStateFactory providerFactory, Object groupKey) {
        PlugInAggregationMultiFunctionStateContext context = new PlugInAggregationMultiFunctionStateContext(agentInstanceId, groupKey);
        return providerFactory.makeAggregationState(context);
    }

    public AggregationState makeCountMinSketch(int agentInstanceId, int groupId, int aggregationId, CountMinSketchSpec specification) {
        return new CountMinSketchAggState(CountMinSketchState.makeState(specification), specification.getAgent());
    }

    public void destroyedAgentInstance(int agentInstanceId) {
        // no action require
    }

    public EngineImportService getEngineImportService() {
        return engineImportService;
    }

    public Object getCriteriaKeyBinding(ExprEvaluator[] evaluators) {
        return null;    // no bindings
    }

    public Object getGroupKeyBinding(ExprNode[] groupKeyExpressions, AggregationGroupByRollupDesc groupByRollupDesc) {
        return null;    // no bindings
    }

    public Object getGroupKeyBinding(AggregationLocalGroupByPlan localGroupByPlan) {
        return null;    // no bindings
    }

    public AggregationFactoryFactory getAggregationFactoryFactory() {
        return AggregationFactoryFactoryDefault.INSTANCE;
    }
}
