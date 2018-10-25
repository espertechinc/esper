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

import com.espertech.esper.common.internal.support.SupportBean_S2;

import java.io.Serializable;

public final class SupportCtorSB2WithObjectArray implements Serializable {
    private final SupportBean_S2 sb;
    private final Object[] arr;

    public SupportCtorSB2WithObjectArray(SupportBean_S2 sb, Object[] arr) {
        this.sb = sb;
        this.arr = arr;
    }

    public SupportBean_S2 getSb() {
        return sb;
    }

    public Object[] getArr() {
        return arr;
    }
}
