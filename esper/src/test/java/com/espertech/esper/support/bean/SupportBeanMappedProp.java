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

import java.util.Map;

public class SupportBeanMappedProp
{
    private final String id;
    private final Map<String, String> mapprop;

    public SupportBeanMappedProp(String id, Map<String, String> mapprop)
    {
        this.id = id;
        this.mapprop = mapprop;
    }

    public String getId()
    {
        return id;
    }

    public String getMapEntry(String key)
    {
        return mapprop.get(key);
    }
}
