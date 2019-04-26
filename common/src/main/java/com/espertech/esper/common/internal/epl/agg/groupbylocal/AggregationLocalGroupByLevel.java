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
import com.espertech.esper.common.internal.epl.agg.core.AggregationRowFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class AggregationLocalGroupByLevel {

    private final AggregationRowFactory rowFactory;
    private final DataInputOutputSerde rowSerde;
    private final Class[] groupKeyTypes;
    private final ExprEvaluator groupKeyEval;
    private final boolean isDefaultLevel;
    private final DataInputOutputSerde<Object> keySerde;

    public AggregationLocalGroupByLevel(AggregationRowFactory rowFactory, DataInputOutputSerde rowSerde, Class[] groupKeyTypes, ExprEvaluator groupKeyEval, boolean isDefaultLevel, DataInputOutputSerde<Object> keySerde) {
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

    public Class[] getGroupKeyTypes() {
        return groupKeyTypes;
    }

    public ExprEvaluator getGroupKeyEval() {
        return groupKeyEval;
    }

    public boolean isDefaultLevel() {
        return isDefaultLevel;
    }

    public DataInputOutputSerde<Object> getKeySerde() {
        return keySerde;
    }
}
