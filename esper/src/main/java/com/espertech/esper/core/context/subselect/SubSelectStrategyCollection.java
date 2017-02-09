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
package com.espertech.esper.core.context.subselect;

import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds stream information for subqueries.
 */
public class SubSelectStrategyCollection {
    private Map<ExprSubselectNode, SubSelectStrategyFactoryDesc> subqueries;

    /**
     * Ctor.
     */
    public SubSelectStrategyCollection() {
    }

    /**
     * Add lookup.
     *
     * @param subselectNode   is the subselect expression node
     * @param prototypeHolder strategy prototype for subselect
     */
    public void add(ExprSubselectNode subselectNode, SubSelectStrategyFactoryDesc prototypeHolder) {
        if (subqueries == null) {
            subqueries = new HashMap<ExprSubselectNode, SubSelectStrategyFactoryDesc>();
        }
        subqueries.put(subselectNode, prototypeHolder);
    }

    public Map<ExprSubselectNode, SubSelectStrategyFactoryDesc> getSubqueries() {
        if (subqueries == null) {
            return Collections.emptyMap();
        }
        return subqueries;
    }
}
