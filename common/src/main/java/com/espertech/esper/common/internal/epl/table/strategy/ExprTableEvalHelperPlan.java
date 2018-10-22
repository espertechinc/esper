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
package com.espertech.esper.common.internal.epl.table.strategy;

import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExprTableEvalHelperPlan {
    public static Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> planTableAccess(Collection<ExprTableAccessNode> tableAccessNodes) {
        Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccessForges = new LinkedHashMap<>();

        for (ExprTableAccessNode entry : tableAccessNodes) {
            ExprTableEvalStrategyFactoryForge forge = entry.getTableAccessFactoryForge();
            tableAccessForges.put(entry, forge);
        }

        return tableAccessForges;
    }
}
