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

public class TxnEventA extends TxnEventBase {
    private String customerId;

    public TxnEventA(String transactionId, long timestamp, String customerId) {
        super(transactionId, timestamp);
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String toString() {
        return super.toString() + " customerId=" + customerId;
    }
}
