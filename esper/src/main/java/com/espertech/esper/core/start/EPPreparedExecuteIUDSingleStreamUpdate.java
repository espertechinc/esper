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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.FireAndForgetSpecUpdate;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.spec.StatementSpecCompiled;
import com.espertech.esper.epl.table.upd.TableUpdateStrategy;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelperFactory;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.filter.FilterSpecCompiled;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPPreparedExecuteIUDSingleStreamUpdate extends EPPreparedExecuteIUDSingleStream {
    public EPPreparedExecuteIUDSingleStreamUpdate(StatementSpecCompiled statementSpec, EPServicesContext services, StatementContext statementContext) throws ExprValidationException {
        super(statementSpec, services, statementContext);
    }

    public EPPreparedExecuteIUDSingleStreamExec getExecutor(FilterSpecCompiled filter, String aliasName) {
        FireAndForgetSpecUpdate updateSpec = (FireAndForgetSpecUpdate) statementSpec.getFireAndForgetSpec();

        StreamTypeServiceImpl assignmentTypeService = new StreamTypeServiceImpl(
                new EventType[]{processor.getEventTypeResultSetProcessor(), null, processor.getEventTypeResultSetProcessor()},
                new String[]{aliasName, "", EPStatementStartMethodOnTrigger.INITIAL_VALUE_STREAM_NAME},
                new boolean[]{true, true, true}, services.getEngineURI(), true);
        assignmentTypeService.setStreamZeroUnambigous(true);
        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, true);
        ExprValidationContext validationContext = new ExprValidationContext(assignmentTypeService, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, false);

        // validate update expressions
        try {
            for (OnTriggerSetAssignment assignment : updateSpec.getAssignments()) {
                ExprNode validated = ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.UPDATEASSIGN, assignment.getExpression(), validationContext);
                assignment.setExpression(validated);
                EPStatementStartMethodHelperValidate.validateNoAggregations(validated, "Aggregation functions may not be used within an update-clause");
            }
        } catch (ExprValidationException e) {
            throw new EPException(e.getMessage(), e);
        }

        // make updater
        EventBeanUpdateHelper updateHelper;
        TableUpdateStrategy tableUpdateStrategy = null;
        try {

            boolean copyOnWrite = !(processor instanceof FireAndForgetProcessorTable);
            updateHelper = EventBeanUpdateHelperFactory.make(processor.getNamedWindowOrTableName(),
                    (EventTypeSPI) processor.getEventTypeResultSetProcessor(), updateSpec.getAssignments(), aliasName, null, copyOnWrite, statementContext.getStatementName(), services.getEngineURI(), services.getEventAdapterService());

            if (processor instanceof FireAndForgetProcessorTable) {
                FireAndForgetProcessorTable tableProcessor = (FireAndForgetProcessorTable) processor;
                tableUpdateStrategy = services.getTableService().getTableUpdateStrategy(tableProcessor.getTableMetadata(), updateHelper, false);
                copyOnWrite = false;
            }
        } catch (ExprValidationException e) {
            throw new EPException(e.getMessage(), e);
        }

        return new EPPreparedExecuteIUDSingleStreamExecUpdate(filter, statementSpec.getFilterRootNode(), statementSpec.getAnnotations(), updateHelper, tableUpdateStrategy, statementSpec.getTableNodes(), services);
    }
}
