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
package com.espertech.esper.common.internal.epl.pattern.core;

public abstract class EvalFactoryNodeBase implements EvalFactoryNode {
    private short factoryNodeId;
    private String textForAudit;

    public void setFactoryNodeId(short factoryNodeId) {
        this.factoryNodeId = factoryNodeId;
    }

    public short getFactoryNodeId() {
        return factoryNodeId;
    }

    public String getTextForAudit() {
        return textForAudit;
    }

    public void setTextForAudit(String textForAudit) {
        this.textForAudit = textForAudit;
    }
}
