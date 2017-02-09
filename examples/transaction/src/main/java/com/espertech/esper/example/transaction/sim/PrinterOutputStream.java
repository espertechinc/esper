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
package com.espertech.esper.example.transaction.sim;

import com.espertech.esper.example.transaction.TxnEventBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Subclass to output events in your preferred format.
 *
 * @author Hans Gilde
 */
public class PrinterOutputStream implements OutputStream {
    private PrintStream os;

    public PrinterOutputStream(PrintStream os) {
        this.os = os;
    }

    public void output(List<TxnEventBase> bucket) throws IOException {
        log.info(".output Start of bucket, " + bucket.size() + " items");
        for (TxnEventBase theEvent : bucket) {
            os.println(theEvent.toString());
        }
        log.info(".output End of bucket");
    }

    private static final Logger log = LoggerFactory.getLogger(PrinterOutputStream.class);
}
