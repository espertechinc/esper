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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.io.Serializable;

public class SupportBeanSourceEvent implements Serializable {
    private SupportBean sb;
    private SupportBean_S0[] s0Arr;

    public SupportBeanSourceEvent(SupportBean sb, SupportBean_S0[] s0Arr) {
        this.sb = sb;
        this.s0Arr = s0Arr;
    }

    public SupportBean getSb() {
        return sb;
    }

    public SupportBean_S0[] getS0Arr() {
        return s0Arr;
    }
}
