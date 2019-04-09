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
    RESULTSETPROCESSORFACTORYPROVIDER(40),
    OUTPUTPROCESSVIEWFACTORYPROVIDER(50),
    STATEMENTAIFACTORYPROVIDER(60),
    STATEMENTPROVIDER(70),
    FAFQUERYMETHODPROVIDER(80),
    FAFPROVIDER(90),
    MODULEPROVIDER(100);

    CodegenClassType(int sortCode) {
        this.sortCode = sortCode;
    }

    private final int sortCode;

    public int getSortCode() {
        return sortCode;
    }
}
