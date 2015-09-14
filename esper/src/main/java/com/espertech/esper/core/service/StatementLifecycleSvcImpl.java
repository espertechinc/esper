/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.core.service;

import com.espertech.esper.client.*;
import com.espertech.esper.client.annotation.Hint;
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.client.annotation.Name;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.collection.NameParameterCountKey;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.service.multimatch.*;
import com.espertech.esper.core.start.*;
import com.espertech.esper.epl.agg.rollup.GroupByExpressionHelper;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.core.MethodResolutionService;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.core.StreamTypeServiceImpl;
import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotNode;
import com.espertech.esper.epl.expression.ops.ExprAndNode;
import com.espertech.esper.epl.expression.ops.ExprAndNodeImpl;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectRowNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierCollectVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeSummaryVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeViewResourceVisitor;
import com.espertech.esper.epl.named.NamedWindowService;
import com.espertech.esper.epl.script.jsr223.JSR223Helper;
import com.espertech.esper.epl.script.mvel.MVELHelper;
import com.espertech.esper.epl.script.mvel.MVELInvoker;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.spec.util.StatementSpecCompiledAnalyzer;
import com.espertech.esper.epl.spec.util.StatementSpecCompiledAnalyzerResult;
import com.espertech.esper.epl.spec.util.StatementSpecRawAnalyzer;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.NativeEventType;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.filter.FilterSpecCompiled;
import com.espertech.esper.filter.FilterSpecParam;
import com.espertech.esper.filter.FilterSpecParamExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.pattern.EvalFilterFactoryNode;
import com.espertech.esper.pattern.EvalNodeAnalysisResult;
import com.espertech.esper.pattern.EvalNodeUtil;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.EventRepresentationUtil;
import com.espertech.esper.util.ManagedReadWriteLock;
import com.espertech.esper.util.UuidGenerator;
import com.espertech.esper.view.ViewProcessingException;
import com.espertech.esper.view.Viewable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Provides statement lifecycle services.
 */
public class StatementLifecycleSvcImpl implements StatementLifecycleSvc
{
    private static Log log = LogFactory.getLog(StatementLifecycleSvcImpl.class);

    /**
     * Services context for statement lifecycle management.
     */
    protected final EPServicesContext services;

    /**
     * Maps of statement id to descriptor.
     */
    protected final Map<String, EPStatementDesc> stmtIdToDescMap;

    /**
     * Map of statement name to statement.
     */
    protected final Map<String, EPStatement> stmtNameToStmtMap;

    private final EPServiceProviderSPI epServiceProvider;
    private final ManagedReadWriteLock eventProcessingRWLock;

    private final Map<String, String> stmtNameToIdMap;

    // Observers to statement-related events
    private final Set<StatementLifecycleObserver> observers;

    /**
     * Ctor.
     * @param epServiceProvider is the engine instance to hand to statement-aware listeners
     * @param services is engine services
     */
    public StatementLifecycleSvcImpl(EPServiceProvider epServiceProvider, EPServicesContext services)
    {
        this.services = services;
        this.epServiceProvider = (EPServiceProviderSPI) epServiceProvider;

        // lock for starting and stopping statements
        this.eventProcessingRWLock = services.getEventProcessingRWLock();

        this.stmtIdToDescMap = new HashMap<String, EPStatementDesc>();
        this.stmtNameToStmtMap = new HashMap<String, EPStatement>();
        this.stmtNameToIdMap = new LinkedHashMap<String, String>();

        observers = new CopyOnWriteArraySet<StatementLifecycleObserver>();
    }

    public void addObserver(StatementLifecycleObserver observer)
    {
        observers.add(observer);
    }

    public void removeObserver(StatementLifecycleObserver observer)
    {
        observers.remove(observer);
    }

    public void destroy()
    {
        this.destroyAllStatements();
    }

    public void init()
    {
        // called after services are activated, to begin statement loading from store
    }

    public Map<String, EPStatement> getStmtNameToStmt() {
        return stmtNameToStmtMap;
    }

    public synchronized EPStatement createAndStart(StatementSpecRaw statementSpec, String expression, boolean isPattern, String optStatementName, Object userObject, EPIsolationUnitServices isolationUnitServices, String statementId, EPStatementObjectModel optionalModel)
    {
        String assignedStatementId = statementId;
        if (assignedStatementId == null) {
            assignedStatementId = UuidGenerator.generate();
        }

        EPStatementDesc desc = createStoppedAssignName(statementSpec, expression, isPattern, optStatementName, assignedStatementId, null, userObject, isolationUnitServices, optionalModel);
        start(assignedStatementId, desc, true, false, false);
        return desc.getEpStatement();
    }

    /**
     * Creates and starts statement.
     * @param statementSpec defines the statement
     * @param expression is the EPL
     * @param isPattern is true for patterns
     * @param optStatementName is the optional statement name
     * @param statementId is the statement id
     * @param optAdditionalContext additional context for use by the statement context
     * @param userObject the application define user object associated to each statement, if supplied
     * @param isolationUnitServices isolated service services
     * @return started statement
     */
    protected synchronized EPStatementDesc createStoppedAssignName(StatementSpecRaw statementSpec, String expression, boolean isPattern, String optStatementName, String statementId, Map<String, Object> optAdditionalContext, Object userObject, EPIsolationUnitServices isolationUnitServices, EPStatementObjectModel optionalModel)
    {
        boolean nameProvided = false;
        String statementName = statementId;

        // compile annotations, can produce a null array
        Annotation[] annotations = AnnotationUtil.compileAnnotations(statementSpec.getAnnotations(), services.getEngineImportService(), expression);

        // find name annotation
        if (optStatementName == null) {
            if (annotations != null && annotations.length != 0) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Name) {
                        Name name = (Name) annotation;
                        if (name.value() != null) {
                            optStatementName = name.value();
                        }
                    }
                }
            }
        }

        // Determine a statement name, i.e. use the id or use/generate one for the name passed in
        if (optStatementName != null) {
            optStatementName = optStatementName.trim();
            statementName = getUniqueStatementName(optStatementName, statementId);
            nameProvided = true;
        }

        return createStopped(statementSpec, annotations, expression, isPattern, statementName, nameProvided, statementId, optAdditionalContext, userObject, isolationUnitServices, false, optionalModel);
    }

    /**
     * Create stopped statement.
     * @param statementSpec - statement definition
     * @param expression is the expression text
     * @param isPattern is true for patterns, false for non-patterns
     * @param statementName is the statement name assigned or given
     * @param statementId is the statement id
     * @param optAdditionalContext additional context for use by the statement context
     * @param statementUserObject the application define user object associated to each statement, if supplied
     * @param isolationUnitServices isolated service services
     * @param isFailed to start the statement in failed state
     * @param nameProvided true when an explicit statement name is provided
     * @return stopped statement
     */
    protected synchronized EPStatementDesc createStopped(StatementSpecRaw statementSpec,
                                                         Annotation[] annotations,
                                                         String expression,
                                                         boolean isPattern,
                                                         String statementName,
                                                         boolean nameProvided,
                                                         String statementId,
                                                         Map<String, Object> optAdditionalContext,
                                                         Object statementUserObject,
                                                         EPIsolationUnitServices isolationUnitServices,
                                                         boolean isFailed,
                                                         EPStatementObjectModel optionalModel)
    {
        EPStatementDesc statementDesc;
        EPStatementStartMethod startMethod;

        // Hint annotations are often driven by variables
        if (annotations != null)
        {
            for (Annotation annotation : annotations)
            {
                if (annotation instanceof Hint)
                {
                    statementSpec.setHasVariables(true);
                }
            }
        }

        // walk subselects, alias expressions, declared expressions, dot-expressions
        ExprNodeSubselectDeclaredDotVisitor visitor;
        try {
            visitor = StatementSpecRawAnalyzer.walkSubselectAndDeclaredDotExpr(statementSpec);
        }
        catch (ExprValidationException ex) {
            throw new EPStatementException(ex.getMessage(), expression);
        }

        // Determine Subselects for compilation, and lambda-expression shortcut syntax for named windows
        List<ExprSubselectNode> subselectNodes = visitor.getSubselects();
        if (!visitor.getChainedExpressionsDot().isEmpty()) {
            rewriteNamedWindowSubselect(visitor.getChainedExpressionsDot(), subselectNodes, services.getNamedWindowService());
        }

        // compile foreign scripts
        validateScripts(expression, statementSpec.getScriptExpressions(), statementSpec.getExpressionDeclDesc());

        // Determine statement type
        StatementType statementType = StatementMetadataFactoryDefault.getStatementType(statementSpec, isPattern);

        // Determine stateless statement
        boolean stateless = determineStatelessSelect(statementType, statementSpec, !subselectNodes.isEmpty(), isPattern);

        // Determine table use
        boolean writesToTables = StatementLifecycleSvcUtil.isWritesToTables(statementSpec, services.getTableService());

        // Make context
        StatementContext statementContext = services.getStatementContextFactory().makeContext(statementId,  statementName, expression, statementType, services, optAdditionalContext, false, annotations, isolationUnitServices, stateless, statementSpec, subselectNodes, writesToTables, statementUserObject);

        StatementSpecCompiled compiledSpec;
        try
        {
            compiledSpec = compile(statementSpec, expression, statementContext, false, false, annotations, visitor.getSubselects(), visitor.getDeclaredExpressions(), services);
        }
        catch (EPStatementException ex)
        {
            stmtNameToIdMap.remove(statementName); // Clean out the statement name as it's already assigned
            throw ex;
        }

        // We keep a reference of the compiled spec as part of the statement context
        statementContext.setStatementSpecCompiled(compiledSpec);

        // For insert-into streams, create a lock taken out as soon as an event is inserted
        // Makes the processing between chained statements more predictable.
        if (statementSpec.getInsertIntoDesc() != null || statementSpec.getOnTriggerDesc() instanceof OnTriggerMergeDesc)
        {
            String insertIntoStreamName;
            if (statementSpec.getInsertIntoDesc() != null) {
                insertIntoStreamName = statementSpec.getInsertIntoDesc().getEventTypeName();
            }
            else {
                insertIntoStreamName = "merge";
            }
            String latchFactoryNameBack = "insert_stream_B_" + insertIntoStreamName + "_" + statementName;
            String latchFactoryNameFront = "insert_stream_F_" + insertIntoStreamName + "_" + statementName;
            long msecTimeout = services.getEngineSettingsService().getEngineSettings().getThreading().getInsertIntoDispatchTimeout();
            ConfigurationEngineDefaults.Threading.Locking locking = services.getEngineSettingsService().getEngineSettings().getThreading().getInsertIntoDispatchLocking();
            InsertIntoLatchFactory latchFactoryFront = new InsertIntoLatchFactory(latchFactoryNameFront, stateless, msecTimeout, locking, services.getTimeSource());
            InsertIntoLatchFactory latchFactoryBack = new InsertIntoLatchFactory(latchFactoryNameBack, stateless, msecTimeout, locking, services.getTimeSource());
            statementContext.getEpStatementHandle().setInsertIntoFrontLatchFactory(latchFactoryFront);
            statementContext.getEpStatementHandle().setInsertIntoBackLatchFactory(latchFactoryBack);
        }

        // determine overall filters, assign the filter spec index to filter boolean expressions
        boolean needDedup = false;
        StatementSpecCompiledAnalyzerResult streamAnalysis = StatementSpecCompiledAnalyzer.analyzeFilters(compiledSpec);
        FilterSpecCompiled[] filterSpecAll = streamAnalysis.getFilters().toArray(new FilterSpecCompiled[streamAnalysis.getFilters().size()]);
        compiledSpec.setFilterSpecsOverall(filterSpecAll);
        for (FilterSpecCompiled filter : filterSpecAll) {
            if (filter.getParameters().length > 1) {
                needDedup = true;
                break;
            }
            assignFilterSpecIds(filter, filterSpecAll);
        }

        MultiMatchHandler multiMatchHandler;
        boolean isSubselectPreeval = services.getEngineSettingsService().getEngineSettings().getExpression().isSelfSubselectPreeval();
        if (!needDedup) {
            // no dedup
            if (subselectNodes.isEmpty()) {
                multiMatchHandler = MultiMatchHandlerNoSubqueryNoDedup.INSTANCE;
            }
            else {
                if (isSubselectPreeval) {
                    multiMatchHandler = MultiMatchHandlerSubqueryPreevalNoDedup.INSTANCE;
                }
                else {
                    multiMatchHandler = MultiMatchHandlerSubqueryPostevalNoDedup.INSTANCE;
                }
            }
        }
        else {
            // with dedup
            if (subselectNodes.isEmpty()) {
                multiMatchHandler = MultiMatchHandlerNoSubqueryWDedup.INSTANCE;
            }
            else {
                multiMatchHandler = new MultiMatchHandlerSubqueryWDedup(isSubselectPreeval);
            }
        }
        statementContext.getEpStatementHandle().setMultiMatchHandler(multiMatchHandler);

        // In a join statements if the same event type or it's deep super types are used in the join more then once,
        // then this is a self-join and the statement handle must know to dispatch the results together
        boolean canSelfJoin = isPotentialSelfJoin(compiledSpec) || needDedup;
        statementContext.getEpStatementHandle().setCanSelfJoin(canSelfJoin);

        // add statically typed event type references: those in the from clause; Dynamic (created) types collected by statement context and added on start
        services.getStatementEventTypeRefService().addReferences(statementName, compiledSpec.getEventTypeReferences());

        // add variable references
        services.getStatementVariableRefService().addReferences(statementName, compiledSpec.getVariableReferences(), compiledSpec.getTableNodes());

        // create metadata
        StatementMetadata statementMetadata = services.getStatementMetadataFactory().create(new StatementMetadataFactoryContext(statementName, statementId, statementContext, statementSpec, expression, isPattern, optionalModel));

        eventProcessingRWLock.acquireWriteLock();
        try
        {
            // create statement - may fail for parser and simple validation errors
            boolean preserveDispatchOrder = services.getEngineSettingsService().getEngineSettings().getThreading().isListenerDispatchPreserveOrder()
                    && !stateless;
            boolean isSpinLocks = services.getEngineSettingsService().getEngineSettings().getThreading().getListenerDispatchLocking() == ConfigurationEngineDefaults.Threading.Locking.SPIN;
            long blockingTimeout = services.getEngineSettingsService().getEngineSettings().getThreading().getListenerDispatchTimeout();
            long timeLastStateChange = services.getSchedulingService().getTime();
            EPStatementSPI statement = services.getEpStatementFactory().make(statementSpec.getExpressionNoAnnotations(), isPattern,
                    services.getDispatchService(), this, timeLastStateChange, preserveDispatchOrder, isSpinLocks, blockingTimeout,
                    services.getTimeSource(), statementMetadata, statementUserObject, statementContext, isFailed, nameProvided);
            statementContext.setStatement(statement);

            boolean isInsertInto = statementSpec.getInsertIntoDesc() != null;
            boolean isDistinct = statementSpec.getSelectClauseSpec().isDistinct();
            boolean isForClause = statementSpec.getForClauseSpec() != null;
            statementContext.getStatementResultService().setContext(statement, epServiceProvider,
                    isInsertInto, isPattern, isDistinct, isForClause, statementContext.getEpStatementHandle().getMetricsHandle());

            // create start method
            startMethod = EPStatementStartMethodFactory.makeStartMethod(compiledSpec);

            statementDesc = new EPStatementDesc(statement, startMethod, statementContext);
            stmtIdToDescMap.put(statementId, statementDesc);
            stmtNameToStmtMap.put(statementName, statement);
            stmtNameToIdMap.put(statementName, statementId);

            dispatchStatementLifecycleEvent(new StatementLifecycleEvent(statement, StatementLifecycleEvent.LifecycleEventType.CREATE));
        }
        catch (RuntimeException ex)
        {
            stmtIdToDescMap.remove(statementId);
            stmtNameToIdMap.remove(statementName);
            stmtNameToStmtMap.remove(statementName);
            throw ex;
        }
        finally
        {
            eventProcessingRWLock.releaseWriteLock();
        }

        return statementDesc;
    }

    // All scripts get compiled/verfied - to ensure they compile (and not just when they are referred to my an expression).
    private void validateScripts(String epl, List<ExpressionScriptProvided> scripts, ExpressionDeclDesc expressionDeclDesc) {
        if (scripts == null) {
            return;
        }
        try {
            Set<NameParameterCountKey> scriptsSet = new HashSet<NameParameterCountKey>();
            for (ExpressionScriptProvided script : scripts) {
                validateScript(script);

                NameParameterCountKey key = new NameParameterCountKey(script.getName(), script.getParameterNames().size());
                if (scriptsSet.contains(key)) {
                    throw new ExprValidationException("Script name '" + script.getName() + "' has already been defined with the same number of parameters");
                }
                scriptsSet.add(key);
            }

            if (expressionDeclDesc != null) {
                for (ExpressionDeclItem declItem : expressionDeclDesc.getExpressions()) {
                    if (scriptsSet.contains(new NameParameterCountKey(declItem.getName(), 0))) {
                        throw new ExprValidationException("Script name '" + declItem.getName() + "' overlaps with another expression of the same name");
                    }
                }
            }
        }
        catch (ExprValidationException ex) {
            throw new EPStatementException(ex.getMessage(), ex, epl);
        }
    }

    private void validateScript(ExpressionScriptProvided script) throws ExprValidationException {
        String dialect = script.getOptionalDialect() == null ? services.getConfigSnapshot().getEngineDefaults().getScripts().getDefaultDialect() : script.getOptionalDialect();
        if (dialect == null) {
            throw new ExprValidationException("Failed to determine script dialect for script '" + script.getName() + "', please configure a default dialect or provide a dialect explicitly");
        }
        if (dialect.trim().toLowerCase().equals("mvel")) {
            if (!MVELInvoker.isMVELInClasspath()) {
                throw new ExprValidationException("MVEL scripting engine not found in classpath, script dialect 'mvel' requires mvel in classpath for script '" + script.getName() + "'");
            }
            MVELHelper.verifyScript(script);
        }
        else {
            JSR223Helper.verifyCompileScript(script, dialect);
        }

        if (!script.getParameterNames().isEmpty()) {
            HashSet<String> parameters = new HashSet<String>();
            for (String param : script.getParameterNames()) {
                if (parameters.contains(param)) {
                    throw new ExprValidationException("Invalid script parameters for script '" + script.getName() + "', parameter '" + param + "' is defined more then once");
                }
                parameters.add(param);
            }
        }
    }

    private boolean isPotentialSelfJoin(StatementSpecCompiled spec)
    {
        // Include create-context as nested contexts that have pattern-initiated sub-contexts may change filters during execution
        if (spec.getContextDesc() != null && spec.getContextDesc().getContextDetail() instanceof ContextDetailNested) {
            return true;
        }

        // if order-by is specified, ans since multiple output rows may produce, ensure dispatch
        if (spec.getOrderByList().length > 0)
        {
            return true;
        }

        for (StreamSpecCompiled streamSpec : spec.getStreamSpecs())
        {
            if (streamSpec instanceof PatternStreamSpecCompiled)
            {
                return true;
            }
        }

        // not a self join
        if ((spec.getStreamSpecs().length <= 1) && (spec.getSubSelectExpressions().length == 0))
        {
            return false;
        }

        // join - determine types joined
        List<EventType> filteredTypes = new ArrayList<EventType>();

        // consider subqueryes
        Set<EventType> optSubselectTypes = populateSubqueryTypes(spec.getSubSelectExpressions());

        boolean hasFilterStream = false;
        for (StreamSpecCompiled streamSpec : spec.getStreamSpecs())
        {
            if (streamSpec instanceof FilterStreamSpecCompiled)
            {
                EventType type = ((FilterStreamSpecCompiled) streamSpec).getFilterSpec().getFilterForEventType();
                filteredTypes.add(type);
                hasFilterStream = true;
            }
        }

        if ((filteredTypes.size() == 1) && (optSubselectTypes.isEmpty()))
        {
            return false;
        }

        // pattern-only streams are not self-joins
        if (!hasFilterStream)
        {
            return false;
        }

        // is type overlap in filters
        for (int i = 0; i < filteredTypes.size(); i++)
        {
            for (int j = i + 1; j < filteredTypes.size(); j++)
            {
                EventType typeOne = filteredTypes.get(i);
                EventType typeTwo = filteredTypes.get(j);
                if (typeOne == typeTwo)
                {
                    return true;
                }

                if (typeOne.getSuperTypes() != null)
                {
                    for (EventType typeOneSuper : typeOne.getSuperTypes())
                    {
                        if (typeOneSuper == typeTwo)
                        {
                            return true;
                        }
                    }
                }
                if (typeTwo.getSuperTypes() != null)
                {
                    for (EventType typeTwoSuper : typeTwo.getSuperTypes())
                    {
                        if (typeOne == typeTwoSuper)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        // analyze subselect types
        if (!optSubselectTypes.isEmpty())
        {
            for (int i = 0; i < filteredTypes.size(); i++)
            {
                EventType typeOne = filteredTypes.get(i);
                if (optSubselectTypes.contains(typeOne))
                {
                    return true;
                }

                if (typeOne.getSuperTypes() != null)
                {
                    for (EventType typeOneSuper : typeOne.getSuperTypes())
                    {
                        if (optSubselectTypes.contains(typeOneSuper))
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private Set<EventType> populateSubqueryTypes(ExprSubselectNode[] subSelectExpressions)
    {
        Set<EventType> set = null;
        for (ExprSubselectNode subselect : subSelectExpressions)
        {
            for (StreamSpecCompiled streamSpec : subselect.getStatementSpecCompiled().getStreamSpecs())
            {
                if (streamSpec instanceof FilterStreamSpecCompiled)
                {
                    EventType type = ((FilterStreamSpecCompiled) streamSpec).getFilterSpec().getFilterForEventType();
                    if (set == null)
                    {
                        set = new HashSet<EventType>();
                    }
                    set.add(type);
                }
                else if (streamSpec instanceof PatternStreamSpecCompiled)
                {
                    EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(((PatternStreamSpecCompiled) streamSpec).getEvalFactoryNode());
                    List<EvalFilterFactoryNode> filterNodes = evalNodeAnalysisResult.getFilterNodes();
                    for (EvalFilterFactoryNode filterNode : filterNodes)
                    {
                        if (set == null)
                        {
                            set = new HashSet<EventType>();
                        }
                        set.add(filterNode.getFilterSpec().getFilterForEventType());
                    }
                }
            }
        }
        if (set == null)
        {
            return Collections.EMPTY_SET;
        }
        return set;
    }

    public synchronized void start(String statementId)
    {
        if (log.isDebugEnabled())
        {
            log.debug(".start Starting statement " + statementId);
        }

        // Acquire a lock for event processing as threads may be in the views used by the statement
        // and that could conflict with the destroy of views
        eventProcessingRWLock.acquireWriteLock();
        try
        {
            EPStatementDesc desc = stmtIdToDescMap.get(statementId);
            if (desc == null)
            {
                throw new IllegalStateException("Cannot start statement, statement is in destroyed state");
            }
            startInternal(statementId, desc, false, false, false);
        }
        finally
        {
            eventProcessingRWLock.releaseWriteLock();
        }
    }

    /**
     * Start the given statement.
     * @param statementId is the statement id
     * @param desc is the cached statement info
     * @param isNewStatement indicator whether the statement is new or a stop-restart statement
     * @param isRecoveringStatement if the statement is recovering or new
     * @param isResilient true if recovering a resilient stmt
     */
    public void start(String statementId, EPStatementDesc desc, boolean isNewStatement, boolean isRecoveringStatement, boolean isResilient)
    {
        if (log.isDebugEnabled())
        {
            log.debug(".start Starting statement " + statementId + " from desc=" + desc);
        }

        // Acquire a lock for event processing as threads may be in the views used by the statement
        // and that could conflict with the destroy of views
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qEngineManagementStmtCompileStart(
                services.getEngineURI(), statementId, desc.getEpStatement().getName(), desc.getEpStatement().getText(), services.getSchedulingService().getTime());}
        eventProcessingRWLock.acquireWriteLock();
        try
        {
            startInternal(statementId, desc, isNewStatement, isRecoveringStatement, isResilient);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qaEngineManagementStmtStarted(
                    services.getEngineURI(), statementId, desc.getEpStatement().getName(), desc.getEpStatement().getText(), services.getSchedulingService().getTime());}

            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aEngineManagementStmtCompileStart(true, null);}
        }
        catch (RuntimeException ex) {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aEngineManagementStmtCompileStart(false, ex.getMessage());}
            throw ex;
        }
        finally
        {
            eventProcessingRWLock.releaseWriteLock();
        }
    }

    private void startInternal(String statementId, EPStatementDesc desc, boolean isNewStatement, boolean isRecoveringStatement, boolean isResilient)
    {
        if (log.isDebugEnabled())
        {
            log.debug(".startInternal Starting statement " + statementId + " from desc=" + desc);
        }

        if (desc.getStartMethod() == null)
        {
            throw new IllegalStateException("Statement start method not found for id " + statementId);
        }

        EPStatementSPI statement = desc.getEpStatement();
        if (statement.getState() == EPStatementState.STARTED)
        {
            log.debug(".startInternal - Statement already started");
            return;
        }

        EPStatementStartResult startResult;
        try
        {
            startResult = desc.getStartMethod().start(services, desc.getStatementContext(), isNewStatement, isRecoveringStatement, isResilient);
        }
        catch (EPStatementException ex)
        {
            handleRemove(statementId, statement.getName());
            log.debug(".start Error starting statement", ex);
            throw ex;
        }
        catch (ExprValidationException ex)
        {
            handleRemove(statementId, statement.getName());
            log.debug(".start Error starting statement", ex);
            throw new EPStatementException("Error starting statement: " + ex.getMessage(), ex, statement.getText());
        }
        catch (ViewProcessingException ex)
        {
            handleRemove(statementId, statement.getName());
            log.debug(".start Error starting statement", ex);
            throw new EPStatementException("Error starting statement: " + ex.getMessage(), ex, statement.getText());
        }
        catch (RuntimeException ex)
        {
            handleRemove(statementId, statement.getName());
            log.debug(".start Error starting statement", ex);
            throw new EPStatementException("Unexpected exception starting statement: " + ex.getMessage(), ex, statement.getText());
        }

        // hook up
        Viewable parentView = startResult.getViewable();
        desc.setStopMethod(startResult.getStopMethod());
        desc.setDestroyMethod(startResult.getDestroyMethod());
        statement.setParentView(parentView);
        long timeLastStateChange = services.getSchedulingService().getTime();
        statement.setCurrentState(EPStatementState.STARTED, timeLastStateChange);

        dispatchStatementLifecycleEvent(new StatementLifecycleEvent(statement, StatementLifecycleEvent.LifecycleEventType.STATECHANGE));
    }

    private void handleRemove(String statementId, String statementName) {
        stmtIdToDescMap.remove(statementId);
        stmtNameToIdMap.remove(statementName);
        stmtNameToStmtMap.remove(statementName);
        services.getStatementEventTypeRefService().removeReferencesStatement(statementName);
    }

    public synchronized void stop(String statementId)
    {
        // Acquire a lock for event processing as threads may be in the views used by the statement
        // and that could conflict with the destroy of views
        eventProcessingRWLock.acquireWriteLock();
        try
        {
            EPStatementDesc desc = stmtIdToDescMap.get(statementId);
            if (desc == null)
            {
                throw new IllegalStateException("Cannot stop statement, statement is in destroyed state");
            }

            EPStatementSPI statement = desc.getEpStatement();
            EPStatementStopMethod stopMethod = desc.getStopMethod();
            if (stopMethod == null)
            {
                throw new IllegalStateException("Stop method not found for statement " + statementId);
            }

            if (statement.getState() == EPStatementState.STOPPED)
            {
                log.debug(".startInternal - Statement already stopped");
                return;
            }

            // fire the statement stop
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qEngineManagementStmtStop(EPStatementState.STOPPED, services.getEngineURI(), statementId, statement.getName(), statement.getText(), services.getSchedulingService().getTime());}

            desc.getStatementContext().getStatementStopService().fireStatementStopped();

            // invoke start-provided stop method
            stopMethod.stop();
            statement.setParentView(null);
            desc.setStopMethod(null);

            long timeLastStateChange = services.getSchedulingService().getTime();
            statement.setCurrentState(EPStatementState.STOPPED, timeLastStateChange);

            ((EPRuntimeSPI) epServiceProvider.getEPRuntime()).clearCaches();

            dispatchStatementLifecycleEvent(new StatementLifecycleEvent(statement, StatementLifecycleEvent.LifecycleEventType.STATECHANGE));
        }
        finally
        {
            eventProcessingRWLock.releaseWriteLock();
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aEngineManagementStmtStop();}
        }
    }

    public synchronized void destroy(String statementId)
    {
        // Acquire a lock for event processing as threads may be in the views used by the statement
        // and that could conflict with the destroy of views
        eventProcessingRWLock.acquireWriteLock();
        try
        {
            EPStatementDesc desc = stmtIdToDescMap.get(statementId);
            if (desc == null)
            {
                log.debug(".startInternal - Statement already destroyed");
                return;
            }

            // fire the statement stop
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qEngineManagementStmtStop(EPStatementState.DESTROYED, services.getEngineURI(), statementId, desc.getEpStatement().getName(), desc.getEpStatement().getText(), services.getSchedulingService().getTime());}

            // remove referenced event types
            services.getStatementEventTypeRefService().removeReferencesStatement(desc.getEpStatement().getName());

            // remove referenced variabkes
            services.getStatementVariableRefService().removeReferencesStatement(desc.getEpStatement().getName());

            // remove the named window lock
            services.getNamedWindowService().removeNamedWindowLock(desc.getEpStatement().getName());

            // remove any pattern subexpression counts
            if (services.getPatternSubexpressionPoolSvc() != null) {
                services.getPatternSubexpressionPoolSvc().removeStatement(desc.getEpStatement().getName());
            }

            // remove any match-recognize counts
            if (services.getMatchRecognizeStatePoolEngineSvc() != null) {
                services.getMatchRecognizeStatePoolEngineSvc().removeStatement(desc.getEpStatement().getName());
            }

            EPStatementSPI statement = desc.getEpStatement();
            if (statement.getState() == EPStatementState.STARTED)
            {
                // fire the statement stop
                desc.getStatementContext().getStatementStopService().fireStatementStopped();

                // invoke start-provided stop method
                EPStatementStopMethod stopMethod = desc.getStopMethod();
                statement.setParentView(null);
                stopMethod.stop();
            }

            if (desc.getDestroyMethod() != null) {
                desc.getDestroyMethod().destroy();
            }

            // finally remove reference to schedulable agent-instance resources (an HA requirements)
            if (services.getSchedulableAgentInstanceDirectory() != null) {
                services.getSchedulableAgentInstanceDirectory().removeStatement(desc.getStatementContext().getEpStatementHandle().getStatementId());
            }

            long timeLastStateChange = services.getSchedulingService().getTime();
            statement.setCurrentState(EPStatementState.DESTROYED, timeLastStateChange);

            stmtNameToStmtMap.remove(statement.getName());
            stmtNameToIdMap.remove(statement.getName());
            stmtIdToDescMap.remove(statementId);

            if (!epServiceProvider.isDestroyed()) {
                ((EPRuntimeSPI) epServiceProvider.getEPRuntime()).clearCaches();
            }

            dispatchStatementLifecycleEvent(new StatementLifecycleEvent(statement, StatementLifecycleEvent.LifecycleEventType.STATECHANGE));
        }
        finally
        {
            eventProcessingRWLock.releaseWriteLock();
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aEngineManagementStmtStop();}
        }
    }

    public synchronized EPStatement getStatementByName(String name)
    {
        return stmtNameToStmtMap.get(name);
    }

    public synchronized StatementSpecCompiled getStatementSpec(String statementId) {
        EPStatementDesc desc = stmtIdToDescMap.get(statementId);
        if (desc != null) {
            return desc.getStartMethod().getStatementSpec();
        }
        return null;
    }

    /**
     * Returns the statement given a statement id.
     * @param id is the statement id
     * @return statement
     */
    public EPStatementSPI getStatementById(String id)
    {
        EPStatementDesc statementDesc = this.stmtIdToDescMap.get(id);
        if (statementDesc == null)
        {
            log.warn("Could not locate statement descriptor for statement id '" + id + "'");
            return null;
        }
        return statementDesc.getEpStatement();
    }

    public synchronized String[] getStatementNames()
    {
        String[] statements = new String[stmtNameToStmtMap.size()];
        int count = 0;
        for (String key : stmtNameToStmtMap.keySet())
        {
            statements[count++] = key;
        }
        return statements;
    }

    public synchronized void startAllStatements() throws EPException
    {
        String[] statementIds = getStatementIds();
        for (int i = 0; i < statementIds.length; i++)
        {
            EPStatement statement = stmtIdToDescMap.get(statementIds[i]).getEpStatement();
            if (statement.getState() == EPStatementState.STOPPED)
            {
                start(statementIds[i]);
            }
        }
    }

    public synchronized void stopAllStatements() throws EPException
    {
        String[] statementIds = getStatementIds();
        for (int i = 0; i < statementIds.length; i++)
        {
            EPStatement statement = stmtIdToDescMap.get(statementIds[i]).getEpStatement();
            if (statement.getState() == EPStatementState.STARTED)
            {
                stop(statementIds[i]);
            }
        }
    }

    public synchronized void destroyAllStatements() throws EPException
    {
        String[] statementIds = getStatementIds();
        for (int i = 0; i < statementIds.length; i++)
        {
            try
            {
                destroy(statementIds[i]);
            }
            catch (Exception ex)
            {
                log.warn("Error destroying statement:" + ex.getMessage(), ex);
            }
        }
    }

    private String[] getStatementIds()
    {
        String[] statementIds = new String[stmtNameToIdMap.size()];
        int count = 0;
        for (String id : stmtNameToIdMap.values())
        {
            statementIds[count++] = id;
        }
        return statementIds;
    }

    private String getUniqueStatementName(String statementName, String statementId)
    {
        String finalStatementName;

        if (stmtNameToIdMap.containsKey(statementName))
        {
            int count = 0;
            while(true)
            {
                finalStatementName = statementName + "--" + count;
                if (!(stmtNameToIdMap.containsKey(finalStatementName)))
                {
                    break;
                }
                if (count > Integer.MAX_VALUE - 2)
                {
                    throw new EPException("Failed to establish a unique statement name");
                }
                count++;
            }
        }
        else
        {
            finalStatementName = statementName;
        }

        stmtNameToIdMap.put(finalStatementName, statementId);
        return finalStatementName;
    }

    @Override
    public String getStatementNameById(String id) {
        EPStatementDesc desc = stmtIdToDescMap.get(id);
        if (desc != null) {
            return desc.getEpStatement().getName();
        }
        return null;
    }

    public void updatedListeners(EPStatement statement, EPStatementListenerSet listeners, boolean isRecovery)
    {
        log.debug(".updatedListeners No action for base implementation");
    }

    /**
     * Compiles a statement returning the compile (verified, non-serializable) form of a statement.
     * @param spec is the statement specification
     * @param eplStatement the statement to compile
     * @param statementContext the statement services
     * @param isSubquery is true for subquery compilation or false for statement compile
     * @param annotations statement annotations
     * @return compiled statement
     * @throws EPStatementException if the statement cannot be compiled
     */
    protected static StatementSpecCompiled compile(StatementSpecRaw spec,
                                                   String eplStatement,
                                                   StatementContext statementContext,
                                                   boolean isSubquery,
                                                   boolean isOnDemandQuery,
                                                   Annotation[] annotations,
                                                   List<ExprSubselectNode> subselectNodes,
                                                   List<ExprDeclaredNode> declaredNodes,
                                                   EPServicesContext servicesContext) throws EPStatementException
    {
        List<StreamSpecCompiled> compiledStreams;
        Set<String> eventTypeReferences = new HashSet<String>();

        // If not using a join and not specifying a data window, make the where-clause, if present, the filter of the stream
        // if selecting using filter spec, and not subquery in where clause
        if ((spec.getStreamSpecs().size() == 1) &&
            (spec.getStreamSpecs().get(0) instanceof FilterStreamSpecRaw) &&
            (spec.getStreamSpecs().get(0).getViewSpecs().length == 0) &&
            (spec.getFilterRootNode() != null) &&
            (spec.getOnTriggerDesc() == null) &&
            !isSubquery &&
            !isOnDemandQuery &&
            (spec.getTableExpressions() == null || spec.getTableExpressions().isEmpty()) )
        {
            boolean disqualified;
            ExprNode whereClause = spec.getFilterRootNode();

            ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
            whereClause.accept(visitor);
            disqualified = visitor.getSubselects().size() > 0 || HintEnum.DISABLE_WHEREEXPR_MOVETO_FILTER.getHint(annotations) != null;

            if (!disqualified)
            {
                ExprNodeViewResourceVisitor viewResourceVisitor = new ExprNodeViewResourceVisitor();
                whereClause.accept(viewResourceVisitor);
                disqualified = viewResourceVisitor.getExprNodes().size() > 0;
            }

            if (!disqualified)
            {
                // If an alias is provided, find all properties to ensure the alias gets removed
                String alias = spec.getStreamSpecs().get(0).getOptionalStreamName();
                if (alias != null) {
                    ExprNodeIdentifierCollectVisitor v = new ExprNodeIdentifierCollectVisitor();
                    whereClause.accept(v);
                    for (ExprIdentNode node : v.getExprProperties()) {
                        if (node.getStreamOrPropertyName() != null && (node.getStreamOrPropertyName().equals(alias))) {
                            node.setStreamOrPropertyName(null);
                        }
                    }
                }

                spec.setFilterExprRootNode(null);
                FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) spec.getStreamSpecs().get(0);
                streamSpec.getRawFilterSpec().getFilterExpressions().add(whereClause);
            }
        }

        // compile select-clause
        SelectClauseSpecCompiled selectClauseCompiled = StatementLifecycleSvcUtil.compileSelectClause(spec.getSelectClauseSpec());

        // Determine subselects in filter streams, these may need special handling for locking
        ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
        StatementLifecycleSvcUtil.walkStreamSpecs(spec, visitor);
        for (ExprSubselectNode subselectNode : visitor.getSubselects()) {
            subselectNode.setFilterStreamSubselect(true);
        }

        // Determine subselects for compilation, and lambda-expression shortcut syntax for named windows
        visitor.reset();
        GroupByClauseExpressions groupByRollupExpressions;
        try {
            StatementLifecycleSvcUtil.walkStatement(spec, visitor);

            groupByRollupExpressions = GroupByExpressionHelper.getGroupByRollupExpressions(spec.getGroupByExpressions(),
                    spec.getSelectClauseSpec(), spec.getHavingExprRootNode(), spec.getOrderByList(), visitor);

            List<ExprSubselectNode> subselects = visitor.getSubselects();
            if (!visitor.getChainedExpressionsDot().isEmpty()) {
                rewriteNamedWindowSubselect(visitor.getChainedExpressionsDot(), subselects, statementContext.getNamedWindowService());
            }
        }
        catch (ExprValidationException ex) {
            throw new EPStatementException(ex.getMessage(), eplStatement);
        }

        if (isSubquery && !visitor.getSubselects().isEmpty()) {
            throw new EPStatementException("Invalid nested subquery, subquery-within-subquery is not supported", eplStatement);
        }
        if (isOnDemandQuery && !visitor.getSubselects().isEmpty()) {
            throw new EPStatementException("Subqueries are not a supported feature of on-demand queries", eplStatement);
        }
        for (ExprSubselectNode subselectNode : visitor.getSubselects()) {
            if (!subselectNodes.contains(subselectNode)) {
                subselectNodes.add(subselectNode);
            }
        }

        // Compile subselects found
        int subselectNumber = 0;
        for (ExprSubselectNode subselect : subselectNodes)
        {
            StatementSpecRaw raw = subselect.getStatementSpecRaw();
            StatementSpecCompiled compiled = compile(raw, eplStatement, statementContext, true, isOnDemandQuery, new Annotation[0], Collections.<ExprSubselectNode>emptyList(), Collections.<ExprDeclaredNode>emptyList(), servicesContext);
            subselectNumber++;
            subselect.setStatementSpecCompiled(compiled, subselectNumber);
        }

        // compile each stream used
        try
        {
            compiledStreams = new ArrayList<StreamSpecCompiled>(spec.getStreamSpecs().size());
            int streamNum = 0;
            for (StreamSpecRaw rawSpec : spec.getStreamSpecs())
            {
                streamNum++;
                StreamSpecCompiled compiled = rawSpec.compile(statementContext, eventTypeReferences, spec.getInsertIntoDesc() != null, Collections.singleton(streamNum), spec.getStreamSpecs().size() > 1, false, spec.getOnTriggerDesc() != null);
                compiledStreams.add(compiled);
            }
        }
        catch (ExprValidationException ex)
        {
            log.info("Failed to compile statement: " + ex.getMessage(), ex);
            if (ex.getMessage() == null)
            {
                throw new EPStatementException("Unexpected exception compiling statement, please consult the log file and report the exception", eplStatement);
            }
            else
            {
                throw new EPStatementException(ex.getMessage(), eplStatement);
            }
        }
        catch (RuntimeException ex)
        {
            String text = "Unexpected error compiling statement";
            log.error(text, ex);
            throw new EPStatementException(text + ": " + ex.getClass().getName() + ":" + ex.getMessage(), eplStatement);
        }

        // for create window statements, we switch the filter to a new event type
        if (spec.getCreateWindowDesc() != null)
        {
            try
            {
                StreamSpecCompiled createWindowTypeSpec = compiledStreams.get(0);
                EventType selectFromType;
                String selectFromTypeName;
                if (createWindowTypeSpec instanceof FilterStreamSpecCompiled)
                {
                    FilterStreamSpecCompiled filterStreamSpec = (FilterStreamSpecCompiled) createWindowTypeSpec;
                    selectFromType = filterStreamSpec.getFilterSpec().getFilterForEventType();
                    selectFromTypeName = filterStreamSpec.getFilterSpec().getFilterForEventTypeName();

                    if (spec.getCreateWindowDesc().isInsert() || spec.getCreateWindowDesc().getInsertFilter() != null)
                    {
                        throw new EPStatementException("A named window by name '" + selectFromTypeName + "' could not be located, use the insert-keyword with an existing named window", eplStatement);
                    }
                }
                else
                {
                    NamedWindowConsumerStreamSpec consumerStreamSpec = (NamedWindowConsumerStreamSpec) createWindowTypeSpec;
                    selectFromType = statementContext.getEventAdapterService().getExistsTypeByName(consumerStreamSpec.getWindowName());
                    selectFromTypeName = consumerStreamSpec.getWindowName();

                    if (spec.getCreateWindowDesc().getInsertFilter() != null)
                    {
                        ExprNode insertIntoFilter = spec.getCreateWindowDesc().getInsertFilter();
                        String checkMinimal = ExprNodeUtility.isMinimalExpression(insertIntoFilter);
                        if (checkMinimal != null)
                        {
                            throw new ExprValidationException("Create window where-clause may not have " + checkMinimal);
                        }
                        StreamTypeService streamTypeService = new StreamTypeServiceImpl(selectFromType, selectFromTypeName, true, statementContext.getEngineURI());
                        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
                        ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, statementContext.getMethodResolutionService(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);
                        ExprNode insertFilter = ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.CREATEWINDOWFILTER, spec.getCreateWindowDesc().getInsertFilter(), validationContext);
                        spec.getCreateWindowDesc().setInsertFilter(insertFilter);
                    }

                    // set the window to insert from
                    spec.getCreateWindowDesc().setInsertFromWindow(consumerStreamSpec.getWindowName());
                }
                Pair<FilterSpecCompiled, SelectClauseSpecRaw> newFilter = handleCreateWindow(selectFromType, selectFromTypeName, spec.getCreateWindowDesc().getColumns(), spec, eplStatement, statementContext, servicesContext);
                eventTypeReferences.add(((EventTypeSPI)newFilter.getFirst().getFilterForEventType()).getMetadata().getPrimaryName());

                // view must be non-empty list
                if (spec.getCreateWindowDesc().getViewSpecs().isEmpty())
                {
                    throw new ExprValidationException(NamedWindowService.ERROR_MSG_DATAWINDOWS);
                }

                // use the filter specification of the newly created event type and the views for the named window
                compiledStreams.clear();
                ViewSpec[] views = ViewSpec.toArray(spec.getCreateWindowDesc().getViewSpecs());
                compiledStreams.add(new FilterStreamSpecCompiled(newFilter.getFirst(), views, null, spec.getCreateWindowDesc().getStreamSpecOptions()));
                spec.setSelectClauseSpec(newFilter.getSecond());
            }
            catch (ExprValidationException e)
            {
                throw new EPStatementException(e.getMessage(), eplStatement);
            }
        }

        return new StatementSpecCompiled(
                spec.getOnTriggerDesc(),
                spec.getCreateWindowDesc(),
                spec.getCreateIndexDesc(),
                spec.getCreateVariableDesc(),
                spec.getCreateTableDesc(),
                spec.getCreateSchemaDesc(),
                spec.getInsertIntoDesc(),
                spec.getSelectStreamSelectorEnum(),
                selectClauseCompiled,
                compiledStreams.toArray(new StreamSpecCompiled[compiledStreams.size()]),
                OuterJoinDesc.toArray(spec.getOuterJoinDescList()),
                spec.getFilterRootNode(),
                spec.getHavingExprRootNode(),
                spec.getOutputLimitSpec(),
                OrderByItem.toArray(spec.getOrderByList()),
                ExprSubselectNode.toArray(subselectNodes),
                ExprNodeUtility.toArray(declaredNodes),
                spec.getReferencedVariables(),
                spec.getRowLimitSpec(),
                CollectionUtil.toArray(eventTypeReferences),
                annotations,
                spec.getUpdateDesc(),
                spec.getMatchRecognizeSpec(),
                spec.getForClauseSpec(),
                spec.getSqlParameters(),
                spec.getCreateContextDesc(),
                spec.getOptionalContextName(),
                spec.getCreateDataFlowDesc(),
                spec.getCreateExpressionDesc(),
                spec.getFireAndForgetSpec(),
                groupByRollupExpressions,
                spec.getIntoTableSpec(),
                spec.getTableExpressions() == null ? null : spec.getTableExpressions().toArray(new ExprTableAccessNode[spec.getTableExpressions().size()]));
    }

    private static boolean determineStatelessSelect(StatementType type, StatementSpecRaw spec, boolean hasSubselects, boolean isPattern) {

        if (hasSubselects || isPattern) {
            return false;
        }
        if (type != StatementType.SELECT && type != StatementType.INSERT_INTO) {
            return false;
        }
        if (spec.getStreamSpecs() == null || spec.getStreamSpecs().size() > 1 || spec.getStreamSpecs().isEmpty()) {
            return false;
        }
        StreamSpecRaw singleStream = spec.getStreamSpecs().get(0);
        if (!(singleStream instanceof FilterStreamSpecRaw) && !(singleStream instanceof NamedWindowConsumerStreamSpec)) {
            return false;
        }
        if (singleStream.getViewSpecs() != null && singleStream.getViewSpecs().length > 0) {
            return false;
        }
        if (spec.getOutputLimitSpec() != null) {
            return false;
        }
        if (spec.getMatchRecognizeSpec() != null) {
            return false;
        }

        List<ExprNode> expressions = StatementSpecRawAnalyzer.collectExpressionsShallow(spec);
        if (expressions.isEmpty()) {
            return true;
        }

        ExprNodeSummaryVisitor visitor = new ExprNodeSummaryVisitor();
        for (ExprNode expr : expressions) {
            if (expr == null) {
                continue;
            }
            expr.accept(visitor);
        }

        return !visitor.isHasAggregation() && !visitor.isHasPreviousPrior() && !visitor.isHasSubselect();
    }

    private static void rewriteNamedWindowSubselect(List<ExprDotNode> chainedExpressionsDot, List<ExprSubselectNode> subselects, NamedWindowService service) {
        for (ExprDotNode dotNode : chainedExpressionsDot) {
            String proposedWindow = dotNode.getChainSpec().get(0).getName();
            if (!service.isNamedWindow(proposedWindow)) {
                continue;
            }

            // build spec for subselect
            StatementSpecRaw raw = new StatementSpecRaw(SelectClauseStreamSelectorEnum.ISTREAM_ONLY);
            FilterSpecRaw filter = new FilterSpecRaw(proposedWindow, Collections.<ExprNode>emptyList(), null);
            raw.getStreamSpecs().add(new FilterStreamSpecRaw(filter, ViewSpec.EMPTY_VIEWSPEC_ARRAY, proposedWindow, new StreamSpecOptions()));

            ExprChainedSpec firstChain = dotNode.getChainSpec().remove(0);
            if (!firstChain.getParameters().isEmpty()) {
                if (firstChain.getParameters().size() == 1) {
                    raw.setFilterExprRootNode(firstChain.getParameters().get(0));
                }
                else {
                    ExprAndNode andNode = new ExprAndNodeImpl();
                    for (ExprNode node : firstChain.getParameters()) {
                        andNode.addChildNode(node);
                    }
                    raw.setFilterExprRootNode(andNode);
                }
            }

            // activate subselect
            ExprSubselectNode subselect = new ExprSubselectRowNode(raw);
            subselects.add(subselect);
            dotNode.setChildNodes(subselect);
        }
    }

    /**
     * Compile a select clause allowing subselects.
     * @param spec to compile
     * @return select clause compiled
     * @throws ExprValidationException when validation fails
     */
    public static SelectClauseSpecCompiled compileSelectAllowSubselect(SelectClauseSpecRaw spec) throws ExprValidationException
    {
        // Look for expressions with sub-selects in select expression list and filter expression
        // Recursively compile the statement within the statement.
        ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
        List<SelectClauseElementCompiled> selectElements = new ArrayList<SelectClauseElementCompiled>();
        for (SelectClauseElementRaw raw : spec.getSelectExprList())
        {
            if (raw instanceof SelectClauseExprRawSpec)
            {
                SelectClauseExprRawSpec rawExpr = (SelectClauseExprRawSpec) raw;
                rawExpr.getSelectExpression().accept(visitor);
                selectElements.add(new SelectClauseExprCompiledSpec(rawExpr.getSelectExpression(), rawExpr.getOptionalAsName(), rawExpr.getOptionalAsName(), rawExpr.isEvents()));
            }
            else if (raw instanceof SelectClauseStreamRawSpec)
            {
                SelectClauseStreamRawSpec rawExpr = (SelectClauseStreamRawSpec) raw;
                selectElements.add(new SelectClauseStreamCompiledSpec(rawExpr.getStreamName(), rawExpr.getOptionalAsName()));
            }
            else if (raw instanceof SelectClauseElementWildcard)
            {
                SelectClauseElementWildcard wildcard = (SelectClauseElementWildcard) raw;
                selectElements.add(wildcard);
            }
            else
            {
                throw new IllegalStateException("Unexpected select clause element class : " + raw.getClass().getName());
            }
        }
        return new SelectClauseSpecCompiled(selectElements.toArray(new SelectClauseElementCompiled[selectElements.size()]), spec.isDistinct());
    }

    // The create window command:
    //      create window windowName[.window_view_list] as [select properties from] type
    //
    // This section expected s single FilterStreamSpecCompiled representing the selected type.
    // It creates a new event type representing the window type and a sets the type selected on the filter stream spec.
    private static Pair<FilterSpecCompiled, SelectClauseSpecRaw> handleCreateWindow(EventType selectFromType,
                                           String selectFromTypeName,
                                           List<ColumnDesc> columns,
                                           StatementSpecRaw spec,
                                           String eplStatement,
                                           StatementContext statementContext,
                                           EPServicesContext servicesContext)
            throws ExprValidationException
    {
        String typeName = spec.getCreateWindowDesc().getWindowName();
        EventType targetType;

        // Validate the select expressions which consists of properties only
        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
        List<NamedWindowSelectedProps> select = compileLimitedSelect(spec.getSelectClauseSpec(), eplStatement, selectFromType, selectFromTypeName, statementContext.getEngineURI(), evaluatorContextStmt, statementContext.getMethodResolutionService(), statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations());

        // Create Map or Wrapper event type from the select clause of the window.
        // If no columns selected, simply create a wrapper type
        // Build a list of properties
        SelectClauseSpecRaw newSelectClauseSpecRaw = new SelectClauseSpecRaw();
        LinkedHashMap<String, Object> properties;
        boolean hasProperties = false;
        if ((columns != null) && (!columns.isEmpty())) {
            properties = EventTypeUtility.buildType(columns, statementContext.getEventAdapterService(), null, statementContext.getMethodResolutionService().getEngineImportService());
            hasProperties = true;
        }
        else {
            properties = new LinkedHashMap<String, Object>();
            for (NamedWindowSelectedProps selectElement : select)
            {
                if (selectElement.getFragmentType() != null)
                {
                    properties.put(selectElement.getAssignedName(), selectElement.getFragmentType());
                }
                else
                {
                    properties.put(selectElement.getAssignedName(), selectElement.getSelectExpressionType());
                }

                // Add any properties to the new select clause for use by consumers to the statement itself
                newSelectClauseSpecRaw.add(new SelectClauseExprRawSpec(new ExprIdentNodeImpl(selectElement.getAssignedName()), null, false));
                hasProperties = true;
            }
        }

        // Create Map or Wrapper event type from the select clause of the window.
        // If no columns selected, simply create a wrapper type
        boolean isOnlyWildcard = spec.getSelectClauseSpec().isOnlyWildcard();
        boolean isWildcard = spec.getSelectClauseSpec().isUsingWildcard();
        if (statementContext.getValueAddEventService().isRevisionTypeName(selectFromTypeName))
        {
            targetType = statementContext.getValueAddEventService().createRevisionType(typeName, selectFromTypeName, statementContext.getStatementStopService(), statementContext.getEventAdapterService(), servicesContext.getEventTypeIdGenerator());
        }
        else if (isWildcard && !isOnlyWildcard)
        {
            targetType = statementContext.getEventAdapterService().addWrapperType(typeName, selectFromType, properties, true, false);
        }
        else
        {
            // Some columns selected, use the types of the columns
            if (hasProperties && !isOnlyWildcard)
            {
                Map<String, Object> compiledProperties = EventTypeUtility.compileMapTypeProperties(properties, statementContext.getEventAdapterService());
                boolean mapType = EventRepresentationUtil.isMap(statementContext.getAnnotations(), servicesContext.getConfigSnapshot(), CreateSchemaDesc.AssignedType.NONE);
                if (mapType) {
                    targetType = statementContext.getEventAdapterService().addNestableMapType(typeName, compiledProperties, null, false, false, false, true, false);
                }
                else {
                    targetType = statementContext.getEventAdapterService().addNestableObjectArrayType(typeName, compiledProperties, null, false, false, false, true, false, false, null);
                }
            }
            else
            {
                // No columns selected, no wildcard, use the type as is or as a wrapped type
                if (selectFromType instanceof ObjectArrayEventType)
                {
                    ObjectArrayEventType objectArrayEventType = (ObjectArrayEventType) selectFromType;
                    targetType = statementContext.getEventAdapterService().addNestableObjectArrayType(typeName, objectArrayEventType.getTypes(), null, false, false, false, true, false, false, null);
                }
                else if (selectFromType instanceof MapEventType)
                {
                    MapEventType mapType = (MapEventType) selectFromType;
                    targetType = statementContext.getEventAdapterService().addNestableMapType(typeName, mapType.getTypes(), null, false, false, false, true, false);
                }
                else if (selectFromType instanceof BeanEventType)
                {
                    BeanEventType beanType = (BeanEventType) selectFromType;
                    targetType = statementContext.getEventAdapterService().addBeanTypeByName(typeName, beanType.getUnderlyingType(), true);
                }
                else
                {
                    Map<String, Object> addOnTypes = new HashMap<String, Object>();
                    targetType = statementContext.getEventAdapterService().addWrapperType(typeName, selectFromType, addOnTypes, true, false);
                }
            }
        }

        FilterSpecCompiled filter = new FilterSpecCompiled(targetType, typeName, new List[0], null);
        return new Pair<FilterSpecCompiled, SelectClauseSpecRaw>(filter, newSelectClauseSpecRaw);
    }

    private static void assignFilterSpecIds(FilterSpecCompiled filterSpec, FilterSpecCompiled[] filterSpecsAll) {
        for (int path = 0; path < filterSpec.getParameters().length; path++) {
            for (FilterSpecParam param : filterSpec.getParameters()[path]) {
                if (param instanceof FilterSpecParamExprNode) {
                    int index = filterSpec.getFilterSpecIndexAmongAll(filterSpecsAll);
                    FilterSpecParamExprNode exprNode = (FilterSpecParamExprNode) param;
                    exprNode.setFilterSpecId(index);
                    exprNode.setFilterSpecParamPathNum(path);
                }
            }
        }
    }

    private static List<NamedWindowSelectedProps> compileLimitedSelect(SelectClauseSpecRaw spec, String eplStatement, EventType singleType, String selectFromTypeName, String engineURI, ExprEvaluatorContext exprEvaluatorContext, MethodResolutionService methodResolutionService, EventAdapterService eventAdapterService, String statementName, String statementId, Annotation[] annotations)
    {
        List<NamedWindowSelectedProps> selectProps = new LinkedList<NamedWindowSelectedProps>();
        StreamTypeService streams = new StreamTypeServiceImpl(new EventType[] {singleType}, new String[] {"stream_0"}, new boolean[] {false}, engineURI, false);

        ExprValidationContext validationContext = new ExprValidationContext(streams, methodResolutionService, null, null, null, null, exprEvaluatorContext, eventAdapterService, statementName, statementId, annotations, null, false, false, false, false, null, false);
        for (SelectClauseElementRaw raw : spec.getSelectExprList())
        {
            if (!(raw instanceof SelectClauseExprRawSpec))
            {
                continue;
            }
            SelectClauseExprRawSpec exprSpec = (SelectClauseExprRawSpec) raw;
            ExprNode validatedExpression;
            try
            {
                validatedExpression = ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.SELECT, exprSpec.getSelectExpression(), validationContext);
            }
            catch (ExprValidationException e)
            {
                throw new EPStatementException(e.getMessage(), e, eplStatement);
            }

            // determine an element name if none assigned
            String asName = exprSpec.getOptionalAsName();
            if (asName == null)
            {
                asName = ExprNodeUtility.toExpressionStringMinPrecedenceSafe(validatedExpression);
            }

            // check for fragments
            EventType fragmentType = null;
            if ((validatedExpression instanceof ExprIdentNode) && (!(singleType instanceof NativeEventType)))
            {
                ExprIdentNode identNode = (ExprIdentNode) validatedExpression;
                FragmentEventType fragmentEventType = singleType.getFragmentType(identNode.getFullUnresolvedName());
                if ((fragmentEventType != null) && (!fragmentEventType.isNative()))
                {
                    fragmentType = fragmentEventType.getFragmentType();
                }
            }

            NamedWindowSelectedProps validatedElement = new NamedWindowSelectedProps(validatedExpression.getExprEvaluator().getType(), asName, fragmentType);
            selectProps.add(validatedElement);
        }

        return selectProps;
    }

    public void dispatchStatementLifecycleEvent(StatementLifecycleEvent theEvent)
    {
        for (StatementLifecycleObserver observer : observers)
        {
            observer.observe(theEvent);
        }
    }

    /**
     * Statement information.
     */
    public static class EPStatementDesc
    {
        private final EPStatementSPI epStatement;
        private final EPStatementStartMethod startMethod;
        private final StatementContext statementContext;

        private EPStatementStopMethod stopMethod;
        private EPStatementDestroyMethod destroyMethod;
        /**
         * Ctor.
         * @param epStatement the statement
         * @param startMethod the start method
         * @param statementContext statement context
         */
        public EPStatementDesc(EPStatementSPI epStatement, EPStatementStartMethod startMethod, StatementContext statementContext)
        {
            this.epStatement = epStatement;
            this.startMethod = startMethod;
            this.statementContext = statementContext;
        }

        /**
         * Returns the statement.
         * @return statement.
         */
        public EPStatementSPI getEpStatement()
        {
            return epStatement;
        }

        /**
         * Returns the start method.
         * @return start method
         */
        public EPStatementStartMethod getStartMethod()
        {
            return startMethod;
        }

        /**
         * Returns the stop method.
         * @return stop method
         */
        public EPStatementStopMethod getStopMethod()
        {
            return stopMethod;
        }

        /**
         * Sets the stop method.
         * @param stopMethod to set
         */
        public void setStopMethod(EPStatementStopMethod stopMethod)
        {
            this.stopMethod = stopMethod;
        }

        /**
         * Returns the statement context.
         * @return statement context
         */
        public StatementContext getStatementContext()
        {
            return statementContext;
        }

        /**
         * Set method to call when destroyed.
         * @param destroyMethod method
         */
        public void setDestroyMethod(EPStatementDestroyMethod destroyMethod) {
            this.destroyMethod = destroyMethod;
        }

        /**
         * Return destroy method.
         * @return method.
         */
        public EPStatementDestroyMethod getDestroyMethod() {
            return destroyMethod;
        }
    }
}
