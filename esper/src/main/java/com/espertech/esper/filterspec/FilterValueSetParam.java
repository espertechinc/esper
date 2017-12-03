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
package com.espertech.esper.filterspec;

import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * This interface represents one filter parameter in an {@link FilterValueSet} filter specification.
 * <p> Each filtering parameter has a lookup-able and operator type, and a value to filter for.
 */
public interface FilterValueSetParam extends Serializable {
    /**
     * Returns the lookup-able for the filter parameter.
     *
     * @return lookup-able
     */
    public ExprFilterSpecLookupable getLookupable();

    /**
     * Returns the filter operator type.
     *
     * @return filter operator type
     */
    public FilterOperator getFilterOperator();

    /**
     * Return the filter parameter constant to filter for.
     *
     * @return filter parameter constant's value
     */
    public Object getFilterForValue();

    public void appendTo(StringWriter writer);
}
