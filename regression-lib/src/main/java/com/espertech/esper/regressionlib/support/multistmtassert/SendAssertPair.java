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
package com.espertech.esper.regressionlib.support.multistmtassert;

public class SendAssertPair {
    private final EventSender sender;
    private final Asserter asserter;

    public SendAssertPair(EventSender sender, Asserter asserter) {
        this.sender = sender;
        this.asserter = asserter;
    }

    public EventSender getSender() {
        return sender;
    }

    public Asserter getAsserter() {
        return asserter;
    }
}
