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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.List;

/**
 * Specification for use of an existing table.
 */
public class TableQueryStreamSpec extends StreamSpecBase implements StreamSpecCompiled {
    private static final long serialVersionUID = 3585037637891782659L;

    private final String tableName;
    private List<ExprNode> filterExpressions;

    public TableQueryStreamSpec(String optionalStreamName, ViewSpec[] viewSpecs, StreamSpecOptions streamSpecOptions, String tableName, List<ExprNode> filterExpressions) {
        super(optionalStreamName, viewSpecs, streamSpecOptions);
        this.tableName = tableName;
        this.filterExpressions = filterExpressions;
    }

    public String getTableName() {
        return tableName;
    }

    public List<ExprNode> getFilterExpressions() {
        return filterExpressions;
    }
}
