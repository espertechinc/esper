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
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.epl.agg.access.AggregationStateMinMaxByEverSpecForge;
import com.espertech.esper.epl.agg.access.AggregationStateSortedSpecForge;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.Comparator;

public class SortedAggregationStateFactoryFactory {

    private final EngineImportService engineImportService;
    private final StatementExtensionSvcContext statementExtensionSvcContext;
    private final ExprNode[] expressions;
    private final boolean[] sortDescending;
    private final boolean ever;
    private final int streamNum;
    private final ExprAggMultiFunctionSortedMinMaxByNode parent;
    private final ExprForge optionalFilter;
    private final boolean join;

    public SortedAggregationStateFactoryFactory(EngineImportService engineImportService, StatementExtensionSvcContext statementExtensionSvcContext, ExprNode[] expressions, boolean[] sortDescending, boolean ever, int streamNum, ExprAggMultiFunctionSortedMinMaxByNode parent, ExprForge optionalFilter, boolean join) {
        this.engineImportService = engineImportService;
        this.statementExtensionSvcContext = statementExtensionSvcContext;
        this.expressions = expressions;
        this.sortDescending = sortDescending;
        this.ever = ever;
        this.streamNum = streamNum;
        this.parent = parent;
        this.optionalFilter = optionalFilter;
        this.join = join;
    }

    public AggregationStateFactoryForge makeForge() {
        boolean sortUsingCollator = engineImportService.isSortUsingCollator();
        Comparator<Object> comparator = ExprNodeUtilityCore.getComparatorHashableMultiKeys(expressions, sortUsingCollator, sortDescending); // hashable-key comparator since we may remove sort keys

        if (ever) {
            AggregationStateMinMaxByEverSpecForge spec = new AggregationStateMinMaxByEverSpecForge(streamNum, expressions, parent.isMax(), comparator, null, optionalFilter);
            return engineImportService.getAggregationFactoryFactory().makeMinMaxEver(statementExtensionSvcContext, parent, spec);
        }

        AggregationStateSortedSpecForge spec = new AggregationStateSortedSpecForge(streamNum, expressions, comparator, null, optionalFilter, join);
        return engineImportService.getAggregationFactoryFactory().makeSorted(statementExtensionSvcContext, parent, spec);
    }
}
