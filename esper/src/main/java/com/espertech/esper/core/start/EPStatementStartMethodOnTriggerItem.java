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
package com.espertech.esper.core.start;

import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.filterspec.PropertyEvaluator;

public class EPStatementStartMethodOnTriggerItem {
    private final ExprNode whereClause;
    private final boolean isNamedWindowInsert;
    private final String insertIntoTableNames;
    private final ResultSetProcessorFactoryDesc factoryDesc;
    private final PropertyEvaluator propertyEvaluator;

    public EPStatementStartMethodOnTriggerItem(ExprNode whereClause, boolean isNamedWindowInsert, String insertIntoTableNames, ResultSetProcessorFactoryDesc factoryDesc, PropertyEvaluator propertyEvaluator) {
        this.whereClause = whereClause;
        this.isNamedWindowInsert = isNamedWindowInsert;
        this.insertIntoTableNames = insertIntoTableNames;
        this.factoryDesc = factoryDesc;
        this.propertyEvaluator = propertyEvaluator;
    }

    public ExprNode getWhereClause() {
        return whereClause;
    }

    public boolean isNamedWindowInsert() {
        return isNamedWindowInsert;
    }

    public String getInsertIntoTableNames() {
        return insertIntoTableNames;
    }

    public ResultSetProcessorFactoryDesc getFactoryDesc() {
        return factoryDesc;
    }

    public PropertyEvaluator getPropertyEvaluator() {
        return propertyEvaluator;
    }
}
