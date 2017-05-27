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

public class SupportSubscriberRowByRowFullWStmt extends SupportSubscriberRowByRowFullBase {
    public SupportSubscriberRowByRowFullWStmt() {
        super(true);
    }

    public void updateStart(EPStatement statement, int lengthIStream, int lengthRStream) {
        addUpdateStart(statement, lengthIStream, lengthRStream);
    }

    public void update(EPStatement statement, String theString, int intPrimitive) {
        addUpdate(statement, new Object[]{theString, intPrimitive});
    }

    public void updateRStream(EPStatement statement, String theString, int intPrimitive) {
        addUpdateRStream(statement, new Object[]{theString, intPrimitive});
    }

    public void updateEnd(EPStatement statement) {
        addUpdateEnd(statement);
    }
}
