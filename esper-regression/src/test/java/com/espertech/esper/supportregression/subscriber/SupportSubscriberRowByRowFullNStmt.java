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

public class SupportSubscriberRowByRowFullNStmt extends SupportSubscriberRowByRowFullBase {
    public SupportSubscriberRowByRowFullNStmt() {
        super(false);
    }

    public void updateStart(int lengthIStream, int lengthRStream) {
        addUpdateStart(lengthIStream, lengthRStream);
    }

    public void update(String theString, int intPrimitive) {
        addUpdate(new Object[]{theString, intPrimitive});
    }

    public void updateRStream(String theString, int intPrimitive) {
        addUpdateRStream(new Object[]{theString, intPrimitive});
    }

    public void updateEnd() {
        addUpdateEnd();
    }
}
