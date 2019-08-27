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
package com.espertech.esper.common.client.configuration.compiler;

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.soda.StreamSelector;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.util.DOMElementIterator;
import org.w3c.dom.Element;

import java.math.MathContext;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.util.DOMUtil.*;

/**
 * Parser for the compiler section of configuration.
 */
public class ConfigurationCompilerParser {

    /**
     * Configure the compiler section from a provided element
     *
     * @param compiler        compiler section
     * @param compilerElement element
     */
    public static void doConfigure(ConfigurationCompiler compiler, Element compilerElement) {
        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(compilerElement.getChildNodes());
        while (eventTypeNodeIterator.hasNext()) {
            Element element = eventTypeNodeIterator.next();
            String nodeName = element.getNodeName();
            if (nodeName.equals("plugin-view")) {
                handlePlugInView(compiler, element);
            } else if (nodeName.equals("plugin-virtualdw")) {
                handlePlugInVirtualDW(compiler, element);
            } else if (nodeName.equals("plugin-aggregation-function")) {
                handlePlugInAggregation(compiler, element);
            } else if (nodeName.equals("plugin-aggregation-multifunction")) {
                handlePlugInMultiFunctionAggregation(compiler, element);
            } else if (nodeName.equals("plugin-singlerow-function")) {
                handlePlugInSingleRow(compiler, element);
            } else if (nodeName.equals("plugin-pattern-guard")) {
                handlePlugInPatternGuard(compiler, element);
            } else if (nodeName.equals("plugin-pattern-observer")) {
                handlePlugInPatternObserver(compiler, element);
            } else if (nodeName.equals("plugin-method-datetime")) {
                handlePlugInDateTimeMethod(compiler, element);
            } else if (nodeName.equals("plugin-method-enum")) {
                handlePlugInEnumMethod(compiler, element);
            } else if (nodeName.equals("bytecode")) {
                handleByteCode(compiler, element);
            } else if (nodeName.equals("logging")) {
                handleLogging(compiler, element);
            } else if (nodeName.equals("stream-selection")) {
                handleStreamSelection(compiler, element);
            } else if (nodeName.equals("language")) {
                handleLanguage(compiler, element);
            } else if (nodeName.equals("scripts")) {
                handleScripts(compiler, element);
            } else if (nodeName.equals("expression")) {
                handleExpression(compiler, element);
            } else if (nodeName.equals("execution")) {
                handleExecution(compiler, element);
            } else if (nodeName.equals("view-resources")) {
                handleViewResources(compiler, element);
            } else if (nodeName.equals("serde-settings")) {
                handleSerdeSettings(compiler, element);
            }
        }
    }

    private static void handleViewResources(ConfigurationCompiler compiler, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("iterable-unbound")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                compiler.getViewResources().setIterableUnbound(value);
            }
            if (subElement.getNodeName().equals("outputlimitopt")) {
                parseRequiredBoolean(subElement, "enabled", b -> compiler.getViewResources().setOutputLimitOpt(b));
            }
        }
    }

    private static void handleExecution(ConfigurationCompiler compiler, Element element) {
        String filterServiceMaxFilterWidthStr = getOptionalAttribute(element, "filter-service-max-filter-width");
        if (filterServiceMaxFilterWidthStr != null) {
            compiler.getExecution().setFilterServiceMaxFilterWidth(Integer.parseInt(filterServiceMaxFilterWidthStr));
        }
        parseOptionalBoolean(element, "enable-declared-expr-value-cache", b -> compiler.getExecution().setEnabledDeclaredExprValueCache(b));
    }

    private static void handleExpression(ConfigurationCompiler compiler, Element element) {
        String integerDivision = getOptionalAttribute(element, "integer-division");
        if (integerDivision != null) {
            boolean isIntegerDivision = Boolean.parseBoolean(integerDivision);
            compiler.getExpression().setIntegerDivision(isIntegerDivision);
        }
        String divZero = getOptionalAttribute(element, "division-by-zero-is-null");
        if (divZero != null) {
            boolean isDivZero = Boolean.parseBoolean(divZero);
            compiler.getExpression().setDivisionByZeroReturnsNull(isDivZero);
        }
        String udfCache = getOptionalAttribute(element, "udf-cache");
        if (udfCache != null) {
            boolean isUdfCache = Boolean.parseBoolean(udfCache);
            compiler.getExpression().setUdfCache(isUdfCache);
        }
        String extendedAggregationStr = getOptionalAttribute(element, "extended-agg");
        if (extendedAggregationStr != null) {
            boolean extendedAggregation = Boolean.parseBoolean(extendedAggregationStr);
            compiler.getExpression().setExtendedAggregation(extendedAggregation);
        }
        String duckTypingStr = getOptionalAttribute(element, "ducktyping");
        if (duckTypingStr != null) {
            boolean duckTyping = Boolean.parseBoolean(duckTypingStr);
            compiler.getExpression().setDuckTyping(duckTyping);
        }
        String mathContextStr = getOptionalAttribute(element, "math-context");
        if (mathContextStr != null) {
            try {
                MathContext mathContext = new MathContext(mathContextStr);
                compiler.getExpression().setMathContext(mathContext);
            } catch (IllegalArgumentException ex) {
                throw new ConfigurationException("Failed to parse '" + mathContextStr + "' as a MathContext");
            }
        }
    }

    private static void handleScripts(ConfigurationCompiler compiler, Element element) {
        String defaultDialect = getOptionalAttribute(element, "default-dialect");
        if (defaultDialect != null) {
            compiler.getScripts().setDefaultDialect(defaultDialect);
        }
        parseOptionalBoolean(element, "enabled", b -> compiler.getScripts().setEnabled(b));
    }

    private static void handleLanguage(ConfigurationCompiler compiler, Element element) {
        parseOptionalBoolean(element, "sort-using-collator", b -> compiler.getLanguage().setSortUsingCollator(b));
    }

    private static void handleStreamSelection(ConfigurationCompiler compiler, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("stream-selector")) {
                String valueText = getRequiredAttribute(subElement, "value");
                if (valueText == null) {
                    throw new ConfigurationException("No value attribute supplied for stream-selector element");
                }
                StreamSelector defaultSelector;
                if (valueText.toUpperCase(Locale.ENGLISH).trim().equals("ISTREAM")) {
                    defaultSelector = StreamSelector.ISTREAM_ONLY;
                } else if (valueText.toUpperCase(Locale.ENGLISH).trim().equals("RSTREAM")) {
                    defaultSelector = StreamSelector.RSTREAM_ONLY;
                } else if (valueText.toUpperCase(Locale.ENGLISH).trim().equals("IRSTREAM")) {
                    defaultSelector = StreamSelector.RSTREAM_ISTREAM_BOTH;
                } else {
                    throw new ConfigurationException("Value attribute for stream-selector element invalid, " +
                        "expected one of the following keywords: istream, irstream, rstream");
                }
                compiler.getStreamSelection().setDefaultStreamSelector(defaultSelector);
            }
        }
    }

    private static void handleLogging(ConfigurationCompiler compiler, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("code")) {
                parseRequiredBoolean(subElement, "enabled", b -> compiler.getLogging().setEnableCode(b));
            }
        }
    }

    private static void handleByteCode(ConfigurationCompiler compiler, Element element) {
        ConfigurationCompilerByteCode codegen = compiler.getByteCode();
        parseOptionalBoolean(element, "include-debugsymbols", codegen::setIncludeDebugSymbols);
        parseOptionalBoolean(element, "include-comments", codegen::setIncludeComments);
        parseOptionalBoolean(element, "attach-epl", codegen::setAttachEPL);
        parseOptionalBoolean(element, "attach-module-epl", codegen::setAttachModuleEPL);
        parseOptionalBoolean(element, "attach-pattern-epl", codegen::setAttachPatternEPL);
        parseOptionalBoolean(element, "instrumented", codegen::setInstrumented);
        parseOptionalBoolean(element, "allow-subscriber", codegen::setAllowSubscriber);
        parseOptionalInteger(element, "threadpool-compiler-num-threads", codegen::setThreadPoolCompilerNumThreads);
        parseOptionalInteger(element, "threadpool-compiler-capacity", codegen::setThreadPoolCompilerCapacity);
        parseOptionalInteger(element, "max-methods-per-class", codegen::setMaxMethodsPerClass);

        parseOptionalAccessMod(element, "access-modifier-context", codegen::setAccessModifierContext);
        parseOptionalAccessMod(element, "access-modifier-event-type", codegen::setAccessModifierEventType);
        parseOptionalAccessMod(element, "access-modifier-expression", codegen::setAccessModifierExpression);
        parseOptionalAccessMod(element, "access-modifier-named-window", codegen::setAccessModifierNamedWindow);
        parseOptionalAccessMod(element, "access-modifier-script", codegen::setAccessModifierScript);
        parseOptionalAccessMod(element, "access-modifier-table", codegen::setAccessModifierTable);
        parseOptionalAccessMod(element, "access-modifier-variable", codegen::setAccessModifierVariable);

        String busModifierEventType = getOptionalAttribute(element, "bus-modifier-event-type");
        if (busModifierEventType != null) {
            try {
                codegen.setBusModifierEventType(EventTypeBusModifier.valueOf(busModifierEventType.trim().toUpperCase(Locale.ENGLISH)));
            } catch (Throwable t) {
                throw new ConfigurationException(t.getMessage(), t);
            }
        }
    }

    private static void parseOptionalAccessMod(Element element, String name, Consumer<NameAccessModifier> accessModifier) {
        String value = getOptionalAttribute(element, name);
        if (value != null) {
            try {
                accessModifier.accept(NameAccessModifier.valueOf(value.trim().toUpperCase(Locale.ENGLISH)));
            } catch (Throwable t) {
                throw new ConfigurationException(t.getMessage(), t);
            }
        }
    }

    private static void handlePlugInView(ConfigurationCompiler configuration, Element element) {
        String namespace = getRequiredAttribute(element, "namespace");
        String name = getRequiredAttribute(element, "name");
        String forgeClassName = getRequiredAttribute(element, "forge-class");
        configuration.addPlugInView(namespace, name, forgeClassName);
    }

    private static void handlePlugInVirtualDW(ConfigurationCompiler configuration, Element element) {
        String namespace = getRequiredAttribute(element, "namespace");
        String name = getRequiredAttribute(element, "name");
        String forgeClassName = getRequiredAttribute(element, "forge-class");
        String config = getOptionalAttribute(element, "config");
        configuration.addPlugInVirtualDataWindow(namespace, name, forgeClassName, config);
    }

    private static void handlePlugInAggregation(ConfigurationCompiler configuration, Element element) {
        String name = getRequiredAttribute(element, "name");
        String forgeClassName = getRequiredAttribute(element, "forge-class");
        configuration.addPlugInAggregationFunctionForge(name, forgeClassName);
    }

    private static void handlePlugInDateTimeMethod(ConfigurationCompiler configuration, Element element) {
        String methodName = getRequiredAttribute(element, "method-name");
        String forgeClassName = getRequiredAttribute(element, "forge-class");
        configuration.addPlugInDateTimeMethod(methodName, forgeClassName);
    }

    private static void handlePlugInEnumMethod(ConfigurationCompiler configuration, Element element) {
        String methodName = getRequiredAttribute(element, "method-name");
        String forgeClassName = getRequiredAttribute(element, "forge-class");
        configuration.addPlugInEnumMethod(methodName, forgeClassName);
    }

    private static void handlePlugInMultiFunctionAggregation(ConfigurationCompiler configuration, Element element) {
        String functionNames = getRequiredAttribute(element, "function-names");
        String forgeClassName = getOptionalAttribute(element, "forge-class");

        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        Map<String, Object> additionalProps = null;
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("init-arg")) {
                String name = getRequiredAttribute(subElement, "name");
                String value = getRequiredAttribute(subElement, "value");
                if (additionalProps == null) {
                    additionalProps = new HashMap<String, Object>();
                }
                additionalProps.put(name, value);
            }
        }

        ConfigurationCompilerPlugInAggregationMultiFunction config = new ConfigurationCompilerPlugInAggregationMultiFunction(functionNames.split(","), forgeClassName);
        config.setAdditionalConfiguredProperties(additionalProps);
        configuration.addPlugInAggregationMultiFunction(config);
    }

    private static void handlePlugInSingleRow(ConfigurationCompiler configuration, Element element) {
        String name = element.getAttributes().getNamedItem("name").getTextContent();
        String functionClassName = element.getAttributes().getNamedItem("function-class").getTextContent();
        String functionMethodName = element.getAttributes().getNamedItem("function-method").getTextContent();
        ConfigurationCompilerPlugInSingleRowFunction.ValueCache valueCache = ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED;
        ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable filterOptimizable = ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED;
        String valueCacheStr = getOptionalAttribute(element, "value-cache");
        if (valueCacheStr != null) {
            valueCache = ConfigurationCompilerPlugInSingleRowFunction.ValueCache.valueOf(valueCacheStr.toUpperCase(Locale.ENGLISH));
        }
        String filterOptimizableStr = getOptionalAttribute(element, "filter-optimizable");
        if (filterOptimizableStr != null) {
            filterOptimizable = ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.valueOf(filterOptimizableStr.toUpperCase(Locale.ENGLISH));
        }
        String rethrowExceptionsStr = getOptionalAttribute(element, "rethrow-exceptions");
        boolean rethrowExceptions = false;
        if (rethrowExceptionsStr != null) {
            rethrowExceptions = Boolean.parseBoolean(rethrowExceptionsStr);
        }
        String eventTypeName = getOptionalAttribute(element, "event-type-name");
        configuration.addPlugInSingleRowFunction(new ConfigurationCompilerPlugInSingleRowFunction(name, functionClassName, functionMethodName, valueCache, filterOptimizable, rethrowExceptions, eventTypeName));
    }

    private static void handlePlugInPatternGuard(ConfigurationCompiler configuration, Element element) {
        String namespace = getRequiredAttribute(element, "namespace");
        String name = getRequiredAttribute(element, "name");
        String forgeClassName = getRequiredAttribute(element, "forge-class");
        configuration.addPlugInPatternGuard(namespace, name, forgeClassName);
    }

    private static void handlePlugInPatternObserver(ConfigurationCompiler configuration, Element element) {
        String namespace = getRequiredAttribute(element, "namespace");
        String name = getRequiredAttribute(element, "name");
        String forgeClassName = getRequiredAttribute(element, "forge-class");
        configuration.addPlugInPatternObserver(namespace, name, forgeClassName);
    }

    private static void handleSerdeSettings(ConfigurationCompiler configuration, Element parentElement) {
        String text = getOptionalAttribute(parentElement, "enable-serializable");
        if (text != null) {
            configuration.getSerde().setEnableSerializable(Boolean.parseBoolean(text));
        }

        text = getOptionalAttribute(parentElement, "enable-externalizable");
        if (text != null) {
            configuration.getSerde().setEnableExternalizable(Boolean.parseBoolean(text));
        }

        text = getOptionalAttribute(parentElement, "enable-extended-builtin");
        if (text != null) {
            configuration.getSerde().setEnableExtendedBuiltin(Boolean.parseBoolean(text));
        }

        text = getOptionalAttribute(parentElement, "enable-serialization-fallback");
        if (text != null) {
            configuration.getSerde().setEnableSerializationFallback(Boolean.parseBoolean(text));
        }

        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("serde-provider-factory")) {
                text = getRequiredAttribute(subElement, "class");
                configuration.getSerde().addSerdeProviderFactory(text);
            }
        }
    }
}
