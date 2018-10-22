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
package com.espertech.esper.example.terminal.mdb;

import com.espertech.esper.example.terminal.common.LowPaper;
import com.espertech.esper.example.terminal.common.TerminalInfo;
import junit.framework.TestCase;

public class TestEPServiceMDBAdapter extends TestCase {
    public void testAdapter() throws Exception {
        SupportOutboundSender sender = new SupportOutboundSender();
        EPServiceMDBAdapter adapter = new EPServiceMDBAdapter(sender);

        adapter.sendEvent(new LowPaper(new TerminalInfo("t1")));
    }
}
