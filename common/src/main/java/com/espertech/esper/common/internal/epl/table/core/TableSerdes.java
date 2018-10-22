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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.internal.serde.DataInputOutputSerdeWCollation;

public class TableSerdes {
    private final DataInputOutputSerdeWCollation[] column;
    private final DataInputOutputSerdeWCollation aggregations;

    public TableSerdes(DataInputOutputSerdeWCollation[] column, DataInputOutputSerdeWCollation aggregations) {
        this.column = column;
        this.aggregations = aggregations;
    }

    public DataInputOutputSerdeWCollation[] getColumnStartingZero() {
        return column;
    }

    public DataInputOutputSerdeWCollation getAggregations() {
        return aggregations;
    }
}
