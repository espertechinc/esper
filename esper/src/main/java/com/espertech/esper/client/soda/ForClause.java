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
import java.util.ArrayList;
import java.util.List;

/**
 * A for-clause is a means to specify listener and observer delivery.
 */
public class ForClause implements Serializable {
    private static final long serialVersionUID = -5295670899343685182L;

    private List<ForClauseItem> items = new ArrayList<ForClauseItem>();

    /**
     * Creates an empty group-by clause, to add to via add methods.
     *
     * @return group-by clause
     */
    public static ForClause create() {
        return new ForClause();
    }

    /**
     * Returns for-clause items.
     *
     * @return items
     */
    public List<ForClauseItem> getItems() {
        return items;
    }

    /**
     * Sets for-clause items.
     *
     * @param items items
     */
    public void setItems(List<ForClauseItem> items) {
        this.items = items;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        String delimiter = "";
        for (ForClauseItem child : items) {
            writer.write(delimiter);
            child.toEPL(writer);
            delimiter = " ";
        }
    }
}