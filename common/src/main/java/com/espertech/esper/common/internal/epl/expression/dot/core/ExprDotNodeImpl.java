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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidationBase;
import com.espertech.esper.common.internal.epl.enummethod.dot.*;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionNode;
import com.espertech.esper.common.internal.epl.expression.chain.Chainable;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableArray;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableCall;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableName;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNodeSubprop;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableIdentNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitorWithParent;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.SettingsApplicationDotMethodPointInsideRectange;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.SettingsApplicationDotMethodRectangeIntersectsRectangle;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.epl.streamtype.PropertyResolutionDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeUtil;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumn;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.property.MappedProperty;
import com.espertech.esper.common.internal.event.property.Property;
import com.espertech.esper.common.internal.event.propertyparser.PropertyParserNoDep;
import com.espertech.esper.common.internal.rettype.*;
import com.espertech.esper.common.internal.settings.ClasspathImportCompileTimeUtil;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.settings.SettingsApplicationDotMethod;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.ValueAndFieldDesc;

import java.io.StringWriter;
import java.util.*;

/**
 * Represents an Dot-operator expression, for use when "(expression).method(...).method(...)"
 */
public class ExprDotNodeImpl extends ExprNodeBase implements ExprDotNode, ExprStreamRefNode, ExprNodeInnerNodeProvider {

    private List<Chainable> chainSpec;
    private final boolean isDuckTyping;
    private final boolean isUDFCache;

    private transient ExprDotNodeForge forge;

    public ExprDotNodeImpl(List<Chainable> chainSpec, boolean isDuckTyping, boolean isUDFCache) {
        this.chainSpec = Collections.unmodifiableList(chainSpec); // for safety, make it unmodifiable the list
        this.isDuckTyping = isDuckTyping;
        this.isUDFCache = isUDFCache;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {

        // check for plannable methods: these are validated according to different rules
        ExprAppDotMethodImpl appDotMethod = getAppDotMethod(validationContext.isFilterExpression());
        if (appDotMethod != null) {
            return appDotMethod;
        }

        // determine if there is an implied binding, replace first chain element with evaluation node if there is
        if (validationContext.getStreamTypeService().hasTableTypes() &&
            validationContext.getTableCompileTimeResolver() != null &&
            chainSpec.size() > 1 && chainSpec.get(0) instanceof ChainableName) {
            Pair<ExprNode, List<Chainable>> tableNode = TableCompileTimeUtil.getTableNodeChainable(validationContext.getStreamTypeService(), chainSpec, validationContext.isAllowTableAggReset(), validationContext.getTableCompileTimeResolver());
            if (tableNode != null) {
                ExprNode node = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.DOTNODE, tableNode.getFirst(), validationContext);
                if (tableNode.getSecond().isEmpty()) {
                    return node;
                }
                List<Chainable> modifiedChain = new ArrayList<>(tableNode.getSecond());
                setChainSpec(modifiedChain);
                this.addChildNode(node);
            }
        }

        // handle aggregation methods: method on aggregation state coming from certain aggregations or from table column (both table-access or table-in-from-clause)
        // this is done here as a walker does not have the information that the validated child node has
        Pair<ExprDotNodeAggregationMethodRootNode, List<Chainable>> aggregationMethodNode = handleAggregationMethod(validationContext);
        if (aggregationMethodNode != null) {
            if (aggregationMethodNode.getSecond().isEmpty()) {
                return aggregationMethodNode.getFirst();
            }
            List<Chainable> modifiedChain = new ArrayList<>(aggregationMethodNode.getSecond());
            this.setChainSpec(modifiedChain);
            this.getChildNodes()[0] = aggregationMethodNode.getFirst();
        }

        // validate all parameters
        ExprNodeUtilityValidate.validate(ExprNodeOrigin.DOTNODEPARAMETER, chainSpec, validationContext);

        // determine if there are enumeration method expressions in the chain
        boolean hasEnumerationMethod = false;
        for (Chainable chain : chainSpec) {
            if (!(chain instanceof ChainableCall)) {
                continue;
            }
            ChainableCall call = (ChainableCall) chain;
            if (EnumMethodResolver.isEnumerationMethod(call.getName(), validationContext.getClasspathImportService())) {
                hasEnumerationMethod = true;
                break;
            }
        }

        // The root node expression may provide the input value:
        //   Such as "window(*).doIt(...)" or "(select * from Window).doIt()" or "prevwindow(sb).doIt(...)", in which case the expression to act on is a child expression
        //
        StreamTypeService streamTypeService = validationContext.getStreamTypeService();
        if (this.getChildNodes().length != 0) {
            // the root expression is the first child node
            ExprNode rootNode = this.getChildNodes()[0];

            // the root expression may also provide a lambda-function input (Iterator<EventBean>)
            // Determine collection-type and evaluator if any for root node
            ExprDotEnumerationSourceForge enumSrc = ExprDotNodeUtility.getEnumerationSource(rootNode, validationContext.getStreamTypeService(), hasEnumerationMethod, validationContext.isDisablePropertyExpressionEventCollCache(), validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());

            EPType typeInfo;
            if (enumSrc.getReturnType() == null) {
                typeInfo = EPTypeHelper.singleValue(rootNode.getForge().getEvaluationType());    // not a collection type, treat as scalar
            } else {
                typeInfo = enumSrc.getReturnType();
            }

            ExprDotNodeRealizedChain evals = ExprDotNodeUtility.getChainEvaluators(enumSrc.getStreamOfProviderIfApplicable(), typeInfo, chainSpec, validationContext, isDuckTyping, new ExprDotNodeFilterAnalyzerInputExpr());
            forge = new ExprDotNodeForgeRootChild(this, null, null, null, hasEnumerationMethod, rootNode.getForge(), enumSrc.getEnumeration(), typeInfo, evals.getChain(), evals.getChainWithUnpack(), false);
            return null;
        }

        // No root node, and this is a 1-element chain i.e. "something(param,...)".
        // Plug-in single-row methods are not handled here.
        // Plug-in aggregation methods are not handled here.
        if (chainSpec.size() == 1) {
            Chainable chainable = chainSpec.get(0);
            if (!(chainable instanceof ChainableCall)) {
                throw new IllegalStateException("Unexpected chainable : " + chainable);
            }
            ChainableCall call = (ChainableCall) chainable;
            if (call.getParameters().isEmpty()) {
                throw handleNotFound(call.getName());
            }

            // single-parameter can resolve to a property
            Pair<PropertyResolutionDescriptor, String> propertyInfoPair = null;
            try {
                propertyInfoPair = ExprIdentNodeUtil.getTypeFromStream(streamTypeService, call.getName(), streamTypeService.hasPropertyAgnosticType(), false, validationContext.getTableCompileTimeResolver());
            } catch (ExprValidationPropertyException ex) {
                // fine
            }

            // if not a property then try built-in single-row non-grammar functions
            if (propertyInfoPair == null && call.getName().toLowerCase(Locale.ENGLISH).equals(ClasspathImportServiceCompileTime.EXT_SINGLEROW_FUNCTION_TRANSPOSE)) {
                if (call.getParameters().size() != 1) {
                    throw new ExprValidationException("The " + ClasspathImportServiceCompileTime.EXT_SINGLEROW_FUNCTION_TRANSPOSE + " function requires a single parameter expression");
                }
                forge = new ExprDotNodeForgeTransposeAsStream(this, call.getParameters().get(0).getForge());
            } else if (call.getParameters().size() != 1) {
                throw handleNotFound(call.getName());
            } else {
                if (propertyInfoPair == null) {
                    throw new ExprValidationException("Unknown single-row function, aggregation function or mapped or indexed property named '" + call.getName() + "' could not be resolved");
                }
                forge = getPropertyPairEvaluator(call.getParameters().get(0).getForge(), propertyInfoPair, validationContext);
            }
            return null;
        }

        // handle the case where the first chain spec element is a stream name.
        ExprValidationException prefixedStreamNumException = null;
        int prefixedStreamNumber = prefixedStreamName(chainSpec, validationContext.getStreamTypeService());
        if (prefixedStreamNumber != -1) {

            ChainableName first = (ChainableName) chainSpec.get(0);
            Chainable specAfterStreamName = chainSpec.get(1);

            // Attempt to resolve as property
            Pair<PropertyResolutionDescriptor, String> propertyInfoPair = null;
            try {
                String propName = first.getName() + "." + specAfterStreamName.getRootNameOrEmptyString();
                propertyInfoPair = ExprIdentNodeUtil.getTypeFromStream(streamTypeService, propName, streamTypeService.hasPropertyAgnosticType(), true, validationContext.getTableCompileTimeResolver());
            } catch (ExprValidationPropertyException ex) {
                // fine
            }

            if (propertyInfoPair != null) {
                List<Chainable> chain = new ArrayList<>(chainSpec);
                // handle "property[x]" and "property(x)"
                if (chain.size() == 2 && specAfterStreamName.getParametersOrEmpty().size() == 1) {
                    forge = getPropertyPairEvaluator(specAfterStreamName.getParametersOrEmpty().get(0).getForge(), propertyInfoPair, validationContext);
                    return null;
                }
                chain.remove(0);
                chain.remove(0);
                PropertyInfoPairDesc desc = handlePropertyInfoPair(true, specAfterStreamName, chain, propertyInfoPair, hasEnumerationMethod, validationContext, this);
                desc.apply(this);
                return null;
            }

            // Attempt to resolve as event-underlying object instance method
            EventType eventType = validationContext.getStreamTypeService().getEventTypes()[prefixedStreamNumber];
            Class type = eventType.getUnderlyingType();

            List<Chainable> remainderChain = new ArrayList<Chainable>(chainSpec);
            remainderChain.remove(0);

            ExprValidationException methodEx = null;
            ExprDotForge[] underlyingMethodChain = null;
            try {
                EPType typeInfo = EPTypeHelper.singleValue(type);
                if (validationContext.getTableCompileTimeResolver().resolveTableFromEventType(eventType) != null) {
                    typeInfo = new ClassEPType(Object[].class);
                }
                underlyingMethodChain = ExprDotNodeUtility.getChainEvaluators(prefixedStreamNumber, typeInfo, remainderChain, validationContext, false, new ExprDotNodeFilterAnalyzerInputStream(prefixedStreamNumber)).getChainWithUnpack();
            } catch (ExprValidationException ex) {
                methodEx = ex;
                // expected - may not be able to find the methods on the underlying
            }

            ExprDotForge[] eventTypeMethodChain = null;
            ExprValidationException enumDatetimeEx = null;
            FilterExprAnalyzerAffector filterExprAnalyzerAffector = null;
            try {
                EPType typeInfo = EPTypeHelper.singleEvent(eventType);
                ExprDotNodeRealizedChain chain = ExprDotNodeUtility.getChainEvaluators(prefixedStreamNumber, typeInfo, remainderChain, validationContext, false, new ExprDotNodeFilterAnalyzerInputStream(prefixedStreamNumber));
                eventTypeMethodChain = chain.getChainWithUnpack();
                filterExprAnalyzerAffector = chain.getFilterAnalyzerDesc();
            } catch (ExprValidationException ex) {
                enumDatetimeEx = ex;
                // expected - may not be able to find the methods on the underlying
            }

            if (underlyingMethodChain != null) {
                forge = new ExprDotNodeForgeStream(this, filterExprAnalyzerAffector, prefixedStreamNumber, eventType, underlyingMethodChain, true);
            } else if (eventTypeMethodChain != null) {
                forge = new ExprDotNodeForgeStream(this, filterExprAnalyzerAffector, prefixedStreamNumber, eventType, eventTypeMethodChain, false);
            }

            if (forge != null) {
                return null;
            } else {
                String remainerName = remainderChain.get(0).getRootNameOrEmptyString();
                if (ExprDotNodeUtility.isDatetimeOrEnumMethod(remainerName, validationContext.getClasspathImportService())) {
                    prefixedStreamNumException = enumDatetimeEx;
                } else {
                    prefixedStreamNumException = new ExprValidationException("Failed to solve '" + remainerName + "' to either an date-time or enumeration method, an event property or a method on the event underlying object: " + methodEx.getMessage(), methodEx);
                }
            }
        }

        // There no root node, in this case the classname or property name is provided as part of the chain.
        // Such as "MyClass.myStaticLib(...)" or "mycollectionproperty.doIt(...)"
        //
        List<Chainable> modifiedChain = new ArrayList<Chainable>(chainSpec);
        Chainable firstItem = modifiedChain.remove(0);
        String firstItemName = firstItem.getRootNameOrEmptyString();

        Pair<PropertyResolutionDescriptor, String> propertyInfoPair = null;
        try {
            propertyInfoPair = ExprIdentNodeUtil.getTypeFromStream(streamTypeService, firstItemName, streamTypeService.hasPropertyAgnosticType(), true, validationContext.getTableCompileTimeResolver());
        } catch (ExprValidationPropertyException ex) {
            // not a property
        }

        // If property then treat it as such
        if (propertyInfoPair != null) {
            PropertyInfoPairDesc desc = handlePropertyInfoPair(false, firstItem, modifiedChain, propertyInfoPair, hasEnumerationMethod, validationContext, this);
            desc.apply(this);
            return null;
        }

        // If variable then resolve as such
        VariableMetaData variable = validationContext.getVariableCompileTimeResolver().resolve(firstItemName);
        if (variable != null) {
            if (variable.getOptionalContextName() != null) {
                throw new ExprValidationException("Method invocation on context-specific variable is not supported");
            }
            EPType typeInfo;
            ExprDotStaticMethodWrap wrap;
            if (variable.getType().isArray()) {
                typeInfo = EPTypeHelper.collectionOfSingleValue(variable.getType().getComponentType());
                wrap = new ExprDotStaticMethodWrapArrayScalar(variable.getVariableName(), variable.getType());
            } else if (variable.getEventType() != null) {
                typeInfo = EPTypeHelper.singleEvent(variable.getEventType());
                wrap = null;
            } else {
                typeInfo = EPTypeHelper.singleValue(variable.getType());
                wrap = null;
            }

            ExprDotNodeRealizedChain evals = ExprDotNodeUtility.getChainEvaluators(null, typeInfo, modifiedChain, validationContext, false, new ExprDotNodeFilterAnalyzerInputStatic());
            forge = new ExprDotNodeForgeVariable(this, variable, wrap, evals.getChainWithUnpack());
            return null;
        }

        // try resolve as enumeration class with value
        ValueAndFieldDesc enumconstantDesc = ClasspathImportCompileTimeUtil.resolveIdentAsEnumConst(firstItemName, validationContext.getClasspathImportService(), validationContext.getClassProvidedClasspathExtension(), false);
        if (enumconstantDesc != null && modifiedChain.get(0) instanceof ChainableCall) {

            // try resolve method
            final ChainableCall methodSpec = (ChainableCall) modifiedChain.get(0);
            final String enumvalue = firstItemName;
            ExprNodeUtilResolveExceptionHandler handler = new ExprNodeUtilResolveExceptionHandler() {
                public ExprValidationException handle(Exception ex) {
                    return new ExprValidationException("Failed to resolve method '" + methodSpec.getName() + "' on enumeration value '" + enumvalue + "': " + ex.getMessage());
                }
            };
            EventType wildcardType = validationContext.getStreamTypeService().getEventTypes().length != 1 ? null : validationContext.getStreamTypeService().getEventTypes()[0];
            ExprNodeUtilMethodDesc methodDesc = ExprNodeUtilityResolve.resolveMethodAllowWildcardAndStream(enumconstantDesc.getValue().getClass().getName(), enumconstantDesc.getValue().getClass(), methodSpec.getName(),
                methodSpec.getParameters(), wildcardType != null, wildcardType, handler, methodSpec.getName(), validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());

            // method resolved, hook up
            modifiedChain.remove(0);    // we identified this piece
            ExprDotStaticMethodWrap optionalLambdaWrap = ExprDotStaticMethodWrapFactory.make(methodDesc.getReflectionMethod(), modifiedChain, null, validationContext);
            EPType typeInfo = optionalLambdaWrap != null ? optionalLambdaWrap.getTypeInfo() : EPTypeHelper.singleValue(methodDesc.getReflectionMethod().getReturnType());

            ExprDotNodeRealizedChain evals = ExprDotNodeUtility.getChainEvaluators(null, typeInfo, modifiedChain, validationContext, false, new ExprDotNodeFilterAnalyzerInputStatic());
            forge = new ExprDotNodeForgeStaticMethod(this, false, firstItemName, methodDesc.getReflectionMethod(),
                methodDesc.getChildForges(), false, evals.getChainWithUnpack(), optionalLambdaWrap, false, enumconstantDesc, validationContext.getStatementName());
            return null;
        }

        // if prefixed by a stream name, we are giving up
        if (prefixedStreamNumException != null) {
            throw prefixedStreamNumException;
        }

        // If class then resolve as class
        Chainable secondItem = modifiedChain.remove(0);

        boolean allowWildcard = validationContext.getStreamTypeService().getEventTypes().length == 1;
        EventType streamZeroType = null;
        if (validationContext.getStreamTypeService().getEventTypes().length > 0) {
            streamZeroType = validationContext.getStreamTypeService().getEventTypes()[0];
        }

        ExprNodeUtilResolveExceptionHandlerDefault msgHandler = new ExprNodeUtilResolveExceptionHandlerDefault(firstItemName + (secondItem.getRootNameOrEmptyString().isEmpty() ? "" : "." + secondItem.getRootNameOrEmptyString()), false);
        ExprNodeUtilMethodDesc method = ExprNodeUtilityResolve.resolveMethodAllowWildcardAndStream(firstItemName, null, secondItem.getRootNameOrEmptyString(), secondItem.getParametersOrEmpty(), allowWildcard, streamZeroType, msgHandler, secondItem.getRootNameOrEmptyString(), validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());

        boolean isConstantParameters = method.isAllConstants() && isUDFCache;
        boolean isReturnsConstantResult = isConstantParameters && modifiedChain.isEmpty();

        // this may return a pair of null if there is no lambda or the result cannot be wrapped for lambda-function use
        ExprDotStaticMethodWrap optionalLambdaWrap = ExprDotStaticMethodWrapFactory.make(method.getReflectionMethod(), modifiedChain, null, validationContext);
        EPType typeInfo = optionalLambdaWrap != null ? optionalLambdaWrap.getTypeInfo() : EPTypeHelper.singleValue(method.getReflectionMethod().getReturnType());

        ExprDotNodeRealizedChain evals = ExprDotNodeUtility.getChainEvaluators(null, typeInfo, modifiedChain, validationContext, false, new ExprDotNodeFilterAnalyzerInputStatic());
        forge = new ExprDotNodeForgeStaticMethod(this, isReturnsConstantResult, firstItemName, method.getReflectionMethod(), method.getChildForges(), isConstantParameters, evals.getChainWithUnpack(), optionalLambdaWrap, false, null, validationContext.getStatementName());

        return null;
    }

    private static PropertyInfoPairDesc handlePropertyInfoPair(boolean nestedComplexProperty, Chainable firstItem, List<Chainable> chain, Pair<PropertyResolutionDescriptor, String> propertyInfoPair, boolean hasEnumerationMethod, ExprValidationContext validationContext, ExprDotNodeImpl myself) throws ExprValidationException {
        StreamTypeService streamTypeService = validationContext.getStreamTypeService();
        String propertyName = propertyInfoPair.getFirst().getPropertyName();
        int streamId = propertyInfoPair.getFirst().getStreamNum();
        EventTypeSPI streamType = (EventTypeSPI) streamTypeService.getEventTypes()[streamId];
        ExprEnumerationForge enumerationForge = null;
        EPType inputType;
        ExprForge rootNodeForge = null;
        EventPropertyGetterSPI getter;
        boolean rootIsEventBean = false;

        if (firstItem instanceof ChainableName) {
            getter = streamType.getGetterSPI(propertyName);
            // Handle first-chainable not an array
            if (!(chain.get(0) instanceof ChainableArray)) {
                boolean allowEnum = nestedComplexProperty || hasEnumerationMethod;
                ExprDotEnumerationSourceForgeForProps propertyEval = ExprDotNodeUtility.getPropertyEnumerationSource(propertyName, streamId, streamType, allowEnum, validationContext.isDisablePropertyExpressionEventCollCache());
                enumerationForge = propertyEval.getEnumeration();
                inputType = propertyEval.getReturnType();
                rootNodeForge = new PropertyDotNonLambdaForge(streamId, getter, JavaClassHelper.getBoxedType(propertyInfoPair.getFirst().getPropertyType()));
            } else {
                // first-chainable is an array, use array-of-fragments or array-of-type
                ChainableArray array = (ChainableArray) chain.get(0);
                ExprNode indexExpression = ChainableArray.validateSingleIndexExpr(array.getIndexes(), () -> "property '" + propertyName + "'");
                Class propertyType = streamType.getPropertyType(propertyName);
                FragmentEventType fragmentEventType = streamType.getFragmentType(propertyName);
                if (fragmentEventType != null && fragmentEventType.isIndexed()) {
                    // handle array-of-fragment by including the array operation in the root
                    inputType = EPTypeHelper.singleEvent(fragmentEventType.getFragmentType());
                    chain = chain.subList(1, chain.size()); // we remove the array operation from the chain as its handled by the forge
                    rootNodeForge = new PropertyDotNonLambdaFragmentIndexedForge(streamId, getter, indexExpression, propertyName);
                    rootIsEventBean = true;
                } else if (propertyType.isArray()) {
                    // handle array-of-type by simple property and array operation as part of chain
                    inputType = EPTypeHelper.singleValue(propertyType);
                    rootNodeForge = new PropertyDotNonLambdaForge(streamId, getter, JavaClassHelper.getBoxedType(propertyInfoPair.getFirst().getPropertyType()));
                } else {
                    throw new ExprValidationException("Invalid array operation for property '" + propertyName + "'");
                }
            }
        } else {
            // property with parameter - mapped or indexed property
            getter = null;
            ChainableCall call = (ChainableCall) firstItem;
            EventPropertyDescriptor desc = EventTypeUtility.getNestablePropertyDescriptor(streamTypeService.getEventTypes()[propertyInfoPair.getFirst().getStreamNum()], call.getName());
            if (call.getParameters().size() > 1) {
                throw new ExprValidationException("Property '" + call.getName() + "' may not be accessed passing 2 or more parameters");
            }
            ExprForge paramEval = call.getParameters().get(0).getForge();
            inputType = EPTypeHelper.singleValue(desc.getPropertyComponentType());
            if (desc.isMapped()) {
                if (paramEval.getEvaluationType() != String.class) {
                    throw new ExprValidationException("Parameter expression to mapped property '" + propertyName + "' is expected to return a string-type value but returns " + JavaClassHelper.getClassNameFullyQualPretty(paramEval.getEvaluationType()));
                }
                EventPropertyGetterMappedSPI mappedGetter = ((EventTypeSPI) propertyInfoPair.getFirst().getStreamEventType()).getGetterMappedSPI(propertyName);
                if (mappedGetter == null) {
                    throw new ExprValidationException("Mapped property named '" + propertyName + "' failed to obtain getter-object");
                }
                rootNodeForge = new PropertyDotNonLambdaMappedForge(streamId, mappedGetter, paramEval, desc.getPropertyComponentType());
            }
            if (desc.isIndexed()) {
                if (JavaClassHelper.getBoxedType(paramEval.getEvaluationType()) != Integer.class) {
                    throw new ExprValidationException("Parameter expression to mapped property '" + propertyName + "' is expected to return a Integer-type value but returns " + JavaClassHelper.getClassNameFullyQualPretty(paramEval.getEvaluationType()));
                }
                EventPropertyGetterIndexedSPI indexedGetter = ((EventTypeSPI) propertyInfoPair.getFirst().getStreamEventType()).getGetterIndexedSPI(propertyName);
                if (indexedGetter == null) {
                    throw new ExprValidationException("Mapped property named '" + propertyName + "' failed to obtain getter-object");
                }
                rootNodeForge = new PropertyDotNonLambdaIndexedForge(streamId, indexedGetter, paramEval, desc.getPropertyComponentType());
            }
        }

        // try to build chain based on the input (non-fragment)
        ExprDotNodeRealizedChain evals;
        ExprDotNodeFilterAnalyzerInputProp filterAnalyzerInputProp = new ExprDotNodeFilterAnalyzerInputProp(propertyInfoPair.getFirst().getStreamNum(), propertyName);
        try {
            evals = ExprDotNodeUtility.getChainEvaluators(streamId, inputType, chain, validationContext, myself.isDuckTyping, filterAnalyzerInputProp);
        } catch (ExprValidationException ex) {
            if (inputType instanceof EventEPType || inputType instanceof EventMultiValuedEPType) {
                throw ex;
            }

            // try building the chain based on the fragment event type (i.e. A.after(B) based on A-configured start time where A is a fragment)
            FragmentEventType fragment = propertyInfoPair.getFirst().getFragmentEventType();
            if (fragment == null) {
                throw ex;
            }

            rootIsEventBean = true;
            EPType fragmentTypeInfo;
            if (!fragment.isIndexed()) {
                if (chain.get(0) instanceof ChainableArray) {
                    throw new ExprValidationException("Cannot perform array operation as property '" + propertyName + "' does not return an array");
                }
                fragmentTypeInfo = EPTypeHelper.singleEvent(fragment.getFragmentType());
            } else {
                fragmentTypeInfo = EPTypeHelper.arrayOfEvents(fragment.getFragmentType());
            }
            inputType = fragmentTypeInfo;
            rootNodeForge = new PropertyDotNonLambdaFragmentForge(streamId, getter, fragment.isIndexed());
            evals = ExprDotNodeUtility.getChainEvaluators(propertyInfoPair.getFirst().getStreamNum(), fragmentTypeInfo, chain, validationContext, myself.isDuckTyping, filterAnalyzerInputProp);
        }

        FilterExprAnalyzerAffector filterExprAnalyzerAffector = evals.getFilterAnalyzerDesc();
        int streamNumReferenced = propertyInfoPair.getFirst().getStreamNum();
        ExprDotNodeForgeRootChild forge = new ExprDotNodeForgeRootChild(myself, filterExprAnalyzerAffector, streamNumReferenced, propertyName, hasEnumerationMethod, rootNodeForge, enumerationForge, inputType, evals.getChain(), evals.getChainWithUnpack(), !rootIsEventBean);
        return new PropertyInfoPairDesc(forge);
    }

    private Pair<ExprDotNodeAggregationMethodRootNode, List<Chainable>> handleAggregationMethod(ExprValidationContext validationContext) throws ExprValidationException {
        if (chainSpec.isEmpty() || getChildNodes().length == 0) {
            return null;
        }
        Chainable chainFirst = chainSpec.get(0);
        if (chainFirst instanceof ChainableArray) {
            return null;
        }
        ExprNode rootNode = getChildNodes()[0];
        ExprNode[] aggMethodParams = chainFirst.getParametersOrEmpty().toArray(new ExprNode[0]);
        String aggMethodName = chainFirst.getRootNameOrEmptyString();

        // handle property, such as "sortedcolumn.floorKey('a')" since "floorKey" can also be a property
        if (chainFirst instanceof ChainableName) {
            Property prop = PropertyParserNoDep.parseAndWalkLaxToSimple(chainFirst.getRootNameOrEmptyString(), false);
            if (prop instanceof MappedProperty) {
                MappedProperty mappedProperty = (MappedProperty) prop;
                aggMethodName = mappedProperty.getPropertyNameAtomic();
                aggMethodParams = new ExprNode[]{new ExprConstantNodeImpl(mappedProperty.getKey())};
            }
        }

        if (!(rootNode instanceof ExprTableAccessNodeSubprop) && !(rootNode instanceof ExprAggMultiFunctionNode) && !(rootNode instanceof ExprTableIdentNode)) {
            return null;
        }

        ExprDotNodeAggregationMethodForge aggregationMethodForge;
        if (rootNode instanceof ExprAggMultiFunctionNode) {
            // handle local aggregation
            ExprAggMultiFunctionNode mf = (ExprAggMultiFunctionNode) rootNode;
            if (!mf.getAggregationForgeFactory().getAggregationPortableValidation().isAggregationMethod(aggMethodName, aggMethodParams, validationContext)) {
                return null;
            }
            aggregationMethodForge = new ExprDotNodeAggregationMethodForgeLocal(this, aggMethodName, aggMethodParams, mf.getAggregationForgeFactory().getAggregationPortableValidation(), mf);
        } else if (rootNode instanceof ExprTableIdentNode) {
            // handle table-column via from-clause
            ExprTableIdentNode tableSubprop = (ExprTableIdentNode) rootNode;
            TableMetadataColumn column = tableSubprop.getTableMetadata().getColumns().get(tableSubprop.getColumnName());
            if (!(column instanceof TableMetadataColumnAggregation)) {
                return null;
            }
            TableMetadataColumnAggregation columnAggregation = (TableMetadataColumnAggregation) column;
            if (aggMethodName.toLowerCase(Locale.ENGLISH).equals("reset")) {
                if (!validationContext.isAllowTableAggReset()) {
                    throw new ExprValidationException(AggregationPortableValidationBase.INVALID_TABLE_AGG_RESET);
                }
                aggregationMethodForge = new ExprDotNodeAggregationMethodForgeTableReset(this, aggMethodName, aggMethodParams, columnAggregation.getAggregationPortableValidation(), tableSubprop, columnAggregation);
            } else {
                if (columnAggregation.isMethodAgg() || !columnAggregation.getAggregationPortableValidation().isAggregationMethod(aggMethodName, aggMethodParams, validationContext)) {
                    return null;
                }
                aggregationMethodForge = new ExprDotNodeAggregationMethodForgeTableIdent(this, aggMethodName, aggMethodParams, columnAggregation.getAggregationPortableValidation(), tableSubprop, columnAggregation);
            }
        } else if (rootNode instanceof ExprTableAccessNodeSubprop) {
            // handle table-column via table-access
            ExprTableAccessNodeSubprop tableSubprop = (ExprTableAccessNodeSubprop) rootNode;
            TableMetadataColumn column = tableSubprop.getTableMeta().getColumns().get(tableSubprop.getSubpropName());
            if (!(column instanceof TableMetadataColumnAggregation)) {
                return null;
            }
            TableMetadataColumnAggregation columnAggregation = (TableMetadataColumnAggregation) column;
            if (columnAggregation.isMethodAgg() || !columnAggregation.getAggregationPortableValidation().isAggregationMethod(aggMethodName, aggMethodParams, validationContext)) {
                return null;
            }
            aggregationMethodForge = new ExprDotNodeAggregationMethodForgeTableAccess(this, aggMethodName, aggMethodParams, columnAggregation.getAggregationPortableValidation(), tableSubprop, columnAggregation);
        } else {
            throw new IllegalStateException("Unhandled aggregation method root node");
        }

        // validate
        aggregationMethodForge.validate(validationContext);

        List<Chainable> newChain = chainSpec.size() == 1 ? Collections.emptyList() : new ArrayList<>(chainSpec.subList(1, chainSpec.size()));
        ExprDotNodeAggregationMethodRootNode root = new ExprDotNodeAggregationMethodRootNode(aggregationMethodForge);
        root.addChildNode(rootNode);
        return new Pair<>(root, newChain);
    }

    public FilterExprAnalyzerAffector getAffector(boolean isOuterJoin) {
        checkValidated(forge);
        return isOuterJoin ? null : forge.getFilterExprAnalyzerAffector();
    }

    private ExprDotNodeForge getPropertyPairEvaluator(ExprForge parameterForge, Pair<PropertyResolutionDescriptor, String> propertyInfoPair, ExprValidationContext validationContext)
        throws ExprValidationException {
        String propertyName = propertyInfoPair.getFirst().getPropertyName();
        EventPropertyDescriptor propertyDesc = EventTypeUtility.getNestablePropertyDescriptor(propertyInfoPair.getFirst().getStreamEventType(), propertyName);
        if (propertyDesc == null || (!propertyDesc.isMapped() && !propertyDesc.isIndexed())) {
            throw new ExprValidationException("Unknown single-row function, aggregation function or mapped or indexed property named '" + propertyName + "' could not be resolved");
        }

        int streamNum = propertyInfoPair.getFirst().getStreamNum();
        EventPropertyGetterMappedSPI mappedGetter = null;
        EventPropertyGetterIndexedSPI indexedGetter = null;

        Class propertyType = Object.class;
        if (propertyDesc.isMapped()) {
            if (parameterForge.getEvaluationType() != String.class) {
                throw new ExprValidationException("Parameter expression to mapped property '" + propertyDesc.getPropertyName() + "' is expected to return a string-type value but returns " + JavaClassHelper.getClassNameFullyQualPretty(parameterForge.getEvaluationType()));
            }
            mappedGetter = ((EventTypeSPI) propertyInfoPair.getFirst().getStreamEventType()).getGetterMappedSPI(propertyInfoPair.getFirst().getPropertyName());
            if (mappedGetter == null) {
                throw new ExprValidationException("Mapped property named '" + propertyName + "' failed to obtain getter-object");
            }
        } else {
            if (JavaClassHelper.getBoxedType(parameterForge.getEvaluationType()) != Integer.class) {
                throw new ExprValidationException("Parameter expression to indexed property '" + propertyDesc.getPropertyName() + "' is expected to return a Integer-type value but returns " + JavaClassHelper.getClassNameFullyQualPretty(parameterForge.getEvaluationType()));
            }
            indexedGetter = ((EventTypeSPI) propertyInfoPair.getFirst().getStreamEventType()).getGetterIndexedSPI(propertyInfoPair.getFirst().getPropertyName());
            if (indexedGetter == null) {
                throw new ExprValidationException("Indexed property named '" + propertyName + "' failed to obtain getter-object");
            }
        }
        if (propertyDesc.getPropertyComponentType() != null) {
            propertyType = JavaClassHelper.getBoxedType(propertyDesc.getPropertyComponentType());
        }

        return new ExprDotNodeForgePropertyExpr(this, validationContext.getStatementName(), propertyDesc.getPropertyName(), streamNum, parameterForge, propertyType, indexedGetter, mappedGetter);
    }

    private int prefixedStreamName(List<Chainable> chainSpec, StreamTypeService streamTypeService) {
        if (chainSpec.size() < 1) {
            return -1;
        }
        Chainable spec = chainSpec.get(0);
        if (!(spec instanceof ChainableName)) {
            return -1;
        }
        ChainableName name = (ChainableName) spec;
        return streamTypeService.getStreamNumForStreamName(name.getName());
    }

    public void accept(ExprNodeVisitor visitor) {
        super.accept(visitor);
        ExprNodeUtilityQuery.acceptChain(visitor, chainSpec);
    }

    public void accept(ExprNodeVisitorWithParent visitor) {
        super.accept(visitor);
        ExprNodeUtilityQuery.acceptChain(visitor, chainSpec);
    }

    public void acceptChildnodes(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        super.acceptChildnodes(visitor, parent);
        ExprNodeUtilityQuery.acceptChain(visitor, chainSpec, this);
    }

    public void replaceUnlistedChildNode(ExprNode nodeToReplace, ExprNode newNode) {
        ExprNodeUtilityModify.replaceChainChildNode(nodeToReplace, newNode, chainSpec);
    }

    public List<Chainable> getChainSpec() {
        return chainSpec;
    }

    public void setChainSpec(List<Chainable> chainSpec) {
        this.chainSpec = Collections.unmodifiableList(chainSpec);
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public boolean isConstantResult() {
        checkValidated(forge);
        return forge.isReturnsConstantResult();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public Integer getStreamReferencedIfAny() {
        checkValidated(forge);
        return forge.getStreamNumReferenced();
    }

    public String getRootPropertyNameIfAny() {
        checkValidated(forge);
        return forge.getRootPropertyName();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (this.getChildNodes().length != 0) {
            writer.append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(this.getChildNodes()[0]));
        }
        ExprNodeUtilityPrint.toExpressionString(chainSpec, writer, this.getChildNodes().length != 0, null);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public Map<String, Object> getEventType() {
        return null;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprDotNodeImpl)) {
            return false;
        }

        ExprDotNodeImpl other = (ExprDotNodeImpl) node;
        if (other.chainSpec.size() != this.chainSpec.size()) {
            return false;
        }
        for (int i = 0; i < chainSpec.size(); i++) {
            if (!(this.chainSpec.get(i).equals(other.chainSpec.get(i)))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<ExprNode> getAdditionalNodes() {
        return ExprNodeUtilityQuery.collectChainParameters(chainSpec);
    }

    public VariableMetaData isVariableOpGetName(VariableCompileTimeResolver variableCompileTimeResolver) {
        if (chainSpec.size() > 0 && chainSpec.get(0) instanceof ChainableName) {
            return variableCompileTimeResolver.resolve(((ChainableName) chainSpec.get(0)).getName());
        }
        return null;
    }

    private ExprValidationException handleNotFound(String name) {
        String appDotMethodDidYouMean = getAppDotMethodDidYouMean();
        String message = "Unknown single-row function, expression declaration, script or aggregation function named '" + name + "' could not be resolved";
        if (appDotMethodDidYouMean != null) {
            message += " (did you mean '" + appDotMethodDidYouMean + "')";
        }
        return new ExprValidationException(message);
    }

    private String getAppDotMethodDidYouMean() {
        String lhsName = chainSpec.get(0).getRootNameOrEmptyString().toLowerCase(Locale.ENGLISH);
        if (lhsName.equals("rectangle")) {
            return "rectangle.intersects";
        }
        if (lhsName.equals("point")) {
            return "point.inside";
        }
        return null;
    }

    private ExprAppDotMethodImpl getAppDotMethod(boolean filterExpression) throws ExprValidationException {
        if (chainSpec.size() < 2) {
            return null;
        }
        if (!(chainSpec.get(1) instanceof ChainableCall)) {
            return null;
        }
        ChainableCall call = (ChainableCall) chainSpec.get(1);
        String lhsName = chainSpec.get(0).getRootNameOrEmptyString();

        String operationName = call.getName().toLowerCase(Locale.ENGLISH);
        boolean pointInside = lhsName.equals("point") && operationName.equals("inside");
        boolean rectangleIntersects = lhsName.equals("rectangle") && operationName.equals("intersects");
        if (!pointInside && !rectangleIntersects) {
            return null;
        }
        if (call.getParameters().size() != 1) {
            throw getAppDocMethodException(lhsName, operationName);
        }
        ExprNode param = call.getParameters().get(0);
        if (!(param instanceof ExprDotNode)) {
            throw getAppDocMethodException(lhsName, operationName);
        }
        ExprDotNode compared = (ExprDotNode) call.getParameters().get(0);
        if (compared.getChainSpec().size() != 1) {
            throw getAppDocMethodException(lhsName, operationName);
        }
        String rhsName = compared.getChainSpec().get(0).getRootNameOrEmptyString().toLowerCase(Locale.ENGLISH);
        boolean pointInsideRectangle = pointInside && rhsName.equals("rectangle");
        boolean rectangleIntersectsRectangle = rectangleIntersects && rhsName.equals("rectangle");
        if (!pointInsideRectangle && !rectangleIntersectsRectangle) {
            throw getAppDocMethodException(lhsName, operationName);
        }

        List<ExprNode> lhsExpressions = chainSpec.get(0).getParametersOrEmpty();
        ExprNode[] indexNamedParameter = null;
        List<ExprNode> lhsExpressionsValues = new ArrayList<>();
        for (ExprNode lhsExpression : lhsExpressions) {
            if (lhsExpression instanceof ExprNamedParameterNode) {
                ExprNamedParameterNode named = (ExprNamedParameterNode) lhsExpression;
                if (named.getParameterName().toLowerCase(Locale.ENGLISH).equals(FILTERINDEX_NAMED_PARAMETER)) {
                    if (!filterExpression) {
                        throw new ExprValidationException("The '" + named.getParameterName() + "' named parameter can only be used in in filter expressions");
                    }
                    indexNamedParameter = named.getChildNodes();
                } else {
                    throw new ExprValidationException(lhsName + " does not accept '" + named.getParameterName() + "' as a named parameter");
                }
            } else {
                lhsExpressionsValues.add(lhsExpression);
            }
        }

        ExprNode[] lhs = ExprNodeUtilityQuery.toArray(lhsExpressionsValues);
        ExprNode[] rhs = ExprNodeUtilityQuery.toArray(compared.getChainSpec().get(0).getParametersOrEmpty());

        SettingsApplicationDotMethod predefined;
        if (pointInsideRectangle) {
            predefined = new SettingsApplicationDotMethodPointInsideRectange(this, lhsName, lhs, operationName, rhsName, rhs, indexNamedParameter);
        } else {
            predefined = new SettingsApplicationDotMethodRectangeIntersectsRectangle(this, lhsName, lhs, operationName, rhsName, rhs, indexNamedParameter);
        }
        return new ExprAppDotMethodImpl(predefined);
    }

    private ExprValidationException getAppDocMethodException(String lhsName, String operationName) {
        return new ExprValidationException(lhsName + "." + operationName + " requires a single rectangle as parameter");
    }

    private static class PropertyInfoPairDesc {
        private final ExprDotNodeForgeRootChild forge;

        public PropertyInfoPairDesc(ExprDotNodeForgeRootChild forge) {
            this.forge = forge;
        }

        public ExprDotNodeForgeRootChild getForge() {
            return forge;
        }

        public void apply(ExprDotNodeImpl node) {
            node.forge = forge;
        }
    }
}

