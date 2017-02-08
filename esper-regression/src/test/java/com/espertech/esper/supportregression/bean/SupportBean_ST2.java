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
package com.espertech.esper.supportregression.bean;

public class SupportBean_ST2 {

    private String id;
    private String key2;
    private int p20;

    public SupportBean_ST2(String id, String key2, int p20) {
        this.id = id;
        this.key2 = key2;
        this.p20 = p20;
    }

    public String getId() {
        return id;
    }

    public String getKey2() {
        return key2;
    }

    public int getP20() {
        return p20;
    }
}
