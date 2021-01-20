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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportByteArrEventLongId implements Serializable {
    private static final long serialVersionUID = 5432726228458550622L;
    private long id;
    private byte[] body;

    public SupportByteArrEventLongId(long id, int size) {
        this.id = id;
        body = new byte[size];
    }

    public long getId() {
        return id;
    }

    public byte[] getBody() {
        return body;
    }
}
