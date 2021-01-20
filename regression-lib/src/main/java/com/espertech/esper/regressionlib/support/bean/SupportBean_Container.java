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

import com.espertech.esper.common.internal.support.SupportBean;

import java.io.Serializable;
import java.util.List;

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportBean_Container implements Serializable {

    private static final long serialVersionUID = 6649472095639240439L;
    private List<SupportBean> beans;

    public SupportBean_Container(List<SupportBean> beans) {
        this.beans = beans;
    }

    public List<SupportBean> getBeans() {
        return beans;
    }

    public void setBeans(List<SupportBean> beans) {
        this.beans = beans;
    }

    public String toString() {
        return "SupportBean_Container{" +
            "beans=" + beans +
            '}';
    }
}
