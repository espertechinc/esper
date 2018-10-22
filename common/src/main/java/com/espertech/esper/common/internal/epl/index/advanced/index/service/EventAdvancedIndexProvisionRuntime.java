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
package com.espertech.esper.common.internal.epl.index.advanced.index.service;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexDescWExpr;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexConfigStatement;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexFactory;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;

public class EventAdvancedIndexProvisionRuntime {
    private String[] indexExpressionTexts;
    private String[] indexProperties;
    private ExprNode[] indexExpressionsOpt;
    private boolean indexExpressionsAllProps;
    private EventAdvancedIndexFactory factory;
    private String[] parameterExpressionTexts;
    private ExprNode[] parameterExpressionsOpt;
    private ExprEvaluator[] parameterEvaluators;
    private EventAdvancedIndexConfigStatement configStatement;
    private String indexTypeName;

    public EventAdvancedIndexProvisionRuntime() {
    }

    public EventAdvancedIndexProvisionCompileTime toCompileTime(EventType eventTypeIndexed, StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
            throws ExprValidationException {
        ExprNode[] indexedExpr;
        if (indexExpressionsOpt != null) {
            indexedExpr = indexExpressionsOpt;
        } else {
            if (indexExpressionsAllProps) {
                indexedExpr = new ExprNode[indexProperties.length];
                for (int i = 0; i < indexProperties.length; i++) {
                    indexedExpr[i] = new ExprIdentNodeImpl(eventTypeIndexed, indexProperties[i], 0);
                }
            } else {
                indexedExpr = new ExprNode[indexProperties.length];
                for (int i = 0; i < indexProperties.length; i++) {
                    indexedExpr[i] = services.getCompilerServices().compileExpression(indexExpressionTexts[i], services);
                    indexedExpr[i] = EPLValidationUtil.validateSimpleGetSubtree(ExprNodeOrigin.CREATEINDEXCOLUMN, indexedExpr[i], eventTypeIndexed, false, statementRawInfo, services);
                }
            }
        }
        AdvancedIndexDescWExpr desc = new AdvancedIndexDescWExpr(indexTypeName, indexedExpr);

        ExprNode[] parameters;
        if (parameterExpressionsOpt != null) {
            parameters = parameterExpressionsOpt;
        } else {
            parameters = new ExprNode[parameterExpressionTexts.length];
            for (int i = 0; i < parameterExpressionTexts.length; i++) {
                parameters[i] = services.getCompilerServices().compileExpression(parameterExpressionTexts[i], services);
            }
        }

        return new EventAdvancedIndexProvisionCompileTime(desc, parameters, factory.getForge(), factory.toConfigStatement(indexedExpr));
    }

    public String[] getIndexExpressionTexts() {
        return indexExpressionTexts;
    }

    public void setIndexExpressionTexts(String[] indexExpressionTexts) {
        this.indexExpressionTexts = indexExpressionTexts;
    }

    public String[] getIndexProperties() {
        return indexProperties;
    }

    public void setIndexProperties(String[] indexProperties) {
        this.indexProperties = indexProperties;
    }

    public void setFactory(EventAdvancedIndexFactory factory) {
        this.factory = factory;
    }

    public EventAdvancedIndexFactory getFactory() {
        return factory;
    }

    public ExprEvaluator[] getParameterEvaluators() {
        return parameterEvaluators;
    }

    public void setParameterEvaluators(ExprEvaluator[] parameterEvaluators) {
        this.parameterEvaluators = parameterEvaluators;
    }

    public String[] getParameterExpressionTexts() {
        return parameterExpressionTexts;
    }

    public void setParameterExpressionTexts(String[] parameterExpressionTexts) {
        this.parameterExpressionTexts = parameterExpressionTexts;
    }

    public String getIndexTypeName() {
        return indexTypeName;
    }

    public EventAdvancedIndexConfigStatement getConfigStatement() {
        return configStatement;
    }

    public void setConfigStatement(EventAdvancedIndexConfigStatement configStatement) {
        this.configStatement = configStatement;
    }

    public void setIndexTypeName(String indexTypeName) {
        this.indexTypeName = indexTypeName;
    }

    public ExprNode[] getIndexExpressionsOpt() {
        return indexExpressionsOpt;
    }

    public void setIndexExpressionsOpt(ExprNode[] indexExpressionsOpt) {
        this.indexExpressionsOpt = indexExpressionsOpt;
    }

    public ExprNode[] getParameterExpressionsOpt() {
        return parameterExpressionsOpt;
    }

    public void setParameterExpressionsOpt(ExprNode[] parameterExpressionsOpt) {
        this.parameterExpressionsOpt = parameterExpressionsOpt;
    }

    public boolean isIndexExpressionsAllProps() {
        return indexExpressionsAllProps;
    }

    public void setIndexExpressionsAllProps(boolean indexExpressionsAllProps) {
        this.indexExpressionsAllProps = indexExpressionsAllProps;
    }
}
