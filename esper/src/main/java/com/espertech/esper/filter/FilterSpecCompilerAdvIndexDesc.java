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
package com.espertech.esper.filter;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.lookup.AdvancedIndexConfigContextPartition;

public class FilterSpecCompilerAdvIndexDesc {
    private final ExprNode[] indexExpressions;
    private final ExprNode[] keyExpressions;
    private final AdvancedIndexConfigContextPartition indexSpec;
    private final String indexType;
    private final String indexName;

    public FilterSpecCompilerAdvIndexDesc(ExprNode[] indexExpressions, ExprNode[] keyExpressions, AdvancedIndexConfigContextPartition indexSpec, String indexType, String indexName) {
        this.indexExpressions = indexExpressions;
        this.keyExpressions = keyExpressions;
        this.indexSpec = indexSpec;
        this.indexType = indexType;
        this.indexName = indexName;
    }

    public ExprNode[] getIndexExpressions() {
        return indexExpressions;
    }

    public ExprNode[] getKeyExpressions() {
        return keyExpressions;
    }

    public AdvancedIndexConfigContextPartition getIndexSpec() {
        return indexSpec;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getIndexType() {
        return indexType;
    }
}
