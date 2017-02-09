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
package com.espertech.esper.supportunit.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.service.UpdateDispatchView;
import com.espertech.esper.view.Viewable;

public class SupportSchemaNeutralView extends SupportBaseView implements UpdateDispatchView {
    public SupportSchemaNeutralView() {
    }

    public SupportSchemaNeutralView(String viewName) {
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        this.lastNewData = newData;
        this.lastOldData = oldData;

        updateChildren(newData, oldData);
    }

    public void setParent(Viewable parent) {
        super.setParent(parent);
        if (parent != null) {
            setEventType(parent.getEventType());
        } else {
            setEventType(null);
        }
    }

    public void newResult(UniformPair<EventBean[]> result) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
