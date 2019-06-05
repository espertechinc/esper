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
import com.espertech.esper.common.internal.type.ClassIdentifierWArray;

import java.util.List;

public class CreateTableColumn {
    private final String columnName;
    private final ExprNode optExpression;
    private final ClassIdentifierWArray optType;
    private final List<AnnotationDesc> annotations;
    private final Boolean primaryKey;

    public CreateTableColumn(String columnName, ExprNode optExpression, ClassIdentifierWArray optType, List<AnnotationDesc> annotations, Boolean primaryKey) {
        this.columnName = columnName;
        this.optExpression = optExpression;
        this.optType = optType;
        this.annotations = annotations;
        this.primaryKey = primaryKey;
    }

    public String getColumnName() {
        return columnName;
    }

    public ExprNode getOptExpression() {
        return optExpression;
    }

    public ClassIdentifierWArray getOptType() {
        return optType;
    }

    public List<AnnotationDesc> getAnnotations() {
        return annotations;
    }

    public Boolean getPrimaryKey() {
        return primaryKey;
    }
}
