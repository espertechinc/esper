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
package com.espertech.esper.plugin;

import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.rettype.EPType;

/**
 * Part of the aggregation multi-function extension API, this class represents
 * one of more aggregation function expression instances. This class is responsible for providing
 * a state reader (called accessor) for returning value from aggregation state, and for
 * providing return type information of the accessor, and for providing state factory
 * information.
 * <p>
 * Note the information returned by {@link #getReturnType()} must match the
 * value objects returned by accessors provided by {@link #getAccessorForge()}.
 * </p>
 * <p>
 * For example, assuming you have an EPL statement such as <code>select search(), query() from MyEvent</code>
 * then you would likely use one handler class and two handler objects (one for search and one for query).
 * </p>
 */
public interface PlugInAggregationMultiFunctionHandler {

    /**
     * Returns the forge for the read function (an 'accessor').
     * <p>
     * Typically your application creates one accessor class
     * for each aggregation function name. So if you have two aggregation
     * functions such as "query" and "search" you would have two
     * accessor classes, one for "query" and one for "search".
     * </p>
     * <p>
     * Each aggregation function as it occurs in an EPL statement
     * obtains its own accessor. Your application can
     * return the same accessor object for all aggregation functions,
     * or different accessor objects for each aggregation function.
     * </p>
     * <p>
     * The objects returned by your accessor must match the
     * return type declared through {@link #getReturnType()}.
     * </p>
     *
     * @return accessor forge
     */
    public AggregationAccessorForge getAccessorForge();

    /**
     * Provide return type.
     * <p>
     * The accessor return values must match the return type declared herein.
     * </p>
     * <p>
     * Use {@link com.espertech.esper.epl.rettype.EPTypeHelper#singleValue(Class)} (Class)} to indicate that the accessor
     * returns a single value. The accessor should return the single value upon invocation of
     * {@link AggregationAccessor#getValue(com.espertech.esper.epl.agg.access.AggregationState, com.espertech.esper.client.EventBean[], boolean, com.espertech.esper.epl.expression.core.ExprEvaluatorContext)}.
     * The accessor should return a null value for all other accessor methods.
     * </p>
     * <p>
     * Use {@link com.espertech.esper.epl.rettype.EPTypeHelper#collectionOfEvents(com.espertech.esper.client.EventType)} to indicate that the accessor
     * returns a collection of events. The accessor should return a value in
     * {@link AggregationAccessor#getEnumerableEvents(com.espertech.esper.epl.agg.access.AggregationState, com.espertech.esper.client.EventBean[], boolean, com.espertech.esper.epl.expression.core.ExprEvaluatorContext)}.
     * The accessor can also return an array of underlying event objects in
     * {@link AggregationAccessor#getValue(com.espertech.esper.epl.agg.access.AggregationState, com.espertech.esper.client.EventBean[], boolean, com.espertech.esper.epl.expression.core.ExprEvaluatorContext)}.
     * The accessor should return a null value for all other accessor methods.
     * </p>
     * <p>
     * Use {@link com.espertech.esper.epl.rettype.EPTypeHelper#singleEvent(com.espertech.esper.client.EventType)} to indicate that the accessor
     * returns a single event. The accessor should return a value in
     * {@link AggregationAccessor#getEnumerableEvent(com.espertech.esper.epl.agg.access.AggregationState, com.espertech.esper.client.EventBean[], boolean, com.espertech.esper.epl.expression.core.ExprEvaluatorContext)}.
     * The accessor can also return the underlying event object in
     * {@link AggregationAccessor#getValue(com.espertech.esper.epl.agg.access.AggregationState, com.espertech.esper.client.EventBean[], boolean, com.espertech.esper.epl.expression.core.ExprEvaluatorContext)}.
     * The accessor should return a null value for all other accessor methods.
     * </p>
     * <p>
     * Use {@link com.espertech.esper.epl.rettype.EPTypeHelper#collectionOfSingleValue(Class)} to indicate that the accessor
     * returns a collection of single values (scalar, object etc.). The accessor should return a java.util.Collection in
     * {@link AggregationAccessor#getValue(com.espertech.esper.epl.agg.access.AggregationState, com.espertech.esper.client.EventBean[], boolean, com.espertech.esper.epl.expression.core.ExprEvaluatorContext)}.
     * The accessor should return a null value for all other accessor methods.
     * </p>
     * <p>
     * Use {@link com.espertech.esper.epl.rettype.EPTypeHelper#array(Class)} to indicate that the accessor
     * returns an array of single values. The accessor should return an array in
     * {@link AggregationAccessor#getValue(com.espertech.esper.epl.agg.access.AggregationState, com.espertech.esper.client.EventBean[], boolean, com.espertech.esper.epl.expression.core.ExprEvaluatorContext)}.
     * The accessor should return a null value for all other accessor methods.
     * </p>
     *
     * @return expression result type
     */
    public EPType getReturnType();

    /**
     * Return a state-key object that determines how the engine shares aggregation state
     * between multiple aggregation functions that may appear in the same EPL statement.
     * <p>
     * The engine applies equals-semantics to determine state sharing. If
     * two {@link AggregationStateKey} instances are equal (implement hashCode and equals)
     * then the engine shares a single aggregation state instance for the two
     * aggregation function expressions.
     * </p>
     * <p>
     * If your aggregation function never needs shared state
     * simple return <code>new AggregationStateKey(){}</code>.
     * </p>
     * <p>
     * If your aggregation function always shares state
     * simple declare <code>private static final AggregationStateKey MY_KEY = new AggregationStateKey() {};</code>
     * and <code>return MY_KEY</code>; (if using multiple handlers declare the key on the factory level).
     * </p>
     *
     * @return state key
     */
    public AggregationStateKey getAggregationStateUniqueKey();

    public PlugInAggregationMultiFunctionStateForge getStateForge();

    public AggregationAgentForge getAggregationAgent(PlugInAggregationMultiFunctionAgentContext agentContext);

    default PlugInAggregationMultiFunctionCodegenType getCodegenType() {
        return PlugInAggregationMultiFunctionCodegenType.CODEGEN_NONE;
    }
}
