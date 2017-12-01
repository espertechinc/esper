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

import com.espertech.esper.client.*;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.deploy.DeploymentStateService;
import com.espertech.esper.core.thread.ThreadingOption;
import com.espertech.esper.core.thread.ThreadingService;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.metric.MetricReportingPath;
import com.espertech.esper.epl.metric.MetricReportingService;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.epl.specmapper.StatementSpecMapper;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.vaevent.ValueAddEventService;
import com.espertech.esper.filter.FilterService;
import com.espertech.esper.metrics.codahale_metrics.metrics.MetricNameFactory;
import com.espertech.esper.metrics.codahale_metrics.metrics.core.MetricName;
import com.espertech.esper.metrics.jmx.CommonJMXUtil;
import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import com.espertech.esper.schedule.SchedulingMgmtService;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.schedule.TimeProvider;
import com.espertech.esper.timer.TimerCallback;
import com.espertech.esper.timer.TimerService;
import com.espertech.esper.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Service provider encapsulates the engine's services for runtime and administration interfaces.
 */
public class EPServiceProviderImpl implements EPServiceProviderSPI {
    private static final Logger log = LoggerFactory.getLogger(EPServiceProviderImpl.class);
    private volatile EPServiceEngine engine;
    private ConfigurationInformation configSnapshot;
    private String engineURI;
    private Set<EPServiceStateListener> serviceListeners;
    private Set<EPStatementStateListener> statementListeners;
    private StatementEventDispatcherUnthreaded stmtEventDispatcher;
    private Map<String, EPServiceProviderSPI> runtimes;

    /**
     * Constructor - initializes services.
     *
     * @param configuration is the engine configuration
     * @param engineURI     is the engine URI or "default" (or null which it assumes as "default") if this is the default provider
     * @param runtimes      map of URI and runtime
     * @throws ConfigurationException is thrown to indicate a configuraton error
     */
    public EPServiceProviderImpl(Configuration configuration, String engineURI, Map<String, EPServiceProviderSPI> runtimes) throws ConfigurationException {
        if (configuration == null) {
            throw new NullPointerException("Unexpected null value received for configuration");
        }
        if (engineURI == null) {
            throw new NullPointerException("Engine URI should not be null at this stage");
        }
        this.runtimes = runtimes;
        this.engineURI = engineURI;
        verifyConfiguration(configuration);

        serviceListeners = new CopyOnWriteArraySet<EPServiceStateListener>();

        configSnapshot = takeSnapshot(configuration);
        statementListeners = new CopyOnWriteArraySet<EPStatementStateListener>();
        doInitialize(null);
    }

    public synchronized EPServiceProviderIsolated getEPServiceIsolated(String name) {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }

        if (!engine.getServices().getConfigSnapshot().getEngineDefaults().getExecution().isAllowIsolatedService()) {
            throw new EPServiceNotAllowedException("Isolated runtime requires execution setting to allow isolated services, please change execution settings under engine defaults");
        }
        if (engine.getServices().getConfigSnapshot().getEngineDefaults().getViewResources().isShareViews()) {
            throw new EPServiceNotAllowedException("Isolated runtime requires view sharing disabled, set engine defaults under view resources and share views to false");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name parameter does not have a value provided");
        }

        return engine.getServices().getStatementIsolationService().getIsolationUnit(name, null);
    }

    /**
     * Invoked after an initialize operation.
     */
    public void postInitialize() {
        // plugin-loaders
        List<ConfigurationPluginLoader> pluginLoaders = engine.getServices().getConfigSnapshot().getPluginLoaders();
        // in the order configured
        for (ConfigurationPluginLoader config : pluginLoaders) {
            try {
                PluginLoader plugin = (PluginLoader) engine.getServices().getEngineEnvContext().lookup("plugin-loader/" + config.getLoaderName());
                plugin.postInitialize();
            } catch (Throwable t) {
                String message = "Error post-initializing plugin class " + config.getClassName() + ": " + t.getMessage();
                log.error(message, t);
                throw new EPException(message, t);
            }
        }
    }

    /**
     * Sets engine configuration information for use in the next initialize.
     *
     * @param configuration is the engine configs
     */
    public void setConfiguration(Configuration configuration) {
        verifyConfiguration(configuration);
        configSnapshot = takeSnapshot(configuration);
    }

    private void verifyConfiguration(Configuration configuration) {
        if (configuration.getEngineDefaults().getExecution().isPrioritized()) {
            if (!configuration.getEngineDefaults().getViewResources().isShareViews()) {
                log.info("Setting engine setting for share-views to false as execution is prioritized");
            }
            configuration.getEngineDefaults().getViewResources().setShareViews(false);
        }
    }

    public String getURI() {
        return engineURI;
    }

    public EPRuntime getEPRuntime() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getRuntime();
    }

    public EPAdministrator getEPAdministrator() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getAdmin();
    }

    public EPServicesContext getServicesContext() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices();
    }

    public ThreadingService getThreadingService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getThreadingService();
    }

    public EventAdapterService getEventAdapterService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getEventAdapterService();
    }

    public SchedulingService getSchedulingService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getSchedulingService();
    }

    public FilterService getFilterService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getFilterService();
    }

    public TimerService getTimerService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getTimerService();
    }

    public ConfigurationInformation getConfigurationInformation() {
        return configSnapshot;
    }

    public NamedWindowMgmtService getNamedWindowMgmtService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getNamedWindowMgmtService();
    }

    public TableService getTableService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getTableService();
    }

    public EngineLevelExtensionServicesContext getExtensionServicesContext() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getEngineLevelExtensionServicesContext();
    }

    public StatementLifecycleSvc getStatementLifecycleSvc() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getStatementLifecycleSvc();
    }

    public MetricReportingService getMetricReportingService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getMetricsReportingService();
    }

    public ValueAddEventService getValueAddEventService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getValueAddEventService();
    }

    public StatementEventTypeRef getStatementEventTypeRef() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getStatementEventTypeRefService();
    }

    public EngineEnvContext getEngineEnvContext() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getEngineEnvContext();
    }

    public Context getContext() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getEngineEnvContext();
    }

    public StatementContextFactory getStatementContextFactory() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getStatementContextFactory();
    }

    public StatementIsolationService getStatementIsolationService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getStatementIsolationService();
    }

    public DeploymentStateService getDeploymentStateService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getDeploymentStateService();
    }

    public synchronized void destroy() {
        if (engine != null) {
            log.info("Destroying engine URI '" + engineURI + "'");

            // first invoke listeners
            for (EPServiceStateListener listener : serviceListeners) {
                try {
                    listener.onEPServiceDestroyRequested(this);
                } catch (RuntimeException ex) {
                    log.error("Runtime exception caught during an onEPServiceDestroyRequested callback:" + ex.getMessage(), ex);
                }
            }

            if (configSnapshot.getEngineDefaults().getMetricsReporting().isJmxEngineMetrics()) {
                destroyEngineMetrics(engine.getServices().getEngineURI());
            }

            // assign null value
            EPServiceEngine engineToDestroy = engine;

            engineToDestroy.getServices().getTimerService().stopInternalClock(false);
            // Give the timer thread a little moment to catch up
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // plugin-loaders - destroy in opposite order
            List<ConfigurationPluginLoader> pluginLoaders = engineToDestroy.getServices().getConfigSnapshot().getPluginLoaders();
            if (!pluginLoaders.isEmpty()) {
                List<ConfigurationPluginLoader> reversed = new ArrayList<ConfigurationPluginLoader>(pluginLoaders);
                Collections.reverse(reversed);
                for (ConfigurationPluginLoader config : reversed) {
                    PluginLoader plugin;
                    try {
                        plugin = (PluginLoader) engineToDestroy.getServices().getEngineEnvContext().lookup("plugin-loader/" + config.getLoaderName());
                        plugin.destroy();
                    } catch (NamingException e) {
                        // expected
                    } catch (RuntimeException e) {
                        log.error("Error destroying plug-in loader: " + config.getLoaderName(), e);
                    }
                }
            }

            engineToDestroy.getServices().getThreadingService().destroy();

            // assign null - making EPRuntime and EPAdministrator unobtainable
            engine = null;

            engineToDestroy.getRuntime().destroy();
            engineToDestroy.getAdmin().destroy();
            engineToDestroy.getServices().destroy();
            runtimes.remove(engineURI);

            engineToDestroy.getServices().initialize();
        }
    }

    public boolean isDestroyed() {
        return engine == null;
    }

    public void initialize() {
        initializeInternal(null);
    }

    public void initialize(Long currentTime) {
        initializeInternal(currentTime);
    }

    private void initializeInternal(Long currentTime) {
        doInitialize(currentTime);
        postInitialize();
    }

    /**
     * Performs the initialization.
     *
     * @param startTime optional start time
     */
    protected void doInitialize(Long startTime) {
        log.info("Initializing engine URI '" + engineURI + "' version " + Version.VERSION);

        // This setting applies to all engines in a given VM
        ExecutionPathDebugLog.setDebugEnabled(configSnapshot.getEngineDefaults().getLogging().isEnableExecutionDebug());
        ExecutionPathDebugLog.setTimerDebugEnabled(configSnapshot.getEngineDefaults().getLogging().isEnableTimerDebug());

        // This setting applies to all engines in a given VM
        MetricReportingPath.setMetricsEnabled(configSnapshot.getEngineDefaults().getMetricsReporting().isEnableMetricsReporting());

        // This setting applies to all engines in a given VM
        AuditPath.setAuditPattern(configSnapshot.getEngineDefaults().getLogging().getAuditPattern());

        // This setting applies to all engines in a given VM
        ThreadingOption.setThreadingEnabled(ThreadingOption.isThreadingEnabled() ||
                configSnapshot.getEngineDefaults().getThreading().isThreadPoolTimerExec() ||
                configSnapshot.getEngineDefaults().getThreading().isThreadPoolInbound() ||
                configSnapshot.getEngineDefaults().getThreading().isThreadPoolRouteExec() ||
                configSnapshot.getEngineDefaults().getThreading().isThreadPoolOutbound());

        if (engine != null) {
            engine.getServices().getTimerService().stopInternalClock(false);
            // Give the timer thread a little moment to catch up
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            if (configSnapshot.getEngineDefaults().getMetricsReporting().isJmxEngineMetrics()) {
                destroyEngineMetrics(engine.getServices().getEngineURI());
            }

            engine.getRuntime().initialize();

            engine.getServices().destroy();
        }

        // Make EP services context factory
        String epServicesContextFactoryClassName = configSnapshot.getEPServicesContextFactoryClassName();
        EPServicesContextFactory epServicesContextFactory;
        if (epServicesContextFactoryClassName == null) {
            // Check system properties
            epServicesContextFactoryClassName = System.getProperty("ESPER_EPSERVICE_CONTEXT_FACTORY_CLASS");
        }
        if (epServicesContextFactoryClassName == null) {
            epServicesContextFactory = new EPServicesContextFactoryDefault();
        } else {
            Class clazz;
            try {
                clazz = TransientConfigurationResolver.resolveClassForNameProvider(configSnapshot.getTransientConfiguration()).classForName(epServicesContextFactoryClassName);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Class '" + epServicesContextFactoryClassName + "' cannot be loaded");
            }

            Object obj;
            try {
                obj = clazz.newInstance();
            } catch (InstantiationException e) {
                throw new ConfigurationException("Class '" + clazz + "' cannot be instantiated");
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Illegal access instantiating class '" + clazz + "'");
            }

            epServicesContextFactory = (EPServicesContextFactory) obj;
        }

        EPServicesContext services = epServicesContextFactory.createServicesContext(this, configSnapshot);

        // New runtime
        EPRuntimeSPI runtimeSPI;
        InternalEventRouteDest routeDest;
        TimerCallback timerCallback;
        String runtimeClassName = configSnapshot.getEngineDefaults().getAlternativeContext().getRuntime();
        if (runtimeClassName == null) {
            // Check system properties
            runtimeClassName = System.getProperty("ESPER_EPRUNTIME_CLASS");
        }
        if (runtimeClassName == null) {
            EPRuntimeImpl runtimeImpl = new EPRuntimeImpl(services);
            runtimeSPI = runtimeImpl;
            routeDest = runtimeImpl;
            timerCallback = runtimeImpl;
        } else {
            Class clazz;
            try {
                clazz = services.getEngineImportService().getClassForNameProvider().classForName(runtimeClassName);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Class '" + epServicesContextFactoryClassName + "' cannot be loaded");
            }

            Object obj;
            try {
                Constructor c = clazz.getConstructor(EPServicesContext.class);
                obj = c.newInstance(services);
            } catch (NoSuchMethodException e) {
                throw new ConfigurationException("Class '" + clazz + "' cannot be instantiated, constructor accepting services was not found");
            } catch (InstantiationException e) {
                throw new ConfigurationException("Class '" + clazz + "' cannot be instantiated");
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Illegal access instantiating class '" + clazz + "'");
            } catch (InvocationTargetException e) {
                throw new ConfigurationException("Exception invoking constructor of class '" + clazz + "'");
            }

            runtimeSPI = (EPRuntimeSPI) obj;
            routeDest = (InternalEventRouteDest) obj;
            timerCallback = (TimerCallback) obj;
        }

        routeDest.setInternalEventRouter(services.getInternalEventRouter());
        services.setInternalEventEngineRouteDest(routeDest);

        // set current time, if applicable
        if (startTime != null) {
            services.getSchedulingService().setTime(startTime);
        }

        // Configure services to use the new runtime
        services.getTimerService().setCallback(timerCallback);

        // Statement lifecycle init
        services.getStatementLifecycleSvc().init();

        // Filter service init
        services.getFilterService().init();

        // Schedule service init
        services.getSchedulingService().init();

        // New admin
        ConfigurationOperations configOps = new ConfigurationOperationsImpl(services.getEventAdapterService(), services.getEventTypeIdGenerator(), services.getEngineImportService(), services.getVariableService(), services.getEngineSettingsService(), services.getValueAddEventService(), services.getMetricsReportingService(), services.getStatementEventTypeRefService(), services.getStatementVariableRefService(), services.getPlugInViews(), services.getFilterService(), services.getPatternSubexpressionPoolSvc(), services.getMatchRecognizeStatePoolEngineSvc(), services.getTableService(), configSnapshot.getTransientConfiguration());
        SelectClauseStreamSelectorEnum defaultStreamSelector = StatementSpecMapper.mapFromSODA(configSnapshot.getEngineDefaults().getStreamSelection().getDefaultStreamSelector());
        EPAdministratorSPI adminSPI;
        String adminClassName = configSnapshot.getEngineDefaults().getAlternativeContext().getAdmin();
        EPAdministratorContext adminContext = new EPAdministratorContext(runtimeSPI, services, configOps, defaultStreamSelector);
        if (adminClassName == null) {
            // Check system properties
            adminClassName = System.getProperty("ESPER_EPADMIN_CLASS");
        }
        if (adminClassName == null) {
            adminSPI = new EPAdministratorImpl(adminContext);
        } else {
            Class clazz;
            try {
                clazz = services.getEngineImportService().getClassForNameProvider().classForName(adminClassName);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Class '" + epServicesContextFactoryClassName + "' cannot be loaded");
            }

            Object obj;
            try {
                Constructor c = clazz.getConstructor(EPAdministratorContext.class);
                obj = c.newInstance(adminContext);
            } catch (NoSuchMethodException e) {
                throw new ConfigurationException("Class '" + clazz + "' cannot be instantiated, constructor accepting context was not found");
            } catch (InstantiationException e) {
                throw new ConfigurationException("Class '" + clazz + "' cannot be instantiated");
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Illegal access instantiating class '" + clazz + "'");
            } catch (InvocationTargetException e) {
                throw new ConfigurationException("Exception invoking constructor of class '" + clazz + "'");
            }

            adminSPI = (EPAdministratorSPI) obj;
        }

        // Start clocking
        if (configSnapshot.getEngineDefaults().getThreading().isInternalTimerEnabled()) {
            if (configSnapshot.getEngineDefaults().getTimeSource().getTimeUnit() != TimeUnit.MILLISECONDS) {
                throw new ConfigurationException("Internal timer requires millisecond time resolution");
            }
            services.getTimerService().startInternalClock();
        }

        // Give the timer thread a little moment to start up
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        // Save engine instance
        engine = new EPServiceEngine(services, runtimeSPI, adminSPI);

        // Load and initialize adapter loader classes
        loadAdapters(services);

        // Initialize extension services
        if (services.getEngineLevelExtensionServicesContext() != null) {
            services.getEngineLevelExtensionServicesContext().init(services, runtimeSPI, adminSPI);
        }

        // Start metrics reporting, if any
        if (configSnapshot.getEngineDefaults().getMetricsReporting().isEnableMetricsReporting()) {
            services.getMetricsReportingService().setContext(runtimeSPI, services);
        }

        // Start engine metrics report
        if (configSnapshot.getEngineDefaults().getMetricsReporting().isJmxEngineMetrics()) {
            startEngineMetrics(services, runtimeSPI);
        }

        // call initialize listeners
        for (EPServiceStateListener listener : serviceListeners) {
            try {
                listener.onEPServiceInitialized(this);
            } catch (RuntimeException ex) {
                log.error("Runtime exception caught during an onEPServiceInitialized callback:" + ex.getMessage(), ex);
            }
        }
    }

    private synchronized void startEngineMetrics(EPServicesContext services, EPRuntime runtime) {
        MetricName filterName = MetricNameFactory.name(services.getEngineURI(), "filter");
        CommonJMXUtil.registerMbean(services.getFilterService(), filterName);
        MetricName scheduleName = MetricNameFactory.name(services.getEngineURI(), "schedule");
        CommonJMXUtil.registerMbean(services.getSchedulingService(), scheduleName);
        MetricName runtimeName = MetricNameFactory.name(services.getEngineURI(), "runtime");
        CommonJMXUtil.registerMbean(runtime, runtimeName);
    }

    private synchronized void destroyEngineMetrics(String engineURI) {
        CommonJMXUtil.unregisterMbean(MetricNameFactory.name(engineURI, "filter"));
        CommonJMXUtil.unregisterMbean(MetricNameFactory.name(engineURI, "schedule"));
        CommonJMXUtil.unregisterMbean(MetricNameFactory.name(engineURI, "runtime"));
    }

    /**
     * Loads and initializes adapter loaders.
     *
     * @param services is the engine instance services
     */
    private void loadAdapters(EPServicesContext services) {
        List<ConfigurationPluginLoader> pluginLoaders = configSnapshot.getPluginLoaders();
        if ((pluginLoaders == null) || (pluginLoaders.size() == 0)) {
            return;
        }
        for (ConfigurationPluginLoader config : pluginLoaders) {
            String className = config.getClassName();
            Class pluginLoaderClass;
            try {
                pluginLoaderClass = services.getEngineImportService().getClassForNameProvider().classForName(className);
            } catch (ClassNotFoundException ex) {
                throw new ConfigurationException("Failed to load adapter loader class '" + className + "'", ex);
            }

            Object pluginLoaderObj;
            try {
                pluginLoaderObj = pluginLoaderClass.newInstance();
            } catch (InstantiationException ex) {
                throw new ConfigurationException("Failed to instantiate adapter loader class '" + className + "' via default constructor", ex);
            } catch (IllegalAccessException ex) {
                throw new ConfigurationException("Illegal access to instantiate adapter loader class '" + className + "' via default constructor", ex);
            }

            if (!(pluginLoaderObj instanceof PluginLoader)) {
                throw new ConfigurationException("Failed to cast adapter loader class '" + className + "' to " + PluginLoader.class.getName());
            }

            PluginLoader pluginLoader = (PluginLoader) pluginLoaderObj;
            PluginLoaderInitContext context = new PluginLoaderInitContext(config.getLoaderName(), config.getConfigProperties(), config.getConfigurationXML(), this);
            pluginLoader.init(context);

            // register adapter loader in JNDI context tree
            try {
                services.getEngineEnvContext().bind("plugin-loader/" + config.getLoaderName(), pluginLoader);
            } catch (NamingException e) {
                throw new EPException("Failed to use context to bind adapter loader", e);
            }
        }
    }

    private static class EPServiceEngine {
        private EPServicesContext services;
        private EPRuntimeSPI runtimeSPI;
        private EPAdministratorSPI admin;

        public EPServiceEngine(EPServicesContext services, EPRuntimeSPI runtimeSPI, EPAdministratorSPI admin) {
            this.services = services;
            this.runtimeSPI = runtimeSPI;
            this.admin = admin;
        }

        public EPServicesContext getServices() {
            return services;
        }

        public EPRuntimeSPI getRuntime() {
            return runtimeSPI;
        }

        public EPAdministratorSPI getAdmin() {
            return admin;
        }
    }

    private ConfigurationInformation takeSnapshot(Configuration configuration) {
        try {
            // Allow variables to have non-serializable values by copying their initial value
            Map<String, Object> variableInitialValues = null;
            if (!configuration.getVariables().isEmpty()) {
                variableInitialValues = new HashMap<>();
                for (Map.Entry<String, ConfigurationVariable> variable : configuration.getVariables().entrySet()) {
                    Object initializationValue = variable.getValue().getInitializationValue();
                    if (initializationValue != null) {
                        variableInitialValues.put(variable.getKey(), initializationValue);
                        variable.getValue().setInitializationValue(null);
                    }
                }
            }

            // Avro schemas are not serializable
            Map<String, ConfigurationEventTypeAvro> avroSchemas = null;
            if (!configuration.getEventTypesAvro().isEmpty()) {
                avroSchemas = new LinkedHashMap<>(configuration.getEventTypesAvro());
                configuration.getEventTypesAvro().clear();
            }

            Configuration copy = (Configuration) SerializableObjectCopier.copy(configuration);
            copy.setTransientConfiguration(configuration.getTransientConfiguration());

            // Restore variable with initial values
            if (variableInitialValues != null && !variableInitialValues.isEmpty()) {
                for (Map.Entry<String, Object> entry : variableInitialValues.entrySet()) {
                    ConfigurationVariable config = copy.getVariables().get(entry.getKey());
                    config.setInitializationValue(entry.getValue());
                }
            }

            // Restore Avro schemas
            if (avroSchemas != null) {
                copy.getEventTypesAvro().putAll(avroSchemas);
            }

            return copy;
        } catch (IOException e) {
            throw new ConfigurationException("Failed to snapshot configuration instance through serialization : " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Failed to snapshot configuration instance through serialization : " + e.getMessage(), e);
        }
    }

    public void addServiceStateListener(EPServiceStateListener listener) {
        serviceListeners.add(listener);
    }

    public boolean removeServiceStateListener(EPServiceStateListener listener) {
        return serviceListeners.remove(listener);
    }

    public void removeAllServiceStateListeners() {
        serviceListeners.clear();
    }

    public synchronized void addStatementStateListener(EPStatementStateListener listener) {
        if (statementListeners.isEmpty()) {
            stmtEventDispatcher = new StatementEventDispatcherUnthreaded(this, statementListeners);
            this.getStatementLifecycleSvc().addObserver(stmtEventDispatcher);
        }
        statementListeners.add(listener);
    }

    public synchronized boolean removeStatementStateListener(EPStatementStateListener listener) {
        boolean result = statementListeners.remove(listener);
        if (statementListeners.isEmpty()) {
            this.getStatementLifecycleSvc().removeObserver(stmtEventDispatcher);
            stmtEventDispatcher = null;
        }
        return result;
    }

    public synchronized void removeAllStatementStateListeners() {
        statementListeners.clear();
        if (statementListeners.isEmpty()) {
            this.getStatementLifecycleSvc().removeObserver(stmtEventDispatcher);
            stmtEventDispatcher = null;
        }
    }

    public String[] getEPServiceIsolatedNames() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getStatementIsolationService().getIsolationUnitNames();
    }

    public SchedulingMgmtService getSchedulingMgmtService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getSchedulingMgmtService();
    }

    public EngineImportService getEngineImportService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getEngineImportService();
    }

    public TimeProvider getTimeProvider() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getSchedulingService();
    }

    public VariableService getVariableService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getVariableService();
    }

    public ContextManagementService getContextManagementService() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getContextManagementService();
    }

    public ReadWriteLock getEngineInstanceWideLock() {
        if (engine == null) {
            throw new EPServiceDestroyedException(engineURI);
        }
        return engine.getServices().getEventProcessingRWLock().getLock();
    }
}
