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

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.core.select.SelectExprEventTypeRegistry;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorFactory;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.named.NamedWindowOnMergeHelper;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.util.UuidGenerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPPreparedExecuteIUDInsertInto extends EPPreparedExecuteIUDSingleStream {
    public EPPreparedExecuteIUDInsertInto(StatementSpecCompiled statementSpec, EPServicesContext services, StatementContext statementContext) throws ExprValidationException {
        super(associatedFromClause(statementSpec), services, statementContext);
    }

    public EPPreparedExecuteIUDSingleStreamExec getExecutor(QueryGraph queryGraph, String aliasName) throws ExprValidationException {

        List<SelectClauseElementCompiled> selectNoWildcard = NamedWindowOnMergeHelper.compileSelectNoWildcard(UuidGenerator.generate(), Arrays.asList(statementSpec.getSelectClauseSpec().getSelectExprList()));

        StreamTypeService streamTypeService = new StreamTypeServiceImpl(statementContext.getEngineURI(), true);
        ExprEvaluatorContextStatement exprEvaluatorContextStatement = new ExprEvaluatorContextStatement(statementContext, true);

        // assign names
        ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, statementContext.getEngineImportService(),
                statementContext.getStatementExtensionServicesContext(), null, statementContext.getTimeProvider(), statementContext.getVariableService(), statementContext.getTableService(), exprEvaluatorContextStatement,
                statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, false);

        // determine whether column names are provided
        // if the "values" keyword was used, allow sequential automatic name assignment
        String[] assignedSequentialNames = null;
        if (statementSpec.getInsertIntoDesc().getColumnNames().isEmpty()) {
            FireAndForgetSpecInsert insert = (FireAndForgetSpecInsert) statementSpec.getFireAndForgetSpec();
            if (insert.isUseValuesKeyword()) {
                assignedSequentialNames = processor.getEventTypePublic().getPropertyNames();
            }
        }

        int count = -1;
        for (SelectClauseElementCompiled compiled : statementSpec.getSelectClauseSpec().getSelectExprList()) {
            count++;
            if (compiled instanceof SelectClauseExprCompiledSpec) {
                SelectClauseExprCompiledSpec expr = (SelectClauseExprCompiledSpec) compiled;
                ExprNode validatedExpression = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, expr.getSelectExpression(), validationContext);
                expr.setSelectExpression(validatedExpression);
                if (expr.getAssignedName() == null) {
                    if (expr.getProvidedName() == null) {
                        if (assignedSequentialNames != null && count < assignedSequentialNames.length) {
                            expr.setAssignedName(assignedSequentialNames[count]);
                        } else {
                            expr.setAssignedName(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(expr.getSelectExpression()));
                        }
                    } else {
                        expr.setAssignedName(expr.getProvidedName());
                    }
                }
            }
        }

        EventType optionalInsertIntoEventType = processor.getEventTypeResultSetProcessor();
        SelectExprEventTypeRegistry selectExprEventTypeRegistry = new SelectExprEventTypeRegistry(statementContext.getStatementName(), statementContext.getStatementEventTypeRef());
        SelectExprProcessorForge insertHelperForge = SelectExprProcessorFactory.getProcessor(Collections.singleton(0),
                selectNoWildcard.toArray(new SelectClauseElementCompiled[selectNoWildcard.size()]), false, statementSpec.getInsertIntoDesc(), optionalInsertIntoEventType, null, streamTypeService,
                statementContext.getEventAdapterService(), statementContext.getStatementResultService(), statementContext.getValueAddEventService(), selectExprEventTypeRegistry,
                statementContext.getEngineImportService(), exprEvaluatorContextStatement, statementContext.getVariableService(), statementContext.getTableService(), statementContext.getTimeProvider(), statementContext.getEngineURI(), statementContext.getStatementId(), statementContext.getStatementName(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), statementContext.getConfigSnapshot(), null, statementContext.getNamedWindowMgmtService(), null, null,
                statementContext.getStatementExtensionServicesContext());
        SelectExprProcessor insertHelper = insertHelperForge.getSelectExprProcessor(statementContext.getEngineImportService(), true, statementContext.getStatementName());

        return new EPPreparedExecuteIUDSingleStreamExecInsert(exprEvaluatorContextStatement, insertHelper, statementSpec.getTableNodes(), services);
    }

    private static StatementSpecCompiled associatedFromClause(StatementSpecCompiled statementSpec) throws ExprValidationException {
        if (statementSpec.getFilterRootNode() != null ||
                statementSpec.getStreamSpecs().length > 0 ||
                statementSpec.getHavingExprRootNode() != null ||
                statementSpec.getOutputLimitSpec() != null ||
                statementSpec.getForClauseSpec() != null ||
                statementSpec.getMatchRecognizeSpec() != null ||
                statementSpec.getOrderByList().length > 0 ||
                statementSpec.getRowLimitSpec() != null) {
            throw new ExprValidationException("Insert-into fire-and-forget query can only consist of an insert-into clause and a select-clause");
        }

        String namedWindowName = statementSpec.getInsertIntoDesc().getEventTypeName();
        NamedWindowConsumerStreamSpec namedWindowStream = new NamedWindowConsumerStreamSpec(namedWindowName, null, new ViewSpec[0], Collections.<ExprNode>emptyList(),
                StreamSpecOptions.DEFAULT, null);
        statementSpec.setStreamSpecs(new StreamSpecCompiled[]{namedWindowStream});
        return statementSpec;
    }
}
