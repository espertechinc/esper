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
package com.espertech.esper.client;

import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.client.context.ContextPartitionVariableState;
import com.espertech.esper.client.dataflow.EPDataFlowRuntime;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.util.EventRenderer;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface to event stream processing runtime services.
 */
public interface EPRuntime {
    /**
     * Send an event represented by a plain Java object to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link #route(Object)}).
     *
     * @param object is the event to sent to the runtime
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    public void sendEvent(Object object) throws EPException;

    /**
     * Send a map containing event property values to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link #route(java.util.Map, String)}).
     *
     * @param map              - map that contains event property values. Keys are expected to be of type String while values
     *                         can be of any type. Keys and values should match those declared via Configuration for the given eventTypeName.
     * @param mapEventTypeName - the name for the Map event type that was previously configured
     * @throws EPException - when the processing of the event leads to an error
     */
    public void sendEvent(Map map, String mapEventTypeName) throws EPException;

    /**
     * Send an object array containing event property values to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link #route(Object[], String)}).
     *
     * @param objectarray              - array that contains event property values. Your application must ensure that property values
     *                                 match the exact same order that the property names and types have been declared, and that the array length matches the number of properties declared.
     * @param objectArrayEventTypeName - the name for the Object-array event type that was previously configured
     * @throws EPException - when the processing of the event leads to an error
     */
    public void sendEvent(Object[] objectarray, String objectArrayEventTypeName);

    /**
     * Send an event represented by a DOM node to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link #route(org.w3c.dom.Node)}).
     *
     * @param node is the DOM node as an event
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    public void sendEvent(org.w3c.dom.Node node) throws EPException;

    /**
     * Number of events evaluated over the lifetime of the event stream processing runtime,
     * or since the last resetStats() call.
     *
     * @return number of events received
     */
    public long getNumEventsEvaluated();

    /**
     * Reset number of events received and emitted
     */
    public void resetStats();

    /**
     * Route the event object back to the event stream processing runtime for internal dispatching,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     * The route event is processed just like it was sent to the runtime, that is any
     * active expressions seeking that event receive it. The routed event has priority over other
     * events sent to the runtime. In a single-threaded application the routed event is
     * processed before the next event is sent to the runtime through the
     * EPRuntime.sendEvent method.
     * <p>
     * Note: when outbound-threading is enabled, the thread delivering to listeners
     * is not the thread processing the original event. Therefore with outbound-threading
     * enabled the sendEvent method should be used by listeners instead.
     * </p>
     *
     * @param theEvent to route internally for processing by the event stream processing runtime
     */
    public void route(final Object theEvent);

    /**
     * Route the event object back to the event stream processing runtime for internal dispatching,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     * The route event is processed just like it was sent to the runtime, that is any
     * active expressions seeking that event receive it. The routed event has priority over other
     * events sent to the runtime. In a single-threaded application the routed event is
     * processed before the next event is sent to the runtime through the
     * EPRuntime.sendEvent method.
     * <p>
     * Note: when outbound-threading is enabled, the thread delivering to listeners
     * is not the thread processing the original event. Therefore with outbound-threading
     * enabled the sendEvent method should be used by listeners instead.
     * </p>
     *
     * @param map           - map that contains event property values. Keys are expected to be of type String while values
     *                      can be of any type. Keys and values should match those declared via Configuration for the given eventTypeName.
     * @param eventTypeName - the name for Map event type that was previously configured
     * @throws EPException - when the processing of the event leads to an error
     */
    public void route(Map map, String eventTypeName) throws EPException;

    /**
     * Route the event object back to the event stream processing runtime for internal dispatching,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     * The route event is processed just like it was sent to the runtime, that is any
     * active expressions seeking that event receive it. The routed event has priority over other
     * events sent to the runtime. In a single-threaded application the routed event is
     * processed before the next event is sent to the runtime through the
     * EPRuntime.sendEvent method.
     * <p>
     * Note: when outbound-threading is enabled, the thread delivering to listeners
     * is not the thread processing the original event. Therefore with outbound-threading
     * enabled the sendEvent method should be used by listeners instead.
     * </p>
     *
     * @param objectArray   - object array that contains event property values. Your application must ensure that property values
     *                      match the exact same order that the property names and types have been declared, and that the array length matches the number of properties declared.
     * @param eventTypeName - the name for Object-array event type that was previously configured
     * @throws EPException - when the processing of the event leads to an error
     */
    public void route(Object[] objectArray, String eventTypeName) throws EPException;

    /**
     * Route the event object back to the event stream processing runtime for internal dispatching,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     * The route event is processed just like it was sent to the runtime, that is any
     * active expressions seeking that event receive it. The routed event has priority over other
     * events sent to the runtime. In a single-threaded application the routed event is
     * processed before the next event is sent to the runtime through the
     * EPRuntime.sendEvent method.
     * <p>
     * Note: when outbound-threading is enabled, the thread delivering to listeners
     * is not the thread processing the original event. Therefore with outbound-threading
     * enabled the sendEvent method should be used by listeners instead.
     * </p>
     *
     * @param node is the DOM node as an event
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    public void route(org.w3c.dom.Node node) throws EPException;

    /**
     * Sets a listener to receive events that are unmatched by any statement.
     * <p>
     * Events that can be unmatched are all events that are send into a runtime via one
     * of the sendEvent methods, or that have been generated via insert-into clause.
     * <p>
     * For an event to be unmatched by any statement, the event must not match any
     * statement's event stream filter criteria (a where-clause is NOT a filter criteria for a stream, as below).
     * <p>
     * Note: In the following statement a MyEvent event does always match
     * this statement's event stream filter criteria, regardless of the value of the 'quantity' property.
     * <pre>select * from MyEvent where quantity &gt; 5</pre>
     * <br>
     * In the following statement only a MyEvent event with a 'quantity' property value of 5 or less does not match
     * this statement's event stream filter criteria:
     * <pre>select * from MyEvent(quantity &gt; 5)</pre>
     *
     * For patterns, if no pattern sub-expression is active for such event, the event is also unmatched.
     *
     * @param listener is the listener to receive notification of unmatched events, or null to unregister a
     *                 previously registered listener
     */
    public void setUnmatchedListener(UnmatchedListener listener);

    /**
     * Returns the current variable value for a global variable. A null value is a valid value for a variable.
     * Not for use with context-partitioned variables.
     *
     * @param variableName is the name of the variable to return the value for
     * @return current variable value
     * @throws VariableNotFoundException if a variable by that name has not been declared
     */
    public Object getVariableValue(String variableName) throws VariableNotFoundException;

    /**
     * Returns the current variable values for a context-partitioned variable, per context partition.
     * A null value is a valid value for a variable.
     * Only for use with context-partitioned variables.
     * Variable names provided must all be associated to the same context partition.
     *
     * @param variableNames            are the names of the variables to return the value for
     * @param contextPartitionSelector selector for the context partition to return the value for
     * @return current variable value
     * @throws VariableNotFoundException if a variable by that name has not been declared
     */
    public Map<String, List<ContextPartitionVariableState>> getVariableValue(Set<String> variableNames, ContextPartitionSelector contextPartitionSelector) throws VariableNotFoundException;

    /**
     * Returns current variable values for each of the global variable names passed in,
     * guaranteeing consistency in the face of concurrent updates to the variables.
     * Not for use with context-partitioned variables.
     *
     * @param variableNames is a set of variable names for which to return values
     * @return map of variable name and variable value
     * @throws VariableNotFoundException if any of the variable names has not been declared
     */
    public Map<String, Object> getVariableValue(Set<String> variableNames) throws VariableNotFoundException;

    /**
     * Returns current variable values for all global variables,
     * guaranteeing consistency in the face of concurrent updates to the variables.
     * Not for use with context-partitioned variables.
     *
     * @return map of variable name and variable value
     */
    public Map<String, Object> getVariableValueAll();

    /**
     * Sets the value of a single global variable.
     * <p>
     * Note that the thread setting the variable value queues the changes, i.e. it does not itself
     * re-evaluate such new variable value for any given statement. The timer thread performs this work.
     * </p>
     * Not for use with context-partitioned variables.
     *
     * @param variableName  is the name of the variable to change the value of
     * @param variableValue is the new value of the variable, with null an allowed value
     * @throws VariableValueException    if the value does not match variable type or cannot be safely coerced
     *                                   to the variable type
     * @throws VariableNotFoundException if the variable name has not been declared
     */
    public void setVariableValue(String variableName, Object variableValue) throws VariableValueException, VariableNotFoundException;

    /**
     * Sets the value of multiple global variables in one update, applying all or none of the changes
     * to variable values in one atomic transaction.
     * <p>
     * Note that the thread setting the variable value queues the changes, i.e. it does not itself
     * re-evaluate such new variable value for any given statement. The timer thread performs this work.
     * </p>
     * Not for use with context-partitioned variables.
     *
     * @param variableValues is the map of variable name and variable value, with null an allowed value
     * @throws VariableValueException    if any value does not match variable type or cannot be safely coerced
     *                                   to the variable type
     * @throws VariableNotFoundException if any of the variable names has not been declared
     */
    public void setVariableValue(Map<String, Object> variableValues) throws VariableValueException, VariableNotFoundException;

    /**
     * Sets the value of multiple context-partitioned variables in one update, applying all or none of the changes
     * to variable values in one atomic transaction.
     * <p>
     * Note that the thread setting the variable value queues the changes, i.e. it does not itself
     * re-evaluate such new variable value for any given statement. The timer thread performs this work.
     * </p>
     * Only for use with context-partitioned variables.
     *
     * @param variableValues  is the map of variable name and variable value, with null an allowed value
     * @param agentInstanceId the id of the context partition
     * @throws VariableValueException    if any value does not match variable type or cannot be safely coerced
     *                                   to the variable type
     * @throws VariableNotFoundException if any of the variable names has not been declared
     */
    public void setVariableValue(Map<String, Object> variableValues, int agentInstanceId) throws VariableValueException, VariableNotFoundException;

    /**
     * Returns a facility to process event objects that are of a known type.
     * <p>
     * Given an event type name this method returns a sender that allows to send in
     * event objects of that type. The event objects send in via the event sender
     * are expected to match the event type, thus the event sender does
     * not inspect the event object other then perform basic checking.
     * <p>
     * For events backed by a Java class (JavaBean events), the sender ensures that the
     * object send in matches in class, or implements or extends the class underlying the event type
     * for the given event type name. Note that event type identity for Java class events is the Java class.
     * When assigning two different event type names to the same Java class the names are an alias for the same
     * event type i.e. there is always a single event type to represent a given Java class.
     * <p>
     * For events backed by a Object[] (Object-array events), the sender does not perform any checking other
     * then checking that the event object indeed is an array of object.
     * <p>
     * For events backed by a java.util.Map (Map events), the sender does not perform any checking other
     * then checking that the event object indeed implements Map.
     * <p>
     * For events backed by a org.w3c.Node (XML DOM events), the sender checks that the root element name
     * indeed does match the root element name for the event type name.
     *
     * @param eventTypeName is the name of the event type
     * @return sender for fast-access processing of event objects of known type (and content)
     * @throws EventTypeException thrown to indicate that the name does not exist
     */
    public EventSender getEventSender(String eventTypeName) throws EventTypeException;

    /**
     * For use with plug-in event representations, returns a facility to process event objects that are of one of a number of types
     * that one or more of the registered plug-in event representation extensions can reflect upon and provide an
     * event for.
     *
     * @param uris is the URIs that specify which plug-in event representations may process an event object.
     *             <p>URIs do not need to match event representation URIs exactly, a child (hierarchical) match is enough
     *             for an event representation to participate.
     *             <p>The order of URIs is relevant as each event representation's factory is asked in turn to
     *             process the event, until the first factory processes the event.
     * @return sender for processing of event objects of one of the plug-in event representations
     * @throws EventTypeException thrown to indicate that the URI list was invalid
     */
    public EventSender getEventSender(URI[] uris) throws EventTypeException;

    /**
     * Execute an on-demand query.
     * <p>
     * On-demand queries are EPL queries that execute non-continuous fire-and-forget queries against named windows.
     *
     * @param epl is the EPL query to execute
     * @return query result
     */
    public EPOnDemandQueryResult executeQuery(String epl);

    /**
     * For use with named windows that have a context declared and that may therefore have multiple context partitions,
     * allows to target context partitions for query execution selectively.
     *
     * @param epl                       is the EPL query to execute
     * @param contextPartitionSelectors selects context partitions to consider
     * @return result
     */
    public EPOnDemandQueryResult executeQuery(String epl, ContextPartitionSelector[] contextPartitionSelectors);

    /**
     * Execute an on-demand query.
     * <p>
     * On-demand queries are EPL queries that execute non-continuous fire-and-forget queries against named windows.
     *
     * @param model is the EPL query to execute, obtain a model object using {@link EPAdministrator#compileEPL(String)}
     *              or via the API
     * @return query result
     */
    public EPOnDemandQueryResult executeQuery(EPStatementObjectModel model);

    /**
     * For use with named windows that have a context declared and that may therefore have multiple context partitions,
     * allows to target context partitions for query execution selectively.
     *
     * @param model                     is the EPL query to execute, obtain a model object using {@link EPAdministrator#compileEPL(String)}
     *                                  or via the API
     * @param contextPartitionSelectors selects context partitions to consider
     * @return result
     */
    public EPOnDemandQueryResult executeQuery(EPStatementObjectModel model, ContextPartitionSelector[] contextPartitionSelectors);

    /**
     * Prepare an unparameterized on-demand query before execution and for repeated execution.
     *
     * @param epl to prepare
     * @return proxy to execute upon, that also provides the event type of the returned results
     */
    public EPOnDemandPreparedQuery prepareQuery(String epl);

    /**
     * Prepare an unparameterized on-demand query before execution and for repeated execution.
     *
     * @param model is the EPL query to prepare, obtain a model object using {@link EPAdministrator#compileEPL(String)}
     *              or via the API
     * @return proxy to execute upon, that also provides the event type of the returned results
     */
    public EPOnDemandPreparedQuery prepareQuery(EPStatementObjectModel model);

    /**
     * Prepare a parameterized on-demand query for repeated parameter setting and execution.
     * Set all values on the returned holder then execute using {@link #executeQuery(EPOnDemandPreparedQueryParameterized)}.
     *
     * @param epl to prepare
     * @return parameter holder upon which to set values
     */
    public EPOnDemandPreparedQueryParameterized prepareQueryWithParameters(String epl);

    /**
     * Execute an on-demand parameterized query.
     * <p>
     * On-demand queries are EPL queries that execute non-continuous fire-and-forget queries against named windows.
     *
     * @param parameterizedQuery contains the query and parameter values
     * @return query result
     */
    public EPOnDemandQueryResult executeQuery(EPOnDemandPreparedQueryParameterized parameterizedQuery);

    /**
     * Execute an on-demand parameterized query.
     * <p>
     * On-demand queries are EPL queries that execute non-continuous fire-and-forget queries against named windows.
     *
     * @param parameterizedQuery        contains the query and parameter values
     * @param contextPartitionSelectors selects context partitions to consider
     * @return query result
     */
    public EPOnDemandQueryResult executeQuery(EPOnDemandPreparedQueryParameterized parameterizedQuery, ContextPartitionSelector[] contextPartitionSelectors);

    /**
     * Returns the event renderer for events generated by this runtime.
     *
     * @return event renderer
     */
    public EventRenderer getEventRenderer();

    /**
     * Returns current engine time.
     * <p>
     * If time is provided externally via timer events, the function returns current time as externally provided.
     *
     * @return current engine time
     */
    public long getCurrentTime();

    /**
     * Returns the time at which the next schedule execution is expected, returns null if no schedule execution is
     * outstanding.
     *
     * @return time of next schedule if any
     */
    public Long getNextScheduledTime();

    /**
     * Returns the data flow runtime.
     *
     * @return data flow runtime
     */
    public EPDataFlowRuntime getDataFlowRuntime();

    /**
     * Returns true for external clocking, false for internal clocking.
     *
     * @return clocking indicator
     */
    public boolean isExternalClockingEnabled();

    /**
     * Send an event represented by a Avro GenericData.Record to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link #routeAvro(Object, String)}}).
     *
     * @param avroGenericDataDotRecord is the event to sent to the runtime
     * @param avroEventTypeName event type name
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    void sendEventAvro(Object avroGenericDataDotRecord, String avroEventTypeName);

    /**
     * Route the event object back to the event stream processing runtime for internal dispatching,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     * The route event is processed just like it was sent to the runtime, that is any
     * active expressions seeking that event receive it. The routed event has priority over other
     * events sent to the runtime. In a single-threaded application the routed event is
     * processed before the next event is sent to the runtime through the
     * EPRuntime.sendEvent method.
     * <p>
     * Note: when outbound-threading is enabled, the thread delivering to listeners
     * is not the thread processing the original event. Therefore with outbound-threading
     * enabled the sendEvent method should be used by listeners instead.
     * </p>
     *
     * @param avroGenericDataDotRecord is the event to sent to the runtime
     * @param avroEventTypeName event type name
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    public void routeAvro(Object avroGenericDataDotRecord, String avroEventTypeName) throws EPException;
}
