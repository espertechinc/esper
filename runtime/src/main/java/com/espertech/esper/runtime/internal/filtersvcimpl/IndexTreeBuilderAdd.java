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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Collections;

public final class IndexTreeBuilderAdd {
    private static final Logger log = LoggerFactory.getLogger(IndexTreeBuilderAdd.class);

    private IndexTreeBuilderAdd() {
    }

    /**
     * Add a filter callback according to the filter specification to the top node returning
     * information to be used to remove the filter callback.
     *
     * @param valueSet       is the filter definition
     * @param filterCallback is the callback to be added
     * @param topNode        node to be added to any subnode beneath it
     * @param lockFactory    lock factory
     */
    public static void add(FilterValueSetParam[][] valueSet,
                           FilterHandle filterCallback,
                           FilterHandleSetNode topNode,
                           FilterServiceGranularLockFactory lockFactory) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".add (" + Thread.currentThread().getId() + ") Adding filter callback, " +
                    "  topNode=" + topNode +
                    "  filterCallback=" + filterCallback);
        }

        if (valueSet.length == 0) {
            addToNode(new ArrayDeque<FilterValueSetParam>(1), filterCallback, topNode, lockFactory);
        } else {
            ArrayDeque<FilterValueSetParam> remainingParameters = new ArrayDeque<FilterValueSetParam>(4);
            for (int i = 0; i < valueSet.length; i++) {
                remainingParameters.clear();
                Collections.addAll(remainingParameters, valueSet[i]);
                addToNode(remainingParameters, filterCallback, topNode, lockFactory);
            }
        }
    }

    /**
     * Add to the current node building up the tree path information.
     *
     * @param currentNode is the node to add to
     */
    private static void addToNode(ArrayDeque<FilterValueSetParam> remainingParameters,
                                  FilterHandle filterCallback,
                                  FilterHandleSetNode currentNode,
                                  FilterServiceGranularLockFactory lockFactory) {

        // If no parameters are specified, add to current node, and done
        if (remainingParameters.isEmpty()) {
            currentNode.getNodeRWLock().writeLock().lock();
            try {
                currentNode.add(filterCallback);
            } finally {
                currentNode.getNodeRWLock().writeLock().unlock();
            }
            return;
        }

        // Need to find an existing index that matches one of the filter parameters
        currentNode.getNodeRWLock().readLock().lock();
        Pair<FilterValueSetParam, FilterParamIndexBase> pair;
        try {
            pair = IndexHelper.findIndex(remainingParameters, currentNode.getIndizes());

            // Found an index matching a filter parameter
            if (pair != null) {
                remainingParameters.remove(pair.getFirst());
                Object filterForValue = pair.getFirst().getFilterForValue();
                FilterParamIndexBase index = pair.getSecond();
                addToIndex(remainingParameters, filterCallback, index, filterForValue, lockFactory);
                return;
            }
        } finally {
            currentNode.getNodeRWLock().readLock().unlock();
        }

        // An index for any of the filter parameters was not found, create one
        currentNode.getNodeRWLock().writeLock().lock();
        try {
            pair = IndexHelper.findIndex(remainingParameters, currentNode.getIndizes());

            // Attempt to find an index again this time under a write lock
            if (pair != null) {
                remainingParameters.remove(pair.getFirst());
                Object filterForValue = pair.getFirst().getFilterForValue();
                FilterParamIndexBase index = pair.getSecond();
                addToIndex(remainingParameters, filterCallback, index, filterForValue, lockFactory);
                return;
            }

            // No index found that matches any parameters, create a new one
            // Pick the next parameter for an index
            FilterValueSetParam parameterPickedForIndex = remainingParameters.removeFirst();

            FilterParamIndexBase index = IndexFactory.createIndex(parameterPickedForIndex.getLookupable(), lockFactory, parameterPickedForIndex.getFilterOperator());

            currentNode.getIndizes().add(index);
            addToIndex(remainingParameters, filterCallback, index, parameterPickedForIndex.getFilterForValue(), lockFactory);
        } finally {
            currentNode.getNodeRWLock().writeLock().unlock();
        }
    }

    /**
     * Add to an index the value to filter for.
     *
     * @param index          is the index to add to
     * @param filterForValue is the filter parameter value to add
     */
    private static void addToIndex(ArrayDeque<FilterValueSetParam> remainingParameters,
                                   FilterHandle filterCallback,
                                   FilterParamIndexBase index,
                                   Object filterForValue,
                                   FilterServiceGranularLockFactory lockFactory) {

        index.getReadWriteLock().readLock().lock();
        EventEvaluator eventEvaluator;
        try {
            eventEvaluator = index.get(filterForValue);

            // The filter parameter value already existed in bean, add and release locks
            if (eventEvaluator != null) {
                boolean added = addToEvaluator(remainingParameters, filterCallback, eventEvaluator, lockFactory);
                if (added) {
                    return;
                }
            }
        } finally {
            index.getReadWriteLock().readLock().unlock();
        }

        // new filter parameter value, need a write lock
        index.getReadWriteLock().writeLock().lock();
        try {
            eventEvaluator = index.get(filterForValue);

            // It may exist now since another thread could have added the entry
            if (eventEvaluator != null) {
                boolean added = addToEvaluator(remainingParameters, filterCallback, eventEvaluator, lockFactory);
                if (added) {
                    return;
                }

                // The found eventEvaluator must be converted to a new FilterHandleSetNode
                FilterParamIndexBase nextIndex = (FilterParamIndexBase) eventEvaluator;
                FilterHandleSetNode newNode = new FilterHandleSetNode(lockFactory.obtainNew());
                newNode.add(nextIndex);
                index.remove(filterForValue);
                index.put(filterForValue, newNode);
                addToNode(remainingParameters, filterCallback, newNode, lockFactory);

                return;
            }

            // The index does not currently have this filterCallback value,
            // if there are no remaining parameters, create a node
            if (remainingParameters.isEmpty()) {
                FilterHandleSetNode node = new FilterHandleSetNode(lockFactory.obtainNew());
                addToNode(remainingParameters, filterCallback, node, lockFactory);
                index.put(filterForValue, node);
                return;
            }

            // If there are remaining parameters, create a new index for the next parameter
            FilterValueSetParam parameterPickedForIndex = remainingParameters.removeFirst();

            FilterParamIndexBase nextIndex = IndexFactory.createIndex(parameterPickedForIndex.getLookupable(), lockFactory, parameterPickedForIndex.getFilterOperator());

            index.put(filterForValue, nextIndex);
            addToIndex(remainingParameters, filterCallback, nextIndex, parameterPickedForIndex.getFilterForValue(), lockFactory);
        } finally {
            index.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Add filter callback to an event evaluator, which could be either an index node or a set node.
     *
     * @param eventEvaluator to add the filterCallback to.
     * @return boolean indicating if the eventEvaluator was successfully added
     */
    private static boolean addToEvaluator(ArrayDeque<FilterValueSetParam> remainingParameters,
                                          FilterHandle filterCallback,
                                          EventEvaluator eventEvaluator,
                                          FilterServiceGranularLockFactory lockFactory) {
        if (eventEvaluator instanceof FilterHandleSetNode) {
            FilterHandleSetNode node = (FilterHandleSetNode) eventEvaluator;
            addToNode(remainingParameters, filterCallback, node, lockFactory);
            return true;
        }

        // Check if the next index matches any of the remaining filterCallback parameters
        FilterParamIndexBase nextIndex = (FilterParamIndexBase) eventEvaluator;

        FilterValueSetParam parameter = IndexHelper.findParameter(remainingParameters, nextIndex);
        if (parameter != null) {
            remainingParameters.remove(parameter);
            addToIndex(remainingParameters, filterCallback, nextIndex, parameter.getFilterForValue(), lockFactory);
            return true;
        }

        // This eventEvaluator does not work with any of the remaining filter parameters
        return false;
    }
}
