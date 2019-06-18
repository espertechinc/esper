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
package com.espertech.esper.common.internal.bytecodemodel.core;

public enum CodegenClassType {
    KEYPROVISIONING(10),
    KEYPROVISIONINGSERDE(20),
    STATEMENTFIELDS(30),
    JSONNESTEDCLASSDELEGATEANDFACTORY(40),
    JSONEVENT(42),
    JSONDELEGATE(43),
    JSONDELEGATEFACTORY(44),
    EVENTSERDE(50),
    RESULTSETPROCESSORFACTORYPROVIDER(60),
    OUTPUTPROCESSVIEWFACTORYPROVIDER(70),
    STATEMENTAIFACTORYPROVIDER(80),
    STATEMENTPROVIDER(90),
    FAFQUERYMETHODPROVIDER(100),
    FAFPROVIDER(110),
    MODULEPROVIDER(120);

    CodegenClassType(int sortCode) {
        this.sortCode = sortCode;
    }

    private final int sortCode;

    public int getSortCode() {
        return sortCode;
    }
}
