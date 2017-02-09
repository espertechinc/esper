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

public class SupportChainChildOne {
    private String text;
    private int value;

    public SupportChainChildOne(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public SupportChainChildTwo getChildTwo(String append) {
        return new SupportChainChildTwo(text + append, 1 + value);
    }
}
