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

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportEventWithCtorSameType implements Serializable {
    private static final long serialVersionUID = 4976249052037292721L;
    private final SupportBean b1;
    private final SupportBean b2;

    public SupportEventWithCtorSameType(SupportBean b1, SupportBean b2) {
        this.b1 = b1;
        this.b2 = b2;
    }

    public SupportBean getB1() {
        return b1;
    }

    public SupportBean getB2() {
        return b2;
    }
}
