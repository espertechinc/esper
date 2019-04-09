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

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryForge;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactoryForge;

import java.util.List;

public class SubqueryIndexForgeDesc {
    private final EventTableFactoryFactoryForge tableForge;
    private final SubordTableLookupStrategyFactoryForge lookupForge;
    private final List<StmtClassForgableFactory> additionalForgeables;

    public SubqueryIndexForgeDesc(EventTableFactoryFactoryForge tableForge, SubordTableLookupStrategyFactoryForge lookupForge, List<StmtClassForgableFactory> additionalForgeables) {
        this.tableForge = tableForge;
        this.lookupForge = lookupForge;
        this.additionalForgeables = additionalForgeables;
    }

    public EventTableFactoryFactoryForge getTableForge() {
        return tableForge;
    }

    public SubordTableLookupStrategyFactoryForge getLookupForge() {
        return lookupForge;
    }

    public List<StmtClassForgableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
