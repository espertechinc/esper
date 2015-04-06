/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.bean;

public class SupportBeanRendererTwo
{
    private String stringVal;
    private SupportEnum enumValue;

    public SupportBeanRendererTwo()
    {
    }

    public SupportEnum getEnumValue()
    {
        return enumValue;
    }

    public void setEnumValue(SupportEnum enumValue)
    {
        this.enumValue = enumValue;
    }

    public String getStringVal()
    {
        return stringVal;
    }

    public void setStringVal(String stringVal)
    {
        this.stringVal = stringVal;
    }
}
