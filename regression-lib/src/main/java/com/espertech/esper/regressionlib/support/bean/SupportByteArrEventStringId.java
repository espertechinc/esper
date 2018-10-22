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

public class SupportByteArrEventStringId implements Serializable {
    private String id;
    private byte[] body;

    public SupportByteArrEventStringId(String id, byte[] body) {
        this.id = id;
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public byte[] getBody() {
        return body;
    }
}
