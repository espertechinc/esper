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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionSortedMinMaxByNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;

public class SortedAggregationStateDesc {

    private final boolean max;
    private final ClasspathImportServiceCompileTime classpathImportService;
    private final ExprNode[] criteria;
    private final Class[] criteriaTypes;
    private final DataInputOutputSerdeForge[] criteriaSerdes;
    private final boolean[] sortDescending;
    private final boolean ever;
    private final int streamNum;
    private final ExprAggMultiFunctionSortedMinMaxByNode parent;
    private final ExprForge optionalFilter;
    private final EventType streamEventType;

    public SortedAggregationStateDesc(boolean max, ClasspathImportServiceCompileTime classpathImportService, ExprNode[] criteria, Class[] criteriaTypes, DataInputOutputSerdeForge[] criteriaSerdes, boolean[] sortDescending, boolean ever, int streamNum, ExprAggMultiFunctionSortedMinMaxByNode parent, ExprForge optionalFilter, EventType streamEventType) {
        this.max = max;
        this.classpathImportService = classpathImportService;
        this.criteria = criteria;
        this.criteriaTypes = criteriaTypes;
        this.criteriaSerdes = criteriaSerdes;
        this.sortDescending = sortDescending;
        this.ever = ever;
        this.streamNum = streamNum;
        this.parent = parent;
        this.optionalFilter = optionalFilter;
        this.streamEventType = streamEventType;
    }

    public ClasspathImportServiceCompileTime getClasspathImportService() {
        return classpathImportService;
    }

    public ExprNode[] getCriteria() {
        return criteria;
    }

    public boolean[] getSortDescending() {
        return sortDescending;
    }

    public boolean isEver() {
        return ever;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public ExprAggMultiFunctionSortedMinMaxByNode getParent() {
        return parent;
    }

    public ExprForge getOptionalFilter() {
        return optionalFilter;
    }

    public EventType getStreamEventType() {
        return streamEventType;
    }

    public boolean isSortUsingCollator() {
        return classpathImportService.isSortUsingCollator();
    }

    public boolean isMax() {
        return max;
    }

    public Class[] getCriteriaTypes() {
        return criteriaTypes;
    }

    public DataInputOutputSerdeForge[] getCriteriaSerdes() {
        return criteriaSerdes;
    }
}
