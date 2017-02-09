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

public class TxnEventBase {
    private String transactionId;
    private long timestamp;

    public TxnEventBase(String transactionId, long timestamp) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String toString() {
        return "transactionId=" + transactionId +
                " timestamp=" + timestamp;
    }
}
