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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;

import java.util.List;

/**
 * Specification for use of an existing table.
 */
public class TableQueryStreamSpec extends StreamSpecBase implements StreamSpecCompiled {
    private final TableMetaData table;
    private List<ExprNode> filterExpressions;

    public TableQueryStreamSpec(String optionalStreamName, ViewSpec[] viewSpecs, StreamSpecOptions streamSpecOptions, TableMetaData table, List<ExprNode> filterExpressions) {
        super(optionalStreamName, viewSpecs, streamSpecOptions);
        this.table = table;
        this.filterExpressions = filterExpressions;
    }

    public TableMetaData getTable() {
        return table;
    }

    public List<ExprNode> getFilterExpressions() {
        return filterExpressions;
    }
}
