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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;

import java.util.List;
import java.util.Map;

public class SubSelectHelperForgePlan {
    private final Map<ExprSubselectNode, SubSelectFactoryForge> subselects;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public SubSelectHelperForgePlan(Map<ExprSubselectNode, SubSelectFactoryForge> subselects, List<StmtClassForgeableFactory> additionalForgeables) {
        this.subselects = subselects;
        this.additionalForgeables = additionalForgeables;
    }

    public Map<ExprSubselectNode, SubSelectFactoryForge> getSubselects() {
        return subselects;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
