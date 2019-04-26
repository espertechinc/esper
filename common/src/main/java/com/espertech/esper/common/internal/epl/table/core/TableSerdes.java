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

import com.espertech.esper.common.client.serde.DataInputOutputSerde;

public class TableSerdes {
    private final DataInputOutputSerde[] column;
    private final DataInputOutputSerde aggregations;

    public TableSerdes(DataInputOutputSerde[] column, DataInputOutputSerde aggregations) {
        if (column == null || aggregations == null) {
            throw new IllegalArgumentException("Expected serdes not received");
        }
        this.column = column;
        this.aggregations = aggregations;
    }

    public DataInputOutputSerde[] getColumnStartingZero() {
        return column;
    }

    public DataInputOutputSerde getAggregations() {
        return aggregations;
    }
}
