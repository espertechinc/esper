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
package com.espertech.esper.epl.core.poll;

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.IterablesArrayIterator;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.db.DataCache;
import com.espertech.esper.epl.db.PollExecStrategy;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierAndStreamRefVisitor;
import com.espertech.esper.epl.join.pollindex.PollResultIndexingStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTableList;
import com.espertech.esper.epl.spec.MethodStreamSpec;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.schedule.TimeProvider;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.HistoricalEventViewable;
import com.espertech.esper.view.View;
import com.espertech.esper.view.ViewSupport;
import net.sf.cglib.reflect.FastMethod;

import java.util.*;

/**
 * Polling-data provider that calls a static method on a class and passed parameters, and wraps the
 * results as POJO events.
 */
public class MethodPollingViewable implements HistoricalEventViewable {
    private final MethodStreamSpec methodStreamSpec;
    private final DataCache dataCache;
    private final EventType eventType;
    private final ThreadLocal<DataCache> dataCacheThreadLocal = new ThreadLocal<DataCache>();
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final MethodPollingViewableMeta metadata;

    private PollExecStrategy pollExecStrategy;
    private SortedSet<Integer> requiredStreams;
    private ExprEvaluator[] validatedExprNodes;
    private StatementContext statementContext;

    private static final EventBean[][] NULL_ROWS;

    static {
        NULL_ROWS = new EventBean[1][];
        NULL_ROWS[0] = new EventBean[1];
    }

    private static final PollResultIndexingStrategy ITERATOR_INDEXING_STRATEGY = new PollResultIndexingStrategy() {
        public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, StatementContext statementContext) {
            return new EventTable[]{new UnindexedEventTableList(pollResult, -1)};
        }

        public String toQueryPlan() {
            return this.getClass().getSimpleName() + " unindexed";
        }
    };

    public MethodPollingViewable(
            MethodStreamSpec methodStreamSpec,
            DataCache dataCache,
            EventType eventType,
            ExprEvaluatorContext exprEvaluatorContext,
            MethodPollingViewableMeta metadata) {
        this.methodStreamSpec = methodStreamSpec;
        this.dataCache = dataCache;
        this.eventType = eventType;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.metadata = metadata;
    }

    public void stop() {
        pollExecStrategy.destroy();
        dataCache.destroy();
    }

    public ThreadLocal<DataCache> getDataCacheThreadLocal() {
        return dataCacheThreadLocal;
    }

    public DataCache getOptionalDataCache() {
        return dataCache;
    }

    public void validate(EngineImportService engineImportService, StreamTypeService streamTypeService, TimeProvider timeProvider,
                         VariableService variableService, TableService tableService, ExprEvaluatorContext exprEvaluatorContext, ConfigurationInformation configSnapshot,
                         SchedulingService schedulingService, String engineURI, Map<Integer, List<ExprNode>> sqlParameters, EventAdapterService eventAdapterService, StatementContext statementContext) throws ExprValidationException {

        this.statementContext = statementContext;

        // validate and visit
        ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, engineImportService, statementContext.getStatementExtensionServicesContext(), null, timeProvider, variableService, tableService, exprEvaluatorContext, eventAdapterService, statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), null, false, false, true, false, null, false);
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(true);
        final List<ExprNode> validatedInputParameters = new ArrayList<ExprNode>();
        for (ExprNode exprNode : methodStreamSpec.getExpressions()) {
            ExprNode validated = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.METHODINVJOIN, exprNode, validationContext);
            validatedInputParameters.add(validated);
            validated.accept(visitor);
        }

        // determine required streams
        requiredStreams = new TreeSet<>();
        for (ExprNodePropOrStreamDesc ref : visitor.getRefs()) {
            requiredStreams.add(ref.getStreamNum());
        }

        // class-based evaluation
        if (metadata.getMethodProviderClass() != null) {
            // resolve actual method to use
            ExprNodeUtilResolveExceptionHandler handler = new ExprNodeUtilResolveExceptionHandler() {
                public ExprValidationException handle(Exception e) {
                    if (methodStreamSpec.getExpressions().size() == 0) {
                        return new ExprValidationException("Method footprint does not match the number or type of expression parameters, expecting no parameters in method: " + e.getMessage());
                    }
                    Class[] resultTypes = ExprNodeUtilityCore.getExprResultTypes(validatedInputParameters);
                    return new ExprValidationException("Method footprint does not match the number or type of expression parameters, expecting a method where parameters are typed '" +
                            JavaClassHelper.getParameterAsString(resultTypes) + "': " + e.getMessage());
                }
            };
            ExprNodeUtilMethodDesc desc = ExprNodeUtilityRich.resolveMethodAllowWildcardAndStream(
                    metadata.getMethodProviderClass().getName(), metadata.isStaticMethod() ? null : metadata.getMethodProviderClass(),
                    methodStreamSpec.getMethodName(), validatedInputParameters, engineImportService, eventAdapterService, statementContext.getStatementId(),
                    false, null, handler, methodStreamSpec.getMethodName(), tableService, statementContext.getEngineURI());
            validatedExprNodes = ExprNodeUtilityRich.getEvaluatorsMayCompile(desc.getChildForges(), engineImportService, this.getClass(), streamTypeService.isOnDemandStreams(), statementContext.getStatementName());

            // Construct polling strategy as a method invocation
            Object invocationTarget = metadata.getInvocationTarget();
            MethodPollingExecStrategyEnum strategy = metadata.getStrategy();
            VariableReader variableReader = metadata.getVariableReader();
            String variableName = metadata.getVariableName();
            FastMethod methodFastClass = desc.getFastMethod();
            if (metadata.getEventTypeEventBeanArray() != null) {
                pollExecStrategy = new MethodPollingExecStrategyEventBeans(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
            } else if (metadata.getOptionalMapType() != null) {
                if (desc.getFastMethod().getReturnType().isArray()) {
                    pollExecStrategy = new MethodPollingExecStrategyMapArray(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                } else if (metadata.isCollection()) {
                    pollExecStrategy = new MethodPollingExecStrategyMapCollection(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                } else if (metadata.isIterator()) {
                    pollExecStrategy = new MethodPollingExecStrategyMapIterator(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                } else {
                    pollExecStrategy = new MethodPollingExecStrategyMapPlain(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                }
            } else if (metadata.getOptionalOaType() != null) {
                if (desc.getFastMethod().getReturnType() == Object[][].class) {
                    pollExecStrategy = new MethodPollingExecStrategyOAArray(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                } else if (metadata.isCollection()) {
                    pollExecStrategy = new MethodPollingExecStrategyOACollection(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                } else if (metadata.isIterator()) {
                    pollExecStrategy = new MethodPollingExecStrategyOAIterator(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                } else {
                    pollExecStrategy = new MethodPollingExecStrategyOAPlain(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                }
            } else {
                if (desc.getFastMethod().getReturnType().isArray()) {
                    pollExecStrategy = new MethodPollingExecStrategyPOJOArray(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                } else if (metadata.isCollection()) {
                    pollExecStrategy = new MethodPollingExecStrategyPOJOCollection(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                } else if (metadata.isIterator()) {
                    pollExecStrategy = new MethodPollingExecStrategyPOJOIterator(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                } else {
                    pollExecStrategy = new MethodPollingExecStrategyPOJOPlain(eventAdapterService, methodFastClass, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
                }
            }
        } else {
            // script-based evaluation
            pollExecStrategy = new MethodPollingExecStrategyScript(metadata.getScriptExpression());
            validatedExprNodes = ExprNodeUtilityRich.getEvaluatorsMayCompile(validatedInputParameters, engineImportService, this.getClass(), streamTypeService.isOnDemandStreams(), statementContext.getStatementName());
        }
    }

    public EventTable[][] poll(EventBean[][] lookupEventsPerStream, PollResultIndexingStrategy indexingStrategy, ExprEvaluatorContext exprEvaluatorContext) {
        DataCache localDataCache = dataCacheThreadLocal.get();
        boolean strategyStarted = false;

        EventTable[][] resultPerInputRow = new EventTable[lookupEventsPerStream.length][];

        // Get input parameters for each row
        for (int row = 0; row < lookupEventsPerStream.length; row++) {
            Object[] methodParams = new Object[validatedExprNodes.length];

            // Build lookup keys
            for (int valueNum = 0; valueNum < validatedExprNodes.length; valueNum++) {
                Object parameterValue = validatedExprNodes[valueNum].evaluate(lookupEventsPerStream[row], true, exprEvaluatorContext);
                methodParams[valueNum] = parameterValue;
            }

            EventTable[] result = null;

            // try the threadlocal iteration cache, if set
            if (localDataCache != null) {
                result = localDataCache.getCached(methodParams, methodStreamSpec.getExpressions().size());
            }

            // try the connection cache
            if (result == null) {
                result = dataCache.getCached(methodParams, methodStreamSpec.getExpressions().size());
                if ((result != null) && (localDataCache != null)) {
                    localDataCache.put(methodParams, methodStreamSpec.getExpressions().size(), result);
                }
            }

            if (result != null) {
                // found in cache
                resultPerInputRow[row] = result;
            } else {
                // not found in cache, get from actual polling (db query)
                try {
                    if (!strategyStarted) {
                        pollExecStrategy.start();
                        strategyStarted = true;
                    }

                    // Poll using the polling execution strategy and lookup values
                    List<EventBean> pollResult = pollExecStrategy.poll(methodParams, exprEvaluatorContext);

                    // index the result, if required, using an indexing strategy
                    EventTable[] indexTable = indexingStrategy.index(pollResult, dataCache.isActive(), statementContext);

                    // assign to row
                    resultPerInputRow[row] = indexTable;

                    // save in cache
                    dataCache.put(methodParams, methodStreamSpec.getExpressions().size(), indexTable);

                    if (localDataCache != null) {
                        localDataCache.put(methodParams, methodStreamSpec.getExpressions().size(), indexTable);
                    }
                } catch (EPException ex) {
                    if (strategyStarted) {
                        pollExecStrategy.done();
                    }
                    throw ex;
                }
            }
        }

        if (strategyStarted) {
            pollExecStrategy.done();
        }

        return resultPerInputRow;
    }

    public View addView(View view) {
        view.setParent(this);
        return view;
    }

    public View[] getViews() {
        return ViewSupport.EMPTY_VIEW_ARRAY;
    }

    public boolean removeView(View view) {
        throw new UnsupportedOperationException("Subviews not supported");
    }

    public void removeAllViews() {
        throw new UnsupportedOperationException("Subviews not supported");
    }

    public boolean hasViews() {
        return false;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Iterator<EventBean> iterator() {
        EventTable[][] result = poll(NULL_ROWS, ITERATOR_INDEXING_STRATEGY, exprEvaluatorContext);
        return new IterablesArrayIterator(result);
    }

    public SortedSet<Integer> getRequiredStreams() {
        return requiredStreams;
    }

    public boolean hasRequiredStreams() {
        return !requiredStreams.isEmpty();
    }
}
