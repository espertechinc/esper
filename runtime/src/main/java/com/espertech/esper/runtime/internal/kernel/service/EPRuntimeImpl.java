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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EPCompilerPathable;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonVariable;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimePluginLoader;
import com.espertech.esper.common.client.context.EPContextPartitionService;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowService;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.metric.EPMetricsService;
import com.espertech.esper.common.client.render.EPRenderEventService;
import com.espertech.esper.common.client.util.AccessorStyle;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.PropertyResolutionStyle;
import com.espertech.esper.common.client.variable.EPVariableService;
import com.espertech.esper.common.internal.epl.util.EPCompilerPathableImpl;
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.epl.variable.core.VariableDeployment;
import com.espertech.esper.common.internal.epl.variable.core.VariableRepositoryPreconfigured;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeStemService;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryRuntime;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactoryImpl;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryImpl;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.common.internal.util.TransientConfigurationResolver;
import com.espertech.esper.common.internal.util.UuidGenerator;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.client.option.*;
import com.espertech.esper.runtime.client.plugin.PluginLoader;
import com.espertech.esper.runtime.client.plugin.PluginLoaderInitContext;
import com.espertech.esper.runtime.client.util.RuntimeVersion;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentRecoveryEntry;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementListenerSet;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import com.espertech.esper.runtime.internal.kernel.thread.ThreadingService;
import com.espertech.esper.runtime.internal.metrics.codahale_metrics.metrics.MetricNameFactory;
import com.espertech.esper.runtime.internal.metrics.codahale_metrics.metrics.core.MetricName;
import com.espertech.esper.runtime.internal.metrics.jmx.CommonJMXUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Service provider encapsulates the runtime's services for runtime and administration interfaces.
 */
public class EPRuntimeImpl implements EPRuntimeSPI {
    private static final Logger log = LoggerFactory.getLogger(EPRuntimeImpl.class);
    private volatile EPRuntimeEnv runtimeEnvironment;
    private Configuration configLastProvided;
    private Configuration configAtInitialization;
    private String runtimeURI;
    private Set<EPRuntimeStateListener> serviceListeners;
    private Map<String, EPRuntimeSPI> runtimes;
    private AtomicBoolean serviceStatusProvider;
    private EPRuntimeCompileReflectiveSPI compileReflective;
    private EPRuntimeStatementSelectionSPI statementSelection;

    /**
     * Constructor - initializes services.
     *
     * @param configuration is the runtimeconfiguration
     * @param runtimeURI    is the runtime URI or "default" (or null which it assumes as "default") if this is the default provider
     * @param runtimes      map of URI and runtime
     * @throws ConfigurationException is thrown to indicate a configuraton error
     */
    public EPRuntimeImpl(Configuration configuration, String runtimeURI, Map<String, EPRuntimeSPI> runtimes) throws ConfigurationException {
        if (configuration == null) {
            throw new NullPointerException("Unexpected null value received for configuration");
        }
        if (runtimeURI == null) {
            throw new NullPointerException("runtime URI should not be null at this stage");
        }
        this.runtimes = runtimes;
        this.runtimeURI = runtimeURI;

        serviceListeners = new CopyOnWriteArraySet<>();

        configLastProvided = takeSnapshot(configuration);

        doInitialize(null);
    }

    /**
     * Invoked after an initialize operation.
     */
    public void postInitialize() {
        // plugin-loaders
        List<ConfigurationRuntimePluginLoader> pluginLoaders = runtimeEnvironment.getServices().getConfigSnapshot().getRuntime().getPluginLoaders();
        // in the order configured
        for (ConfigurationRuntimePluginLoader config : pluginLoaders) {
            try {
                PluginLoader plugin = (PluginLoader) runtimeEnvironment.getServices().getRuntimeEnvContext().lookup("plugin-loader/" + config.getLoaderName());
                plugin.postInitialize();
            } catch (Throwable t) {
                String message = "Error post-initializing plugin class " + config.getClassName() + ": " + t.getMessage();
                log.error(message, t);
                throw new EPException(message, t);
            }
        }
    }

    /**
     * Sets runtime configuration information for use in the next initialize.
     *
     * @param configuration is the runtimeconfigs
     */
    public void setConfiguration(Configuration configuration) {
        configLastProvided = takeSnapshot(configuration);
    }

    public String getURI() {
        return runtimeURI;
    }

    public EPEventService getEventService() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getRuntime();
    }

    public EPDeploymentService getDeploymentService() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getDeploymentService();
    }

    public EPServicesContext getServicesContext() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getServices();
    }

    public Configuration getConfigurationDeepCopy() {
        return takeSnapshot(configAtInitialization);
    }

    public Map<String, Object> getConfigurationTransient() {
        return configLastProvided.getCommon().getTransientConfiguration();
    }

    public synchronized void destroy() {
        if (runtimeEnvironment != null) {
            log.info("Destroying runtime URI '" + runtimeURI + "'");

            // first invoke listeners
            for (EPRuntimeStateListener listener : serviceListeners) {
                try {
                    listener.onEPRuntimeDestroyRequested(this);
                } catch (RuntimeException ex) {
                    log.error("Runtime exception caught during an onEPRuntimeDestroyRequested callback:" + ex.getMessage(), ex);
                }
            }

            if (configLastProvided.getRuntime().getMetricsReporting().isJmxRuntimeMetrics()) {
                destroyEngineMetrics(runtimeEnvironment.getServices().getRuntimeURI());
            }

            // assign null value
            EPRuntimeEnv runtimeToDestroy = runtimeEnvironment;
            runtimeToDestroy.getServices().getTimerService().stopInternalClock(false);

            // plugin-loaders - destroy in opposite order
            List<ConfigurationRuntimePluginLoader> pluginLoaders = runtimeToDestroy.getServices().getConfigSnapshot().getRuntime().getPluginLoaders();
            if (!pluginLoaders.isEmpty()) {
                List<ConfigurationRuntimePluginLoader> reversed = new ArrayList<ConfigurationRuntimePluginLoader>(pluginLoaders);
                Collections.reverse(reversed);
                for (ConfigurationRuntimePluginLoader config : reversed) {
                    PluginLoader plugin;
                    try {
                        plugin = (PluginLoader) runtimeToDestroy.getServices().getRuntimeEnvContext().lookup("plugin-loader/" + config.getLoaderName());
                        plugin.destroy();
                    } catch (NamingException e) {
                        // expected
                    } catch (RuntimeException e) {
                        log.error("Error destroying plug-in loader: " + config.getLoaderName(), e);
                    }
                }
            }

            runtimeToDestroy.getServices().getThreadingService().destroy();

            // assign null - making EPRuntime and EPAdministrator unobtainable
            runtimeEnvironment = null;

            runtimeToDestroy.getRuntime().destroy();
            runtimeToDestroy.getDeploymentService().destroy();
            runtimeToDestroy.getServices().destroy();
            runtimes.remove(runtimeURI);

            runtimeToDestroy.getServices().initialize();
        }
    }

    public boolean isDestroyed() {
        return runtimeEnvironment == null;
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
        log.info("Initializing runtime URI '" + runtimeURI + "' version " + RuntimeVersion.RUNTIME_VERSION);

        // Retain config-at-initialization since config-last-provided can be set to new values and "initialize" can be called
        this.configAtInitialization = configLastProvided;

        // Verify settings
        if (configLastProvided.getRuntime().getThreading().isInternalTimerEnabled() && configLastProvided.getCommon().getTimeSource().getTimeUnit() != TimeUnit.MILLISECONDS) {
            throw new ConfigurationException("Internal timer requires millisecond time resolution");
        }

        // This setting applies to all runtimes in a given VM
        ExecutionPathDebugLog.setDebugEnabled(configLastProvided.getRuntime().getLogging().isEnableExecutionDebug());
        ExecutionPathDebugLog.setTimerDebugEnabled(configLastProvided.getRuntime().getLogging().isEnableTimerDebug());

        // This setting applies to all runtimes in a given VM
        AuditPath.setAuditPattern(configLastProvided.getRuntime().getLogging().getAuditPattern());

        if (runtimeEnvironment != null) {
            if (serviceStatusProvider != null) {
                serviceStatusProvider.set(false);
            }

            runtimeEnvironment.getServices().getTimerService().stopInternalClock(false);

            if (configLastProvided.getRuntime().getMetricsReporting().isJmxRuntimeMetrics()) {
                destroyEngineMetrics(runtimeEnvironment.getServices().getRuntimeURI());
            }

            runtimeEnvironment.getRuntime().initialize();

            runtimeEnvironment.getServices().destroy();
        }

        serviceStatusProvider = new AtomicBoolean(true);
        // Make EP services context factory
        String epServicesContextFactoryClassName = configLastProvided.getRuntime().getEPServicesContextFactoryClassName();
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
                clazz = TransientConfigurationResolver.resolveClassForNameProvider(configLastProvided.getCommon().getTransientConfiguration()).classForName(epServicesContextFactoryClassName);
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

        EPServicesContext services;
        try {
            services = epServicesContextFactory.createServicesContext(this, configLastProvided);
        } catch (Throwable t) {
            throw new ConfigurationException("Failed runtime startup: " + t.getMessage(), t);
        }

        // new runtime
        EPEventServiceImpl eventService = epServicesContextFactory.createEPRuntime(services, serviceStatusProvider);


        eventService.setInternalEventRouter(services.getInternalEventRouter());
        services.setInternalEventRouteDest(eventService);

        // set current time, if applicable
        if (startTime != null) {
            services.getSchedulingService().setTime(startTime);
        }

        // Configure services to use the new runtime
        services.getTimerService().setCallback(eventService);

        // New services
        EPDeploymentServiceSPI deploymentService = new EPDeploymentServiceImpl(services, this);
        EPEventTypeServiceImpl eventTypeService = new EPEventTypeServiceImpl(services);
        EPContextPartitionService contextPartitionService = new EPContextPartitionServiceImpl(services);
        EPVariableService variableService = new EPVariableServiceImpl(services);
        EPMetricsService metricsService = new EPMetricsServiceImpl(services);
        EPFireAndForgetService fireAndForgetService = new EPFireAndForgetServiceImpl(services, serviceStatusProvider);

        // Build runtime environment
        runtimeEnvironment = new EPRuntimeEnv(services, eventService, deploymentService, eventTypeService, contextPartitionService, variableService, metricsService, fireAndForgetService);

        // Deployment Recovery
        Iterator<Map.Entry<String, DeploymentRecoveryEntry>> deploymentIterator = services.getDeploymentRecoveryService().deployments();
        Set<EventType> protectedVisibleTypes = new LinkedHashSet<>();
        while (deploymentIterator.hasNext()) {
            Map.Entry<String, DeploymentRecoveryEntry> entry = deploymentIterator.next();

            StatementUserObjectRuntimeOption userObjectResolver = new StatementUserObjectRuntimeOption() {
                public Object getUserObject(StatementUserObjectRuntimeContext env) {
                    return entry.getValue().getUserObjectsRuntime().get(env.getStatementId());
                }
            };

            StatementNameRuntimeOption statementNameResolver = new StatementNameRuntimeOption() {
                public String getStatementName(StatementNameRuntimeContext env) {
                    return entry.getValue().getStatementNamesWhenProvidedByAPI().get(env.getStatementId());
                }
            };

            StatementSubstitutionParameterOption substitutionParameterResolver = new StatementSubstitutionParameterOption() {
                public void setStatementParameters(StatementSubstitutionParameterContext env) {
                    Map<Integer, Object> param = entry.getValue().getSubstitutionParameters().get(env.getStatementId());
                    if (param == null) {
                        return;
                    }
                    if (env.getSubstitutionParameterNames() != null) {
                        for (Map.Entry<String, Integer> name : env.getSubstitutionParameterNames().entrySet()) {
                            env.setObject(name.getKey(), param.get(name.getValue()));
                        }
                    } else {
                        for (int i = 0; i < env.getSubstitutionParameterTypes().length; i++) {
                            env.setObject(i + 1, param.get(i + 1));
                        }
                    }
                }
            };

            DeploymentInternal deployerResult;
            try {
                deployerResult = Deployer.deployRecover(entry.getKey(), entry.getValue().getStatementIdFirstStatement(), entry.getValue().getCompiled(), statementNameResolver, userObjectResolver, substitutionParameterResolver, this);
            } catch (EPDeployException ex) {
                throw new EPException(ex.getMessage(), ex);
            }
            for (EventType eventType : deployerResult.getDeploymentTypes().values()) {
                if (eventType.getMetadata().getBusModifier() == EventTypeBusModifier.BUS ||
                    eventType.getMetadata().getTypeClass() == EventTypeTypeClass.NAMED_WINDOW ||
                    eventType.getMetadata().getTypeClass() == EventTypeTypeClass.STREAM) {
                    protectedVisibleTypes.add(eventType);
                }
            }
        }

        // Listener Recovery
        Iterator<Map.Entry<Integer, UpdateListener[]>> listenerIterator = services.getListenerRecoveryService().listeners();
        while (listenerIterator.hasNext()) {
            Map.Entry<Integer, UpdateListener[]> deployment = listenerIterator.next();
            EPStatementSPI epStatement = services.getStatementLifecycleService().getStatementById(deployment.getKey());
            epStatement.recoveryUpdateListeners(new EPStatementListenerSet(deployment.getValue()));
        }

        // Filter service init
        Set<EventType> filterServiceTypes = new LinkedHashSet<>(services.getEventTypeRepositoryBus().getAllTypes());
        filterServiceTypes.addAll(protectedVisibleTypes);
        Supplier<Collection<EventType>> availableTypes = new Supplier<Collection<EventType>>() {
            public Collection<EventType> get() {
                return filterServiceTypes;
            }
        };
        services.getFilterService().init(availableTypes);

        // Schedule service init
        services.getSchedulingService().init();

        // Start clocking
        if (configLastProvided.getRuntime().getThreading().isInternalTimerEnabled()) {
            services.getTimerService().startInternalClock();
        }

        // Load and initialize adapter loader classes
        loadAdapters(services);

        // Initialize extension services
        if (services.getRuntimeExtensionServices() != null) {
            ((RuntimeExtensionServicesSPI) services.getRuntimeExtensionServices()).init(services, eventService, deploymentService);
        }

        // Start metrics reporting, if any
        if (configLastProvided.getRuntime().getMetricsReporting().isEnableMetricsReporting()) {
            services.getMetricReportingService().setContext(services.getFilterService(), services.getSchedulingService(), eventService);
        }

        // Start runtimes metrics report
        if (configLastProvided.getRuntime().getMetricsReporting().isJmxRuntimeMetrics()) {
            startEngineMetrics(services, eventService);
        }

        // call initialize listeners
        for (EPRuntimeStateListener listener : serviceListeners) {
            try {
                listener.onEPRuntimeInitialized(this);
            } catch (RuntimeException ex) {
                log.error("Runtime exception caught during an onEPRuntimeInitialized callback:" + ex.getMessage(), ex);
            }
        }
    }

    public Context getContext() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getServices().getRuntimeEnvContext();
    }

    public ReadWriteLock getRuntimeInstanceWideLock() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getServices().getEventProcessingRWLock().getLock();
    }

    private synchronized void startEngineMetrics(EPServicesContext services, EPEventService runtime) {
        MetricName filterName = MetricNameFactory.name(services.getRuntimeURI(), "filter");
        CommonJMXUtil.registerMbean(services.getFilterService(), filterName);
        MetricName scheduleName = MetricNameFactory.name(services.getRuntimeURI(), "schedule");
        CommonJMXUtil.registerMbean(services.getSchedulingService(), scheduleName);
        MetricName runtimeName = MetricNameFactory.name(services.getRuntimeURI(), "runtime");
        CommonJMXUtil.registerMbean(runtime, runtimeName);
    }

    private synchronized void destroyEngineMetrics(String runtimeURI) {
        CommonJMXUtil.unregisterMbean(MetricNameFactory.name(runtimeURI, "filter"));
        CommonJMXUtil.unregisterMbean(MetricNameFactory.name(runtimeURI, "schedule"));
        CommonJMXUtil.unregisterMbean(MetricNameFactory.name(runtimeURI, "runtime"));
    }

    /**
     * Loads and initializes adapter loaders.
     *
     * @param services is the runtime instance services
     */
    private void loadAdapters(EPServicesContext services) {
        List<ConfigurationRuntimePluginLoader> pluginLoaders = configLastProvided.getRuntime().getPluginLoaders();
        if ((pluginLoaders == null) || (pluginLoaders.size() == 0)) {
            return;
        }
        for (ConfigurationRuntimePluginLoader config : pluginLoaders) {
            String className = config.getClassName();
            Class pluginLoaderClass;
            try {
                pluginLoaderClass = services.getClassForNameProvider().classForName(className);
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
                services.getRuntimeEnvContext().bind("plugin-loader/" + config.getLoaderName(), pluginLoader);
            } catch (NamingException e) {
                throw new EPException("Failed to use context to bind adapter loader", e);
            }
        }
    }

    private Configuration takeSnapshot(Configuration configuration) {
        try {
            // Allow variables to have non-serializable values by copying their initial value
            Map<String, Object> variableInitialValues = null;
            if (!configuration.getCommon().getVariables().isEmpty()) {
                variableInitialValues = new HashMap<>();
                for (Map.Entry<String, ConfigurationCommonVariable> variable : configuration.getCommon().getVariables().entrySet()) {
                    Object initializationValue = variable.getValue().getInitializationValue();
                    if (initializationValue != null) {
                        variableInitialValues.put(variable.getKey(), initializationValue);
                        variable.getValue().setInitializationValue(null);
                    }
                }
            }

            // Avro schemas are not serializable
            Map<String, ConfigurationCommonEventTypeAvro> avroSchemas = null;
            if (!configuration.getCommon().getEventTypesAvro().isEmpty()) {
                avroSchemas = new LinkedHashMap<>(configuration.getCommon().getEventTypesAvro());
                configuration.getCommon().getEventTypesAvro().clear();
            }

            // Transient configuration may not be copyEPDataFlowDescriptor-able
            Map<String, Object> transients = null;
            Map<String, Object> transientsProvidedByConfig = configuration.getCommon().getTransientConfiguration();
            if (transientsProvidedByConfig != null && !transientsProvidedByConfig.isEmpty()) {
                transients = new HashMap<>(transientsProvidedByConfig);
                // no need to clear, it is marked as transient
            }

            Configuration copy = SerializableObjectCopier.copy(configuration);

            // Restore transient
            if (transients != null) {
                copy.getCommon().setTransientConfiguration(transients);
            } else {
                copy.getCommon().setTransientConfiguration(Collections.emptyMap());
            }

            // Restore variable with initial values
            if (variableInitialValues != null && !variableInitialValues.isEmpty()) {
                for (Map.Entry<String, Object> entry : variableInitialValues.entrySet()) {
                    ConfigurationCommonVariable config = copy.getCommon().getVariables().get(entry.getKey());
                    config.setInitializationValue(entry.getValue());
                }
                for (Map.Entry<String, Object> entry : variableInitialValues.entrySet()) {
                    ConfigurationCommonVariable config = configuration.getCommon().getVariables().get(entry.getKey());
                    config.setInitializationValue(entry.getValue());
                }
            }

            // Restore Avro schemas
            if (avroSchemas != null) {
                copy.getCommon().getEventTypesAvro().putAll(avroSchemas);
                configuration.getCommon().getEventTypesAvro().putAll(avroSchemas);
            }

            return copy;
        } catch (IOException e) {
            throw new ConfigurationException("Failed to snapshot configuration instance through serialization : " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Failed to snapshot configuration instance through serialization : " + e.getMessage(), e);
        }
    }

    public AtomicBoolean getServiceStatusProvider() {
        return serviceStatusProvider;
    }

    public void addRuntimeStateListener(EPRuntimeStateListener listener) {
        serviceListeners.add(listener);
    }

    public boolean removeRuntimeStateListener(EPRuntimeStateListener listener) {
        return serviceListeners.remove(listener);
    }

    public EPEventServiceSPI getEventServiceSPI() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getRuntime();
    }

    public void removeAllRuntimeStateListeners() {
        serviceListeners.clear();
    }

    public EPDataFlowService getDataFlowService() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getServices().getDataflowService();
    }

    public EPContextPartitionService getContextPartitionService() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getContextPartitionService();
    }

    public EPVariableService getVariableService() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getVariableService();
    }

    public EPMetricsService getMetricsService() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getMetricsService();
    }

    public EPEventTypeService getEventTypeService() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getEventTypeService();
    }

    public EPRenderEventService getRenderEventService() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getServices().getEventRenderer();
    }

    public EPFireAndForgetService getFireAndForgetService() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getFireAndForgetService();
    }

    public ThreadingService getThreadingService() {
        if (runtimeEnvironment == null) {
            throw new EPRuntimeDestroyedException(runtimeURI);
        }
        return runtimeEnvironment.getServices().getThreadingService();
    }

    public EPCompilerPathable getRuntimePath() {
        EPServicesContext services = runtimeEnvironment.getServices();

        VariableRepositoryPreconfigured variables = new VariableRepositoryPreconfigured();
        for (Map.Entry<String, VariableDeployment> entry : services.getVariableManagementService().getDeploymentsWithVariables().entrySet()) {
            for (Map.Entry<String, Variable> variableEntry : entry.getValue().getVariables().entrySet()) {
                if (variableEntry.getValue().getMetaData().isPreconfigured()) {
                    variables.addVariable(variableEntry.getKey(), variableEntry.getValue().getMetaData());
                }
            }
        }

        EventTypeRepositoryImpl eventTypes = new EventTypeRepositoryImpl(true);
        for (Map.Entry<String, EventType> entry : services.getEventTypeRepositoryBus().getNameToTypeMap().entrySet()) {
            if (entry.getValue().getMetadata().getAccessModifier() == NameAccessModifier.PRECONFIGURED) {
                eventTypes.addType(entry.getValue());
            }
        }

        return new EPCompilerPathableImpl(
            services.getVariablePathRegistry().copy(),
            services.getEventTypePathRegistry().copy(),
            services.getExprDeclaredPathRegistry().copy(),
            services.getNamedWindowPathRegistry().copy(),
            services.getTablePathRegistry().copy(),
            services.getContextPathRegistry().copy(),
            services.getScriptPathRegistry().copy(),
            eventTypes,
            variables);
    }

    public void traverseStatements(BiConsumer<EPDeployment, EPStatement> consumer) {
        for (String deploymentId : getDeploymentService().getDeployments()) {
            EPDeployment deployment = getDeploymentService().getDeployment(deploymentId);
            if (deployment == null) {
                continue;
            }
            for (EPStatement stmt : deployment.getStatements()) {
                consumer.accept(deployment, stmt);
            }
        }
    }

    public EPRuntimeStatementSelectionSPI getStatementSelectionSvc() {
        if (statementSelection == null) {
            statementSelection = new EPRuntimeStatementSelectionSPI(this);
        }
        return statementSelection;
    }

    public EPRuntimeCompileReflectiveSPI getReflectiveCompileSvc() {
        if (compileReflective == null) {
            compileReflective = new EPRuntimeCompileReflectiveSPI(new EPRuntimeCompileReflectiveService(), this);
        }
        return compileReflective;
    }

    public BeanEventType makeBeanAnonymousType(Class clazz) {
        BeanEventTypeStemService stemSvc = new BeanEventTypeStemService(Collections.emptyMap(), null, PropertyResolutionStyle.CASE_SENSITIVE, AccessorStyle.JAVABEAN);
        BeanEventTypeFactoryPrivate factoryPrivate = new BeanEventTypeFactoryPrivate(new EventBeanTypedEventFactoryRuntime(null), EventTypeFactoryImpl.INSTANCE, stemSvc);
        EventTypeMetadata metadata = new EventTypeMetadata(UuidGenerator.generate(), null, EventTypeTypeClass.STREAM, EventTypeApplicationType.CLASS, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        BeanEventTypeStem stem = stemSvc.getCreateStem(clazz, null);
        return new BeanEventType(stem, metadata, factoryPrivate, null, null, null, null);
    }
}