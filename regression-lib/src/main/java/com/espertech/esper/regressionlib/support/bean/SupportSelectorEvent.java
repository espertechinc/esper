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

import java.io.Serializable;

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportSelectorEvent implements Serializable {
    private static final long serialVersionUID = 1740288356316362577L;
    private final String selectorId;
    private final String selector;

    public SupportSelectorEvent(String selectorId, String selector) {
        this.selectorId = selectorId;
        this.selector = selector;
    }

    public String getSelectorId() {
        return selectorId;
    }

    public String getSelector() {
        return selector;
    }
}
