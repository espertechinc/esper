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
package com.espertech.esper.regressionlib.support.extend.aggfunc;

import com.espertech.esper.common.client.hook.aggfunc.AggregationFunction;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SupportConcatWManagedAggregationFunctionSerde {
    public static void write(DataOutput output, AggregationFunction value) throws IOException {
        SupportConcatWManagedAggregationFunction agg = (SupportConcatWManagedAggregationFunction) value;
        output.writeUTF(agg.getValue());
    }

    public static AggregationFunction read(DataInput input) throws IOException {
        String current = input.readUTF();
        if (current.isEmpty()) {
            return new SupportConcatWManagedAggregationFunction();
        }
        return new SupportConcatWManagedAggregationFunction(new StringBuilder(current));
    }
}
