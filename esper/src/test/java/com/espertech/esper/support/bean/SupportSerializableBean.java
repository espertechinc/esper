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

import java.io.Serializable;

public class SupportSerializableBean implements Serializable
{
    private String id;

    public SupportSerializableBean(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof SupportSerializableBean))
        {
            return false;
        }
        SupportSerializableBean other = (SupportSerializableBean) obj;
        return other.id.equals(id);
    }

    public int hashCode()
    {
        return id.hashCode();
    }

    public String toString()
    {
        return this.getClass().getSimpleName() + " id=" + id; 
    }
}
