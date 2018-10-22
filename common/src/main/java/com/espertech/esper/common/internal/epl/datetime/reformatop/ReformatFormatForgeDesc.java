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
package com.espertech.esper.common.internal.epl.datetime.reformatop;

public class ReformatFormatForgeDesc {
    private final boolean java8;
    private final Class formatterType;

    public ReformatFormatForgeDesc(boolean java8, Class formatterType) {
        this.java8 = java8;
        this.formatterType = formatterType;
    }

    public boolean isJava8() {
        return java8;
    }

    public Class getFormatterType() {
        return formatterType;
    }
}
