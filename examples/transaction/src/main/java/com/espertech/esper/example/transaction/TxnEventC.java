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

public class TxnEventC extends TxnEventBase {
    private String supplierId;

    public TxnEventC(String transactionId, long timestamp, String supplierId) {
        super(transactionId, timestamp);
        this.supplierId = supplierId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String toString() {
        return super.toString() + " supplierId=" + supplierId;
    }

}
