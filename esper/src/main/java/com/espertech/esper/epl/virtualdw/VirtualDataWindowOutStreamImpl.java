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
package com.espertech.esper.epl.virtualdw;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.VirtualDataWindowOutStream;
import com.espertech.esper.view.ViewSupport;

public class VirtualDataWindowOutStreamImpl implements VirtualDataWindowOutStream {
    private ViewSupport view;

    public void setView(ViewSupport view) {
        this.view = view;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        view.updateChildren(newData, oldData);
    }
}
