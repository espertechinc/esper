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
package com.espertech.esper.common.internal.epl.expression.core;

public class ExprEnumerationForgeDesc {
    private final ExprEnumerationForge forge;
    private final boolean istreamOnly;
    private final int directIndexStreamNumber;

    public ExprEnumerationForgeDesc(ExprEnumerationForge forge, boolean istreamOnly, int directIndexStreamNumber) {
        this.forge = forge;
        this.istreamOnly = istreamOnly;
        this.directIndexStreamNumber = directIndexStreamNumber;
    }

    public ExprEnumerationForge getForge() {
        return forge;
    }

    public boolean isIstreamOnly() {
        return istreamOnly;
    }

    public int getDirectIndexStreamNumber() {
        return directIndexStreamNumber;
    }
}
