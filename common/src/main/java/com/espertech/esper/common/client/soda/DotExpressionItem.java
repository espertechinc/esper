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
package com.espertech.esper.common.client.soda;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;

/**
 * Dot-expresson item is for use in "root_expression.dot_expression".
 * Root-expressions can be name or call.
 * Dot-expressions can be name, call or array.
 * Name is an identifier without parameters.
 * Call is an identfier with zero or more parameters.
 * Array is an index expression.
 * <p>
 * Each item represent an individual chain item and may either be a name, or a call or an array.
 */
public abstract class DotExpressionItem implements Serializable {

    private static final long serialVersionUID = -1710117870920314996L;

    /**
     * Render to EPL.
     * @param writer    writer to output to
     */
    public abstract void renderItem(StringWriter writer);

    /**
     * Ctor.
     */
    public DotExpressionItem() {
    }

    /**
     * Render to EPL.
     * @param chain     chain to render
     * @param writer    writer to output to
     * @param prefixDot indicator whether to prefix with "."
     */
    protected static void render(List<DotExpressionItem> chain, StringWriter writer, boolean prefixDot) {
        String delimiterOuter = prefixDot ? "." : "";
        for (DotExpressionItem item : chain) {
            if (!(item instanceof DotExpressionItemArray)) {
                writer.write(delimiterOuter);
            }
            item.renderItem(writer);
            delimiterOuter = ".";
        }
    }
}
