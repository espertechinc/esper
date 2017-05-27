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
package com.espertech.esper.supportregression.subscriber;

import com.espertech.esper.client.EPStatement;

import static org.junit.Assert.fail;

public class SupportSubscriberUpdateBothFootprints extends SupportSubscriberRowByRowSpecificBase {

    public SupportSubscriberUpdateBothFootprints() {
        super(true);
    }

    public void update(String theString, int intPrimitive) {
        fail();
    }

    public void update(EPStatement stmt, String theString, int intPrimitive) {
        addIndication(stmt, new Object[]{theString, intPrimitive});
    }
}
