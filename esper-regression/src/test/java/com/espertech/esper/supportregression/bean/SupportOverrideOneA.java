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

import java.io.Serializable;

public class SupportOverrideOneA extends SupportOverrideOne implements Serializable {
    private String valOneA;

    public SupportOverrideOneA(String valOneA, String valOne, String valBase) {
        super(valOne, valBase);
        this.valOneA = valOneA;
    }

    public String getVal() {
        return valOneA;
    }
}
