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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.filterspec.FilterValueSetParam;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Collections;

/**
 * Builder manipulates a tree structure consisting of {@link FilterHandleSetNode} and {@link FilterParamIndexBase} instances.
 * Filters can be added to a top node (an instance of FilterHandleSetNode) via the add method. This method returns
 * an instance of {@link EventTypeIndexBuilderIndexLookupablePair} which represents an element in the tree path (list of indizes) that the filter callback was
 * added to. To remove filters the same IndexTreePath instance must be passed in.
 * <p>The implementation is designed to be multithread-safe in conjunction with the node classes manipulated by this class.
 */
public final class IndexTreeBuilder {
    private IndexTreeBuilder() {
    }

    /**
     * Add a filter callback according to the filter specification to the top node returning
     * information to be used to remove the filter callback.
     *
     * @param filterValueSet is the filter definition
     * @param filterCallback is the callback to be added
     * @param topNode        node to be added to any subnode beneath it
     * @param lockFactory    lock factory
     * @return an encapsulation of information need to allow for safe removal of the filter tree.
     */
    public static ArrayDeque<EventTypeIndexBuilderIndexLookupablePair>[] add(FilterValueSet filterValueSet,
                                                                             FilterHandle filterCallback,
                                                                             FilterHandleSetNode topNode,
                                                                             FilterServiceGranularLockFactory lockFactory) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".add (" + Thread.currentThread().getId() + ") Adding filter callback, " +
                    "  topNode=" + topNode +
                    "  filterCallback=" + filterCallback);
        }

        ArrayDeque<EventTypeIndexBuilderIndexLookupablePair>[] treePathInfo;
        if (filterValueSet.getParameters().length == 0) {
            treePathInfo = allocateTreePath(1);
            treePathInfo[0] = new ArrayDeque<EventTypeIndexBuilderIndexLookupablePair>(1);
            addToNode(new ArrayDeque<FilterValueSetParam>(1), filterCallback, topNode, treePathInfo[0], lockFactory);
        } else {
            treePathInfo = allocateTreePath(filterValueSet.getParameters().length);
            ArrayDeque<FilterValueSetParam> remainingParameters = new ArrayDeque<FilterValueSetParam>(4);
            for (int i = 0; i < filterValueSet.getParameters().length; i++) {
                treePathInfo[i] = new ArrayDeque<EventTypeIndexBuilderIndexLookupablePair>(filterValueSet.getParameters()[i].length);
                remainingParameters.clear();
                Collections.addAll(remainingParameters, filterValueSet.getParameters()[i]);
                addToNode(remainingParameters, filterCallback, topNode, treePathInfo[i], lockFactory);
            }
        }

        return treePathInfo;
    }

    /**
     * Remove an filterCallback from the given top node. The IndexTreePath instance passed in must be the
     * same as obtained when the same filterCallback was added.
     *
     * @param filterCallback filter callback  to be removed
     * @param treePathInfo   encapsulates information need to allow for safe removal of the filterCallback
     * @param topNode        The top tree node beneath which the filterCallback was added
     * @param eventType      event type
     */
    public static void remove(
            EventType eventType,
            FilterHandle filterCallback,
            EventTypeIndexBuilderIndexLookupablePair[] treePathInfo,
            FilterHandleSetNode topNode) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".remove (" + Thread.currentThread().getId() + ") Removing filterCallback " +
                    " type " + eventType.getName() +
                    " topNode=" + topNode +
                    " filterCallback=" + filterCallback);
        }

        removeFromNode(filterCallback, topNode, treePathInfo, 0);
    }

    /**
     * Add to the current node building up the tree path information.
     *
     * @param currentNode  is the node to add to
     * @param treePathInfo is filled with information about which indizes were chosen to add the filter to
     */
    private static void addToNode(ArrayDeque<FilterValueSetParam> remainingParameters,
                                  FilterHandle filterCallback,
                                  FilterHandleSetNode currentNode,
                                  ArrayDeque<EventTypeIndexBuilderIndexLookupablePair> treePathInfo,
                                  FilterServiceGranularLockFactory lockFactory) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".addToNode (" + Thread.currentThread().getId() + ") Adding filterCallback, node=" + currentNode +
                    "  remainingParameters=" + printRemainingParameters(remainingParameters));
        }

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
                treePathInfo.add(new EventTypeIndexBuilderIndexLookupablePair(index, filterForValue));
                addToIndex(remainingParameters, filterCallback, index, filterForValue, treePathInfo, lockFactory);
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
                treePathInfo.add(new EventTypeIndexBuilderIndexLookupablePair(index, filterForValue));
                addToIndex(remainingParameters, filterCallback, index, filterForValue, treePathInfo, lockFactory);
                return;
            }

            // No index found that matches any parameters, create a new one
            // Pick the next parameter for an index
            FilterValueSetParam parameterPickedForIndex = remainingParameters.removeFirst();

            FilterParamIndexBase index = IndexFactory.createIndex(parameterPickedForIndex.getLookupable(), lockFactory, parameterPickedForIndex.getFilterOperator());

            currentNode.getIndizes().add(index);
            treePathInfo.add(new EventTypeIndexBuilderIndexLookupablePair(index, parameterPickedForIndex.getFilterForValue()));
            addToIndex(remainingParameters, filterCallback, index, parameterPickedForIndex.getFilterForValue(), treePathInfo, lockFactory);
        } finally {
            currentNode.getNodeRWLock().writeLock().unlock();
        }
    }

    // Remove an filterCallback from the current node, return true if the node is the node is empty now
    private static boolean removeFromNode(FilterHandle filterCallback,
                                          FilterHandleSetNode currentNode,
                                          EventTypeIndexBuilderIndexLookupablePair[] treePathInfo,
                                          int treePathPosition) {
        EventTypeIndexBuilderIndexLookupablePair nextPair = treePathPosition < treePathInfo.length ? treePathInfo[treePathPosition++] : null;

        // No remaining filter parameters
        if (nextPair == null) {
            currentNode.getNodeRWLock().writeLock().lock();

            try {
                boolean isRemoved = currentNode.remove(filterCallback);
                boolean isEmpty = currentNode.isEmpty();

                if (!isRemoved) {
                    log.warn(".removeFromNode (" + Thread.currentThread().getId() + ") Could not find the filterCallback to be removed within the supplied node , node=" +
                            currentNode + "  filterCallback=" + filterCallback);
                }

                return isEmpty;
            } finally {
                currentNode.getNodeRWLock().writeLock().unlock();
            }
        }

        // Remove from index
        FilterParamIndexBase nextIndex = nextPair.getIndex();
        Object filteredForValue = nextPair.getLookupable();

        currentNode.getNodeRWLock().writeLock().lock();
        try {
            boolean isEmpty = removeFromIndex(filterCallback, nextIndex, treePathInfo, treePathPosition, filteredForValue);

            if (!isEmpty) {
                return false;
            }

            // Remove the index if the index is now empty
            if (nextIndex.isEmpty()) {
                boolean isRemoved = currentNode.remove(nextIndex);

                if (!isRemoved) {
                    log.warn(".removeFromNode (" + Thread.currentThread().getId() + ") Could not find the index in index list for removal, index=" +
                            nextIndex.toString() + "  filterCallback=" + filterCallback);
                    return false;
                }
            }

            return currentNode.isEmpty();
        } finally {
            currentNode.getNodeRWLock().writeLock().unlock();
        }
    }

    // Remove filterCallback from index, returning true if index empty after removal
    private static boolean removeFromIndex(FilterHandle filterCallback,
                                           FilterParamIndexBase index,
                                           EventTypeIndexBuilderIndexLookupablePair[] treePathInfo,
                                           int treePathPosition,
                                           Object filterForValue) {
        index.getReadWriteLock().writeLock().lock();
        try {
            EventEvaluator eventEvaluator = index.get(filterForValue);

            if (eventEvaluator == null) {
                log.warn(".removeFromIndex (" + Thread.currentThread().getId() + ") Could not find the filterCallback value in index, index=" +
                        index.toString() + "  value=" + filterForValue.toString() + "  filterCallback=" + filterCallback);
                return false;
            }

            if (eventEvaluator instanceof FilterHandleSetNode) {
                FilterHandleSetNode node = (FilterHandleSetNode) eventEvaluator;
                boolean isEmpty = removeFromNode(filterCallback, node, treePathInfo, treePathPosition);

                if (isEmpty) {
                    // Since we are holding a write lock to this index, there should not be a chance that
                    // another thread had been adding anything to this FilterHandleSetNode
                    index.remove(filterForValue);
                }
                return index.isEmpty();
            }

            FilterParamIndexBase nextIndex = (FilterParamIndexBase) eventEvaluator;
            EventTypeIndexBuilderIndexLookupablePair nextPair = treePathPosition < treePathInfo.length ? treePathInfo[treePathPosition++] : null;

            if (nextPair == null) {
                log.error(".removeFromIndex Expected an inner index to this index, this=" + filterCallback.toString());
                assert false;
                return false;
            }

            if (nextPair.getIndex() != nextIndex) {
                log.error(".removeFromIndex Expected an index for filterCallback that differs from the found index, this=" + filterCallback.toString() +
                        "  expected=" + nextPair.getIndex());
                assert false;
                return false;
            }

            Object nextExpressionValue = nextPair.getLookupable();

            boolean isEmpty = removeFromIndex(filterCallback, nextPair.getIndex(), treePathInfo, treePathPosition, nextExpressionValue);

            if (isEmpty) {
                // Since we are holding a write lock to this index, there should not be a chance that
                // another thread had been adding anything to this FilterHandleSetNode
                index.remove(filterForValue);
            }
            return index.isEmpty();
        } finally {
            index.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Add to an index the value to filter for.
     *
     * @param index          is the index to add to
     * @param filterForValue is the filter parameter value to add
     * @param treePathInfo   is the specification to fill on where is was added
     */
    private static void addToIndex(ArrayDeque<FilterValueSetParam> remainingParameters,
                                   FilterHandle filterCallback,
                                   FilterParamIndexBase index,
                                   Object filterForValue,
                                   ArrayDeque<EventTypeIndexBuilderIndexLookupablePair> treePathInfo,
                                   FilterServiceGranularLockFactory lockFactory) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".addToIndex (" + Thread.currentThread().getId() + ") Adding to index " +
                    index.toString() +
                    "  expressionValue=" + filterForValue);
        }

        index.getReadWriteLock().readLock().lock();
        EventEvaluator eventEvaluator;
        try {
            eventEvaluator = index.get(filterForValue);

            // The filter parameter value already existed in bean, add and release locks
            if (eventEvaluator != null) {
                boolean added = addToEvaluator(remainingParameters, filterCallback, eventEvaluator, treePathInfo, lockFactory);
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
                boolean added = addToEvaluator(remainingParameters, filterCallback, eventEvaluator, treePathInfo, lockFactory);
                if (added) {
                    return;
                }

                // The found eventEvaluator must be converted to a new FilterHandleSetNode
                FilterParamIndexBase nextIndex = (FilterParamIndexBase) eventEvaluator;
                FilterHandleSetNode newNode = new FilterHandleSetNode(lockFactory.obtainNew());
                newNode.add(nextIndex);
                index.remove(filterForValue);
                index.put(filterForValue, newNode);
                addToNode(remainingParameters, filterCallback, newNode, treePathInfo, lockFactory);

                return;
            }

            // The index does not currently have this filterCallback value,
            // if there are no remaining parameters, create a node
            if (remainingParameters.isEmpty()) {
                FilterHandleSetNode node = new FilterHandleSetNode(lockFactory.obtainNew());
                addToNode(remainingParameters, filterCallback, node, treePathInfo, lockFactory);
                index.put(filterForValue, node);
                return;
            }

            // If there are remaining parameters, create a new index for the next parameter
            FilterValueSetParam parameterPickedForIndex = remainingParameters.removeFirst();

            FilterParamIndexBase nextIndex = IndexFactory.createIndex(parameterPickedForIndex.getLookupable(), lockFactory, parameterPickedForIndex.getFilterOperator());

            index.put(filterForValue, nextIndex);
            treePathInfo.add(new EventTypeIndexBuilderIndexLookupablePair(nextIndex, parameterPickedForIndex.getFilterForValue()));
            addToIndex(remainingParameters, filterCallback, nextIndex, parameterPickedForIndex.getFilterForValue(), treePathInfo, lockFactory);
        } finally {
            index.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Add filter callback to an event evaluator, which could be either an index node or a set node.
     *
     * @param eventEvaluator to add the filterCallback to.
     * @param treePathInfo   is for holding the information on where the add occured
     * @return boolean indicating if the eventEvaluator was successfully added
     */
    private static boolean addToEvaluator(ArrayDeque<FilterValueSetParam> remainingParameters,
                                          FilterHandle filterCallback,
                                          EventEvaluator eventEvaluator,
                                          ArrayDeque<EventTypeIndexBuilderIndexLookupablePair> treePathInfo,
                                          FilterServiceGranularLockFactory lockFactory) {
        if (eventEvaluator instanceof FilterHandleSetNode) {
            FilterHandleSetNode node = (FilterHandleSetNode) eventEvaluator;
            addToNode(remainingParameters, filterCallback, node, treePathInfo, lockFactory);
            return true;
        }

        // Check if the next index matches any of the remaining filterCallback parameters
        FilterParamIndexBase nextIndex = (FilterParamIndexBase) eventEvaluator;

        FilterValueSetParam parameter = IndexHelper.findParameter(remainingParameters, nextIndex);
        if (parameter != null) {
            remainingParameters.remove(parameter);
            treePathInfo.add(new EventTypeIndexBuilderIndexLookupablePair(nextIndex, parameter.getFilterForValue()));
            addToIndex(remainingParameters, filterCallback, nextIndex, parameter.getFilterForValue(), treePathInfo, lockFactory);
            return true;
        }

        // This eventEvaluator does not work with any of the remaining filter parameters
        return false;
    }

    private static String printRemainingParameters(ArrayDeque<FilterValueSetParam> remainingParameters) {
        StringBuilder buffer = new StringBuilder();

        int count = 0;
        for (FilterValueSetParam parameter : remainingParameters) {
            buffer.append("  param(").append(count).append(')');
            buffer.append(" property=").append(parameter.getLookupable());
            buffer.append(" operator=").append(parameter.getFilterOperator());
            buffer.append(" value=").append(parameter.getFilterForValue());
            count++;
        }

        return buffer.toString();
    }

    private static ArrayDeque<EventTypeIndexBuilderIndexLookupablePair>[] allocateTreePath(int size) {
        return (ArrayDeque<EventTypeIndexBuilderIndexLookupablePair>[])
                new ArrayDeque[size];
    }

    private static final Logger log = LoggerFactory.getLogger(IndexTreeBuilder.class);
}
