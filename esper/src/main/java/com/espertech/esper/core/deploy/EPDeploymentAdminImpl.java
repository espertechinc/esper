/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.core.deploy;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.deploy.*;
import com.espertech.esper.core.service.*;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.filter.FilterService;
import com.espertech.esper.util.DependencyGraph;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Deployment administrative implementation.
 */
public class EPDeploymentAdminImpl implements EPDeploymentAdminSPI
{
    private static Log log = LogFactory.getLog(EPDeploymentAdminImpl.class);

    private final EPAdministratorSPI epService;
    private final DeploymentStateService deploymentStateService;
    private final StatementEventTypeRef statementEventTypeRef;
    private final EventAdapterService eventAdapterService;
    private final StatementIsolationService statementIsolationService;
    private final StatementIdGenerator optionalStatementIdGenerator;
    private final FilterService filterService;
    private final TimeZone timeZone;
    private final ConfigurationEngineDefaults.ExceptionHandling.UndeployRethrowPolicy undeployRethrowPolicy;

    /**
     * Ctor.
     * @param epService administrative SPI
     * @param deploymentStateService deployment state maintenance service
     * @param statementEventTypeRef maintains statement-eventtype relationship
     * @param eventAdapterService event wrap service
     * @param statementIsolationService for isolated statement execution
     */
    public EPDeploymentAdminImpl(EPAdministratorSPI epService, DeploymentStateService deploymentStateService, StatementEventTypeRef statementEventTypeRef, EventAdapterService eventAdapterService, StatementIsolationService statementIsolationService, StatementIdGenerator optionalStatementIdGenerator, FilterService filterService, TimeZone timeZone, ConfigurationEngineDefaults.ExceptionHandling.UndeployRethrowPolicy undeployRethrowPolicy) {
        this.epService = epService;
        this.deploymentStateService = deploymentStateService;
        this.statementEventTypeRef = statementEventTypeRef;
        this.eventAdapterService = eventAdapterService;
        this.statementIsolationService = statementIsolationService;
        this.optionalStatementIdGenerator = optionalStatementIdGenerator;
        this.filterService = filterService;
        this.timeZone = timeZone;
        this.undeployRethrowPolicy = undeployRethrowPolicy;
    }

    public Module read(InputStream stream, String uri) throws IOException, ParseException
    {
        if (log.isDebugEnabled()) {
            log.debug("Reading module from input stream");
        }
        return EPLModuleUtil.readInternal(stream, uri);
    }

    public Module read(File file) throws IOException, ParseException {
        if (log.isDebugEnabled()) {
            log.debug("Reading resource '" + file.getAbsolutePath() + "'");
        }
        return EPLModuleUtil.readFile(file);
    }

    public Module read(URL url) throws IOException, ParseException {
        if (log.isDebugEnabled())
        {
            log.debug( "Reading resource from url: " + url.toString() );
        }
        return EPLModuleUtil.readInternal(url.openStream(), url.toString());
    }

    public Module read(String resource) throws IOException, ParseException {
        if (log.isDebugEnabled()) {
            log.debug("Reading resource '" + resource + "'");
        }
        return EPLModuleUtil.readResource(resource);
    }

    public synchronized DeploymentResult deploy(Module module, DeploymentOptions options, String assignedDeploymentId) throws DeploymentActionException
    {
        if (deploymentStateService.getDeployment(assignedDeploymentId) != null) {
            throw new IllegalArgumentException("Assigned deployment id '" + assignedDeploymentId + "' is already in use");
        }
        return deployInternal(module, options, assignedDeploymentId, Calendar.getInstance(timeZone));
    }

    public synchronized DeploymentResult deploy(Module module, DeploymentOptions options) throws DeploymentActionException
    {
        String deploymentId = deploymentStateService.nextDeploymentId();
        return deployInternal(module, options, deploymentId, Calendar.getInstance(timeZone));
    }

    private DeploymentResult deployInternal(Module module, DeploymentOptions options, String deploymentId, Calendar addedDate) throws DeploymentActionException
    {
        if (options == null) {
            options = new DeploymentOptions();
        }

        if (log.isDebugEnabled()) {
            log.debug("Deploying module " + module);
        }
        List<String> imports;
        if (module.getImports() != null) {
            for (String imported : module.getImports()) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding import " + imported);
                }
                epService.getConfiguration().addImport(imported);
            }
            imports = new ArrayList<String>(module.getImports());
        }
        else {
             imports = Collections.emptyList();
        }

        if (options.isCompile()) {
            List<DeploymentItemException> exceptions = new ArrayList<DeploymentItemException>();
            for (ModuleItem item : module.getItems()) {
                if (item.isCommentOnly()) {
                    continue;
                }

                try {
                    epService.compileEPL(item.getExpression());
                }
                catch (RuntimeException ex) {
                    exceptions.add(new DeploymentItemException(ex.getMessage(), item.getExpression(), ex, item.getLineNumber()));
                }
            }

            if (!exceptions.isEmpty()) {
                throw buildException("Compilation failed", module, exceptions);
            }
        }

        if (options.isCompileOnly()) {
            return null;
        }

        List<DeploymentItemException> exceptions = new ArrayList<DeploymentItemException>();
        List<DeploymentInformationItem> statementNames = new ArrayList<DeploymentInformationItem>();
        List<EPStatement> statements = new ArrayList<EPStatement>();
        Set<String> eventTypesReferenced = new HashSet<String>();

        for (ModuleItem item : module.getItems()) {
            if (item.isCommentOnly()) {
                continue;
            }

            String statementName = null;
            Object userObject = null;
            if (options.getStatementNameResolver() != null || options.getStatementUserObjectResolver() != null) {
                StatementDeploymentContext ctx = new StatementDeploymentContext(item.getExpression(), module, item, deploymentId);
                statementName = options.getStatementNameResolver() != null ? options.getStatementNameResolver().getStatementName(ctx) : null;
                userObject = options.getStatementUserObjectResolver() != null ? options.getStatementUserObjectResolver().getUserObject(ctx) : null;
            }

            try {
                EPStatement stmt;
                if (optionalStatementIdGenerator == null) {
                    if (options.getIsolatedServiceProvider() == null) {
                        stmt = epService.createEPL(item.getExpression(), statementName, userObject);
                    }
                    else {
                        EPServiceProviderIsolated unit = statementIsolationService.getIsolationUnit(options.getIsolatedServiceProvider(), -1);
                        stmt = unit.getEPAdministrator().createEPL(item.getExpression(), statementName, userObject);
                    }
                }
                else {
                    String statementId = optionalStatementIdGenerator.getNextStatementId();
                    if (options.getIsolatedServiceProvider() == null) {
                        stmt = epService.createEPLStatementId(item.getExpression(), statementName, userObject, statementId);
                    }
                    else {
                        EPServiceProviderIsolated unit = statementIsolationService.getIsolationUnit(options.getIsolatedServiceProvider(), -1);
                        EPAdministratorIsolatedSPI spi = (EPAdministratorIsolatedSPI) unit.getEPAdministrator();
                        stmt = spi.createEPLStatementId(item.getExpression(), statementName, userObject, statementId);
                    }
                }
                statementNames.add(new DeploymentInformationItem(stmt.getName(), stmt.getText()));
                statements.add(stmt);

                String[] types = statementEventTypeRef.getTypesForStatementName(stmt.getName());
                if (types != null) {
                    eventTypesReferenced.addAll(Arrays.asList(types));
                }
            }
            catch (EPException ex) {
                exceptions.add(new DeploymentItemException(ex.getMessage(), item.getExpression(), ex, item.getLineNumber()));
                if (options.isFailFast()) {
                    break;
                }
            }
        }

        if (!exceptions.isEmpty()) {
            if (options.isRollbackOnFail()) {
                log.debug("Rolling back intermediate statements for deployment");
                for (EPStatement stmt : statements) {
                    try {
                        stmt.destroy();
                    }
                    catch (Exception ex) {
                        log.debug("Failed to destroy created statement during rollback: " + ex.getMessage(), ex);
                    }
                }
                EPLModuleUtil.undeployTypes(eventTypesReferenced, statementEventTypeRef, eventAdapterService, filterService);
            }
            String text = "Deployment failed";
            if (options.isValidateOnly()) {
                text = "Validation failed";
            }
            throw buildException(text, module, exceptions);
        }

        if (options.isValidateOnly()) {
            log.debug("Rolling back created statements for validate-only");
            for (EPStatement stmt : statements) {
                try {
                    stmt.destroy();
                }
                catch (Exception ex) {
                    log.debug("Failed to destroy created statement during rollback: " + ex.getMessage(), ex);
                }
            }
            EPLModuleUtil.undeployTypes(eventTypesReferenced, statementEventTypeRef, eventAdapterService, filterService);
            return null;
        }

        DeploymentInformationItem[] deploymentInfoArr = statementNames.toArray(new DeploymentInformationItem[statementNames.size()]);
        DeploymentInformation desc = new DeploymentInformation(deploymentId, module, addedDate, Calendar.getInstance(timeZone), deploymentInfoArr, DeploymentState.DEPLOYED);
        deploymentStateService.addUpdateDeployment(desc);

        if (log.isDebugEnabled()) {
            log.debug("Module " + module + " was successfully deployed.");
        }
        return new DeploymentResult(desc.getDeploymentId(), Collections.unmodifiableList(statements), imports);
    }

    private DeploymentActionException buildException(String msg, Module module, List<DeploymentItemException> exceptions)
    {
        String message = msg;
        if (module.getName() != null) {
            message += " in module '" + module.getName() + "'";
        }
        if (module.getUri() != null) {
            message += " in module url '" + module.getUri() + "'";
        }
        if (exceptions.size() > 0) {
            message += " in expression '" + getAbbreviated(exceptions.get(0).getExpression()) + "' : " + exceptions.get(0).getMessage();
        }
        return new DeploymentActionException(message, exceptions);
    }

    private String getAbbreviated(String expression) {
        if (expression.length() < 60) {
            return replaceNewline(expression);
        }
        String subtext = expression.substring(0, 50) + "...(" + expression.length() + " chars)";
        return replaceNewline(subtext);
    }

    private String replaceNewline(String text) {
        text = text.replaceAll("\\n"," ");
        text = text.replaceAll("\\t"," ");
        text = text.replaceAll("\\r"," ");
        return text;
    }

    public Module parse(String eplModuleText) throws IOException, ParseException
    {
        return EPLModuleUtil.parseInternal(eplModuleText, null);
    }

    public synchronized UndeploymentResult undeployRemove(String deploymentId) throws DeploymentNotFoundException {
        return undeployRemoveInternal(deploymentId, new UndeploymentOptions());
    }

    public synchronized UndeploymentResult undeployRemove(String deploymentId, UndeploymentOptions undeploymentOptions) throws DeploymentNotFoundException {
        return undeployRemoveInternal(deploymentId, undeploymentOptions == null ? new UndeploymentOptions() : undeploymentOptions);
    }

    public synchronized UndeploymentResult undeploy(String deploymentId) throws DeploymentStateException, DeploymentNotFoundException {
        return undeployInternal(deploymentId, new UndeploymentOptions());
    }

    public synchronized UndeploymentResult undeploy(String deploymentId, UndeploymentOptions undeploymentOptions) throws DeploymentException {
        return undeployInternal(deploymentId, undeploymentOptions == null ? new UndeploymentOptions() : undeploymentOptions);
    }

    public synchronized String[] getDeployments()
    {
        return deploymentStateService.getDeployments();
    }

    public synchronized DeploymentInformation getDeployment(String deploymentId)
    {
        return deploymentStateService.getDeployment(deploymentId);
    }

    public synchronized DeploymentInformation[] getDeploymentInformation()
    {
        return deploymentStateService.getAllDeployments();
    }

    public synchronized DeploymentOrder getDeploymentOrder(Collection<Module> modules, DeploymentOrderOptions options) throws DeploymentOrderException
    {
        if (options == null) {
            options = new DeploymentOrderOptions();
        }
        String[] deployments = deploymentStateService.getDeployments();

        List<Module> proposedModules = new ArrayList<Module>();
        proposedModules.addAll(modules);

        Set<String> availableModuleNames = new HashSet<String>();
        for (Module proposedModule : proposedModules) {
            if (proposedModule.getName() != null) {
                availableModuleNames.add(proposedModule.getName());
            }
        }

        // Collect all uses-dependencies of existing modules
        Map<String, Set<String>> usesPerModuleName = new HashMap<String, Set<String>>();
        for (String deployment : deployments) {
            DeploymentInformation info = deploymentStateService.getDeployment(deployment);
            if (info == null) {
                continue;
            }
            if ((info.getModule().getName() == null) || (info.getModule().getUses() == null)) {
                continue;
            }
            Set<String> usesSet = usesPerModuleName.get(info.getModule().getName());
            if (usesSet == null) {
                usesSet = new HashSet<String>();
                usesPerModuleName.put(info.getModule().getName(), usesSet);
            }
            usesSet.addAll(info.getModule().getUses());
        }

        // Collect uses-dependencies of proposed modules
        for (Module proposedModule : proposedModules) {

            // check uses-dependency is available
            if (options.isCheckUses()) {
                if (proposedModule.getUses() != null) {
                    for (String uses : proposedModule.getUses()) {
                        if (availableModuleNames.contains(uses)) {
                            continue;
                        }
                        if (isDeployed(uses)) {
                            continue;
                        }
                        String message = "Module-dependency not found";
                        if (proposedModule.getName() != null) {
                            message += " as declared by module '" + proposedModule.getName() + "'";
                        }
                        message += " for uses-declaration '" + uses + "'";
                        throw new DeploymentOrderException(message);
                    }
                }
            }

            if ((proposedModule.getName() == null) || (proposedModule.getUses() == null)) {
                continue;
            }
            Set<String> usesSet = usesPerModuleName.get(proposedModule.getName());
            if (usesSet == null) {
                usesSet = new HashSet<String>();
                usesPerModuleName.put(proposedModule.getName(), usesSet);
            }
            usesSet.addAll(proposedModule.getUses());
        }

        Map<String, SortedSet<Integer>> proposedModuleNames = new HashMap<String, SortedSet<Integer>>();
        int count = 0;
        for (Module proposedModule : proposedModules) {
            SortedSet<Integer> moduleNumbers = proposedModuleNames.get(proposedModule.getName());
            if (moduleNumbers == null) {
                moduleNumbers = new TreeSet<Integer>();
                proposedModuleNames.put(proposedModule.getName(), moduleNumbers);
            }
            moduleNumbers.add(count);
            count++;
        }

        DependencyGraph graph = new DependencyGraph(proposedModules.size(), false);
        int fromModule = 0;
        for (Module proposedModule : proposedModules) {
            if ((proposedModule.getUses() == null) || (proposedModule.getUses().isEmpty())) {
                fromModule++;
                continue;
            }
            SortedSet<Integer> dependentModuleNumbers = new TreeSet<Integer>();
            for (String use : proposedModule.getUses()) {
                SortedSet<Integer> moduleNumbers = proposedModuleNames.get(use);
                if (moduleNumbers == null) {
                    continue;
                }
                dependentModuleNumbers.addAll(moduleNumbers);
            }
            dependentModuleNumbers.remove(fromModule);
            graph.addDependency(fromModule, dependentModuleNumbers);
            fromModule++;
        }

        if (options.isCheckCircularDependency()) {
            Stack<Integer> circular = graph.getFirstCircularDependency();
            if (circular != null) {
                String message = "";
                String delimiter = "";
                for (int i : circular) {
                    message += delimiter;
                    message += "module '" + proposedModules.get(i).getName() + "'";
                    delimiter = " uses (depends on) ";
                }
                throw new DeploymentOrderException("Circular dependency detected in module uses-relationships: " + message);
            }
        }

        List<Module> reverseDeployList = new ArrayList<Module>();
        Set<Integer> ignoreList = new HashSet<Integer>();
        while(ignoreList.size() < proposedModules.size()) {

            // seconardy sort according to the order of listing
            Set<Integer> rootNodes = new TreeSet<Integer>(new Comparator<Integer>() {
                public int compare(Integer o1, Integer o2)
                {
                    return -1 * o1.compareTo(o2);
                }
            });
            rootNodes.addAll(graph.getRootNodes(ignoreList));

            if (rootNodes.isEmpty()) {   // circular dependency could cause this
                for (int i = 0; i < proposedModules.size(); i++) {
                    if (!ignoreList.contains(i)) {
                        rootNodes.add(i);
                        break;
                    }
                }
            }

            for (Integer root : rootNodes) {
                ignoreList.add(root);
                reverseDeployList.add(proposedModules.get(root));
            }
        }

        Collections.reverse(reverseDeployList);
        return new DeploymentOrder(reverseDeployList);
    }

    public synchronized boolean isDeployed(String moduleName) {
        DeploymentInformation[] infos = deploymentStateService.getAllDeployments();
        if (infos == null) {
            return false;
        }
        for (DeploymentInformation info : infos) {
            if ((info.getModule().getName() != null) && (info.getModule().getName().equals(moduleName))) {
                return info.getState() == DeploymentState.DEPLOYED;
            }
        }
        return false;
    }

    public synchronized DeploymentResult readDeploy(InputStream stream, String moduleURI, String moduleArchive, Object userObject) throws IOException, ParseException, DeploymentOrderException, DeploymentActionException
    {
        Module module = EPLModuleUtil.readInternal(stream, moduleURI);
        return deployQuick(module, moduleURI, moduleArchive, userObject);
    }

    public synchronized DeploymentResult readDeploy(String resource, String moduleURI, String moduleArchive, Object userObject) throws IOException, ParseException, DeploymentOrderException, DeploymentActionException
    {
        Module module = read(resource);
        return deployQuick(module, moduleURI, moduleArchive, userObject);
    }

    public synchronized DeploymentResult parseDeploy(String eplModuleText) throws IOException, ParseException, DeploymentException {
        return parseDeploy(eplModuleText, null, null, null);
    }

    public synchronized DeploymentResult parseDeploy(String buffer, String moduleURI, String moduleArchive, Object userObject) throws IOException, ParseException, DeploymentOrderException, DeploymentActionException
    {
        Module module = EPLModuleUtil.parseInternal(buffer, moduleURI);
        return deployQuick(module, moduleURI, moduleArchive, userObject);
    }

    public synchronized void add(Module module, String assignedDeploymentId)
    {
        if (deploymentStateService.getDeployment(assignedDeploymentId) != null) {
            throw new IllegalArgumentException("Assigned deployment id '" + assignedDeploymentId + "' is already in use");
        }
        addInternal(module, assignedDeploymentId);
    }

    public synchronized String add(Module module)
    {
        String deploymentId = deploymentStateService.nextDeploymentId();
        addInternal(module, deploymentId);
        return deploymentId;
    }

    private void addInternal(Module module, String deploymentId) {

        DeploymentInformation desc = new DeploymentInformation(deploymentId, module, Calendar.getInstance(timeZone), Calendar.getInstance(timeZone), new DeploymentInformationItem[0], DeploymentState.UNDEPLOYED);
        deploymentStateService.addUpdateDeployment(desc);
    }

    public synchronized DeploymentResult deploy(String deploymentId, DeploymentOptions options) throws DeploymentNotFoundException, DeploymentStateException, DeploymentOrderException, DeploymentActionException
    {
        DeploymentInformation info = deploymentStateService.getDeployment(deploymentId);
        if (info == null) {
            throw new DeploymentNotFoundException("Deployment by id '" + deploymentId + "' could not be found");
        }
        if (info.getState() == DeploymentState.DEPLOYED) {
            throw new DeploymentStateException("Module by deployment id '" + deploymentId + "' is already in deployed state");
        }
        getDeploymentOrder(Collections.singletonList(info.getModule()), null);
        return deployInternal(info.getModule(), options, deploymentId, info.getAddedDate());
    }

    public synchronized void remove(String deploymentId) throws DeploymentStateException, DeploymentNotFoundException
    {
        DeploymentInformation info = deploymentStateService.getDeployment(deploymentId);
        if (info == null) {
            throw new DeploymentNotFoundException("Deployment by id '" + deploymentId + "' could not be found");
        }
        if (info.getState() == DeploymentState.DEPLOYED) {
            throw new DeploymentStateException("Deployment by id '" + deploymentId + "' is in deployed state, please undeploy first");
        }
        deploymentStateService.remove(deploymentId);
    }

    private synchronized UndeploymentResult undeployRemoveInternal(String deploymentId, UndeploymentOptions options) throws DeploymentNotFoundException {
        DeploymentInformation info = deploymentStateService.getDeployment(deploymentId);
        if (info == null) {
            throw new DeploymentNotFoundException("Deployment by id '" + deploymentId + "' could not be found");
        }

        UndeploymentResult result;
        if (info.getState() == DeploymentState.DEPLOYED) {
            result = undeployRemoveInternal(info, options);
        }
        else {
            result = new UndeploymentResult(deploymentId, Collections.<DeploymentInformationItem>emptyList());
        }
        deploymentStateService.remove(deploymentId);
        return result;
    }

    private UndeploymentResult undeployInternal(String deploymentId, UndeploymentOptions undeploymentOptions) throws DeploymentStateException, DeploymentNotFoundException
    {
        DeploymentInformation info = deploymentStateService.getDeployment(deploymentId);
        if (info == null) {
            throw new DeploymentNotFoundException("Deployment by id '" + deploymentId + "' could not be found");
        }
        if (info.getState() == DeploymentState.UNDEPLOYED) {
            throw new DeploymentStateException("Deployment by id '" + deploymentId + "' is already in undeployed state");
        }

        UndeploymentResult result = undeployRemoveInternal(info, undeploymentOptions);
        DeploymentInformation updated = new DeploymentInformation(deploymentId, info.getModule(), info.getAddedDate(), Calendar.getInstance(timeZone), new DeploymentInformationItem[0], DeploymentState.UNDEPLOYED);
        deploymentStateService.addUpdateDeployment(updated);
        return result;
    }

    private UndeploymentResult undeployRemoveInternal(DeploymentInformation info, UndeploymentOptions undeploymentOptions)
    {
        DeploymentInformationItem[] reverted = new DeploymentInformationItem[info.getItems().length];
        for (int i = 0; i < info.getItems().length; i++) {
            reverted[i] = info.getItems()[info.getItems().length - 1 - i];
        }

        List<DeploymentInformationItem> revertedStatements = new ArrayList<DeploymentInformationItem>();
        if (undeploymentOptions.isDestroyStatements()) {
            Set<String> referencedTypes = new HashSet<String>();

            RuntimeException firstExceptionEncountered = null;

            for (DeploymentInformationItem item : reverted) {
                EPStatement statement = epService.getStatement(item.getStatementName());
                if (statement == null) {
                    log.debug("Deployment id '" + info.getDeploymentId() + "' statement name '" + item + "' not found");
                    continue;
                }
                referencedTypes.addAll(Arrays.asList(statementEventTypeRef.getTypesForStatementName(statement.getName())));
                if (statement.isDestroyed()) {
                    continue;
                }
                try {
                    statement.destroy();
                }
                catch (RuntimeException ex) {
                    log.warn("Unexpected exception destroying statement: " + ex.getMessage(), ex);
                    if (firstExceptionEncountered == null) {
                        firstExceptionEncountered = ex;
                    }
                }
                revertedStatements.add(item);
            }
            EPLModuleUtil.undeployTypes(referencedTypes, statementEventTypeRef, eventAdapterService, filterService);
            Collections.reverse(revertedStatements);

            if (firstExceptionEncountered != null && undeployRethrowPolicy == ConfigurationEngineDefaults.ExceptionHandling.UndeployRethrowPolicy.RETHROW_FIRST) {
                throw firstExceptionEncountered;
            }
        }

        return new UndeploymentResult(info.getDeploymentId(), revertedStatements);
    }

    private DeploymentResult deployQuick(Module module, String moduleURI, String moduleArchive, Object userObject) throws IOException, ParseException, DeploymentOrderException, DeploymentActionException
    {
        module.setUri(moduleURI);
        module.setArchiveName(moduleArchive);
        module.setUserObject(userObject);
        getDeploymentOrder(Collections.singletonList(module), null);
        return deploy(module, null);
    }
}
