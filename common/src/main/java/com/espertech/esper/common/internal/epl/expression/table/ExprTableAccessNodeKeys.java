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
package com.espertech.esper.common.internal.epl.expression.table;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyEnum;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;

import java.io.StringWriter;

public class ExprTableAccessNodeKeys extends ExprTableAccessNode {

    public ExprTableAccessNodeKeys(String tableName) {
        super(tableName);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toPrecedenceFreeEPLInternal(writer);
        writer.append(".keys()");
    }

    protected void validateBindingInternal(ExprValidationContext validationContext) throws ExprValidationException {
    }

    public ExprTableEvalStrategyFactoryForge getTableAccessFactoryForge() {
        ExprTableEvalStrategyFactoryForge forge = new ExprTableEvalStrategyFactoryForge(tableMeta, null);
        forge.setStrategyEnum(ExprTableEvalStrategyEnum.KEYS);
        return forge;
    }

    protected boolean equalsNodeInternal(ExprTableAccessNode other) {
        return true;
    }

    public Class getEvaluationType() {
        return Object[].class;
    }

    public ExprForge getForge() {
        return this;
    }

    protected String getInstrumentationQName() {
        return "ExprTableTop";
    }

    protected CodegenExpression[] getInstrumentationQParams() {
        return new CodegenExpression[0];
    }
}
