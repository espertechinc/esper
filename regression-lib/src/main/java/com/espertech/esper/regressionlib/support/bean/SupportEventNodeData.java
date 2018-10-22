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

public class SupportEventNodeData {

    public String nodeId;
    public String value;

    public SupportEventNodeData(String nodeId, String value) {
        this.nodeId = nodeId;
        this.value = value;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getValue() {
        return value;
    }
}
