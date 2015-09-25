/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.core.start;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryCreateVariable;
import com.espertech.esper.core.context.mgr.ContextManagedStatementCreateVariableDesc;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.context.mgr.ContextPropertyRegistryImpl;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextMergeView;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.*;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.CreateVariableDesc;
import com.espertech.esper.epl.spec.SelectClauseElementWildcard;
import com.espertech.esper.epl.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.epl.spec.StatementSpecCompiled;
import com.espertech.esper.epl.variable.*;
import com.espertech.esper.epl.view.OutputProcessViewBase;
import com.espertech.esper.epl.view.OutputProcessViewFactory;
import com.espertech.esper.epl.view.OutputProcessViewFactoryFactory;
import com.espertech.esper.util.DestroyCallback;
import com.espertech.esper.view.StatementStopCallback;
import com.espertech.esper.view.ViewProcessingException;
import com.espertech.esper.view.Viewable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodCreateVariable extends EPStatementStartMethodBase
{
    private static final Log log = LogFactory.getLog(EPStatementStartMethodCreateVariable.class);

    public EPStatementStartMethodCreateVariable(StatementSpecCompiled statementSpec) {
        super(statementSpec);
    }

    public EPStatementStartResult startInternal(final EPServicesContext services, final StatementContext statementContext, boolean isNewStatement, boolean isRecoveringStatement, boolean isRecoveringResilient) throws ExprValidationException, ViewProcessingException {
        final CreateVariableDesc createDesc = statementSpec.getCreateVariableDesc();

        VariableServiceUtil.checkAlreadyDeclaredTable(createDesc.getVariableName(), services.getTableService());

        // Get assignment value
        Object value = null;
        if (createDesc.getAssignment() != null)
        {
            // Evaluate assignment expression
            StreamTypeService typeService = new StreamTypeServiceImpl(new EventType[0], new String[0], new boolean[0], services.getEngineURI(), false);
            ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
            ExprValidationContext validationContext = new ExprValidationContext(typeService, statementContext.getMethodResolutionService(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);
            ExprNode validated = ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.VARIABLEASSIGN, createDesc.getAssignment(), validationContext);
            value = validated.getExprEvaluator().evaluate(null, true, evaluatorContextStmt);
        }

        // Create variable
        try {
            services.getVariableService().createNewVariable(statementSpec.getOptionalContextName(), createDesc.getVariableName(), createDesc.getVariableType(), createDesc.isConstant(), createDesc.isArray(), createDesc.isArrayOfPrimitive(), value, services.getEngineImportService());
        }
        catch (VariableExistsException ex) {
            // for new statement we don't allow creating the same variable
            if (isNewStatement) {
                throw new ExprValidationException("Cannot create variable: " + ex.getMessage(), ex);
            }
        }
        catch (VariableDeclarationException ex) {
            throw new ExprValidationException("Cannot create variable: " + ex.getMessage(), ex);
        }

        EPStatementDestroyCallbackList destroyMethod = new EPStatementDestroyCallbackList();
        destroyMethod.addCallback(new DestroyCallback() {
            public void destroy() {
                try {
                    services.getStatementVariableRefService().removeReferencesStatement(statementContext.getStatementName());
                }
                catch (RuntimeException ex) {
                    log.error("Error removing variable '" + createDesc.getVariableName() + "': " + ex.getMessage());
                }
            }
        });

        EPStatementStopMethod stopMethod = new EPStatementStopMethod(){
            public void stop()
            {
            }
        };

        VariableMetaData variableMetaData = services.getVariableService().getVariableMetaData(createDesc.getVariableName());
        Viewable outputView;
        EventType eventType = CreateVariableView.getEventType(statementContext.getStatementId(), services.getEventAdapterService(), variableMetaData);
        StatementAgentInstanceFactoryCreateVariable contextFactory = new StatementAgentInstanceFactoryCreateVariable(statementContext, services, variableMetaData, eventType);
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
        }
        else {
            // allocate
            services.getVariableService().allocateVariableState(createDesc.getVariableName(), VariableService.NOCONTEXT_AGENTINSTANCEID, statementContext.getStatementExtensionServicesContext());
            final CreateVariableView createView = new CreateVariableView(statementContext.getStatementId(), services.getEventAdapterService(), services.getVariableService(), createDesc.getVariableName(), statementContext.getStatementResultService());

            services.getVariableService().registerCallback(createDesc.getVariableName(), 0, createView);
            statementContext.getStatementStopService().addSubscriber(new StatementStopCallback() {
                public void statementStopped()
                {
                    services.getVariableService().unregisterCallback(createDesc.getVariableName(), 0, createView);
                }
            });

            // Create result set processor, use wildcard selection
            statementSpec.getSelectClauseSpec().setSelectExprList(new SelectClauseElementWildcard());
            statementSpec.setSelectStreamDirEnum(SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH);
            StreamTypeService typeService = new StreamTypeServiceImpl(new EventType[] {createView.getEventType()}, new String[] {"create_variable"}, new boolean[] {true}, services.getEngineURI(), false);
            AgentInstanceContext agentInstanceContext = getDefaultAgentInstanceContext(statementContext);
            ResultSetProcessorFactoryDesc resultSetProcessorPrototype = ResultSetProcessorFactoryFactory.getProcessorPrototype(
                    statementSpec, statementContext, typeService, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, services.getConfigSnapshot());
            ResultSetProcessor resultSetProcessor = EPStatementStartMethodHelperAssignExpr.getAssignResultSetProcessor(agentInstanceContext, resultSetProcessorPrototype);

            // Attach output view
            OutputProcessViewFactory outputViewFactory = OutputProcessViewFactoryFactory.make(statementSpec, services.getInternalEventRouter(), agentInstanceContext.getStatementContext(), resultSetProcessor.getResultEventType(), null, services.getTableService(), resultSetProcessorPrototype.getResultSetProcessorFactory().getResultSetProcessorType());
            OutputProcessViewBase outputViewBase = outputViewFactory.makeView(resultSetProcessor, agentInstanceContext);
            createView.addView(outputViewBase);
            outputView = outputViewBase;
        }

        services.getStatementVariableRefService().addReferences(statementContext.getStatementName(), Collections.singleton(createDesc.getVariableName()), null);
        return new EPStatementStartResult(outputView, stopMethod, destroyMethod);
    }
}
