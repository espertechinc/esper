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
package com.espertech.esper.dataflow.ops.epl;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.service.UpdateDispatchView;
import com.espertech.esper.dataflow.ops.Select;
import com.espertech.esper.view.ViewSupport;

import java.util.Iterator;

public class EPLSelectUpdateDispatchView extends ViewSupport implements UpdateDispatchView {

    private final Select select;

    public EPLSelectUpdateDispatchView(Select select) {
        this.select = select;
    }

    public void newResult(UniformPair<EventBean[]> result) {
        select.outputOutputRateLimited(result);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
    }

    public EventType getEventType() {
        return null;
    }

    public Iterator<EventBean> iterator() {
        return null;
    }
}
