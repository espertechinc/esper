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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceIsolationException;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.core.service.resource.StatementResourceService;
import com.espertech.esper.epl.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.epl.specmapper.StatementSpecMapper;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.filter.FilterSet;
import com.espertech.esper.schedule.ScheduleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementation for the admin interface.
 */
public class EPAdministratorIsolatedImpl implements EPAdministratorIsolatedSPI {
    private final static Logger log = LoggerFactory.getLogger(EPAdministratorIsolatedImpl.class);

    private final String isolatedServiceName;
    private final EPIsolationUnitServices services;
    private final EPServicesContext unisolatedServices;
    private final EPRuntimeIsolatedSPI isolatedRuntime;
    private final Set<String> statementNames = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Ctor.
     *
     * @param isolatedServiceName name of the isolated service
     * @param services            isolated services
     * @param unisolatedServices  engine services
     * @param isolatedRuntime     the runtime for this isolated service
     */
    public EPAdministratorIsolatedImpl(String isolatedServiceName, EPIsolationUnitServices services, EPServicesContext unisolatedServices, EPRuntimeIsolatedSPI isolatedRuntime) {
        this.isolatedServiceName = isolatedServiceName;
        this.services = services;
        this.unisolatedServices = unisolatedServices;
        this.isolatedRuntime = isolatedRuntime;
    }

    public EPStatement createEPL(String eplStatement, String statementName, Object userObject) throws EPException {
        return createEPLStatementId(eplStatement, statementName, userObject, null);
    }

    public EPStatement createEPLStatementId(String eplStatement, String statementName, Object userObject, Integer optionalStatementId) throws EPException {
        SelectClauseStreamSelectorEnum defaultStreamSelector = StatementSpecMapper.mapFromSODA(unisolatedServices.getConfigSnapshot().getEngineDefaults().getStreamSelection().getDefaultStreamSelector());
        StatementSpecRaw statementSpec = EPAdministratorHelper.compileEPL(eplStatement, eplStatement, true, statementName, unisolatedServices, defaultStreamSelector);
        EPStatement statement = unisolatedServices.getStatementLifecycleSvc().createAndStart(statementSpec, eplStatement, false, statementName, userObject, services, optionalStatementId, null);
        EPStatementSPI stmtSpi = (EPStatementSPI) statement;
        stmtSpi.getStatementContext().setInternalEventEngineRouteDest(isolatedRuntime);
        stmtSpi.setServiceIsolated(isolatedServiceName);
        statementNames.add(stmtSpi.getName());
        return statement;
    }

    public String[] getStatementNames() {
        return statementNames.toArray(new String[statementNames.size()]);
    }

    public void addStatement(String name) {
        statementNames.add(name);   // for recovery
    }

    public void addStatement(EPStatement stmt) {

        addStatement(new EPStatement[]{stmt});
    }

    public void addStatement(EPStatement[] stmt) {

        unisolatedServices.getEventProcessingRWLock().acquireWriteLock();

        try {
            long fromTime = unisolatedServices.getSchedulingService().getTime();
            long toTime = services.getSchedulingService().getTime();
            long delta = toTime - fromTime;

            // perform checking
            Set<Integer> statementIds = new HashSet<Integer>();
            for (EPStatement aStmt : stmt) {
                if (aStmt == null) {
                    throw new EPServiceIsolationException("Illegal argument, a null value was provided in the statement list");
                }
                EPStatementSPI stmtSpi = (EPStatementSPI) aStmt;
                statementIds.add(stmtSpi.getStatementId());

                if (aStmt.getServiceIsolated() != null) {
                    throw new EPServiceIsolationException("Statement named '" + aStmt.getName() + "' already in service isolation under '" + stmtSpi.getServiceIsolated() + "'");
                }
            }

            // start txn
            unisolatedServices.getStatementIsolationService().beginIsolatingStatements(isolatedServiceName, services.getUnitId(), stmt);

            FilterSet filters = unisolatedServices.getFilterService().take(statementIds);
            ScheduleSet schedules = unisolatedServices.getSchedulingService().take(statementIds);

            services.getFilterService().apply(filters);
            services.getSchedulingService().apply(schedules);

            for (EPStatement aStmt : stmt) {
                EPStatementSPI stmtSpi = (EPStatementSPI) aStmt;
                stmtSpi.getStatementContext().setFilterService(services.getFilterService());
                stmtSpi.getStatementContext().setSchedulingService(services.getSchedulingService());
                stmtSpi.getStatementContext().setInternalEventEngineRouteDest(isolatedRuntime);
                stmtSpi.getStatementContext().getScheduleAdjustmentService().adjust(delta);
                statementNames.add(stmtSpi.getName());
                stmtSpi.setServiceIsolated(isolatedServiceName);

                applyFilterVersion(stmtSpi, services.getFilterService().getFiltersVersion());
            }

            // commit txn
            unisolatedServices.getStatementIsolationService().commitIsolatingStatements(isolatedServiceName, services.getUnitId(), stmt);
        } catch (EPServiceIsolationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            unisolatedServices.getStatementIsolationService().rollbackIsolatingStatements(isolatedServiceName, services.getUnitId(), stmt);

            String message = "Unexpected exception taking statements: " + ex.getMessage();
            log.error(message, ex);
            throw new EPException(message, ex);
        } finally {
            unisolatedServices.getEventProcessingRWLock().releaseWriteLock();
        }
    }

    public void removeStatement(EPStatement stmt) {
        removeStatement(new EPStatement[]{stmt});
    }

    public void removeStatement(EPStatement[] stmt) {

        unisolatedServices.getEventProcessingRWLock().acquireWriteLock();

        try {
            long fromTime = services.getSchedulingService().getTime();
            long toTime = unisolatedServices.getSchedulingService().getTime();
            long delta = toTime - fromTime;

            Set<Integer> statementIds = new HashSet<Integer>();
            for (EPStatement aStmt : stmt) {
                if (aStmt == null) {
                    throw new EPServiceIsolationException("Illegal argument, a null value was provided in the statement list");
                }

                EPStatementSPI stmtSpi = (EPStatementSPI) aStmt;
                statementIds.add(stmtSpi.getStatementId());

                if (aStmt.getServiceIsolated() == null) {
                    throw new EPServiceIsolationException("Statement named '" + aStmt.getName() + "' is not currently in service isolation");
                }
                if (!aStmt.getServiceIsolated().equals(isolatedServiceName)) {
                    throw new EPServiceIsolationException("Statement named '" + aStmt.getName() + "' not in this service isolation but under service isolation '" + aStmt.getName() + "'");
                }
            }

            // start txn
            unisolatedServices.getStatementIsolationService().beginUnisolatingStatements(isolatedServiceName, services.getUnitId(), stmt);

            FilterSet filters = services.getFilterService().take(statementIds);
            ScheduleSet schedules = services.getSchedulingService().take(statementIds);

            unisolatedServices.getFilterService().apply(filters);
            unisolatedServices.getSchedulingService().apply(schedules);

            for (EPStatement aStmt : stmt) {
                EPStatementSPI stmtSpi = (EPStatementSPI) aStmt;
                stmtSpi.getStatementContext().setFilterService(unisolatedServices.getFilterService());
                stmtSpi.getStatementContext().setSchedulingService(unisolatedServices.getSchedulingService());
                stmtSpi.getStatementContext().setInternalEventEngineRouteDest(unisolatedServices.getInternalEventEngineRouteDest());
                stmtSpi.getStatementContext().getScheduleAdjustmentService().adjust(delta);
                statementNames.remove(stmtSpi.getName());
                stmtSpi.setServiceIsolated(null);

                applyFilterVersion(stmtSpi, unisolatedServices.getFilterService().getFiltersVersion());
            }

            // commit txn
            unisolatedServices.getStatementIsolationService().commitUnisolatingStatements(isolatedServiceName, services.getUnitId(), stmt);
        } catch (EPServiceIsolationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            unisolatedServices.getStatementIsolationService().rollbackUnisolatingStatements(isolatedServiceName, services.getUnitId(), stmt);

            String message = "Unexpected exception taking statements: " + ex.getMessage();
            log.error(message, ex);
            throw new EPException(message, ex);
        } finally {
            unisolatedServices.getEventProcessingRWLock().releaseWriteLock();
        }
    }

    /**
     * Remove all statements from isolated services, such as upon destroy.
     */
    public void removeAllStatements() {
        List<EPStatement> statements = new ArrayList<EPStatement>();
        for (String stmtName : statementNames) {
            EPStatement stmt = unisolatedServices.getStatementLifecycleSvc().getStatementByName(stmtName);
            if (stmt == null) {
                log.debug("Statement '" + stmtName + "', the statement could not be found");
                continue;
            }

            if (stmt.getServiceIsolated() != null && (!stmt.getServiceIsolated().equals(isolatedServiceName))) {
                log.error("Error returning statement '" + stmtName + "', the internal isolation information is incorrect, isolated service for statement is currently '" +
                        stmt.getServiceIsolated() + "' and mismatches this isolated services named '" + isolatedServiceName + "'");
                continue;
            }

            statements.add(stmt);
        }

        removeStatement(statements.toArray(new EPStatement[statements.size()]));
    }

    private void applyFilterVersion(EPStatementSPI stmtSpi, long filtersVersion) {
        StatementResourceService resources = stmtSpi.getStatementContext().getStatementExtensionServicesContext().getStmtResources();
        if (resources.getUnpartitioned() != null) {
            applyFilterVersion(resources.getUnpartitioned(), filtersVersion);
        } else {
            for (Map.Entry<Integer, StatementResourceHolder> entry : resources.getResourcesPartitioned().entrySet()) {
                applyFilterVersion(entry.getValue(), filtersVersion);
            }
        }
    }

    private void applyFilterVersion(StatementResourceHolder holder, long filtersVersion) {
        holder.getAgentInstanceContext().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }
}
