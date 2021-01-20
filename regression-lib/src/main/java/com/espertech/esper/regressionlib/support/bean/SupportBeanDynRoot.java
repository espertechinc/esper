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

public class SupportBeanDynRoot implements SupportMarkerInterface, Serializable {
    private static final long serialVersionUID = 6510012311845616744L;
    private Object item;

    public SupportBeanDynRoot(Object inner) {
        this.item = inner;
    }

    public Object getItem() {
        return item;
    }
}
