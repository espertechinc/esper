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
package com.espertech.esper.regressionlib.support.extend.aggmultifunc;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionState;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIOSerializableObjectSerde;
import com.espertech.esper.common.internal.support.SupportBean;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SupportAggMFEventsAsListStateSerde {
    public static void write(DataOutput output, EventBeanCollatedWriter writer, AggregationMultiFunctionState stateMF) throws IOException {
        SupportAggMFEventsAsListState state = (SupportAggMFEventsAsListState) stateMF;
        output.writeInt(state.getEvents().size());
        for (SupportBean supportBean : state.getEvents()) {
            DIOSerializableObjectSerde.serializeTo(supportBean, output);
        }
    }

    public static AggregationMultiFunctionState read(DataInput input) throws IOException {
        int size = input.readInt();
        List<SupportBean> events = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            events.add((SupportBean) DIOSerializableObjectSerde.deserializeFrom(input));
        }
        return new SupportAggMFEventsAsListState(events);
    }
}
