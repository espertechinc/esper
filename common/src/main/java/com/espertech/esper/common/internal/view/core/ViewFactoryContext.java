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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.type.EPTypeClass;

public class ViewFactoryContext {
    public final static EPTypeClass EPTYPE = new EPTypeClass(ViewFactoryContext.class);

    private int streamNum;
    private Integer subqueryNumber;

    public void setStreamNum(int streamNum) {
        this.streamNum = streamNum;
    }

    public void setSubqueryNumber(Integer subqueryNumber) {
        this.subqueryNumber = subqueryNumber;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public Integer getSubqueryNumber() {
        return subqueryNumber;
    }
}
