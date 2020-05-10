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
package com.espertech.esper.common.internal.compile.stage2;

public enum FilterSpecCompilerIndexPlannerHint {
    LKUPCOMPOSITE("lkupcomposite"),
    VALUECOMPOSITE("valuecomposite"),
    BOOLCOMPOSITE("boolcomposite"),
    CONDITIONS("condition");

    private final String nameLowercase;

    private FilterSpecCompilerIndexPlannerHint(String nameLowercase) {
        this.nameLowercase = nameLowercase;
    }

    public String getNameLowercase() {
        return nameLowercase;
    }
}
