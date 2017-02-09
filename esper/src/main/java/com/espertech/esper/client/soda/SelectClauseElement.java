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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Item in a select-clause to describe individual select-clause expressions or wildcard(s).
 */
public interface SelectClauseElement extends Serializable {
    /**
     * Output the string rendering of the select clause element.
     *
     * @param writer to output to
     */
    public void toEPLElement(StringWriter writer);
}
