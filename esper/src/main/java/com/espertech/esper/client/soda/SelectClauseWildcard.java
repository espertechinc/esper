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
 * Represents a wildcard in the select-clause.
 */
public class SelectClauseWildcard implements SelectClauseElement {
    private static final long serialVersionUID = 7047821688887542393L;

    /**
     * Ctor.
     */
    public SelectClauseWildcard() {
    }

    /**
     * Renders the element in textual representation.
     *
     * @param writer to output to
     */
    public void toEPLElement(StringWriter writer) {
        writer.write("*");
    }
}
