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
 * Dot-expresson item representing an array operation.
 */
public class DotExpressionItemArray extends DotExpressionItem implements Serializable {
    private List<Expression> indexes;

    /**
     * Ctor.
     */
    public DotExpressionItemArray() {
    }

    /**
     * Ctor.
     * @param indexes array index expressions
     */
    public DotExpressionItemArray(List<Expression> indexes) {
        this.indexes = indexes;
    }

    /**
     * Returns array index expressions
     * @return array index expressions
     */
    public List<Expression> getIndexes() {
        return indexes;
    }

    /**
     * Sets array index expressions
     * @param  indexes array index expressions
     */
    public void setIndexes(List<Expression> indexes) {
        this.indexes = indexes;
    }

    public void renderItem(StringWriter writer) {
        writer.append('[');
        String delimiter = "";
        for (Expression index : indexes) {
            writer.write(delimiter);
            delimiter = ",";
            index.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        writer.append(']');
    }
}
