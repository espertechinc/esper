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
package com.espertech.esper.common.internal.epl.agg.groupbylocal;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRowFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class AggregationLocalGroupByLevel {
    public final static EPTypeClass EPTYPE = new EPTypeClass(AggregationLocalGroupByLevel.class);
    public final static EPTypeClass EPTYPEARRAY = new EPTypeClass(AggregationLocalGroupByLevel[].class);

    private final AggregationRowFactory rowFactory;
    private final DataInputOutputSerde rowSerde;
    private final EPType[] groupKeyTypes;
    private final ExprEvaluator groupKeyEval;
    private final boolean isDefaultLevel;
    private final DataInputOutputSerde keySerde;

    public AggregationLocalGroupByLevel(AggregationRowFactory rowFactory, DataInputOutputSerde rowSerde, EPType[] groupKeyTypes, ExprEvaluator groupKeyEval, boolean isDefaultLevel, DataInputOutputSerde keySerde) {
        this.rowFactory = rowFactory;
        this.rowSerde = rowSerde;
        this.groupKeyTypes = groupKeyTypes;
        this.groupKeyEval = groupKeyEval;
        this.isDefaultLevel = isDefaultLevel;
        this.keySerde = keySerde;
    }

    public AggregationRowFactory getRowFactory() {
        return rowFactory;
    }

    public DataInputOutputSerde getRowSerde() {
        return rowSerde;
    }

    public EPType[] getGroupKeyTypes() {
        return groupKeyTypes;
    }

    public ExprEvaluator getGroupKeyEval() {
        return groupKeyEval;
    }

    public boolean isDefaultLevel() {
        return isDefaultLevel;
    }

    public DataInputOutputSerde getKeySerde() {
        return keySerde;
    }
}
