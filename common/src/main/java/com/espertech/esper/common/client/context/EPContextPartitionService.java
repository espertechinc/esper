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
package com.espertech.esper.common.client.context;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Service interface for administration of contexts and context partitions.
 */
public interface EPContextPartitionService {
    /**
     * Returns the statement names associated to the context of the given name.
     * <p>
     * Returns null if a context declaration for the name does not exist.
     * </p>
     *
     * @param deploymentId deployment id of context (deployment id of create-context statement)
     * @param contextName  context name to return statements for
     * @return statement names, or null if the context does not exist, or empty list if no statements are
     * associated to the context (counting started and stopped statements, not destroyed ones).
     */
    String[] getContextStatementNames(String deploymentId, String contextName);

    /**
     * Returns the nesting level for the context declaration, i.e. 1 for unnested and &gt;1 for nested contexts.
     *
     * @param deploymentId deployment id of context (deployment id of create-context statement)
     * @param contextName  context name
     * @return nesting level
     * @throws IllegalArgumentException if a context by that name was not declared
     */
    int getContextNestingLevel(String deploymentId, String contextName);

    /**
     * Returns information about selected context partitions including state.
     *
     * @param deploymentId deployment id of context (deployment id of create-context statement)
     * @param contextName  context name
     * @param selector     a selector that identifies the context partitions
     * @return collection of the context partition ids and descriptors
     * @throws IllegalArgumentException        if a context by that name was not declared
     * @throws InvalidContextPartitionSelector if the selector type and context declaration mismatch
     */
    ContextPartitionCollection getContextPartitions(String deploymentId, String contextName, ContextPartitionSelector selector);

    /**
     * Returns the context partition ids.
     *
     * @param deploymentId deployment id of context (deployment id of create-context statement)
     * @param contextName  context name
     * @param selector     a selector that identifies the context partitions
     * @return set of the context partition ids
     * @throws IllegalArgumentException        if a context by that name was not declared
     * @throws InvalidContextPartitionSelector if the selector type and context declaration mismatch
     */
    Set<Integer> getContextPartitionIds(String deploymentId, String contextName, ContextPartitionSelector selector);

    /**
     * Returns the current count of context partition.
     *
     * @param deploymentId deployment id of context (deployment id of create-context statement)
     * @param contextName  context name
     * @return context partition count
     * @throws IllegalArgumentException        if a context by that name was not declared
     */
    long getContextPartitionCount(String deploymentId, String contextName);

    /**
     * Returning the descriptor of a given context partition.
     *
     * @param deploymentId    deployment id of context (deployment id of create-context statement)
     * @param contextName     context name
     * @param agentInstanceId the context partition id number
     * @return identifier or null if the context partition is not found
     * @throws IllegalArgumentException if a context by that name was not declared
     */
    ContextPartitionIdentifier getIdentifier(String deploymentId, String contextName, int agentInstanceId);

    /**
     * Add a context state listener
     *
     * @param listener to add
     */
    void addContextStateListener(ContextStateListener listener);

    /**
     * Remove a context state listener
     *
     * @param listener to remove
     */
    void removeContextStateListener(ContextStateListener listener);

    /**
     * Returns an iterator of context state listeners (read-only)
     *
     * @return listeners
     */
    Iterator<ContextStateListener> getContextStateListeners();

    /**
     * Removes all context state listener
     */
    void removeContextStateListeners();

    /**
     * Add context partition state listener for the given context
     *
     * @param deploymentId deployment id of context (deployment id of create-context statement)
     * @param contextName  context name
     * @param listener     to add
     */
    void addContextPartitionStateListener(String deploymentId, String contextName, ContextPartitionStateListener listener);

    /**
     * Remove a context partition state listener for the given context
     *
     * @param deploymentId deployment id of context (deployment id of create-context statement)
     * @param contextName  context name
     * @param listener     to remove
     */
    void removeContextPartitionStateListener(String deploymentId, String contextName, ContextPartitionStateListener listener);

    /**
     * Returns an iterator of context partition state listeners (read-only) for the given context
     *
     * @param deploymentId deployment id of context (deployment id of create-context statement)
     * @param contextName  context name
     * @return listeners
     */
    Iterator<ContextPartitionStateListener> getContextPartitionStateListeners(String deploymentId, String contextName);

    /**
     * Removes all context partition state listener for the given context
     *
     * @param deploymentId deployment id of context (deployment id of create-context statement)
     * @param contextName  context name
     */
    void removeContextPartitionStateListeners(String deploymentId, String contextName);

    /**
     * Returns the context properties for a given deployment id, context name and context partition id.
     *
     * @param deploymentId       deployment id of context (deployment id of create-context statement)
     * @param contextName        context name
     * @param contextPartitionId context partition id
     * @return map of built-in properties wherein values representing event are EventBean instances
     */
    Map<String, Object> getContextProperties(String deploymentId, String contextName, int contextPartitionId);
}
