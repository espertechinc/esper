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
package com.espertech.esper.client.context;

/**
 * Context partition identifier for category context.
 */
public class ContextPartitionIdentifierCategory extends ContextPartitionIdentifier {
    private static final long serialVersionUID = -3619019398605079495L;
    private String label;

    /**
     * Ctor.
     */
    public ContextPartitionIdentifierCategory() {
    }

    /**
     * Ctor.
     *
     * @param label of category
     */
    public ContextPartitionIdentifierCategory(String label) {
        this.label = label;
    }

    /**
     * Returns the category label.
     *
     * @return label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the category label.
     *
     * @param label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    public boolean compareTo(ContextPartitionIdentifier other) {
        if (!(other instanceof ContextPartitionIdentifierCategory)) {
            return false;
        }
        return label.equals(((ContextPartitionIdentifierCategory) other).label);
    }

    public String toString() {
        return "ContextPartitionIdentifierCategory{" +
                "label='" + label + '\'' +
                '}';
    }
}
