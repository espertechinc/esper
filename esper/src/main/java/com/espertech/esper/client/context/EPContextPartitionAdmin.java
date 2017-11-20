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
package com.espertech.esper.client.context;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Service interface for administration of contexts and context partitions.
 */
public interface EPContextPartitionAdmin {
    /**
     * Returns the statement names associated to the context of the given name.
     * <p>
     * Returns null if a context declaration for the name does not exist.
     * </p>
     *
     * @param contextName context name to return statements for
     * @return statement names, or null if the context does not exist, or empty list if no statements are
     * associated to the context (counting started and stopped statements, not destroyed ones).
     */
    public String[] getContextStatementNames(String contextName);

    /**
     * Returns the nesting level for the context declaration, i.e. 1 for unnested and &gt;1 for nested contexts.
     *
     * @param contextName context name
     * @return nesting level
     * @throws IllegalArgumentException if a context by that name was not declared
     */
    public int getContextNestingLevel(String contextName);

    /**
     * Destroy one or more context partitions dropping the associated state and removing associated context partition metadata.
     * <p>
     * For key-partitioned contexts and hash-segmented contexts the next event for such
     * context partition allocates a new context partition for that key or hash.
     * </p>
     * <p>
     * If context partitions cannot be found they are not part of the collection returned.
     * Only context partitions in stopped or started state can be destroyed.
     * </p>
     *
     * @param contextName context name
     * @param selector    a selector that identifies the context partitions
     * @return collection of the destroyed context partition ids and descriptors
     * @throws IllegalArgumentException        if a context by that name was not declared
     * @throws InvalidContextPartitionSelector if the selector type and context declaration mismatch
     */
    public ContextPartitionCollection destroyContextPartitions(String contextName, ContextPartitionSelector selector);

    /**
     * Stop one or more context partitions that are currently started, dropping the associated state and but keeping
     * associated context partition metadata for the purpose of starting it again.
     * <p>
     * Stopping a context partition means any associated statements no longer process
     * events or time for that context partition only, and dropping all such associated state.
     * </p>
     * <p>
     * If context partitions cannot be found they are not part of the collection returned.
     * Stopped context partitions remain stopped and are not returned.
     * </p>
     *
     * @param contextName context name
     * @param selector    a selector that identifies the context partitions
     * @return collection of the stopped context partition ids and descriptors
     * @throws IllegalArgumentException        if a context by that name was not declared
     * @throws InvalidContextPartitionSelector if the selector type and context declaration mismatch
     */
    public ContextPartitionCollection stopContextPartitions(String contextName, ContextPartitionSelector selector);

    /**
     * Start one or more context partitions that were previously stopped.
     * <p>
     * Starting a context partition means any associated statements beging to process
     * events or time for that context partition, starting fresh with newly allocated state.
     * </p>
     * <p>
     * If context partitions cannot be found they are not part of the collection returned.
     * Started context partitions remain started and are not returned.
     * </p>
     *
     * @param contextName context name
     * @param selector    a selector that identifies the context partitions
     * @return collection of the started context partition ids and descriptors
     * @throws IllegalArgumentException        if a context by that name was not declared
     * @throws InvalidContextPartitionSelector if the selector type and context declaration mismatch
     */
    public ContextPartitionCollection startContextPartitions(String contextName, ContextPartitionSelector selector);

    /**
     * Returns information about selected context partitions including state.
     *
     * @param contextName context name
     * @param selector    a selector that identifies the context partitions
     * @return collection of the context partition ids and descriptors
     * @throws IllegalArgumentException        if a context by that name was not declared
     * @throws InvalidContextPartitionSelector if the selector type and context declaration mismatch
     */
    public ContextPartitionCollection getContextPartitions(String contextName, ContextPartitionSelector selector);

    /**
     * Returns the context partition ids.
     *
     * @param contextName context name
     * @param selector    a selector that identifies the context partitions
     * @return set of the context partition ids
     * @throws IllegalArgumentException        if a context by that name was not declared
     * @throws InvalidContextPartitionSelector if the selector type and context declaration mismatch
     */
    public Set<Integer> getContextPartitionIds(String contextName, ContextPartitionSelector selector);

    /**
     * Destroy the context partition returning its descriptor.
     * <p>
     * For key-partitioned contexts and hash-segmented contexts the next event for such
     * context partition allocates a new context partition for that key or hash.
     * </p>
     * <p>
     * Only context partitions in stopped or started state can be destroyed.
     * </p>
     *
     * @param contextName     context name
     * @param agentInstanceId the context partition id number
     * @return descriptor or null if the context partition is not found
     * @throws IllegalArgumentException if a context by that name was not declared
     */
    public ContextPartitionDescriptor destroyContextPartition(String contextName, int agentInstanceId);

    /**
     * Stop the context partition if it is currently started and returning its descriptor.
     *
     * @param contextName     context name
     * @param agentInstanceId the context partition id number
     * @return descriptor or null if the context partition is not found or is already stopped
     * @throws IllegalArgumentException if a context by that name was not declared
     */
    public ContextPartitionDescriptor stopContextPartition(String contextName, int agentInstanceId);

    /**
     * Start the context partition if it is currently stopped and returning its descriptor.
     *
     * @param contextName     context name
     * @param agentInstanceId the context partition id number
     * @return descriptor or null if the context partition is not found or is already started
     * @throws IllegalArgumentException if a context by that name was not declared
     */
    public ContextPartitionDescriptor startContextPartition(String contextName, int agentInstanceId);

    /**
     * Returning the descriptor of a given context partition.
     *
     * @param contextName     context name
     * @param agentInstanceId the context partition id number
     * @return descriptor or null if the context partition is not found
     * @throws IllegalArgumentException if a context by that name was not declared
     */
    public ContextPartitionDescriptor getDescriptor(String contextName, int agentInstanceId);

    /**
     * Add a context state listener
     * @param listener to add
     */
    void addContextStateListener(ContextStateListener listener);

    /**
     * Remove a context state listener
     * @param listener to remove
     */
    void removeContextStateListener(ContextStateListener listener);

    /**
     * Returns an iterator of context state listeners (read-only)
     * @return listeners
     */
    Iterator<ContextStateListener> getContextStateListeners();

    /**
     * Removes all context state listener
     */
    void removeContextStateListeners();

    /**
     * Add context partition state listener for the given context
     * @param contextName context name
     * @param listener to add
     */
    void addContextPartitionStateListener(String contextName, ContextPartitionStateListener listener);

    /**
     * Remove a context partition state listener for the given context
     * @param contextName context name
     * @param listener to remove
     */
    void removeContextPartitionStateListener(String contextName, ContextPartitionStateListener listener);

    /**
     * Returns an iterator of context partition state listeners (read-only) for the given context
     * @param contextName context name
     * @return listeners
     */
    Iterator<ContextPartitionStateListener> getContextPartitionStateListeners(String contextName);

    /**
     * Removes all context partition state listener for the given context
     * @param contextName context name
     */
    void removeContextPartitionStateListeners(String contextName);

    /**
     * Returns the context properties for a given context name and context partition id.
     * @param contextName context name
     * @param contextPartitionId context partition id
     * @return map of built-in properties wherein values representing event are EventBean instances
     */
    Map<String, Object> getContextProperties(String contextName, int contextPartitionId);
}
