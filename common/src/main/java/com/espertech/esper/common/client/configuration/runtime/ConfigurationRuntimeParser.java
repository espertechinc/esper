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
package com.espertech.esper.common.client.configuration.runtime;

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.util.FilterServiceProfile;
import com.espertech.esper.common.client.util.Locking;
import com.espertech.esper.common.client.util.TimeSourceType;
import com.espertech.esper.common.client.util.UndeployRethrowPolicy;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.type.StringPatternSet;
import com.espertech.esper.common.internal.type.StringPatternSetLike;
import com.espertech.esper.common.internal.type.StringPatternSetRegex;
import com.espertech.esper.common.internal.util.DOMElementIterator;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.*;

import static com.espertech.esper.common.internal.util.DOMUtil.*;

/**
 * Parser for the runtime section of configuration.
 */
public class ConfigurationRuntimeParser {
    /**
     * Configure the runtime section from a provided element
     *
     * @param runtime        runtime section
     * @param runtimeElement element
     */
    public static void doConfigure(ConfigurationRuntime runtime, Element runtimeElement) {
        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(runtimeElement.getChildNodes());
        while (eventTypeNodeIterator.hasNext()) {
            Element element = eventTypeNodeIterator.next();
            String nodeName = element.getNodeName();
            if (nodeName.equals("plugin-loader")) {
                handlePluginLoaders(runtime, element);
            } else if (nodeName.equals("threading")) {
                handleThreading(runtime, element);
            } else if (nodeName.equals("logging")) {
                handleLogging(runtime, element);
            } else if (nodeName.equals("variables")) {
                handleVariables(runtime, element);
            } else if (nodeName.equals("time-source")) {
                handleTimeSource(runtime, element);
            } else if (nodeName.equals("metrics-reporting")) {
                handleMetricsReporting(runtime, element);
            } else if (nodeName.equals("exceptionHandling")) {
                handleExceptionHandling(runtime, element);
            } else if (nodeName.equals("conditionHandling")) {
                handleConditionHandling(runtime, element);
            } else if (nodeName.equals("patterns")) {
                handlePatterns(runtime, element);
            } else if (nodeName.equals("match-recognize")) {
                handleMatchRecognize(runtime, element);
            } else if (nodeName.equals("expression")) {
                handleExpression(runtime, element);
            } else if (nodeName.equals("execution")) {
                handleExecution(runtime, element);
            }
        }
    }

    private static void handleExecution(ConfigurationRuntime runtime, Element parentElement) {
        parseOptionalBoolean(parentElement, "prioritized", b -> runtime.getExecution().setPrioritized(b));
        parseOptionalBoolean(parentElement, "fairlock", b -> runtime.getExecution().setFairlock(b));
        parseOptionalBoolean(parentElement, "disable-locking", b -> runtime.getExecution().setDisableLocking(b));

        String filterServiceProfileStr = getOptionalAttribute(parentElement, "filter-service-profile");
        if (filterServiceProfileStr != null) {
            FilterServiceProfile profile = FilterServiceProfile.valueOf(filterServiceProfileStr.toUpperCase(Locale.ENGLISH));
            runtime.getExecution().setFilterServiceProfile(profile);
        }

        String declExprValueCacheSizeStr = getOptionalAttribute(parentElement, "declared-expr-value-cache-size");
        if (declExprValueCacheSizeStr != null) {
            runtime.getExecution().setDeclaredExprValueCacheSize(Integer.parseInt(declExprValueCacheSizeStr));
        }
    }

    private static void handleExpression(ConfigurationRuntime runtime, Element element) {
        parseOptionalBoolean(element, "self-subselect-preeval", b -> runtime.getExpression().setSelfSubselectPreeval(b));

        String timeZoneStr = getOptionalAttribute(element, "time-zone");
        if (timeZoneStr != null) {
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
            runtime.getExpression().setTimeZone(timeZone);
        }
    }

    private static void handleMatchRecognize(ConfigurationRuntime runtime, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("max-state")) {
                String valueText = getRequiredAttribute(subElement, "value");
                Long value = Long.parseLong(valueText);
                runtime.getMatchRecognize().setMaxStates(value);

                String preventText = getOptionalAttribute(subElement, "prevent-start");
                if (preventText != null) {
                    runtime.getMatchRecognize().setMaxStatesPreventStart(Boolean.parseBoolean(preventText));
                }
            }
        }
    }

    private static void handlePatterns(ConfigurationRuntime runtime, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("max-subexpression")) {
                String valueText = getRequiredAttribute(subElement, "value");
                Long value = Long.parseLong(valueText);
                runtime.getPatterns().setMaxSubexpressions(value);

                String preventText = getOptionalAttribute(subElement, "prevent-start");
                if (preventText != null) {
                    runtime.getPatterns().setMaxSubexpressionPreventStart(Boolean.parseBoolean(preventText));
                }
            }
        }
    }

    private static void handleConditionHandling(ConfigurationRuntime runtime, Element element) {
        runtime.getConditionHandling().addClasses(getHandlerFactories(element));
    }

    private static void handleExceptionHandling(ConfigurationRuntime runtime, Element element) {
        runtime.getExceptionHandling().addClasses(getHandlerFactories(element));
        String enableUndeployRethrowStr = getOptionalAttribute(element, "undeploy-rethrow-policy");
        if (enableUndeployRethrowStr != null) {
            runtime.getExceptionHandling().setUndeployRethrowPolicy(UndeployRethrowPolicy.valueOf(enableUndeployRethrowStr.toUpperCase(Locale.ENGLISH)));
        }
    }

    private static void handleMetricsReporting(ConfigurationRuntime runtime, Element element) {
        parseOptionalBoolean(element, "enabled", b -> runtime.getMetricsReporting().setEnableMetricsReporting(b));

        String runtimeInterval = getOptionalAttribute(element, "runtime-interval");
        if (runtimeInterval != null) {
            runtime.getMetricsReporting().setRuntimeInterval(Long.parseLong(runtimeInterval));
        }

        String statementInterval = getOptionalAttribute(element, "statement-interval");
        if (statementInterval != null) {
            runtime.getMetricsReporting().setStatementInterval(Long.parseLong(statementInterval));
        }

        String threading = getOptionalAttribute(element, "threading");
        if (threading != null) {
            runtime.getMetricsReporting().setThreading(Boolean.parseBoolean(threading));
        }

        String jmxRuntimeMetrics = getOptionalAttribute(element, "jmx-runtime-metrics");
        if (jmxRuntimeMetrics != null) {
            runtime.getMetricsReporting().setJmxRuntimeMetrics(Boolean.parseBoolean(jmxRuntimeMetrics));
        }

        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("stmtgroup")) {
                String name = getRequiredAttribute(subElement, "name");
                long interval = Long.parseLong(getRequiredAttribute(subElement, "interval"));

                ConfigurationRuntimeMetricsReporting.StmtGroupMetrics metrics = new ConfigurationRuntimeMetricsReporting.StmtGroupMetrics();
                metrics.setInterval(interval);
                runtime.getMetricsReporting().addStmtGroup(name, metrics);

                String defaultInclude = getOptionalAttribute(subElement, "default-include");
                if (defaultInclude != null) {
                    metrics.setDefaultInclude(Boolean.parseBoolean(defaultInclude));
                }

                String numStmts = getOptionalAttribute(subElement, "num-stmts");
                if (numStmts != null) {
                    metrics.setNumStatements(Integer.parseInt(numStmts));
                }

                String reportInactive = getOptionalAttribute(subElement, "report-inactive");
                if (reportInactive != null) {
                    metrics.setReportInactive(Boolean.parseBoolean(reportInactive));
                }

                handleMetricsReportingPatterns(metrics, subElement);
            }
        }

    }

    private static void handleTimeSource(ConfigurationRuntime runtime, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("time-source-type")) {
                String valueText = getRequiredAttribute(subElement, "value");
                if (valueText == null) {
                    throw new ConfigurationException("No value attribute supplied for time-source element");
                }
                TimeSourceType timeSourceType;
                if (valueText.toUpperCase(Locale.ENGLISH).trim().equals("NANO")) {
                    timeSourceType = TimeSourceType.NANO;
                } else if (valueText.toUpperCase(Locale.ENGLISH).trim().equals("MILLI")) {
                    timeSourceType = TimeSourceType.MILLI;
                } else {
                    throw new ConfigurationException("Value attribute for time-source element invalid, " +
                            "expected one of the following keywords: nano, milli");
                }
                runtime.getTimeSource().setTimeSourceType(timeSourceType);
            }
        }
    }

    private static void handleVariables(ConfigurationRuntime runtime, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("msec-version-release")) {
                String valueText = getRequiredAttribute(subElement, "value");
                Long value = Long.parseLong(valueText);
                runtime.getVariables().setMsecVersionRelease(value);
            }
        }
    }

    private static void handleLogging(ConfigurationRuntime runtime, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("execution-path")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                runtime.getLogging().setEnableExecutionDebug(value);
            }
            if (subElement.getNodeName().equals("timer-debug")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                runtime.getLogging().setEnableTimerDebug(value);
            }
            if (subElement.getNodeName().equals("audit")) {
                runtime.getLogging().setAuditPattern(getOptionalAttribute(subElement, "pattern"));
            }
        }
    }

    private static void handleThreading(ConfigurationRuntime runtime, Element element) {
        parseOptionalBoolean(element, "runtime-fairlock", b -> runtime.getThreading().setRuntimeFairlock(b));

        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("listener-dispatch")) {
                String preserveOrderText = getRequiredAttribute(subElement, "preserve-order");
                Boolean preserveOrder = Boolean.parseBoolean(preserveOrderText);
                runtime.getThreading().setListenerDispatchPreserveOrder(preserveOrder);

                if (subElement.getAttributes().getNamedItem("timeout-msec") != null) {
                    String timeoutMSecText = subElement.getAttributes().getNamedItem("timeout-msec").getTextContent();
                    Long timeoutMSec = Long.parseLong(timeoutMSecText);
                    runtime.getThreading().setListenerDispatchTimeout(timeoutMSec);
                }

                if (subElement.getAttributes().getNamedItem("locking") != null) {
                    String value = subElement.getAttributes().getNamedItem("locking").getTextContent();
                    runtime.getThreading().setListenerDispatchLocking(
                            Locking.valueOf(value.toUpperCase(Locale.ENGLISH)));
                }
            }
            if (subElement.getNodeName().equals("insert-into-dispatch")) {
                String preserveOrderText = getRequiredAttribute(subElement, "preserve-order");
                Boolean preserveOrder = Boolean.parseBoolean(preserveOrderText);
                runtime.getThreading().setInsertIntoDispatchPreserveOrder(preserveOrder);

                if (subElement.getAttributes().getNamedItem("timeout-msec") != null) {
                    String timeoutMSecText = subElement.getAttributes().getNamedItem("timeout-msec").getTextContent();
                    Long timeoutMSec = Long.parseLong(timeoutMSecText);
                    runtime.getThreading().setInsertIntoDispatchTimeout(timeoutMSec);
                }

                if (subElement.getAttributes().getNamedItem("locking") != null) {
                    String value = subElement.getAttributes().getNamedItem("locking").getTextContent();
                    runtime.getThreading().setInsertIntoDispatchLocking(
                            Locking.valueOf(value.toUpperCase(Locale.ENGLISH)));
                }
            }
            if (subElement.getNodeName().equals("named-window-consumer-dispatch")) {
                String preserveOrderText = getRequiredAttribute(subElement, "preserve-order");
                Boolean preserveOrder = Boolean.parseBoolean(preserveOrderText);
                runtime.getThreading().setNamedWindowConsumerDispatchPreserveOrder(preserveOrder);

                if (subElement.getAttributes().getNamedItem("timeout-msec") != null) {
                    String timeoutMSecText = subElement.getAttributes().getNamedItem("timeout-msec").getTextContent();
                    Long timeoutMSec = Long.parseLong(timeoutMSecText);
                    runtime.getThreading().setNamedWindowConsumerDispatchTimeout(timeoutMSec);
                }

                if (subElement.getAttributes().getNamedItem("locking") != null) {
                    String value = subElement.getAttributes().getNamedItem("locking").getTextContent();
                    runtime.getThreading().setNamedWindowConsumerDispatchLocking(
                            Locking.valueOf(value.toUpperCase(Locale.ENGLISH)));
                }
            }
            if (subElement.getNodeName().equals("internal-timer")) {
                String enabledText = getRequiredAttribute(subElement, "enabled");
                Boolean enabled = Boolean.parseBoolean(enabledText);
                String msecResolutionText = getRequiredAttribute(subElement, "msec-resolution");
                Long msecResolution = Long.parseLong(msecResolutionText);
                runtime.getThreading().setInternalTimerEnabled(enabled);
                runtime.getThreading().setInternalTimerMsecResolution(msecResolution);
            }
            if (subElement.getNodeName().equals("threadpool-inbound")) {
                ThreadPoolConfig result = parseThreadPoolConfig(subElement);
                runtime.getThreading().setThreadPoolInbound(result.isEnabled());
                runtime.getThreading().setThreadPoolInboundNumThreads(result.getNumThreads());
                runtime.getThreading().setThreadPoolInboundCapacity(result.getCapacity());
            }
            if (subElement.getNodeName().equals("threadpool-outbound")) {
                ThreadPoolConfig result = parseThreadPoolConfig(subElement);
                runtime.getThreading().setThreadPoolOutbound(result.isEnabled());
                runtime.getThreading().setThreadPoolOutboundNumThreads(result.getNumThreads());
                runtime.getThreading().setThreadPoolOutboundCapacity(result.getCapacity());
            }
            if (subElement.getNodeName().equals("threadpool-timerexec")) {
                ThreadPoolConfig result = parseThreadPoolConfig(subElement);
                runtime.getThreading().setThreadPoolTimerExec(result.isEnabled());
                runtime.getThreading().setThreadPoolTimerExecNumThreads(result.getNumThreads());
                runtime.getThreading().setThreadPoolTimerExecCapacity(result.getCapacity());
            }
            if (subElement.getNodeName().equals("threadpool-routeexec")) {
                ThreadPoolConfig result = parseThreadPoolConfig(subElement);
                runtime.getThreading().setThreadPoolRouteExec(result.isEnabled());
                runtime.getThreading().setThreadPoolRouteExecNumThreads(result.getNumThreads());
                runtime.getThreading().setThreadPoolRouteExecCapacity(result.getCapacity());
            }
        }
    }

    private static void handlePluginLoaders(ConfigurationRuntime configuration, Element element) {
        String loaderName = getRequiredAttribute(element, "name");
        String className = getRequiredAttribute(element, "class-name");
        Properties properties = new Properties();
        String configXML = null;
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("init-arg")) {
                String name = getRequiredAttribute(subElement, "name");
                String value = getRequiredAttribute(subElement, "value");
                properties.put(name, value);
            }
            if (subElement.getNodeName().equals("config-xml")) {
                DOMElementIterator nodeIter = new DOMElementIterator(subElement.getChildNodes());
                if (!nodeIter.hasNext()) {
                    throw new ConfigurationException("Error handling config-xml for plug-in loader '" + loaderName + "', no child node found under initializer element, expecting an element node");
                }

                StringWriter output = new StringWriter();
                try {
                    TransformerFactory.newInstance().newTransformer().transform(new DOMSource(nodeIter.next()), new StreamResult(output));
                } catch (TransformerException e) {
                    throw new ConfigurationException("Error handling config-xml for plug-in loader '" + loaderName + "' :" + e.getMessage(), e);
                }
                configXML = output.toString();
            }
        }
        configuration.addPluginLoader(loaderName, className, properties, configXML);
    }

    private static ThreadPoolConfig parseThreadPoolConfig(Element parentElement) {
        String enabled = getRequiredAttribute(parentElement, "enabled");
        boolean isEnabled = Boolean.parseBoolean(enabled);

        String numThreadsStr = getRequiredAttribute(parentElement, "num-threads");
        int numThreads = Integer.parseInt(numThreadsStr);

        String capacityStr = getOptionalAttribute(parentElement, "capacity");
        Integer capacity = null;
        if (capacityStr != null) {
            capacity = Integer.parseInt(capacityStr);
        }

        return new ThreadPoolConfig(isEnabled, numThreads, capacity);
    }

    private static void handleMetricsReportingPatterns(ConfigurationRuntimeMetricsReporting.StmtGroupMetrics groupDef, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("include-regex")) {
                String text = subElement.getChildNodes().item(0).getTextContent();
                groupDef.getPatterns().add(new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex(text), true));
            }
            if (subElement.getNodeName().equals("exclude-regex")) {
                String text = subElement.getChildNodes().item(0).getTextContent();
                groupDef.getPatterns().add(new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex(text), false));
            }
            if (subElement.getNodeName().equals("include-like")) {
                String text = subElement.getChildNodes().item(0).getTextContent();
                groupDef.getPatterns().add(new Pair<StringPatternSet, Boolean>(new StringPatternSetLike(text), true));
            }
            if (subElement.getNodeName().equals("exclude-like")) {
                String text = subElement.getChildNodes().item(0).getTextContent();
                groupDef.getPatterns().add(new Pair<StringPatternSet, Boolean>(new StringPatternSetLike(text), false));
            }
        }
    }

    private static List<String> getHandlerFactories(Element parentElement) {
        List<String> list = new ArrayList<String>();
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("handlerFactory")) {
                String text = getRequiredAttribute(subElement, "class");
                list.add(text);
            }
        }
        return list;
    }

    private static class ThreadPoolConfig {
        private boolean enabled;
        private int numThreads;
        private Integer capacity;

        public ThreadPoolConfig(boolean enabled, int numThreads, Integer capacity) {
            this.enabled = enabled;
            this.numThreads = numThreads;
            this.capacity = capacity;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getNumThreads() {
            return numThreads;
        }

        public Integer getCapacity() {
            return capacity;
        }
    }
}
