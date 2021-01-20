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

import java.io.Serializable;

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportEventInnerTypeWGetIds implements Serializable {

    private static final long serialVersionUID = -2409464759004736570L;
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
