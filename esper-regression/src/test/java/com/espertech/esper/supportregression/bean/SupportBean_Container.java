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

import java.util.List;

public class SupportBean_Container {

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
}
