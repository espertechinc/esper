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

public class SupportIdEventA implements Serializable {
    private final String id;
    private final String pa;
    private final Integer mysec;

    public SupportIdEventA(String id, String pa, Integer mysec) {
        this.id = id;
        this.pa = pa;
        this.mysec = mysec;
    }

    public String getId() {
        return id;
    }

    public String getPa() {
        return pa;
    }

    public Integer getMysec() {
        return mysec;
    }
}
