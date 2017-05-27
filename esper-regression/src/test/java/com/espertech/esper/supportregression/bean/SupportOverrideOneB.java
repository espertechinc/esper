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

public class SupportOverrideOneB extends SupportOverrideOne {
    private String valOneB;

    public SupportOverrideOneB(String valOneB, String valOne, String valBase) {
        super(valOne, valBase);
        this.valOneB = valOneB;
    }

    public String getVal() {
        return valOneB;
    }
}
