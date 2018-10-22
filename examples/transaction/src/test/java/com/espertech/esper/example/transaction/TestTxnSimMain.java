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
package com.espertech.esper.example.transaction;

import com.espertech.esper.example.transaction.sim.TxnGenMain;
import junit.framework.TestCase;

public class TestTxnSimMain extends TestCase {
    public void testTiny() throws Exception {
        TxnGenMain main = new TxnGenMain(20, 200, "TransactionExample", false);
        main.run();
    }

    public void testSmall() throws Exception {
        TxnGenMain main = new TxnGenMain(1000, 3000, "TransactionExample", false);
        main.run();
    }
}
