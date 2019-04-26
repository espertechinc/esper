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
package com.espertech.esper.common.internal.epl.table.compiletime;

import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRowStateForgeDesc;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.List;
import java.util.Map;

public class TableAccessAnalysisResult {
    private final Map<String, TableMetadataColumn> tableColumns;
    private final ObjectArrayEventType internalEventType;
    private final DataInputOutputSerdeForge[] internalEventTypePropertySerdes;
    private final ObjectArrayEventType publicEventType;
    private final TableMetadataColumnPairPlainCol[] colsPlain;
    private final TableMetadataColumnPairAggMethod[] colsAggMethod;
    private final TableMetadataColumnPairAggAccess[] colsAccess;
    private final AggregationRowStateForgeDesc aggDesc;
    private final String[] primaryKeyColumns;
    private final EventPropertyGetterSPI[] primaryKeyGetters;
    private final Class[] primaryKeyTypes;
    private final int[] primaryKeyColNums;
    private final MultiKeyClassRef primaryKeyMultikeyClasses;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public TableAccessAnalysisResult(Map<String, TableMetadataColumn> tableColumns, ObjectArrayEventType internalEventType, DataInputOutputSerdeForge[] internalEventTypePropertySerdes, ObjectArrayEventType publicEventType, TableMetadataColumnPairPlainCol[] colsPlain, TableMetadataColumnPairAggMethod[] colsAggMethod, TableMetadataColumnPairAggAccess[] colsAccess, AggregationRowStateForgeDesc aggDesc, String[] primaryKeyColumns, EventPropertyGetterSPI[] primaryKeyGetters, Class[] primaryKeyTypes, int[] primaryKeyColNums, MultiKeyClassRef primaryKeyMultikeyClasses, List<StmtClassForgeableFactory> additionalForgeables) {
        this.tableColumns = tableColumns;
        this.internalEventType = internalEventType;
        this.internalEventTypePropertySerdes = internalEventTypePropertySerdes;
        this.publicEventType = publicEventType;
        this.colsPlain = colsPlain;
        this.colsAggMethod = colsAggMethod;
        this.colsAccess = colsAccess;
        this.aggDesc = aggDesc;
        this.primaryKeyColumns = primaryKeyColumns;
        this.primaryKeyGetters = primaryKeyGetters;
        this.primaryKeyTypes = primaryKeyTypes;
        this.primaryKeyColNums = primaryKeyColNums;
        this.primaryKeyMultikeyClasses = primaryKeyMultikeyClasses;
        this.additionalForgeables = additionalForgeables;
    }

    public Map<String, TableMetadataColumn> getTableColumns() {
        return tableColumns;
    }

    public ObjectArrayEventType getInternalEventType() {
        return internalEventType;
    }

    public ObjectArrayEventType getPublicEventType() {
        return publicEventType;
    }

    public TableMetadataColumnPairPlainCol[] getColsPlain() {
        return colsPlain;
    }

    public TableMetadataColumnPairAggMethod[] getColsAggMethod() {
        return colsAggMethod;
    }

    public TableMetadataColumnPairAggAccess[] getColsAccess() {
        return colsAccess;
    }

    public AggregationRowStateForgeDesc getAggDesc() {
        return aggDesc;
    }

    public EventPropertyGetterSPI[] getPrimaryKeyGetters() {
        return primaryKeyGetters;
    }

    public Class[] getPrimaryKeyTypes() {
        return primaryKeyTypes;
    }

    public String[] getPrimaryKeyColumns() {
        return primaryKeyColumns;
    }

    public int[] getPrimaryKeyColNums() {
        return primaryKeyColNums;
    }

    public MultiKeyClassRef getPrimaryKeyMultikeyClasses() {
        return primaryKeyMultikeyClasses;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }

    public DataInputOutputSerdeForge[] getInternalEventTypePropertySerdes() {
        return internalEventTypePropertySerdes;
    }
}
