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
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryCreateIndex;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryCreateIndexResult;
import com.espertech.esper.core.context.mgr.ContextManagedStatementCreateIndexDesc;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextMergeView;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.lookup.AdvancedIndexDesc;
import com.espertech.esper.epl.lookup.EventTableIndexUtil;
import com.espertech.esper.epl.lookup.IndexMultiKey;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.spec.CreateIndexDesc;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.util.DestroyCallback;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.ViewProcessingException;
import com.espertech.esper.view.Viewable;
import com.espertech.esper.view.ViewableDefaultImpl;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodCreateIndex extends EPStatementStartMethodBase {
    public EPStatementStartMethodCreateIndex(StatementSpecCompiled statementSpec) {
        super(statementSpec);
    }

    public EPStatementStartResult startInternal(EPServicesContext services, final StatementContext statementContext, boolean isNewStatement, boolean isRecoveringStatement, boolean isRecoveringResilient) throws ExprValidationException, ViewProcessingException {
        CreateIndexDesc spec = statementSpec.getCreateIndexDesc();
        final NamedWindowProcessor namedWindowProcessor = services.getNamedWindowMgmtService().getProcessor(spec.getWindowName());
        final TableMetadata tableMetadata = services.getTableService().getTableMetadata(spec.getWindowName());
        if (namedWindowProcessor == null && tableMetadata == null) {
            throw new ExprValidationException("A named window or table by name '" + spec.getWindowName() + "' does not exist");
        }
        EventType indexedEventType = namedWindowProcessor != null ? namedWindowProcessor.getNamedWindowType() : tableMetadata.getInternalEventType();
        String infraContextName = namedWindowProcessor != null ? namedWindowProcessor.getContextName() : tableMetadata.getContextName();
        EPLValidationUtil.validateContextName(namedWindowProcessor == null, spec.getWindowName(), infraContextName, statementSpec.getOptionalContextName(), true);

        // validate index
        QueryPlanIndexItem explicitIndexDesc = EventTableIndexUtil.validateCompileExplicitIndex(spec.getIndexName(), spec.isUnique(), spec.getColumns(), indexedEventType, statementContext);
        AdvancedIndexDesc advancedIndexDesc = explicitIndexDesc.getAdvancedIndexProvisionDesc() == null ? null : explicitIndexDesc.getAdvancedIndexProvisionDesc().getIndexDesc();
        final IndexMultiKey imk = new IndexMultiKey(spec.isUnique(), explicitIndexDesc.getHashPropsAsList(), explicitIndexDesc.getBtreePropsAsList(), advancedIndexDesc);

        // for tables we add the index to metadata
        if (tableMetadata != null) {
            services.getTableService().validateAddIndex(statementContext.getStatementName(), tableMetadata, spec.getIndexName(), explicitIndexDesc, imk);
        } else {
            namedWindowProcessor.validateAddIndex(statementContext.getStatementName(), spec.getIndexName(), explicitIndexDesc, imk);
        }

        // allocate context factory
        Viewable viewable = new ViewableDefaultImpl(indexedEventType);
        StatementAgentInstanceFactoryCreateIndex contextFactory = new StatementAgentInstanceFactoryCreateIndex(services, spec, viewable, namedWindowProcessor, tableMetadata == null ? null : tableMetadata.getTableName(), statementSpec.getOptionalContextName(), explicitIndexDesc);
        statementContext.setStatementAgentInstanceFactory(contextFactory);

        // provide destroy method which de-registers interest in this index
        final TableService finalTableService = services.getTableService();
        final String finalStatementName = statementContext.getStatementName();
        EPStatementDestroyCallbackList destroyMethod = new EPStatementDestroyCallbackList();
        if (tableMetadata != null) {
            destroyMethod.addCallback(new DestroyCallback() {
                public void destroy() {
                    finalTableService.removeIndexReferencesStmtMayRemoveIndex(finalStatementName, tableMetadata);
                }
            });
        } else {
            destroyMethod.addCallback(new DestroyCallback() {
                public void destroy() {
                    namedWindowProcessor.removeIndexReferencesStmtMayRemoveIndex(imk, finalStatementName);
                }
            });
        }

        EPStatementStopMethod stopMethod;
        if (statementSpec.getOptionalContextName() != null) {
            ContextMergeView mergeView = new ContextMergeView(indexedEventType);
            ContextManagedStatementCreateIndexDesc statement = new ContextManagedStatementCreateIndexDesc(statementSpec, statementContext, mergeView, contextFactory);
            services.getContextManagementService().addStatement(statementSpec.getOptionalContextName(), statement, isRecoveringResilient);
            stopMethod = new EPStatementStopMethod() {
                public void stop() {
                }
            };

            final ContextManagementService contextManagementService = services.getContextManagementService();
            destroyMethod.addCallback(new DestroyCallback() {
                public void destroy() {
                    contextManagementService.destroyedStatement(statementSpec.getOptionalContextName(), statementContext.getStatementName(), statementContext.getStatementId());
                }
            });
        } else {
            AgentInstanceContext defaultAgentInstanceContext = getDefaultAgentInstanceContext(statementContext);
            StatementAgentInstanceFactoryCreateIndexResult result;
            try {
                result = contextFactory.newContext(defaultAgentInstanceContext, isRecoveringResilient);
            } catch (EPException ex) {
                if (ex.getCause() instanceof ExprValidationException) {
                    throw (ExprValidationException) ex.getCause();
                }
                destroyMethod.destroy();
                throw ex;
            } catch (Throwable t) {
                destroyMethod.destroy();
                throw t;
            }
            final StopCallback stopCallback = services.getEpStatementFactory().makeStopMethod(result);
            stopMethod = new EPStatementStopMethod() {
                public void stop() {
                    stopCallback.stop();
                }
            };

            if (statementContext.getStatementExtensionServicesContext() != null && statementContext.getStatementExtensionServicesContext().getStmtResources() != null) {
                StatementResourceHolder holder = statementContext.getStatementExtensionServicesContext().extractStatementResourceHolder(result);
                statementContext.getStatementExtensionServicesContext().getStmtResources().setUnpartitioned(holder);
                statementContext.getStatementExtensionServicesContext().postProcessStart(result, isRecoveringResilient);
            }
        }

        if (tableMetadata != null) {
            services.getStatementVariableRefService().addReferences(statementContext.getStatementName(), tableMetadata.getTableName());
        } else {
            services.getStatementVariableRefService().addReferences(statementContext.getStatementName(), namedWindowProcessor.getNamedWindowType().getName());
        }

        return new EPStatementStartResult(viewable, stopMethod, destroyMethod);
    }
}
