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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public final class IndexTreeBuilderRemove {
    private IndexTreeBuilderRemove() {
    }

    /**
     * Remove an filterCallback from the given top node. The IndexTreePath instance passed in must be the
     * same as obtained when the same filterCallback was added.
     *
     * @param filterCallback filter callback  to be removed
     * @param topNode        The top tree node beneath which the filterCallback was added
     * @param eventType      event type
     * @param params         params
     */
    public static void remove(
            EventType eventType,
            FilterHandle filterCallback,
            FilterValueSetParam[] params,
            FilterHandleSetNode topNode) {

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".remove (" + Thread.currentThread().getId() + ") Removing filterCallback " +
                    " type " + eventType.getName() +
                    " topNode=" + topNode +
                    " filterCallback=" + filterCallback);
        }

        boolean isRemoved = removeFromNode(filterCallback, topNode, params, 0);

        if (!isRemoved) {
            log.warn(".removeFromNode (" + Thread.currentThread().getId() + ") Could not find the filterCallback to be removed within the supplied node, params=" +
                    Arrays.asList(params) + "  filterCallback=" + filterCallback);
        }
    }

    private static boolean removeFromNode(FilterHandle filterCallback,
                                          FilterHandleSetNode currentNode,
                                          FilterValueSetParam[] params,
                                          int currentLevel) {

        // No remaining filter parameters
        if (currentLevel == params.length) {
            currentNode.getNodeRWLock().writeLock().lock();

            try {
                return currentNode.remove(filterCallback);
            } finally {
                currentNode.getNodeRWLock().writeLock().unlock();
            }
        }

        if (currentLevel > params.length) {
            log.warn(".removeFromNode (" + Thread.currentThread().getId() + ") Current level exceed parameter length, node=" + currentNode + "  filterCallback=" + filterCallback);
            return false;
        }

        // Remove from index
        currentNode.getNodeRWLock().writeLock().lock();
        try {

            FilterParamIndexBase indexFound = null;

            // find matching index
            for (FilterParamIndexBase index : currentNode.getIndizes()) {
                for (int i = 0; i < params.length; i++) {
                    FilterValueSetParam param = params[i];
                    // if property-based index, we prefer this in matching
                    if (index instanceof FilterParamIndexLookupableBase) {
                        FilterParamIndexLookupableBase baseIndex = (FilterParamIndexLookupableBase) index;
                        if ((param.getLookupable().getExpression().equals(baseIndex.getLookupable().getExpression())) &&
                                (param.getFilterOperator().equals(baseIndex.getFilterOperator()))) {
                            boolean found = removeFromIndex(filterCallback, index, params, currentLevel + 1, param.getFilterForValue());
                            if (found) {
                                indexFound = baseIndex;
                                break;
                            }
                        }
                    } else if (index instanceof FilterParamIndexBooleanExpr && currentLevel == params.length - 1) {
                        // if boolean-expression then match only if this is the last parameter,
                        // all others considered are higher order and sort ahead
                        if (param.getFilterOperator().equals(FilterOperator.BOOLEAN_EXPRESSION)) {
                            FilterParamIndexBooleanExpr booleanIndex = (FilterParamIndexBooleanExpr) index;
                            boolean found = booleanIndex.removeMayNotExist(param.getFilterForValue());
                            if (found) {
                                indexFound = booleanIndex;
                                break;
                            }
                        }
                    }
                }

                if (indexFound != null) {
                    break;
                }
            }

            if (indexFound == null) {
                return false;
            }

            // Remove the index if the index is now empty
            if (indexFound.isEmpty()) {
                boolean isRemoved = currentNode.remove(indexFound);

                if (!isRemoved) {
                    log.warn(".removeFromNode (" + Thread.currentThread().getId() + ") Could not find the index in index list for removal, index=" +
                            indexFound.toString() + "  filterCallback=" + filterCallback);
                    return true;
                }
            }

            return true;
        } finally {
            currentNode.getNodeRWLock().writeLock().unlock();
        }
    }

    private static boolean removeFromIndex(FilterHandle filterCallback,
                                           FilterParamIndexBase index,
                                           FilterValueSetParam[] params,
                                           int currentLevel,
                                           Object filterForValue) {
        index.getReadWriteLock().writeLock().lock();
        try {
            EventEvaluator eventEvaluator = index.get(filterForValue);

            if (eventEvaluator == null) {
                // This is possible as there can be another path
                return false;
            }

            if (eventEvaluator instanceof FilterHandleSetNode) {
                FilterHandleSetNode node = (FilterHandleSetNode) eventEvaluator;
                boolean found = removeFromNode(filterCallback, node, params, currentLevel);
                if (!found) {
                    return false;
                }

                boolean isEmpty = node.isEmpty();

                if (isEmpty) {
                    // Since we are holding a write lock to this index, there should not be a chance that
                    // another thread had been adding anything to this FilterHandleSetNode
                    index.remove(filterForValue);
                }
                return true;
            }

            FilterParamIndexBase nextIndex = (FilterParamIndexBase) eventEvaluator;
            FilterParamIndexBase indexFound = null;
            if (nextIndex instanceof FilterParamIndexLookupableBase) {
                FilterParamIndexLookupableBase baseIndex = (FilterParamIndexLookupableBase) nextIndex;
                for (int i = 0; i < params.length; i++) {
                    FilterValueSetParam param = params[i];
                    if ((param.getLookupable().getExpression().equals(baseIndex.getLookupable().getExpression())) &&
                            (param.getFilterOperator().equals(baseIndex.getFilterOperator()))) {
                        boolean found = removeFromIndex(filterCallback, baseIndex, params, currentLevel + 1, param.getFilterForValue());
                        if (found) {
                            indexFound = baseIndex;
                            break;
                        }
                    }
                }
            } else {
                FilterParamIndexBooleanExpr booleanIndex = (FilterParamIndexBooleanExpr) nextIndex;
                for (int i = 0; i < params.length; i++) {
                    FilterValueSetParam param = params[i];
                    // if boolean-expression then match only if this is the last parameter,
                    // all others considered are higher order and sort ahead
                    if (param.getFilterOperator().equals(FilterOperator.BOOLEAN_EXPRESSION)) {
                        boolean found = booleanIndex.removeMayNotExist(param.getFilterForValue());
                        if (found) {
                            indexFound = booleanIndex;
                            break;
                        }
                    }
                }
            }

            if (indexFound == null) {
                return false;
            }

            boolean isEmpty = nextIndex.isEmpty();
            if (isEmpty) {
                // Since we are holding a write lock to this index, there should not be a chance that
                // another thread had been adding anything to this FilterHandleSetNode
                index.remove(filterForValue);
            }
            return true;
        } finally {
            index.getReadWriteLock().writeLock().unlock();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(IndexTreeBuilderRemove.class);
}
