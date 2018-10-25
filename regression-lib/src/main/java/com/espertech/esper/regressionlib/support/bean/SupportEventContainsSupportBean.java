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

import java.io.Serializable;

public final class SupportEventContainsSupportBean implements Serializable {
    private final SupportBean sb;

    public SupportEventContainsSupportBean(SupportBean sb) {
        this.sb = sb;
    }

    public SupportBean getSb() {
        return sb;
    }
}
