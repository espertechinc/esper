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
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryCreateVariable;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryCreateVariableResult;
import com.espertech.esper.core.context.mgr.ContextManagedStatementCreateVariableDesc;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.context.util.ContextMergeView;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.CreateVariableDesc;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.variable.*;
import com.espertech.esper.util.DestroyCallback;
import com.espertech.esper.view.ViewProcessingException;
import com.espertech.esper.view.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodCreateVariable extends EPStatementStartMethodBase {
    private static final Logger log = LoggerFactory.getLogger(EPStatementStartMethodCreateVariable.class);

    public EPStatementStartMethodCreateVariable(StatementSpecCompiled statementSpec) {
        super(statementSpec);
    }

    public EPStatementStartResult startInternal(final EPServicesContext services, final StatementContext statementContext, boolean isNewStatement, boolean isRecoveringStatement, boolean isRecoveringResilient) throws ExprValidationException, ViewProcessingException {
        final CreateVariableDesc createDesc = statementSpec.getCreateVariableDesc();

        VariableServiceUtil.checkAlreadyDeclaredTable(createDesc.getVariableName(), services.getTableService());

        // Get assignment value
        Object value = null;
        if (createDesc.getAssignment() != null) {
            // Evaluate assignment expression
            StreamTypeService typeService = new StreamTypeServiceImpl(new EventType[0], new String[0], new boolean[0], services.getEngineURI(), false, false);
            ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
            ExprValidationContext validationContext = new ExprValidationContext(typeService, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);
            ExprNode validated = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.VARIABLEASSIGN, createDesc.getAssignment(), validationContext);
            value = validated.getForge().getExprEvaluator().evaluate(null, true, evaluatorContextStmt);
        }

        // Create variable
        try {
            services.getVariableService().createNewVariable(statementSpec.getOptionalContextName(), createDesc.getVariableName(), createDesc.getVariableType(), createDesc.isConstant(), createDesc.isArray(), createDesc.isArrayOfPrimitive(), value, services.getEngineImportService());
        } catch (VariableExistsException ex) {
            // for new statement we don't allow creating the same variable
            if (isNewStatement) {
                throw new ExprValidationException("Cannot create variable: " + ex.getMessage(), ex);
            }
        } catch (VariableDeclarationException ex) {
            throw new ExprValidationException("Cannot create variable: " + ex.getMessage(), ex);
        }

        EPStatementDestroyCallbackList destroyMethod = new EPStatementDestroyCallbackList();
        EPStatementStopMethod stopMethod = new EPStatementStopMethod() {
            public void stop() {
            }
        };

        VariableMetaData variableMetaData = services.getVariableService().getVariableMetaData(createDesc.getVariableName());
        Viewable outputView;
        EventType eventType = CreateVariableView.getEventType(statementContext.getStatementId(), services.getEventAdapterService(), variableMetaData);
        StatementAgentInstanceFactoryCreateVariable contextFactory = new StatementAgentInstanceFactoryCreateVariable(createDesc, statementSpec, statementContext, services, variableMetaData, eventType);
        statementContext.setStatementAgentInstanceFactory(contextFactory);

        if (statementSpec.getOptionalContextName() != null) {
            ContextMergeView mergeView = new ContextMergeView(eventType);
            outputView = mergeView;
            ContextManagedStatementCreateVariableDesc statement = new ContextManagedStatementCreateVariableDesc(statementSpec, statementContext, mergeView, contextFactory);
            services.getContextManagementService().addStatement(statementSpec.getOptionalContextName(), statement, isRecoveringResilient);

            final ContextManagementService contextManagementService = services.getContextManagementService();
            destroyMethod.addCallback(new DestroyCallback() {
                public void destroy() {
                    contextManagementService.destroyedStatement(statementSpec.getOptionalContextName(), statementContext.getStatementName(), statementContext.getStatementId());
                }
            });
        } else {
            StatementAgentInstanceFactoryCreateVariableResult resultOfStart = (StatementAgentInstanceFactoryCreateVariableResult) contextFactory.newContext(getDefaultAgentInstanceContext(statementContext), isRecoveringResilient);
            outputView = resultOfStart.getFinalView();

            if (statementContext.getStatementExtensionServicesContext() != null && statementContext.getStatementExtensionServicesContext().getStmtResources() != null) {
                StatementResourceHolder holder = statementContext.getStatementExtensionServicesContext().extractStatementResourceHolder(resultOfStart);
                statementContext.getStatementExtensionServicesContext().getStmtResources().setUnpartitioned(holder);
                statementContext.getStatementExtensionServicesContext().postProcessStart(resultOfStart, isRecoveringResilient);
            }
        }

        services.getStatementVariableRefService().addReferences(statementContext.getStatementName(), Collections.singleton(createDesc.getVariableName()), null);
        return new EPStatementStartResult(outputView, stopMethod, destroyMethod);
    }
}
