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

import java.io.IOException;
import java.util.List;

/**
 * Interface to output events in your preferred format.
 */
public interface OutputStream {
    public void output(List<TxnEventBase> bucket) throws IOException;
}
