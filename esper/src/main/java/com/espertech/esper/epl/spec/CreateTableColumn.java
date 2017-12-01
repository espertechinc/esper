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

import java.io.Serializable;
import java.util.List;

public class CreateTableColumn implements Serializable {
    private static final long serialVersionUID = 5158525273388684702L;

    private final String columnName;
    private final ExprNode optExpression;
    private final String optTypeName;
    private final Boolean optTypeIsArray;
    private final Boolean optTypeIsPrimitiveArray;
    private final List<AnnotationDesc> annotations;
    private final Boolean primaryKey;

    public CreateTableColumn(String columnName, ExprNode optExpression, String optTypeName, Boolean optTypeIsArray, Boolean optTypeIsPrimitiveArray, List<AnnotationDesc> annotations, Boolean primaryKey) {
        this.columnName = columnName;
        this.optExpression = optExpression;
        this.optTypeName = optTypeName;
        this.optTypeIsArray = optTypeIsArray;
        this.optTypeIsPrimitiveArray = optTypeIsPrimitiveArray;
        this.annotations = annotations;
        this.primaryKey = primaryKey;
    }

    public String getColumnName() {
        return columnName;
    }

    public ExprNode getOptExpression() {
        return optExpression;
    }

    public String getOptTypeName() {
        return optTypeName;
    }

    public Boolean getOptTypeIsArray() {
        return optTypeIsArray;
    }

    public List<AnnotationDesc> getAnnotations() {
        return annotations;
    }

    public Boolean getPrimaryKey() {
        return primaryKey;
    }

    public Boolean getOptTypeIsPrimitiveArray() {
        return optTypeIsPrimitiveArray;
    }
}
