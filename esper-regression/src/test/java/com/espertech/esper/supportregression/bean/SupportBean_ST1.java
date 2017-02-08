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

public class SupportBean_ST1 {

    private String id;
    private String key1;
    private int p10;
    private Long p11Long;
    private String pcommon;

    public SupportBean_ST1(String id, String key1, int p10) {
        this.id = id;
        this.key1 = key1;
        this.p10 = p10;
    }

    public SupportBean_ST1(String id, int p10) {
        this.id = id;
        this.p10 = p10;
    }

    public SupportBean_ST1(String id, Long p11Long) {
        this.id = id;
        this.p11Long = p11Long;
    }

    public SupportBean_ST1(String id, int p10, String pcommon) {
        this.id = id;
        this.p10 = p10;
        this.pcommon = pcommon;
    }

    public String getId() {
        return id;
    }

    public String getKey1() {
        return key1;
    }

    public int getP10() {
        return p10;
    }

    public Long getP11Long() {
        return p11Long;
    }

    public String getPcommon() {
        return pcommon;
    }

    public void setPcommon(String pcommon) {
        this.pcommon = pcommon;
    }
}
