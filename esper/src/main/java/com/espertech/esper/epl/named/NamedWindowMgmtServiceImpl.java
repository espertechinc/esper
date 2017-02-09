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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.StatementAgentInstanceLock;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.lookup.IndexMultiKey;
import com.espertech.esper.epl.metric.MetricReportingService;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;
import com.espertech.esper.view.ViewProcessingException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This service hold for each named window a dedicated processor and a lock to the named window.
 * This lock is shrared between the named window and on-delete statements.
 */
public class NamedWindowMgmtServiceImpl implements NamedWindowMgmtService {
    private final Map<String, NamedWindowProcessor> processors;
    private final Map<String, NamedWindowLockPair> windowStatementLocks;
    private final Set<NamedWindowLifecycleObserver> observers;
    private final boolean enableQueryPlanLog;
    private final MetricReportingService metricReportingService;

    public NamedWindowMgmtServiceImpl(boolean enableQueryPlanLog,
                                      MetricReportingService metricReportingService) {
        this.processors = new HashMap<String, NamedWindowProcessor>();
        this.windowStatementLocks = new HashMap<String, NamedWindowLockPair>();
        this.observers = new HashSet<NamedWindowLifecycleObserver>();
        this.enableQueryPlanLog = enableQueryPlanLog;
        this.metricReportingService = metricReportingService;
    }

    public void destroy() {
        processors.clear();
    }

    public String[] getNamedWindows() {
        Set<String> names = processors.keySet();
        return names.toArray(new String[names.size()]);
    }

    public StatementAgentInstanceLock getNamedWindowLock(String windowName) {
        NamedWindowLockPair pair = windowStatementLocks.get(windowName);
        if (pair == null) {
            return null;
        }
        return pair.getLock();
    }

    public void addNamedWindowLock(String windowName, StatementAgentInstanceLock statementResourceLock, String statementName) {
        windowStatementLocks.put(windowName, new NamedWindowLockPair(statementName, statementResourceLock));
    }

    public void removeNamedWindowLock(String statementName) {
        for (Map.Entry<String, NamedWindowLockPair> entry : windowStatementLocks.entrySet()) {
            if (entry.getValue().getStatementName().equals(statementName)) {
                windowStatementLocks.remove(entry.getKey());
                return;
            }
        }
    }

    public boolean isNamedWindow(String name) {
        return processors.containsKey(name);
    }

    public NamedWindowProcessor getProcessor(String name) {
        return processors.get(name);
    }

    public IndexMultiKey[] getNamedWindowIndexes(String windowName) {
        NamedWindowProcessor processor = processors.get(windowName);
        if (processor == null) {
            return null;
        }
        return processor.getProcessorInstance(null).getIndexDescriptors();
    }

    public void removeNamedWindowIfFound(String namedWindowName) {
        NamedWindowProcessor processor = processors.get(namedWindowName);
        if (processor == null) {
            return;
        }
        processor.clearProcessorInstances();
        removeProcessor(namedWindowName);
    }

    public NamedWindowProcessor addProcessor(String name, String contextName, EventType eventType, StatementResultService statementResultService,
                                             ValueAddEventProcessor revisionProcessor, String eplExpression, String statementName, boolean isPrioritized,
                                             boolean isEnableSubqueryIndexShare, boolean isBatchingDataWindow,
                                             boolean isVirtualDataWindow,
                                             Set<String> optionalUniqueKeyProps, String eventTypeAsName,
                                             StatementContext statementContextCreateWindow,
                                             NamedWindowDispatchService namedWindowDispatchService) throws ViewProcessingException {
        if (processors.containsKey(name)) {
            throw new ViewProcessingException("A named window by name '" + name + "' has already been created");
        }

        NamedWindowProcessor processor = namedWindowDispatchService.createProcessor(name, this, namedWindowDispatchService, contextName, eventType, statementResultService, revisionProcessor, eplExpression, statementName, isPrioritized, isEnableSubqueryIndexShare, enableQueryPlanLog, metricReportingService, isBatchingDataWindow, isVirtualDataWindow, optionalUniqueKeyProps, eventTypeAsName, statementContextCreateWindow);
        processors.put(name, processor);

        if (!observers.isEmpty()) {
            NamedWindowLifecycleEvent theEvent = new NamedWindowLifecycleEvent(name, processor, NamedWindowLifecycleEvent.LifecycleEventType.CREATE);
            for (NamedWindowLifecycleObserver observer : observers) {
                observer.observe(theEvent);
            }
        }

        return processor;
    }

    public void removeProcessor(String name) {
        NamedWindowProcessor processor = processors.get(name);
        if (processor != null) {
            processor.destroy();
            processors.remove(name);

            if (!observers.isEmpty()) {
                NamedWindowLifecycleEvent theEvent = new NamedWindowLifecycleEvent(name, processor, NamedWindowLifecycleEvent.LifecycleEventType.DESTROY);
                for (NamedWindowLifecycleObserver observer : observers) {
                    observer.observe(theEvent);
                }
            }
        }
    }

    public void addObserver(NamedWindowLifecycleObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(NamedWindowLifecycleObserver observer) {
        observers.remove(observer);
    }

    private static class NamedWindowLockPair {
        private final String statementName;
        private final StatementAgentInstanceLock lock;

        private NamedWindowLockPair(String statementName, StatementAgentInstanceLock lock) {
            this.statementName = statementName;
            this.lock = lock;
        }

        public String getStatementName() {
            return statementName;
        }

        public StatementAgentInstanceLock getLock() {
            return lock;
        }
    }
}
