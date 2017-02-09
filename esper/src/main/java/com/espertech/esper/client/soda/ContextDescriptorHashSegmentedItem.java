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

import java.io.StringWriter;

/**
 * Context detail for a library-func and filter pair for the hash segmented context.
 */
public class ContextDescriptorHashSegmentedItem implements ContextDescriptor {

    private static final long serialVersionUID = 5445043920003179875L;
    private Expression hashFunction;    // expecting SingleRowMethodExpression
    private Filter filter;

    /**
     * Ctor.
     */
    public ContextDescriptorHashSegmentedItem() {
    }

    /**
     * Ctor.
     *
     * @param hashFunction the hash function, expecting SingleRowMethodExpression
     * @param filter       the event types to apply to
     */
    public ContextDescriptorHashSegmentedItem(Expression hashFunction, Filter filter) {
        this.hashFunction = hashFunction;
        this.filter = filter;
    }

    /**
     * Returns the filter.
     *
     * @return filter
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Sets the filter.
     *
     * @param filter filter
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * Returns the hash function.
     *
     * @return hash function
     */
    public Expression getHashFunction() {
        return hashFunction;
    }

    /**
     * Set the hash function (SingleRowMethodExpression)
     *
     * @param hashFunction to set
     */
    public void setHashFunction(Expression hashFunction) {
        this.hashFunction = hashFunction;
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        if (hashFunction != null) {
            hashFunction.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        writer.append(" from ");
        filter.toEPL(writer, formatter);
    }
}
