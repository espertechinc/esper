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
package com.espertech.esper.regressionlib.support.bean;

import com.espertech.esper.common.client.EventBean;

public class SupportEventInnerTypeWGetIds {

    private final int[] ids;

    public SupportEventInnerTypeWGetIds(int[] ids) {
        this.ids = ids;
    }

    public int[] getIds() {
        return ids;
    }

    public int getIds(int subkey) {
        return ids[subkey];
    }

    public int getIds(EventBean event, String name) {
        return 999999;
    }
}
