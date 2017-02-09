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
package com.espertech.esper.supportunit.bean;

public class SupportSelfReferenceEvent {
    private SupportSelfReferenceEvent selfRef;

    private String value;

    public SupportSelfReferenceEvent() {
    }

    public SupportSelfReferenceEvent getSelfRef() {
        return selfRef;
    }

    public String getValue() {
        return value;
    }

    public void setSelfRef(SupportSelfReferenceEvent selfRef) {
        this.selfRef = selfRef;
    }
}
