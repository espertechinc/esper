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
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.access.AggregationStateMinMaxByEverSpec;
import com.espertech.esper.epl.agg.access.AggregationServicePassThru;
import com.espertech.esper.epl.agg.access.AggregationStateSortedSpec;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.agg.service.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlan;
import com.espertech.esper.epl.approx.CountMinSketchSpec;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateFactory;
import com.espertech.esper.type.MinMaxTypeEnum;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Service for resolving methods and aggregation functions, and for creating managing aggregation instances.
 */
public interface MethodResolutionService
{
    /**
     * Returns true to cache UDF results for constant parameter sets.
     * @return cache UDF results config
     */
    public boolean isUdfCache();

    public boolean isDuckType();

    public boolean isSortUsingCollator();

    /**
     * Resolves a given method name and list of parameter types to an instance or static method exposed by the given class.
     *
     * @param clazz is the class to look for a fitting method
     * @param methodName is the method name
     * @param paramTypes is parameter types match expression sub-nodes
     * @return method this resolves to
     * @throws EngineImportException if the method cannot be resolved to a visible static or instance method
     */
    public Method resolveMethod(Class clazz, String methodName, Class[] paramTypes, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType) throws EngineImportException;

    /**
     * Resolves matching available constructors to a list of parameter types to an instance or static method exposed by the given class.
     * @param clazz is the class to look for a fitting method
     * @param paramTypes is parameter types match expression sub-nodes
     * @return method this resolves to
     * @throws EngineImportException if the method cannot be resolved to a visible static or instance method
     */
    public Constructor resolveCtor(Class clazz, Class[] paramTypes) throws EngineImportException;

    /**
     * Resolves a given class, method and list of parameter types to a static method.
     *
     * @param className is the class name to use
     * @param methodName is the method name
     * @param paramTypes is parameter types match expression sub-nodes
     * @param allowEventBeanType allow event bean type footprint
     * @return method this resolves to
     * @throws EngineImportException if the method cannot be resolved to a visible static method
     */
    public Method resolveMethod(String className, String methodName, Class[] paramTypes, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType) throws EngineImportException;

    /**
     * Resolves a given class and method name to a static method, not allowing overloaded methods
     * and expecting the method to be found exactly once with zero or more parameters.
     * @param className is the class name to use
     * @param methodName is the method name
     * @return method this resolves to
     * @throws EngineImportException if the method cannot be resolved to a visible static method, or if the method exists more
     * then once with different parameters
     */
    public Method resolveMethod(String className, String methodName) throws EngineImportException;

    /**
     * Resolves a given class and method name to a non-static method, not allowing overloaded methods
     * and expecting the method to be found exactly once with zero or more parameters.
     * @param clazz is the clazz to use
     * @param methodName is the method name
     * @return method this resolves to
     * @throws EngineImportException if the method cannot be resolved to a visible static method, or if the method exists more
     * then once with different parameters
     */
    public Method resolveNonStaticMethod(Class clazz, String methodName) throws EngineImportException;

    /**
     * Resolves a given class name, either fully qualified and simple and imported to a class.
     * @param className is the class name to use
     * @return class this resolves to
     * @throws EngineImportException if there was an error resolving the class
     */
    public Class resolveClass(String className) throws EngineImportException;

    /**
     * Returns a plug-in aggregation function factory for a given configured aggregation function name.
     * @param functionName is the aggregation function name
     * @return aggregation-factory
     * @throws EngineImportUndefinedException is the function name cannot be found
     * @throws EngineImportException if there was an error resolving class information
     */
    public AggregationFunctionFactory resolveAggregationFactory(String functionName) throws EngineImportUndefinedException, EngineImportException;

    /**
     * Used at statement compile-time to try and resolve a given function name into an
     * single-row function. Matches function name case-neutral.
     * @param functionName is the function name
     * @throws EngineImportUndefinedException if the function is not a configured single-row function
     * @throws EngineImportException if the function providing class could not be loaded or doesn't match
     */
    public Pair<Class, EngineImportSingleRowDesc> resolveSingleRow(String functionName) throws EngineImportUndefinedException, EngineImportException;

    /**
     * Makes a new count-aggregator.
     *
     * @param agentInstanceId
     * @param groupId
     *@param aggregationId
     * @param isIgnoreNull is true to ignore nulls, or false to count nulls  @return aggregator
     */
    public AggregationMethod makeCountAggregator(int agentInstanceId, int groupId, int aggregationId, boolean isIgnoreNull, boolean hasFilter);

    /**
     * Makes a new first-value aggregator.
     *
     * @param agentInstanceId
     * @param groupId
     *@param aggregationId
     * @param type of value  @return aggregator
     */
    public AggregationMethod makeFirstEverValueAggregator(int agentInstanceId, int groupId, int aggregationId, Class type, boolean hasFilter);

    /**
     * Makes a new countever-value aggregator.
     *
     * @param agentInstanceId
     * @param groupId
     * @param aggregationId
     * @param ignoreNulls
     * */
    public AggregationMethod makeCountEverValueAggregator(int agentInstanceId, int groupId, int aggregationId, boolean hasFilter, boolean ignoreNulls);

    /**
     * Makes a new last-value aggregator.
     *
     * @param agentInstanceId
     * @param groupId
     *@param aggregationId
     * @param type of value  @return aggregator
     */
    public AggregationMethod makeLastEverValueAggregator(int agentInstanceId, int groupId, int aggregationId, Class type, boolean hasFilter);

    /**
     * Makes a new sum-aggregator.
     *
     * @param agentInstanceId
     * @param groupId
     *@param aggregationId
     * @param type is the type to be summed up, i.e. float, long etc.  @return aggregator
     */
    public AggregationMethod makeSumAggregator(int agentInstanceId, int groupId, int aggregationId, Class type, boolean hasFilter);

    public Class getSumAggregatorType(Class inputValueType);

    /**
     * Makes a new distinct-value-aggregator.
     *
     * @param agentInstanceId
     * @param groupId
     *@param aggregationId
     * @param aggregationMethod is the inner aggregation method
     * @param childType is the return type of the inner expression to aggregate, if any   @return aggregator
     */
    public AggregationMethod makeDistinctAggregator(int agentInstanceId, int groupId, int aggregationId, AggregationMethod aggregationMethod, Class childType, boolean hasFilter);

    /**
     * Makes a new avg-aggregator.
     *
     * @param agentInstanceId
     * @param groupId
     *@param aggregationId
     * @param type the expression return type  @return aggregator
     */
    public AggregationMethod makeAvgAggregator(int agentInstanceId, int groupId, int aggregationId, Class type, boolean hasFilter);
    public Class getAvgAggregatorType(Class childType);

    /**
     * Makes a new avedev-aggregator.
     * @return aggregator
     */
    public AggregationMethod makeAvedevAggregator(int agentInstanceId, int groupId, int aggregationId, boolean hasFilter);

    /**
     * Makes a new median-aggregator.
     * @return aggregator
     * @param agentInstanceId
     * @param groupId
     * @param aggregationId
     * @param hasFilter
     */
    public AggregationMethod makeMedianAggregator(int agentInstanceId, int groupId, int aggregationId, boolean hasFilter);

    /**
     * Makes a new min-max-aggregator.
     *
     * @param agentInstanceId
     * @param groupId
     *@param aggregationId
     * @param minMaxType dedicates whether to do min or max
     * @param targetType is the type to max or min
     * @param isHasDataWindows true for has data windows    @return aggregator to use
     */
    public AggregationMethod makeMinMaxAggregator(int agentInstanceId, int groupId, int aggregationId, MinMaxTypeEnum minMaxType, Class targetType, boolean isHasDataWindows, boolean hasFilter);

    /**
     * Makes a new stddev-aggregator.
     * @return aggregator
     */
    public AggregationMethod makeStddevAggregator(int agentInstanceId, int groupId, int aggregationId, boolean hasFilter);

    /**
     * Makes a new rate-aggregator.
     * @return aggregator
     * @param agentInstanceId
     * @param groupId
     * @param aggregationId
     */
    public AggregationMethod makeRateAggregator(int agentInstanceId, int groupId, int aggregationId);

    /**
     * Makes a new rate-aggregator.
     * @param interval seconds
     * @return aggregator to use
     */
    public AggregationMethod makeRateEverAggregator(int agentInstanceId, int groupId, int aggregationId, long interval);

    /**
     * Makes a Nth element aggregator.
     *
     * @param agentInstanceId
     * @param groupId
     *@param aggregationId
     * @param returnType of aggregation
     * @param size of elements   @return aggregator
     */
    public AggregationMethod makeNthAggregator(int agentInstanceId, int groupId, int aggregationId, Class returnType, int size);

    /**
     * Make leaving agg.
     * @return agg
     * @param agentInstanceId
     * @param groupId
     * @param aggregationId
     */
    public AggregationMethod makeLeavingAggregator(int agentInstanceId, int groupId, int aggregationId);

    /**
     * Returns a new set of aggregators given an existing prototype-set of aggregators for a given context partition and group key.
     *
     *
     *
     * @param prototypes is the prototypes
     * @param agentInstanceId context partition
     * @param groupKey is the key to group-by for
     * @param groupByRollupLevel
     * @return new set of aggregators for this group
     */
    public AggregationMethod[] newAggregators(AggregationMethodFactory[] prototypes, int agentInstanceId, Object groupKey, Object groupKeyBinding, AggregationGroupByRollupLevel groupByRollupLevel);

    /**
     * Returns a new set of aggregators given an existing prototype-set of aggregators for a given context partition (no groups).
     *
     * @param agentInstanceId context partition
     * @return new set of aggregators for this group
     */
    public AggregationMethod[] newAggregators(AggregationMethodFactory[] aggregators, int agentInstanceId);

    /**
     * Opportunity to remove aggregations for a group.
     * @param agentInstanceId
     * @param groupKey that is no longer used
     * @param level
     */
    public void removeAggregators(int agentInstanceId, Object groupKey, Object groupKeyBinding, AggregationGroupByRollupLevel level);

    /**
     * Returns the current row count of an aggregation, for use with resilience.
     * @param aggregators aggregators
     * @return row count
     */
    public long getCurrentRowCount(AggregationMethod[] aggregators, AggregationState[] states);

    public void destroyedAgentInstance(int agentInstanceId);

    public EngineImportService getEngineImportService();

    public AggregationState[] newAccesses(int agentInstanceId, boolean isJoin, AggregationStateFactory[] accessAggSpecs, AggregationServicePassThru passThru);

    public AggregationState[] newAccesses(int agentInstanceId, boolean isJoin, AggregationStateFactory[] accessAggSpecs, Object groupKey, Object groupKeyBinding, AggregationGroupByRollupLevel groupByRollupLevel, AggregationServicePassThru passThru);

    public AggregationState makeAccessAggLinearNonJoin(int agentInstanceId, int groupId, int aggregationId, int streamNum, AggregationServicePassThru passThru);

    public AggregationState makeAccessAggLinearJoin(int agentInstanceId, int groupId, int aggregationId, int streamNum, AggregationServicePassThru passThru);

    public AggregationState makeAccessAggSortedNonJoin(int agentInstanceId, int groupId, int aggregationId, AggregationStateSortedSpec spec, AggregationServicePassThru passThru);

    public AggregationState makeAccessAggSortedJoin(int agentInstanceId, int groupId, int aggregationId, AggregationStateSortedSpec spec, AggregationServicePassThru passThru);

    public AggregationState makeAccessAggMinMaxEver(int agentInstanceId, int groupId, int aggregationId, AggregationStateMinMaxByEverSpec spec, AggregationServicePassThru passThru);

    public AggregationState makeAccessAggPlugin(int agentInstanceId, int groupId, int aggregationId, boolean join, PlugInAggregationMultiFunctionStateFactory providerFactory, Object groupKey);

    public AggregationState makeCountMinSketch(int agentInstanceId, int groupId, int aggregationId, CountMinSketchSpec specification);

    public Object getCriteriaKeyBinding(ExprEvaluator[] evaluators);

    public Object getGroupKeyBinding(ExprNode[] groupKeyExpressions, AggregationGroupByRollupDesc groupByRollupDesc);

    public Object getGroupKeyBinding(AggregationLocalGroupByPlan localGroupByPlan);
}
