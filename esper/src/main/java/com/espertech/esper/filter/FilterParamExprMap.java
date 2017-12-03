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

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.filterspec.FilterSpecParam;

import java.util.*;

/**
 * A two-sided map for filter parameters mapping filter expression nodes to filter parameters and
 * back. For use in optimizing filter expressions.
 */
public class FilterParamExprMap {
    private Map<ExprNode, FilterSpecParam> exprNodes;
    private Map<FilterSpecParam, ExprNode> specParams;

    /**
     * Ctor.
     */
    public FilterParamExprMap() {
        exprNodes = new LinkedHashMap<ExprNode, FilterSpecParam>();
        specParams = new LinkedHashMap<FilterSpecParam, ExprNode>();
    }

    /**
     * Add a node and filter param.
     *
     * @param exprNode is the node to add
     * @param param    is null if the expression node has not optimized form
     */
    public void put(ExprNode exprNode, FilterSpecParam param) {
        exprNodes.put(exprNode, param);
        if (param != null) {
            specParams.put(param, exprNode);
        }
    }

    /**
     * Returns all expression nodes for which no filter parameter exists.
     *
     * @return list of expression nodes
     */
    public List<ExprNode> getUnassignedExpressions() {
        List<ExprNode> unassigned = new ArrayList<ExprNode>();
        for (Map.Entry<ExprNode, FilterSpecParam> entry : exprNodes.entrySet()) {
            if (entry.getValue() == null) {
                unassigned.add(entry.getKey());
            }
        }
        return unassigned;
    }

    public int countUnassignedExpressions() {
        int count = 0;
        for (Map.Entry<ExprNode, FilterSpecParam> entry : exprNodes.entrySet()) {
            if (entry.getValue() == null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns all filter parameters.
     *
     * @return filter parameters
     */
    public Collection<FilterSpecParam> getFilterParams() {
        return specParams.keySet();
    }

    public void removeNode(ExprNode node) {
        FilterSpecParam param = exprNodes.remove(node);
        if (param != null) {
            specParams.remove(param);
        }
    }

    /**
     * Removes a filter parameter and it's associated expression node
     *
     * @param param is the parameter to remove
     * @return expression node removed
     */
    public ExprNode removeEntry(FilterSpecParam param) {
        ExprNode exprNode = specParams.get(param);
        if (exprNode == null) {
            throw new IllegalStateException("Not found in collection param: " + param);
        }

        specParams.remove(param);
        exprNodes.remove(exprNode);

        return exprNode;
    }

    /**
     * Remove a filter parameter leaving the expression node in place.
     *
     * @param param filter parameter to remove
     */
    public void removeValue(FilterSpecParam param) {
        ExprNode exprNode = specParams.get(param);
        if (exprNode == null) {
            throw new IllegalStateException("Not found in collection param: " + param);
        }

        specParams.remove(param);
        exprNodes.put(exprNode, null);
    }

    public void clear() {
        exprNodes.clear();
        specParams.clear();
    }

    public void add(FilterParamExprMap other) {
        exprNodes.putAll(other.exprNodes);
        specParams.putAll(other.specParams);
    }
}
