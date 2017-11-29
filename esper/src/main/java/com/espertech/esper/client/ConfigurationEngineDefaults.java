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
package com.espertech.esper.client;

import com.espertech.esper.client.hook.ObjectValueTypeWidenerFactory;
import com.espertech.esper.client.hook.TypeRepresentationMapper;
import com.espertech.esper.client.soda.StreamSelector;
import com.espertech.esper.client.util.EventUnderlyingType;

import java.io.Serializable;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Provides access to engine configuration defaults for modification.
 */
public class ConfigurationEngineDefaults implements Serializable {
    private static final long serialVersionUID = -528835191586154300L;

    private Threading threading;
    private ViewResources viewResources;
    private EventMeta eventMeta;
    private Logging logging;
    private Variables variables;
    private StreamSelection streamSelection;
    private TimeSource timeSource;
    private Language language;
    private Expression expression;
    private Execution execution;
    private ExceptionHandling exceptionHandling;
    private ConditionHandling conditionHandling;
    private ConfigurationMetricsReporting metricsReporting;
    private AlternativeContext alternativeContext;
    private Patterns patterns;
    private MatchRecognize matchRecognize;
    private Scripts scripts;
    private ByteCodeGeneration byteCodeGeneration;

    /**
     * Ctor.
     */
    protected ConfigurationEngineDefaults() {
        threading = new Threading();
        viewResources = new ViewResources();
        eventMeta = new EventMeta();
        logging = new Logging();
        variables = new Variables();
        streamSelection = new StreamSelection();
        timeSource = new TimeSource();
        metricsReporting = new ConfigurationMetricsReporting();
        language = new Language();
        expression = new Expression();
        execution = new Execution();
        exceptionHandling = new ExceptionHandling();
        conditionHandling = new ConditionHandling();
        alternativeContext = new AlternativeContext();
        patterns = new Patterns();
        matchRecognize = new MatchRecognize();
        scripts = new Scripts();
        byteCodeGeneration = new ByteCodeGeneration();
    }

    /**
     * Returns threading settings.
     *
     * @return threading settings object
     */
    public Threading getThreading() {
        return threading;
    }

    /**
     * Returns view resources defaults.
     *
     * @return view resources defaults
     */
    public ViewResources getViewResources() {
        return viewResources;
    }

    /**
     * Returns event representation default settings.
     *
     * @return event representation default settings
     */
    public EventMeta getEventMeta() {
        return eventMeta;
    }

    /**
     * Returns logging settings applicable to the engine, other then Log4J settings.
     *
     * @return logging settings
     */
    public Logging getLogging() {
        return logging;
    }

    /**
     * Returns engine defaults applicable to variables.
     *
     * @return variable engine defaults
     */
    public Variables getVariables() {
        return variables;
    }

    /**
     * Returns engine defaults applicable to streams (insert and remove, insert only or remove only) selected for a statement.
     *
     * @return stream selection defaults
     */
    public StreamSelection getStreamSelection() {
        return streamSelection;
    }

    /**
     * Returns the time source configuration.
     *
     * @return time source enum
     */
    public TimeSource getTimeSource() {
        return timeSource;
    }

    /**
     * Returns the metrics reporting configuration.
     *
     * @return metrics reporting config
     */
    public ConfigurationMetricsReporting getMetricsReporting() {
        return metricsReporting;
    }

    /**
     * Returns the language-related settings for the engine.
     *
     * @return language-related settings
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * Returns the expression-related settings for the engine.
     *
     * @return expression-related settings
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Returns statement execution-related settings, settings that
     * influence event/schedule to statement processing.
     *
     * @return execution settings
     */
    public Execution getExecution() {
        return execution;
    }

    /**
     * For software-provider-interface use.
     *
     * @return alternative context
     */
    public AlternativeContext getAlternativeContext() {
        return alternativeContext;
    }

    /**
     * For software-provider-interface use.
     *
     * @param alternativeContext alternative context
     */
    public void setAlternativeContext(AlternativeContext alternativeContext) {
        this.alternativeContext = alternativeContext;
    }

    /**
     * Returns the exception handling configuration.
     *
     * @return exception handling configuration
     */
    public ExceptionHandling getExceptionHandling() {
        return exceptionHandling;
    }

    /**
     * Sets the exception handling configuration.
     *
     * @param exceptionHandling exception handling configuration
     */
    public void setExceptionHandling(ExceptionHandling exceptionHandling) {
        this.exceptionHandling = exceptionHandling;
    }

    /**
     * Returns the condition handling configuration.
     *
     * @return condition handling configuration
     */
    public ConditionHandling getConditionHandling() {
        return conditionHandling;
    }

    /**
     * Sets the condition handling configuration.
     *
     * @param conditionHandling exception handling configuration
     */
    public void setConditionHandling(ConditionHandling conditionHandling) {
        this.conditionHandling = conditionHandling;
    }

    /**
     * Return pattern settings.
     *
     * @return pattern settings
     */
    public Patterns getPatterns() {
        return patterns;
    }

    /**
     * Sets pattern settings.
     *
     * @param patterns settings to set
     */
    public void setPatterns(Patterns patterns) {
        this.patterns = patterns;
    }

    /**
     * Return match-recognize settings.
     *
     * @return match-recognize settings
     */
    public MatchRecognize getMatchRecognize() {
        return matchRecognize;
    }

    /**
     * Sets match-recognize settings.
     *
     * @param matchRecognize settings to set
     */
    public void setMatchRecognize(MatchRecognize matchRecognize) {
        this.matchRecognize = matchRecognize;
    }

    /**
     * Returns script engine settings.
     *
     * @return script engine settings
     */
    public Scripts getScripts() {
        return scripts;
    }

    /**
     * Sets script engine settings.
     *
     * @param scripts script engine settings
     */
    public void setScripts(Scripts scripts) {
        this.scripts = scripts;
    }

    /**
     * Returns code generation settings
     * @return code generation settings
     */
    public ByteCodeGeneration getByteCodeGeneration() {
        return byteCodeGeneration;
    }

    /**
     * Sets code generation settings
     * @param byteCodeGeneration settings
     */
    public void setByteCodeGeneration(ByteCodeGeneration byteCodeGeneration) {
        this.byteCodeGeneration = byteCodeGeneration;
    }

    /**
     * Holds threading settings.
     */
    public static class Threading implements Serializable {
        private static final long serialVersionUID = 6504606101119059962L;

        private boolean isListenerDispatchPreserveOrder;
        private long listenerDispatchTimeout;
        private Locking listenerDispatchLocking;

        private boolean isInsertIntoDispatchPreserveOrder;
        private long insertIntoDispatchTimeout;
        private Locking insertIntoDispatchLocking;

        private boolean isNamedWindowConsumerDispatchPreserveOrder;
        private long namedWindowConsumerDispatchTimeout;
        private Locking namedWindowConsumerDispatchLocking;

        private long internalTimerMsecResolution;
        private boolean internalTimerEnabled;

        private boolean isThreadPoolTimerExec;
        private boolean isThreadPoolInbound;
        private boolean isThreadPoolRouteExec;
        private boolean isThreadPoolOutbound;
        private int threadPoolTimerExecNumThreads;
        private int threadPoolInboundNumThreads;
        private int threadPoolRouteExecNumThreads;
        private int threadPoolOutboundNumThreads;
        private Integer threadPoolTimerExecCapacity;
        private Integer threadPoolInboundCapacity;
        private Integer threadPoolRouteExecCapacity;
        private Integer threadPoolOutboundCapacity;

        private boolean engineFairlock;

        /**
         * Ctor - sets up defaults.
         */
        public Threading() {
            listenerDispatchTimeout = 1000;
            isListenerDispatchPreserveOrder = true;
            listenerDispatchLocking = Locking.SPIN;

            insertIntoDispatchTimeout = 100;
            isInsertIntoDispatchPreserveOrder = true;
            insertIntoDispatchLocking = Locking.SPIN;

            namedWindowConsumerDispatchTimeout = Long.MAX_VALUE;
            isNamedWindowConsumerDispatchPreserveOrder = true;
            namedWindowConsumerDispatchLocking = Locking.SPIN;

            internalTimerEnabled = true;
            internalTimerMsecResolution = 100;

            isThreadPoolInbound = false;
            isThreadPoolOutbound = false;
            isThreadPoolRouteExec = false;
            isThreadPoolTimerExec = false;

            threadPoolTimerExecNumThreads = 2;
            threadPoolInboundNumThreads = 2;
            threadPoolRouteExecNumThreads = 2;
            threadPoolOutboundNumThreads = 2;
        }

        /**
         * In multithreaded environments, this setting controls whether dispatches to listeners preserve
         * the ordering in which the statement processes events.
         *
         * @param value is true to preserve ordering, or false if not
         */
        public void setListenerDispatchPreserveOrder(boolean value) {
            isListenerDispatchPreserveOrder = value;
        }

        /**
         * In multithreaded environments, this setting controls when dispatches to listeners preserve
         * the ordering the timeout to complete any outstanding dispatches.
         *
         * @param value is the timeout in milliseconds that the engine may spend
         *              waiting for a listener dispatch to complete before dispatching further
         *              results for the same statement to listeners for that statement
         */
        public void setListenerDispatchTimeout(long value) {
            listenerDispatchTimeout = value;
        }

        /**
         * In multithreaded environments, this setting controls whether insert-into streams preserve
         * the order of events inserted into them by one or more statements
         * such that statements that consume other statement's events behave deterministic
         *
         * @param value is true to indicate to preserve order, or false to not preserve order
         */
        public void setInsertIntoDispatchPreserveOrder(boolean value) {
            isInsertIntoDispatchPreserveOrder = value;
        }

        /**
         * Returns true to indicate preserve order for dispatch to listeners,
         * or false to indicate not to preserve order
         *
         * @return true or false
         */
        public boolean isListenerDispatchPreserveOrder() {
            return isListenerDispatchPreserveOrder;
        }

        /**
         * Returns the timeout in millisecond to wait for listener code to complete
         * before dispatching the next result, if dispatch order is preserved
         *
         * @return listener dispatch timeout
         */
        public long getListenerDispatchTimeout() {
            return listenerDispatchTimeout;
        }

        /**
         * Returns true to indicate preserve order for inter-statement insert-into,
         * or false to indicate not to preserve order
         *
         * @return true or false
         */
        public boolean isInsertIntoDispatchPreserveOrder() {
            return isInsertIntoDispatchPreserveOrder;
        }

        /**
         * Sets the use of internal timer.
         * <p>
         * By setting internal timer to true (the default) the engine starts the internal timer thread
         * and relies on internal timer events to supply the time.
         * <p>
         * By setting internal timer to false the engine does not start the internal timer thread
         * and relies on external application-supplied timer events to supply the time.
         *
         * @param internalTimerEnabled is true for internal timer enabled, or false if the application supplies timer events
         */
        public void setInternalTimerEnabled(boolean internalTimerEnabled) {
            this.internalTimerEnabled = internalTimerEnabled;
        }

        /**
         * Returns true if internal timer is enabled (the default), or false for internal timer disabled.
         *
         * @return true for internal timer enabled, false for internal timer disabled
         */
        public boolean isInternalTimerEnabled() {
            return internalTimerEnabled;
        }

        /**
         * Returns the millisecond resolutuion of the internal timer thread.
         *
         * @return number of msec between timer processing intervals
         */
        public long getInternalTimerMsecResolution() {
            return internalTimerMsecResolution;
        }

        /**
         * Sets the length of the interval (resolution) of the timer thread.
         *
         * @param internalTimerMsecResolution is the millisecond interval length
         */
        public void setInternalTimerMsecResolution(long internalTimerMsecResolution) {
            this.internalTimerMsecResolution = internalTimerMsecResolution;
        }

        /**
         * Returns the number of milliseconds that a thread may maximually be blocking
         * to deliver statement results from a producing statement that employs insert-into
         * to a consuming statement.
         *
         * @return millisecond timeout for order-of-delivery blocking between statements
         */
        public long getInsertIntoDispatchTimeout() {
            return insertIntoDispatchTimeout;
        }

        /**
         * Sets the blocking strategy to use when multiple threads deliver results for
         * a single statement to listeners, and the guarantee of order of delivery must be maintained.
         *
         * @param listenerDispatchLocking is the blocking technique
         */
        public void setListenerDispatchLocking(Locking listenerDispatchLocking) {
            this.listenerDispatchLocking = listenerDispatchLocking;
        }

        /**
         * Sets the number of milliseconds that a thread may maximually be blocking
         * to deliver statement results from a producing statement that employs insert-into
         * to a consuming statement.
         *
         * @param msecTimeout timeout for order-of-delivery blocking between statements
         */
        public void setInsertIntoDispatchTimeout(long msecTimeout) {
            this.insertIntoDispatchTimeout = msecTimeout;
        }

        /**
         * Sets the blocking strategy to use when multiple threads deliver results for
         * a single statement to consuming statements of an insert-into, and the guarantee of order of delivery must be maintained.
         *
         * @param insertIntoDispatchLocking is the blocking technique
         */
        public void setInsertIntoDispatchLocking(Locking insertIntoDispatchLocking) {
            this.insertIntoDispatchLocking = insertIntoDispatchLocking;
        }

        /**
         * Returns the blocking strategy to use when multiple threads deliver results for
         * a single statement to listeners, and the guarantee of order of delivery must be maintained.
         *
         * @return is the blocking technique
         */
        public Locking getListenerDispatchLocking() {
            return listenerDispatchLocking;
        }

        /**
         * Returns the blocking strategy to use when multiple threads deliver results for
         * a single statement to consuming statements of an insert-into, and the guarantee of order of delivery must be maintained.
         *
         * @return is the blocking technique
         */
        public Locking getInsertIntoDispatchLocking() {
            return insertIntoDispatchLocking;
        }

        /**
         * Returns true for inbound threading enabled, the default is false for not enabled.
         *
         * @return indicator whether inbound threading is enabled
         */
        public boolean isThreadPoolInbound() {
            return isThreadPoolInbound;
        }

        /**
         * Set to true for inbound threading enabled, the default is false for not enabled.
         *
         * @param threadPoolInbound indicator whether inbound threading is enabled
         */
        public void setThreadPoolInbound(boolean threadPoolInbound) {
            isThreadPoolInbound = threadPoolInbound;
        }

        /**
         * Returns true for timer execution threading enabled, the default is false for not enabled.
         *
         * @return indicator whether timer execution threading is enabled
         */
        public boolean isThreadPoolTimerExec() {
            return isThreadPoolTimerExec;
        }

        /**
         * Set to true for timer execution threading enabled, the default is false for not enabled.
         *
         * @param threadPoolTimerExec indicator whether timer execution threading is enabled
         */
        public void setThreadPoolTimerExec(boolean threadPoolTimerExec) {
            isThreadPoolTimerExec = threadPoolTimerExec;
        }

        /**
         * Returns true for route execution threading enabled, the default is false for not enabled.
         *
         * @return indicator whether route execution threading is enabled
         */
        public boolean isThreadPoolRouteExec() {
            return isThreadPoolRouteExec;
        }

        /**
         * Set to true for route execution threading enabled, the default is false for not enabled.
         *
         * @param threadPoolRouteExec indicator whether route execution threading is enabled
         */
        public void setThreadPoolRouteExec(boolean threadPoolRouteExec) {
            isThreadPoolRouteExec = threadPoolRouteExec;
        }

        /**
         * Returns true for outbound threading enabled, the default is false for not enabled.
         *
         * @return indicator whether outbound threading is enabled
         */
        public boolean isThreadPoolOutbound() {
            return isThreadPoolOutbound;
        }

        /**
         * Set to true for outbound threading enabled, the default is false for not enabled.
         *
         * @param threadPoolOutbound indicator whether outbound threading is enabled
         */
        public void setThreadPoolOutbound(boolean threadPoolOutbound) {
            isThreadPoolOutbound = threadPoolOutbound;
        }

        /**
         * Returns the number of thread in the inbound threading pool.
         *
         * @return number of threads
         */
        public int getThreadPoolInboundNumThreads() {
            return threadPoolInboundNumThreads;
        }

        /**
         * Sets the number of threads in the thread pool for inbound threading.
         *
         * @param num number of threads
         */
        public void setThreadPoolInboundNumThreads(int num) {
            this.threadPoolInboundNumThreads = num;
        }

        /**
         * Returns the number of thread in the outbound threading pool.
         *
         * @return number of threads
         */
        public int getThreadPoolOutboundNumThreads() {
            return threadPoolOutboundNumThreads;
        }

        /**
         * Sets the number of threads in the thread pool for outbound threading.
         *
         * @param num number of threads
         */
        public void setThreadPoolOutboundNumThreads(int num) {
            this.threadPoolOutboundNumThreads = num;
        }

        /**
         * Returns the number of thread in the route execution thread pool.
         *
         * @return number of threads
         */
        public int getThreadPoolRouteExecNumThreads() {
            return threadPoolRouteExecNumThreads;
        }

        /**
         * Sets the number of threads in the thread pool for route exec threading.
         *
         * @param num number of threads
         */
        public void setThreadPoolRouteExecNumThreads(int num) {
            this.threadPoolRouteExecNumThreads = num;
        }

        /**
         * Returns the number of thread in the timer execution threading pool.
         *
         * @return number of threads
         */
        public int getThreadPoolTimerExecNumThreads() {
            return threadPoolTimerExecNumThreads;
        }

        /**
         * Sets the number of threads in the thread pool for timer exec threading.
         *
         * @param num number of threads
         */
        public void setThreadPoolTimerExecNumThreads(int num) {
            this.threadPoolTimerExecNumThreads = num;
        }

        /**
         * Returns the capacity of the timer execution queue, or null if none defined (the unbounded case, default).
         *
         * @return capacity or null if none defined
         */
        public Integer getThreadPoolTimerExecCapacity() {
            return threadPoolTimerExecCapacity;
        }

        /**
         * Sets the capacity of the timer execution queue, or null if none defined (the unbounded case, default).
         *
         * @param capacity capacity or null if none defined
         */
        public void setThreadPoolTimerExecCapacity(Integer capacity) {
            this.threadPoolTimerExecCapacity = capacity;
        }

        /**
         * Returns the capacity of the inbound execution queue, or null if none defined (the unbounded case, default).
         *
         * @return capacity or null if none defined
         */
        public Integer getThreadPoolInboundCapacity() {
            return threadPoolInboundCapacity;
        }

        /**
         * Sets the capacity of the inbound queue, or null if none defined (the unbounded case, default).
         *
         * @param capacity capacity or null if none defined
         */
        public void setThreadPoolInboundCapacity(Integer capacity) {
            this.threadPoolInboundCapacity = capacity;
        }

        /**
         * Returns the capacity of the route execution queue, or null if none defined (the unbounded case, default).
         *
         * @return capacity or null if none defined
         */
        public Integer getThreadPoolRouteExecCapacity() {
            return threadPoolRouteExecCapacity;
        }

        /**
         * Sets the capacity of the route execution queue, or null if none defined (the unbounded case, default).
         *
         * @param capacity capacity or null if none defined
         */
        public void setThreadPoolRouteExecCapacity(Integer capacity) {
            this.threadPoolRouteExecCapacity = capacity;
        }

        /**
         * Returns the capacity of the outbound queue, or null if none defined (the unbounded case, default).
         *
         * @return capacity or null if none defined
         */
        public Integer getThreadPoolOutboundCapacity() {
            return threadPoolOutboundCapacity;
        }

        /**
         * Sets the capacity of the outbound queue, or null if none defined (the unbounded case, default).
         *
         * @param capacity capacity or null if none defined
         */
        public void setThreadPoolOutboundCapacity(Integer capacity) {
            this.threadPoolOutboundCapacity = capacity;
        }

        /**
         * Returns true if the engine-level lock is configured as a fair lock (default is false).
         * <p>
         * This lock coordinates
         * event processing threads (threads that send events) with threads that
         * perform administrative functions (threads that start or destroy statements, for example).
         *
         * @return true for fair lock
         */
        public boolean isEngineFairlock() {
            return engineFairlock;
        }

        /**
         * Set to true to configured the engine-level lock as a fair lock (default is false).
         * <p>
         * This lock coordinates
         * event processing threads (threads that send events) with threads that
         * perform administrative functions (threads that start or destroy statements, for example).
         *
         * @param engineFairlock true for fair lock
         */
        public void setEngineFairlock(boolean engineFairlock) {
            this.engineFairlock = engineFairlock;
        }

        /**
         * In multithreaded environments, this setting controls whether named window dispatches to named window consumers preserve
         * the order of events inserted and removed such that statements that consume a named windows delta stream
         * behave deterministic (true by default).
         *
         * @return flag
         */
        public boolean isNamedWindowConsumerDispatchPreserveOrder() {
            return isNamedWindowConsumerDispatchPreserveOrder;
        }

        /**
         * In multithreaded environments, this setting controls whether named window dispatches to named window consumers preserve
         * the order of events inserted and removed such that statements that consume a named windows delta stream
         * behave deterministic (true by default).
         *
         * @param isNamedWindowConsumerDispatchPreserveOrder flag
         */
        public void setNamedWindowConsumerDispatchPreserveOrder(boolean isNamedWindowConsumerDispatchPreserveOrder) {
            this.isNamedWindowConsumerDispatchPreserveOrder = isNamedWindowConsumerDispatchPreserveOrder;
        }

        /**
         * Returns the timeout millisecond value for named window dispatches to named window consumers.
         *
         * @return timeout milliseconds
         */
        public long getNamedWindowConsumerDispatchTimeout() {
            return namedWindowConsumerDispatchTimeout;
        }

        /**
         * Sets the timeout millisecond value for named window dispatches to named window consumers.
         *
         * @param namedWindowConsumerDispatchTimeout timeout milliseconds
         */
        public void setNamedWindowConsumerDispatchTimeout(long namedWindowConsumerDispatchTimeout) {
            this.namedWindowConsumerDispatchTimeout = namedWindowConsumerDispatchTimeout;
        }

        /**
         * Returns the locking strategy value for named window dispatches to named window consumers (default is spin).
         *
         * @return strategy
         */
        public Locking getNamedWindowConsumerDispatchLocking() {
            return namedWindowConsumerDispatchLocking;
        }

        /**
         * Sets the locking strategy value for named window dispatches to named window consumers (default is spin).
         *
         * @param namedWindowConsumerDispatchLocking strategy
         */
        public void setNamedWindowConsumerDispatchLocking(Locking namedWindowConsumerDispatchLocking) {
            this.namedWindowConsumerDispatchLocking = namedWindowConsumerDispatchLocking;
        }

        /**
         * Enumeration of blocking techniques.
         */
        public enum Locking {
            /**
             * Spin lock blocking is good for locks held very shortly or generally uncontended locks and
             * is therefore the default.
             */
            SPIN,

            /**
             * Blocking that suspends a thread and notifies a thread to wake up can be
             * more expensive then spin locks.
             */
            SUSPEND
        }
    }

    /**
     * Holds view resources settings.
     */
    public static class ViewResources implements Serializable {
        private boolean shareViews;
        private boolean allowMultipleExpiryPolicies;
        private boolean iterableUnbound;
        private boolean outputLimitOpt;
        private static final long serialVersionUID = 2527853225433208362L;

        /**
         * Ctor - sets up defaults.
         */
        protected ViewResources() {
            shareViews = false;
            allowMultipleExpiryPolicies = false;
            iterableUnbound = false;
            outputLimitOpt = true;
        }

        /**
         * Returns false to indicate the engine does not implicitly share similar view resources between statements (false is the default),
         * or true to indicate that the engine may implicitly share view resources between statements.
         *
         * @return indicator whether view resources are shared between statements if
         * statements share same-views and the engine sees opportunity to reuse an existing view.
         */
        public boolean isShareViews() {
            return shareViews;
        }

        /**
         * Set the flag to instruct the engine whether to implicitly share view resources between
         * statements or not (false by default).
         *
         * @param shareViews is true to share view resources between statements, or false to not share view
         *                   resources between statements declaring same-views
         */
        public void setShareViews(boolean shareViews) {
            this.shareViews = shareViews;
        }

        /**
         * By default this setting is false and thereby multiple expiry policies
         * provided by views can only be combined if any of the retain-keywords is also specified for the stream.
         * <p>
         * If set to true then multiple expiry policies are allowed and the following statement compiles without exception:
         * "select * from MyEvent#time(10)#time(10)".
         *
         * @return allowMultipleExpiryPolicies indicator whether to allow combining expiry policies provided by views
         */
        public boolean isAllowMultipleExpiryPolicies() {
            return allowMultipleExpiryPolicies;
        }

        /**
         * Set to false (the default) and thereby disallow multiple expiry policies
         * provided by views and only allow if any of the retain-keywords are also specified for the stream.
         * <p>
         * If set to true then multiple expiry policies are allowed and the following statement compiles without exception:
         * "select * from MyEvent#time(10)#time(10)".
         *
         * @param allowMultipleExpiryPolicies indicator whether to allow combining expiry policies provided by views
         */
        public void setAllowMultipleExpiryPolicies(boolean allowMultipleExpiryPolicies) {
            this.allowMultipleExpiryPolicies = allowMultipleExpiryPolicies;
        }

        /**
         * Returns flag to indicate whether engine-wide unbound statements are iterable and return the last event.
         *
         * @return indicator
         */
        public boolean isIterableUnbound() {
            return iterableUnbound;
        }

        /**
         * Sets flag to indicate whether engine-wide unbound statements are iterable and return the last event.
         *
         * @param iterableUnbound to set
         */
        public void setIterableUnbound(boolean iterableUnbound) {
            this.iterableUnbound = iterableUnbound;
        }

        /**
         * Returns indicator whether for output limiting the options are enabled by default.
         * Has the same effect as adding "@hint("ENABLE_OUTPUTLIMIT_OPT") to all statements (true by default).
         * @return flag
         */
        public boolean isOutputLimitOpt() {
            return outputLimitOpt;
        }

        /**
         * Sets indicator whether for output limiting the options are enabled by default.
         * Has the same effect as adding "@hint("ENABLE_OUTPUTLIMIT_OPT") to all statements (true by default).
         * @param outputLimitOpt flag
         */
        public void setOutputLimitOpt(boolean outputLimitOpt) {
            this.outputLimitOpt = outputLimitOpt;
        }
    }

    /**
     * Event representation metadata.
     */
    public static class EventMeta implements Serializable {
        private static final long serialVersionUID = -6091772368103140370L;

        private Configuration.PropertyResolutionStyle classPropertyResolutionStyle;
        private ConfigurationEventTypeLegacy.AccessorStyle defaultAccessorStyle;
        private EventUnderlyingType defaultEventRepresentation;
        private int anonymousCacheSize = 5;
        private AvroSettings avroSettings;

        /**
         * Ctor.
         */
        public EventMeta() {
            this.classPropertyResolutionStyle = Configuration.PropertyResolutionStyle.getDefault();
            this.defaultAccessorStyle = ConfigurationEventTypeLegacy.AccessorStyle.JAVABEAN;
            this.defaultEventRepresentation = EventUnderlyingType.getDefault();
            this.avroSettings = new AvroSettings();
        }

        /**
         * Returns the default accessor style, JavaBean unless changed.
         *
         * @return style enum
         */
        public ConfigurationEventTypeLegacy.AccessorStyle getDefaultAccessorStyle() {
            return defaultAccessorStyle;
        }

        /**
         * Sets the default accessor style, which is JavaBean unless changed.
         *
         * @param defaultAccessorStyle style enum
         */
        public void setDefaultAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle defaultAccessorStyle) {
            this.defaultAccessorStyle = defaultAccessorStyle;
        }

        /**
         * Returns the property resolution style to use for resolving property names
         * of Java classes.
         *
         * @return style of property resolution
         */
        public Configuration.PropertyResolutionStyle getClassPropertyResolutionStyle() {
            return classPropertyResolutionStyle;
        }

        /**
         * Sets the property resolution style to use for resolving property names
         * of Java classes.
         *
         * @param classPropertyResolutionStyle style of property resolution
         */
        public void setClassPropertyResolutionStyle(Configuration.PropertyResolutionStyle classPropertyResolutionStyle) {
            this.classPropertyResolutionStyle = classPropertyResolutionStyle;
        }

        /**
         * Sets the default event representation.
         *
         * @param defaultEventRepresentation to set
         */
        public void setDefaultEventRepresentation(EventUnderlyingType defaultEventRepresentation) {
            this.defaultEventRepresentation = defaultEventRepresentation;
        }

        /**
         * Returns the default event representation.
         *
         * @return setting
         */
        public EventUnderlyingType getDefaultEventRepresentation() {
            return defaultEventRepresentation;
        }

        /**
         * Returns the cache size for anonymous event types.
         *
         * @return cache size
         */
        public int getAnonymousCacheSize() {
            return anonymousCacheSize;
        }

        /**
         * Sets the cache size for anonymous event types.
         *
         * @param anonymousCacheSize cache size
         */
        public void setAnonymousCacheSize(int anonymousCacheSize) {
            this.anonymousCacheSize = anonymousCacheSize;
        }

        /**
         * Returns the Avro settings.
         * @return avro settings
         */
        public AvroSettings getAvroSettings() {
            return avroSettings;
        }

        /**
         * Sets the Avro settings.
         * @param avroSettings avro settings
         */
        public void setAvroSettings(AvroSettings avroSettings) {
            this.avroSettings = avroSettings;
        }

        /**
         * Avro settings.
         */
        public static class AvroSettings implements Serializable {
            private static final long serialVersionUID = 2977645210525767203L;
            private boolean enableAvro = true;
            private boolean enableNativeString = true;
            private boolean enableSchemaDefaultNonNull = true;
            private String typeRepresentationMapperClass;
            private String objectValueTypeWidenerFactoryClass;

            /**
             * Returns the indicator whether Avro support is enabled when available (true by default).
             * @return indicator
             */
            public boolean isEnableAvro() {
                return enableAvro;
            }

            /**
             * Sets the indicator whether Avro support is enabled when available (true by default).
             * @param enableAvro indicator to set
             */
            public void setEnableAvro(boolean enableAvro) {
                this.enableAvro = enableAvro;
            }

            /**
             * Returns indicator whether for String-type values to use the "avro.java.string=String" (true by default)
             * @return indicator
             */
            public boolean isEnableNativeString() {
                return enableNativeString;
            }

            /**
             * Sets indicator whether for String-type values to use the "avro.java.string=String" (true by default)
             * @param enableNativeString indicator
             */
            public void setEnableNativeString(boolean enableNativeString) {
                this.enableNativeString = enableNativeString;
            }

            /**
             * Returns indicator whether generated schemas should assume non-null values (true by default)
             * @return indicator
             */
            public boolean isEnableSchemaDefaultNonNull() {
                return enableSchemaDefaultNonNull;
            }

            /**
             * Sets indicator whether generated schemas should assume non-null values (true by default)
             * @param enableSchemaDefaultNonNull indicator
             */
            public void setEnableSchemaDefaultNonNull(boolean enableSchemaDefaultNonNull) {
                this.enableSchemaDefaultNonNull = enableSchemaDefaultNonNull;
            }

            /**
             * Sets class name of mapping provider that maps types to an Avro schema; a mapper should implement {@link TypeRepresentationMapper}
             * (null by default, using default mapping)
             * @param typeRepresentationMapperClass class name
             */
            public void setTypeRepresentationMapperClass(String typeRepresentationMapperClass) {
                this.typeRepresentationMapperClass = typeRepresentationMapperClass;
            }

            /**
             * Returns class name of mapping provider that maps types to an Avro schema; a mapper should implement {@link TypeRepresentationMapper}
             * (null by default, using default mapping)
             * @return class name
             */
            public String getTypeRepresentationMapperClass() {
                return typeRepresentationMapperClass;
            }

            /**
             * Returns the class name of widening provider that widens, coerces or transforms object values to an Avro field value or record; a widener should implement {@link ObjectValueTypeWidenerFactory}
             * (null by default, using default widening)
             * @return class name
             */
            public String getObjectValueTypeWidenerFactoryClass() {
                return objectValueTypeWidenerFactoryClass;
            }

            /**
             * Sets the class name of widening provider that widens, coerces or transforms object values to an Avro field value or record; a widener should implement {@link ObjectValueTypeWidenerFactory}
             * (null by default, using default widening)
             * @param objectValueTypeWidenerFactoryClass class name
             */
            public void setObjectValueTypeWidenerFactoryClass(String objectValueTypeWidenerFactoryClass) {
                this.objectValueTypeWidenerFactoryClass = objectValueTypeWidenerFactoryClass;
            }
        }
    }

    /**
     * Holds view logging settings other then the Apache commons or Log4J settings.
     */
    public static class Logging implements Serializable {
        private boolean enableExecutionDebug;
        private boolean enableTimerDebug;
        private boolean enableQueryPlan;
        private boolean enableJDBC;
        private boolean enableCode;
        private String auditPattern;
        private static final long serialVersionUID = -8129836306582810327L;

        /**
         * Ctor - sets up defaults.
         */
        protected Logging() {
            enableExecutionDebug = false;
            enableTimerDebug = true;
            enableQueryPlan = false;
            enableJDBC = false;
            enableCode = false;
        }

        /**
         * Returns true if execution path debug logging is enabled.
         * <p>
         * Only if this flag is set to true, in addition to LOG4J settings set to DEBUG, does an engine instance,
         * produce debug out.
         *
         * @return true if debug logging through Log4j is enabled for any event processing execution paths
         */
        public boolean isEnableExecutionDebug() {
            return enableExecutionDebug;
        }

        /**
         * Set the debug flag for debugging the execution path, in which case the engine logs
         * to Log4j in debug-level during execution.
         *
         * @param enableExecutionDebug false to disable debug logging in the execution path, true to enable
         */
        public void setEnableExecutionDebug(boolean enableExecutionDebug) {
            this.enableExecutionDebug = enableExecutionDebug;
        }

        /**
         * Returns true if timer debug level logging is enabled (true by default).
         * <p>
         * Set this value to false to reduce the debug-level logging output for the timer thread(s).
         * For use only when debug-level logging is enabled.
         *
         * @return indicator whether timer execution is noisy in debug or not
         */
        public boolean isEnableTimerDebug() {
            return enableTimerDebug;
        }

        /**
         * Set this value to false to reduce the debug-level logging output for the timer thread(s).
         * For use only when debug-level logging is enabled.
         *
         * @param enableTimerDebug indicator whether timer execution is noisy in debug or not (true is noisy)
         */
        public void setEnableTimerDebug(boolean enableTimerDebug) {
            this.enableTimerDebug = enableTimerDebug;
        }

        /**
         * Returns indicator whether query plan logging is enabled or not.
         *
         * @return indicator
         */
        public boolean isEnableQueryPlan() {
            return enableQueryPlan;
        }

        /**
         * Set indicator whether query plan logging is enabled, by default it is disabled.
         *
         * @param enableQueryPlan indicator
         */
        public void setEnableQueryPlan(boolean enableQueryPlan) {
            this.enableQueryPlan = enableQueryPlan;
        }

        /**
         * Returns an indicator whether JDBC query reporting is enabled.
         *
         * @return indicator
         */
        public boolean isEnableJDBC() {
            return enableJDBC;
        }

        /**
         * Set the indicator whether JDBC query reporting is enabled.
         *
         * @param enableJDBC set to true for JDBC query reorting enabled
         */
        public void setEnableJDBC(boolean enableJDBC) {
            this.enableJDBC = enableJDBC;
        }

        /**
         * Returns the pattern that formats audit logs.
         * <p>
         * Available conversion characters are:
         * </p>
         * <p>
         * %m      - Used to output the audit message.
         * %s      - Used to output the statement name.
         * %u      - Used to output the engine URI.
         * </p>
         *
         * @return audit formatting pattern
         */
        public String getAuditPattern() {
            return auditPattern;
        }

        /**
         * Sets the audit formatting pattern that formats audit logs, or null if using default format.
         *
         * @param auditPattern pattern to use
         */
        public void setAuditPattern(String auditPattern) {
            this.auditPattern = auditPattern;
        }

        /**
         * Returns indicator whether code generation logging is enabled or not.
         *
         * @return indicator
         */
        public boolean isEnableCode() {
            return enableCode;
        }

        /**
         * Set indicator whether code generation logging is enabled, by default it is disabled.
         *
         * @param enableCode indicator
         */
        public void setEnableCode(boolean enableCode) {
            this.enableCode = enableCode;
        }

    }

    /**
     * Holds variables settings.
     */
    public static class Variables implements Serializable {
        private long msecVersionRelease;
        private static final long serialVersionUID = 8276015152830052323L;

        /**
         * Ctor - sets up defaults.
         */
        protected Variables() {
            msecVersionRelease = 15000;
        }

        /**
         * Returns the number of milliseconds that a version of a variables is held stable for
         * use by very long-running atomic statement execution.
         * <p>
         * A slow-executing statement such as an SQL join may use variables that, at the time
         * the statement starts to execute, have certain values. The engine guarantees that during
         * statement execution the value of the variables stays the same as long as the statement
         * does not take longer then the given number of milliseconds to execute. If the statement does take longer
         * to execute then the variables release time, the current variables value applies instead.
         *
         * @return millisecond time interval that a variables version is guaranteed to be stable
         * in the context of an atomic statement execution
         */
        public long getMsecVersionRelease() {
            return msecVersionRelease;
        }

        /**
         * Sets the number of milliseconds that a version of a variables is held stable for
         * use by very long-running atomic statement execution.
         *
         * @param msecVersionRelease millisecond time interval that a variables version is guaranteed to be stable
         *                           in the context of an atomic statement execution
         */
        public void setMsecVersionRelease(long msecVersionRelease) {
            this.msecVersionRelease = msecVersionRelease;
        }
    }

    /**
     * Holder for script settings.
     */
    public static class Scripts implements Serializable {
        private static final long serialVersionUID = -3111856398932434323L;
        private String defaultDialect = "js";

        /**
         * Returns the default script dialect.
         *
         * @return dialect
         */
        public String getDefaultDialect() {
            return defaultDialect;
        }

        /**
         * Sets the default script dialect.
         *
         * @param defaultDialect dialect
         */
        public void setDefaultDialect(String defaultDialect) {
            this.defaultDialect = defaultDialect;
        }
    }

    /**
     * Holds pattern settings.
     */
    public static class Patterns implements Serializable {
        private static final long serialVersionUID = -7596853289989573800L;
        private Long maxSubexpressions;
        private boolean maxSubexpressionPreventStart = true;

        /**
         * Returns the maximum number of subexpressions
         *
         * @return subexpression count
         */
        public Long getMaxSubexpressions() {
            return maxSubexpressions;
        }

        /**
         * Sets the maximum number of subexpressions
         *
         * @param maxSubexpressions subexpression count
         */
        public void setMaxSubexpressions(Long maxSubexpressions) {
            this.maxSubexpressions = maxSubexpressions;
        }

        /**
         * Returns true, the default, to indicate that if there is a maximum defined
         * it is being enforced and new subexpressions are not allowed.
         *
         * @return indicate whether enforced or not
         */
        public boolean isMaxSubexpressionPreventStart() {
            return maxSubexpressionPreventStart;
        }

        /**
         * Set to true, the default, to indicate that if there is a maximum defined
         * it is being enforced and new subexpressions are not allowed.
         *
         * @param maxSubexpressionPreventStart indicate whether enforced or not
         */
        public void setMaxSubexpressionPreventStart(boolean maxSubexpressionPreventStart) {
            this.maxSubexpressionPreventStart = maxSubexpressionPreventStart;
        }
    }

    /**
     * Holds match-reconize settings.
     */
    public static class MatchRecognize implements Serializable {
        private static final long serialVersionUID = 4541565245238216789L;

        private Long maxStates;
        private boolean maxStatesPreventStart = true;

        /**
         * Returns the maximum number of states
         *
         * @return state count
         */
        public Long getMaxStates() {
            return maxStates;
        }

        /**
         * Sets the maximum number of states
         *
         * @param maxStates state count
         */
        public void setMaxStates(Long maxStates) {
            this.maxStates = maxStates;
        }

        /**
         * Returns true, the default, to indicate that if there is a maximum defined
         * it is being enforced and new states are not allowed.
         *
         * @return indicate whether enforced or not
         */
        public boolean isMaxStatesPreventStart() {
            return maxStatesPreventStart;
        }

        /**
         * Set to true, the default, to indicate that if there is a maximum defined
         * it is being enforced and new states are not allowed.
         *
         * @param maxStatesPreventStart indicate whether enforced or not
         */
        public void setMaxStatesPreventStart(boolean maxStatesPreventStart) {
            this.maxStatesPreventStart = maxStatesPreventStart;
        }
    }

    /**
     * Holds default settings for stream selection in the select-clause.
     */
    public static class StreamSelection implements Serializable {
        private StreamSelector defaultStreamSelector;
        private static final long serialVersionUID = -7943748323859161674L;

        /**
         * Ctor - sets up defaults.
         */
        protected StreamSelection() {
            defaultStreamSelector = StreamSelector.ISTREAM_ONLY;
        }

        /**
         * Returns the default stream selector.
         * <p>
         * Statements that select data from streams and that do not use one of the explicit stream
         * selection keywords (istream/rstream/irstream), by default,
         * generate selection results for the insert stream only, and not for the remove stream.
         * <p>
         * This setting can be used to change the default behavior: Use the RSTREAM_ISTREAM_BOTH
         * value to have your statements generate both insert and remove stream results
         * without the use of the "irstream" keyword in the select clause.
         *
         * @return default stream selector, which is ISTREAM_ONLY unless changed
         */
        public StreamSelector getDefaultStreamSelector() {
            return defaultStreamSelector;
        }

        /**
         * Sets the default stream selector.
         * <p>
         * Statements that select data from streams and that do not use one of the explicit stream
         * selection keywords (istream/rstream/irstream), by default,
         * generate selection results for the insert stream only, and not for the remove stream.
         * <p>
         * This setting can be used to change the default behavior: Use the RSTREAM_ISTREAM_BOTH
         * value to have your statements generate both insert and remove stream results
         * without the use of the "irstream" keyword in the select clause.
         *
         * @param defaultStreamSelector default stream selector
         */
        public void setDefaultStreamSelector(StreamSelector defaultStreamSelector) {
            this.defaultStreamSelector = defaultStreamSelector;
        }
    }

    /**
     * Time source configuration, the default in MILLI (millisecond resolution from System.currentTimeMillis).
     */
    public static class TimeSource implements Serializable {
        private TimeSourceType timeSourceType;
        private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        private static final long serialVersionUID = 2075039404763313824L;

        /**
         * Ctor.
         */
        public TimeSource() {
            timeSourceType = TimeSourceType.MILLI;
        }

        /**
         * Returns the time source type.
         *
         * @return time source type enum
         */
        public TimeSourceType getTimeSourceType() {
            return timeSourceType;
        }

        /**
         * Sets the time source type.
         *
         * @param timeSourceType time source type enum
         */
        public void setTimeSourceType(TimeSourceType timeSourceType) {
            this.timeSourceType = timeSourceType;
        }

        /**
         * Returns the time unit time resolution level of time tracking
         * @return time resolution
         */
        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        /**
         * Sets the time unit time resolution level of time tracking
         * @param timeUnit time resolution
         */
        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }
    }

    /**
     * Language settings in the engine are for string comparisons.
     */
    public static class Language implements Serializable {
        private boolean sortUsingCollator;
        private static final long serialVersionUID = -6237674558477894392L;

        /**
         * Ctor.
         */
        public Language() {
            sortUsingCollator = false;
        }

        /**
         * Returns true to indicate to perform locale-independent string comparisons using Collator.
         * <p>
         * By default this setting is false, i.e. string comparisons use the compare method.
         *
         * @return indicator whether to use Collator for string comparisons
         */
        public boolean isSortUsingCollator() {
            return sortUsingCollator;
        }

        /**
         * Set to true to indicate to perform locale-independent string comparisons using Collator.
         * <p>
         * Set to false to perform string comparisons via the compare method (the default).
         *
         * @param sortUsingCollator indicator whether to use Collator for string comparisons
         */
        public void setSortUsingCollator(boolean sortUsingCollator) {
            this.sortUsingCollator = sortUsingCollator;
        }
    }

    /**
     * Expression evaluation settings in the engine are for results of expressions.
     */
    public static class Expression implements Serializable {
        private static final long serialVersionUID = 3192205923560011213L;

        private boolean integerDivision;
        private boolean divisionByZeroReturnsNull;
        private boolean udfCache;
        private boolean selfSubselectPreeval;
        private boolean extendedAggregation;
        private boolean duckTyping;
        private MathContext mathContext;
        private TimeZone timeZone;

        /**
         * Ctor.
         */
        public Expression() {
            integerDivision = false;
            divisionByZeroReturnsNull = false;
            udfCache = true;
            selfSubselectPreeval = true;
            extendedAggregation = true;
            timeZone = TimeZone.getDefault();
        }

        /**
         * Returns false (the default) for integer division returning double values.
         * <p>
         * Returns true to signal that Java-convention integer division semantics
         * are used for divisions, whereas the division between two non-FP numbers
         * returns only the whole number part of the result and any fractional part is dropped.
         *
         * @return indicator
         */
        public boolean isIntegerDivision() {
            return integerDivision;
        }

        /**
         * Set to false (default) for integer division returning double values.
         * Set to true to signal the Java-convention integer division semantics
         * are used for divisions, whereas the division between two non-FP numbers
         * returns only the whole number part of the result and any fractional part is dropped.
         *
         * @param integerDivision true for integer division returning integer, false (default) for
         */
        public void setIntegerDivision(boolean integerDivision) {
            this.integerDivision = integerDivision;
        }

        /**
         * Returns false (default) when division by zero returns Double.Infinity.
         * Returns true when division by zero return null.
         * <p>
         * If integer devision is set, then division by zero for non-FP operands also returns null.
         *
         * @return indicator for division-by-zero results
         */
        public boolean isDivisionByZeroReturnsNull() {
            return divisionByZeroReturnsNull;
        }

        /**
         * Set to false (default) to have division by zero return Double.Infinity.
         * Set to true to have division by zero return null.
         * <p>
         * If integer devision is set, then division by zero for non-FP operands also returns null.
         *
         * @param divisionByZeroReturnsNull indicator for division-by-zero results
         */
        public void setDivisionByZeroReturnsNull(boolean divisionByZeroReturnsNull) {
            this.divisionByZeroReturnsNull = divisionByZeroReturnsNull;
        }

        /**
         * By default true, indicates that user-defined functions cache return results
         * if the parameter set is empty or has constant-only return values.
         *
         * @return cache flag
         */
        public boolean isUdfCache() {
            return udfCache;
        }

        /**
         * Set to true (the default) to indicate that user-defined functions cache return results
         * if the parameter set is empty or has constant-only return values.
         *
         * @param udfCache cache flag
         */
        public void setUdfCache(boolean udfCache) {
            this.udfCache = udfCache;
        }

        /**
         * Set to true (the default) to indicate that sub-selects within a statement are updated first when a new
         * event arrives. This is only relevant for statements in which both subselects
         * and the from-clause may react to the same exact event.
         *
         * @return indicator whether to evaluate sub-selects first or last on new event arrival
         */
        public boolean isSelfSubselectPreeval() {
            return selfSubselectPreeval;
        }

        /**
         * Set to true (the default) to indicate that sub-selects within a statement are updated first when a new
         * event arrives. This is only relevant for statements in which both subselects
         * and the from-clause may react to the same exact event.
         *
         * @param selfSubselectPreeval indicator whether to evaluate sub-selects first or last on new event arrival
         */
        public void setSelfSubselectPreeval(boolean selfSubselectPreeval) {
            this.selfSubselectPreeval = selfSubselectPreeval;
        }

        /**
         * Enables or disables non-SQL standard builtin aggregation functions.
         *
         * @return indicator
         */
        public boolean isExtendedAggregation() {
            return extendedAggregation;
        }

        /**
         * Enables or disables non-SQL standard builtin aggregation functions.
         *
         * @param extendedAggregation indicator
         */
        public void setExtendedAggregation(boolean extendedAggregation) {
            this.extendedAggregation = extendedAggregation;
        }

        /**
         * Returns true to indicate that duck typing is enable for the specific syntax where it is allowed (check the documentation).
         *
         * @return indicator
         */
        public boolean isDuckTyping() {
            return duckTyping;
        }

        /**
         * Set to true to indicate that duck typing is enable for the specific syntax where it is allowed (check the documentation).
         *
         * @param duckTyping indicator
         */
        public void setDuckTyping(boolean duckTyping) {
            this.duckTyping = duckTyping;
        }

        /**
         * Returns the math context for big decimal operations, or null to leave the math context undefined.
         *
         * @return math context or null
         */
        public MathContext getMathContext() {
            return mathContext;
        }

        /**
         * Sets the math context for big decimal operations, or null to leave the math context undefined.
         *
         * @param mathContext math context or null
         */
        public void setMathContext(MathContext mathContext) {
            this.mathContext = mathContext;
        }

        /**
         * Returns the time zone for calendar operations.
         *
         * @return time zone
         */
        public TimeZone getTimeZone() {
            return timeZone;
        }

        /**
         * Sets the time zone for calendar operations.
         *
         * @param timeZone time zone
         */
        public void setTimeZone(TimeZone timeZone) {
            this.timeZone = timeZone;
        }
    }

    /**
     * Holds engine execution-related settings.
     */
    public static class Execution implements Serializable {
        private boolean prioritized;
        private boolean fairlock;
        private boolean disableLocking;
        private boolean allowIsolatedService;
        private ThreadingProfile threadingProfile = ThreadingProfile.NORMAL;
        private FilterServiceProfile filterServiceProfile = FilterServiceProfile.READMOSTLY;
        private int filterServiceMaxFilterWidth = 16;
        private int declaredExprValueCacheSize = 1;

        private static final long serialVersionUID = 0L;

        /**
         * Ctor - sets up defaults.
         */
        protected Execution() {
            prioritized = false;
        }

        /**
         * Returns false (the default) if the engine does not consider statement priority and preemptive instructions,
         * or true to enable priority-based statement execution order.
         *
         * @return false by default to indicate unprioritized statement execution
         */
        public boolean isPrioritized() {
            return prioritized;
        }

        /**
         * Set to false (the default) if the engine does not consider statement priority and preemptive instructions,
         * or true for enable priority-based statement execution order.
         *
         * @param prioritized false by default to indicate unprioritized statement execution
         */
        public void setPrioritized(boolean prioritized) {
            this.prioritized = prioritized;
        }

        /**
         * Returns true for fair locking, false for unfair locks.
         *
         * @return fairness flag
         */
        public boolean isFairlock() {
            return fairlock;
        }

        /**
         * Set to true for fair locking, false for unfair locks.
         *
         * @param fairlock fairness flag
         */
        public void setFairlock(boolean fairlock) {
            this.fairlock = fairlock;
        }

        /**
         * Returns indicator whether statement-level locks are disabled.
         * The default is false meaning statement-level locks are taken by default and depending on EPL optimizations.
         * If set to true statement-level locks are never taken.
         *
         * @return indicator for statement-level locks
         */
        public boolean isDisableLocking() {
            return disableLocking;
        }

        /**
         * Set to true to indicate that statement-level locks are disabled.
         * The default is false meaning statement-level locks are taken by default and depending on EPL optimizations.
         * If set to true statement-level locks are never taken.
         *
         * @param disableLocking false to take statement-level locks as required, or true to disable statement-level locking
         */
        public void setDisableLocking(boolean disableLocking) {
            this.disableLocking = disableLocking;
        }

        /**
         * Returns the threading profile
         *
         * @return profile
         */
        public ThreadingProfile getThreadingProfile() {
            return threadingProfile;
        }

        /**
         * Sets the threading profile
         *
         * @param threadingProfile profile to set
         */
        public void setThreadingProfile(ThreadingProfile threadingProfile) {
            this.threadingProfile = threadingProfile;
        }

        /**
         * Returns indicator whether isolated services providers are enabled or disabled (the default).
         *
         * @return indicator value
         */
        public boolean isAllowIsolatedService() {
            return allowIsolatedService;
        }

        /**
         * Sets indicator whether isolated services providers are enabled or disabled (the default).
         *
         * @param allowIsolatedService indicator to set
         */
        public void setAllowIsolatedService(boolean allowIsolatedService) {
            this.allowIsolatedService = allowIsolatedService;
        }

        /**
         * Returns the filter service profile for tuning filtering operations.
         *
         * @return filter service profile
         */
        public FilterServiceProfile getFilterServiceProfile() {
            return filterServiceProfile;
        }

        /**
         * Set the filter service profile for tuning filtering operations.
         *
         * @param filterServiceProfile filter service profile
         */
        public void setFilterServiceProfile(FilterServiceProfile filterServiceProfile) {
            this.filterServiceProfile = filterServiceProfile;
        }

        /**
         * Returns the maximum width for breaking up "or" expression in filters to
         * subexpressions for reverse indexing.
         *
         * @return max filter width
         */
        public int getFilterServiceMaxFilterWidth() {
            return filterServiceMaxFilterWidth;
        }

        /**
         * Sets the maximum width for breaking up "or" expression in filters to
         * subexpressions for reverse indexing.
         *
         * @param filterServiceMaxFilterWidth max filter width
         */
        public void setFilterServiceMaxFilterWidth(int filterServiceMaxFilterWidth) {
            this.filterServiceMaxFilterWidth = filterServiceMaxFilterWidth;
        }

        /**
         * Returns the cache size for declared expression values
         *
         * @return value
         */
        public int getDeclaredExprValueCacheSize() {
            return declaredExprValueCacheSize;
        }

        /**
         * Sets the cache size for declared expression values
         *
         * @param declaredExprValueCacheSize value
         */
        public void setDeclaredExprValueCacheSize(int declaredExprValueCacheSize) {
            this.declaredExprValueCacheSize = declaredExprValueCacheSize;
        }
    }

    /**
     * Threading profile.
     */
    public enum ThreadingProfile {
        /**
         * Large for use with 100 threads or more. Please see the documentation for more information.
         */
        LARGE,

        /**
         * For use with 100 threads or less.
         */
        NORMAL
    }

    /**
     * Filter service profile.
     */
    public enum FilterServiceProfile {
        /**
         * If filters are mostly static, the default.
         */
        READMOSTLY,

        /**
         * For very dynamic filters that come and go in a highly threaded environment.
         */
        READWRITE
    }

    /**
     * Time source type.
     */
    public enum TimeSourceType {
        /**
         * Millisecond time source type with time originating from System.currentTimeMillis
         */
        MILLI,

        /**
         * Nanosecond time source from a wallclock-adjusted System.nanoTime
         */
        NANO
    }

    /**
     * Returns the provider for runtime and administrative interfaces.
     */
    public static class AlternativeContext implements Serializable {
        private static final long serialVersionUID = 4488861684585251042L;

        private String runtime;
        private String admin;
        private String eventTypeIdGeneratorFactory;
        private String virtualDataWindowViewFactory;
        private String statementMetadataFactory;
        private Object userConfiguration;
        private String memberName;

        /**
         * Class name of runtime provider.
         *
         * @return provider class
         */
        public String getRuntime() {
            return runtime;
        }

        /**
         * Set the class name of the runtime provider.
         *
         * @param runtime provider class
         */
        public void setRuntime(String runtime) {
            this.runtime = runtime;
        }

        /**
         * Class name of admin provider.
         *
         * @return provider class
         */
        public String getAdmin() {
            return admin;
        }

        /**
         * Set the class name of the admin provider.
         *
         * @param admin provider class
         */
        public void setAdmin(String admin) {
            this.admin = admin;
        }

        /**
         * Returns the class name of the event type id generator.
         *
         * @return class name
         */
        public String getEventTypeIdGeneratorFactory() {
            return eventTypeIdGeneratorFactory;
        }

        /**
         * Sets the class name of the event type id generator.
         *
         * @param factory class name
         */
        public void setEventTypeIdGeneratorFactory(String factory) {
            this.eventTypeIdGeneratorFactory = factory;
        }

        /**
         * Sets the class name of the virtual data window view factory.
         *
         * @param factory class name
         */
        public void setVirtualDataWindowViewFactory(String factory) {
            this.virtualDataWindowViewFactory = factory;
        }

        /**
         * Returns the class name of the virtual data window view factory.
         *
         * @return factory class name
         */
        public String getVirtualDataWindowViewFactory() {
            return virtualDataWindowViewFactory;
        }

        /**
         * Sets the class name of the statement metadata factory.
         *
         * @return factory class name
         */
        public String getStatementMetadataFactory() {
            return statementMetadataFactory;
        }

        /**
         * Sets the class name of the statement metadata factory.
         *
         * @param factory class name
         */
        public void setStatementMetadataFactory(String factory) {
            this.statementMetadataFactory = factory;
        }

        /**
         * Returns the application-provided configurarion object carried as part of the configurations.
         *
         * @return config user object
         */
        public Object getUserConfiguration() {
            return userConfiguration;
        }

        /**
         * Sets an application-provided configurarion object carried as part of the configurations.
         *
         * @param userConfiguration to set
         */
        public void setUserConfiguration(Object userConfiguration) {
            this.userConfiguration = userConfiguration;
        }

        /**
         * Returns the member name.
         *
         * @return member name
         */
        public String getMemberName() {
            return memberName;
        }

        /**
         * Sets the member name.
         *
         * @param memberName member name
         */
        public void setMemberName(String memberName) {
            this.memberName = memberName;
        }
    }

    /**
     * Configuration object for defining exception handling behavior.
     */
    public static class ExceptionHandling implements Serializable {
        private static final long serialVersionUID = -708367341332718634L;
        private List<String> handlerFactories;
        private UndeployRethrowPolicy undeployRethrowPolicy = UndeployRethrowPolicy.WARN;

        /**
         * Returns the list of exception handler factory class names,
         * see {@link com.espertech.esper.client.hook.ExceptionHandlerFactory}
         *
         * @return list of fully-qualified class names
         */
        public List<String> getHandlerFactories() {
            return handlerFactories;
        }

        /**
         * Add an exception handler factory class name.
         * <p>
         * Provide a fully-qualified class name of the implementation
         * of the {@link com.espertech.esper.client.hook.ExceptionHandlerFactory}
         * interface.
         *
         * @param exceptionHandlerFactoryClassName class name of exception handler factory
         */
        public void addClass(String exceptionHandlerFactoryClassName) {
            if (handlerFactories == null) {
                handlerFactories = new ArrayList<String>();
            }
            handlerFactories.add(exceptionHandlerFactoryClassName);
        }

        /**
         * Add a list of exception handler class names.
         *
         * @param classNames to add
         */
        public void addClasses(List<String> classNames) {
            if (handlerFactories == null) {
                handlerFactories = new ArrayList<String>();
            }
            handlerFactories.addAll(classNames);
        }

        /**
         * Add an exception handler factory class.
         * <p>
         * The class provided should implement the
         * {@link com.espertech.esper.client.hook.ExceptionHandlerFactory}
         * interface.
         *
         * @param exceptionHandlerFactoryClass class of implementation
         */
        public void addClass(Class exceptionHandlerFactoryClass) {
            addClass(exceptionHandlerFactoryClass.getName());
        }

        /**
         * Returns the policy to instruct the engine whether a module un-deploy rethrows runtime exceptions that are encountered
         * during the undeploy for any statement that is undeployed. By default we are logging exceptions.
         *
         * @return indicator
         */
        public UndeployRethrowPolicy getUndeployRethrowPolicy() {
            return undeployRethrowPolicy;
        }

        /**
         * Sets the policy to instruct the engine whether a module un-deploy rethrows runtime exceptions that are encountered
         * during the undeploy for any statement that is undeployed. By default we are logging exceptions.
         *
         * @param undeployRethrowPolicy indicator
         */
        public void setUndeployRethrowPolicy(UndeployRethrowPolicy undeployRethrowPolicy) {
            this.undeployRethrowPolicy = undeployRethrowPolicy;
        }

        /**
         * Enumeration of blocking techniques.
         */
        public enum UndeployRethrowPolicy {
            /**
             * Warn.
             */
            WARN,

            /**
             * Rethrow First Encountered Exception.
             */
            RETHROW_FIRST
        }
    }

    /**
     * Configuration object for defining condition handling behavior.
     */
    public static class ConditionHandling implements Serializable {
        private static final long serialVersionUID = -708367341332718634L;
        private List<String> handlerFactories;

        /**
         * Returns the list of condition handler factory class names,
         * see {@link com.espertech.esper.client.hook.ConditionHandlerFactory}
         *
         * @return list of fully-qualified class names
         */
        public List<String> getHandlerFactories() {
            return handlerFactories;
        }

        /**
         * Add an condition handler factory class name.
         * <p>
         * Provide a fully-qualified class name of the implementation
         * of the {@link com.espertech.esper.client.hook.ConditionHandlerFactory}
         * interface.
         *
         * @param className class name of condition handler factory
         */
        public void addClass(String className) {
            if (handlerFactories == null) {
                handlerFactories = new ArrayList<String>();
            }
            handlerFactories.add(className);
        }

        /**
         * Add a list of condition handler class names.
         *
         * @param classNames to add
         */
        public void addClasses(List<String> classNames) {
            if (handlerFactories == null) {
                handlerFactories = new ArrayList<String>();
            }
            handlerFactories.addAll(classNames);
        }

        /**
         * Add an condition handler factory class.
         * <p>
         * The class provided should implement the
         * {@link com.espertech.esper.client.hook.ConditionHandlerFactory}
         * interface.
         *
         * @param clazz class of implementation
         */
        public void addClass(Class clazz) {
            addClass(clazz.getName());
        }
    }

    /**
     * Code generation settings.
     */
    public static class ByteCodeGeneration implements Serializable {
        private static final long serialVersionUID = -4582655217918634675L;
        private boolean enablePropertyGetter = false;
        private boolean enableExpression = true;
        private boolean enableSelectClause = false;
        private boolean enableResultSet = true;
        private boolean enableAggregation = true;
        private boolean enableFallback = true;
        private boolean includeDebugSymbols = false;
        private boolean includeComments = false;

        /**
         * Disable all code generation.
         */
        public void disableAll() {
            setAll(false);
        }

        /**
         * Enable all code generation.
         */
        public void enableAll() {
            setAll(true);
        }

        /**
         * Returns flag whether any byte code generation is enabled.
         * @return flag
         */
        public boolean isEnabledAny() {
            return enablePropertyGetter | enableExpression | enableSelectClause | enableResultSet | enableAggregation;
        }

        private void setAll(boolean value) {
            enablePropertyGetter = value;
            enableExpression = value;
            enableSelectClause = value;
            enableResultSet = value;
            enableAggregation = value;
        }

        /**
         * Returns indicator whether to enable code generation for event property getters (false by default),
         * relevant only when {@link EventType#getGetter(String)} getGetter}.
         * @return indicator
         */
        public boolean isEnablePropertyGetter() {
            return enablePropertyGetter;
        }

        /**
         * Sets indicator whether to enable code generation for event property getters (false by default),
         * relevant only when {@link EventType#getGetter(String)} getGetter}.
         * @param enablePropertyGetter indicator
         */
        public void setEnablePropertyGetter(boolean enablePropertyGetter) {
            this.enablePropertyGetter = enablePropertyGetter;
        }

        /**
         * Returns indicator whether to enable code generation for expressions.
         * @return indicator
         */
        public boolean isEnableExpression() {
            return enableExpression;
        }

        /**
         * Sets indicator whether to enable code generation for expressions.
         * @param enableExpression indicator
         */
        public void setEnableExpression(boolean enableExpression) {
            this.enableExpression = enableExpression;
        }

        /**
         * Returns indicator whether to enable code generation for the select-clause.
         * @return indicator
         */
        public boolean isEnableSelectClause() {
            return enableSelectClause;
        }

        /**
         * Sets indicator whether to enable code generation for the select-clause.
         * @param enableSelectClause indicator
         */
        public void setEnableSelectClause(boolean enableSelectClause) {
            this.enableSelectClause = enableSelectClause;
        }

        /**
         * Returns indicator whether to fall back to non-generated evaluation
         * in the case that code generation failed .
         * @return indicator
         */
        public boolean isEnableFallback() {
            return enableFallback;
        }

        /**
         * Returns indicator whether to enable code generation for the all of result-set processing.
         * @return indicator
         */
        public boolean isEnableResultSet() {
            return enableResultSet;
        }

        /**
         * Sets indicator whether to enable code generation for the all of result-set processing.
         * @param enableResultSet flag
         */
        public void setEnableResultSet(boolean enableResultSet) {
            this.enableResultSet = enableResultSet;
        }

        /**
         * Sets indicator whether to fall back to non-generated evaluation
         * in the case that code generation failed.
         * @param enableFallback indicator
         */
        public void setEnableFallback(boolean enableFallback) {
            this.enableFallback = enableFallback;
        }

        /**
         * Returns indicator whether the binary class code should include debug symbols
         * @return indicator
         */
        public boolean isIncludeDebugSymbols() {
            return includeDebugSymbols;
        }

        /**
         * Sets indicator whether the binary class code should include debug symbols
         * @param includeDebugSymbols indicator
         */
        public void setIncludeDebugSymbols(boolean includeDebugSymbols) {
            this.includeDebugSymbols = includeDebugSymbols;
        }

        /**
         * Returns indicator whether the generated source code should include comments for tracing back
         * @return indicator
         */
        public boolean isIncludeComments() {
            return includeComments;
        }

        /**
         * Sets indicator whether the generated source code should include comments for tracing back
         * @param includeComments indicator
         */
        public void setIncludeComments(boolean includeComments) {
            this.includeComments = includeComments;
        }

        /**
         * Returns indicator whether to enable code generation for aggregation processing.
         * @return indicator
         */
        public boolean isEnableAggregation() {
            return enableAggregation;
        }

        /**
         * Sets indicator whether to enable code generation for aggregation processing.
         * @param enableAggregation flag
         */
        public void setEnableAggregation(boolean enableAggregation) {
            this.enableAggregation = enableAggregation;
        }
    }
}
