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

public class SupportEventNode {
    public String id;

    public SupportEventNode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String compute(Object data) {
        if (data == null) {
            return null;
        }
        SupportEventNodeData nodeData = (SupportEventNodeData) data;
        return id + nodeData.getValue();
    }
}
