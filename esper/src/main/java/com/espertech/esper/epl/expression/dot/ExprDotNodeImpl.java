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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.start.EPStatementStartMethod;
import com.espertech.esper.epl.core.engineimport.EngineImportApplicationDotMethod;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.engineimport.EngineImportUtil;
import com.espertech.esper.epl.core.streamtype.PropertyResolutionDescriptor;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.enummethod.dot.*;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeVisitorWithParent;
import com.espertech.esper.epl.index.quadtree.EngineImportApplicationDotMethodPointInsideRectange;
import com.espertech.esper.epl.index.quadtree.EngineImportApplicationDotMethodRectangeIntersectsRectangle;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.*;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents an Dot-operator expression, for use when "(expression).method(...).method(...)"
 */
public class ExprDotNodeImpl extends ExprNodeBase implements ExprDotNode, ExprNodeInnerNodeProvider, ExprStreamRefNode {
    private static final long serialVersionUID = 8105121208330622813L;

    private final List<ExprChainedSpec> chainSpec;
    private final boolean isDuckTyping;
    private final boolean isUDFCache;

    private transient ExprDotNodeForge forge;

    public ExprDotNodeImpl(List<ExprChainedSpec> chainSpec, boolean isDuckTyping, boolean isUDFCache) {
        this.chainSpec = new ArrayList<ExprChainedSpec>(chainSpec); // for safety, copy the list
        this.isDuckTyping = isDuckTyping;
        this.isUDFCache = isUDFCache;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {

        // check for plannable methods: these are validated according to different rules
        ExprAppDotMethodImpl appDotMethod = getAppDotMethod(validationContext.isFilterExpression());
        if (appDotMethod != null) {
            return appDotMethod;
        }

        // validate all parameters
        ExprNodeUtilityRich.validate(ExprNodeOrigin.DOTNODEPARAMETER, chainSpec, validationContext);

        // determine if there are enumeration method expressions in the chain
        boolean hasEnumerationMethod = false;
        for (ExprChainedSpec chain : chainSpec) {
            if (EnumMethodEnum.isEnumerationMethod(chain.getName())) {
                hasEnumerationMethod = true;
                break;
            }
        }

        // determine if there is an implied binding, replace first chain element with evaluation node if there is
        if (validationContext.getStreamTypeService().hasTableTypes() &&
                validationContext.getTableService() != null &&
                chainSpec.size() > 1 && chainSpec.get(0).isProperty()) {
            Pair<ExprNode, List<ExprChainedSpec>> tableNode = validationContext.getTableService().getTableNodeChainable(validationContext.getStreamTypeService(), chainSpec, validationContext.getEngineImportService());
            if (tableNode != null) {
                ExprNode node = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.DOTNODE, tableNode.getFirst(), validationContext);
                if (tableNode.getSecond().isEmpty()) {
                    return node;
                }
                chainSpec.clear();
                chainSpec.addAll(tableNode.getSecond());
                this.addChildNode(node);
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
            ExprDotEnumerationSourceForge enumSrc = ExprDotNodeUtility.getEnumerationSource(rootNode, validationContext.getStreamTypeService(), validationContext.getEventAdapterService(), validationContext.getStatementId(), hasEnumerationMethod, validationContext.isDisablePropertyExpressionEventCollCache());

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
            ExprChainedSpec spec = chainSpec.get(0);
            if (spec.getParameters().isEmpty()) {
                throw handleNotFound(spec.getName());
            }

            // single-parameter can resolve to a property
            Pair<PropertyResolutionDescriptor, String> propertyInfoPair = null;
            try {
                propertyInfoPair = ExprIdentNodeUtil.getTypeFromStream(streamTypeService, spec.getName(), streamTypeService.hasPropertyAgnosticType(), false);
            } catch (ExprValidationPropertyException ex) {
                // fine
            }

            // if not a property then try built-in single-row non-grammar functions
            if (propertyInfoPair == null && spec.getName().toLowerCase(Locale.ENGLISH).equals(EngineImportService.EXT_SINGLEROW_FUNCTION_TRANSPOSE)) {
                if (spec.getParameters().size() != 1) {
                    throw new ExprValidationException("The " + EngineImportService.EXT_SINGLEROW_FUNCTION_TRANSPOSE + " function requires a single parameter expression");
                }
                forge = new ExprDotNodeForgeTransposeAsStream(this, chainSpec.get(0).getParameters().get(0).getForge());
            } else if (spec.getParameters().size() != 1) {
                throw handleNotFound(spec.getName());
            } else {
                if (propertyInfoPair == null) {
                    throw new ExprValidationException("Unknown single-row function, aggregation function or mapped or indexed property named '" + spec.getName() + "' could not be resolved");
                }
                forge = getPropertyPairEvaluator(spec.getParameters().get(0).getForge(), propertyInfoPair, validationContext);
            }
            return null;
        }

        // handle the case where the first chain spec element is a stream name.
        ExprValidationException prefixedStreamNumException = null;
        int prefixedStreamNumber = prefixedStreamName(chainSpec, validationContext.getStreamTypeService());
        if (prefixedStreamNumber != -1) {

            ExprChainedSpec specAfterStreamName = chainSpec.get(1);

            // Attempt to resolve as property
            Pair<PropertyResolutionDescriptor, String> propertyInfoPair = null;
            try {
                String propName = chainSpec.get(0).getName() + "." + specAfterStreamName.getName();
                propertyInfoPair = ExprIdentNodeUtil.getTypeFromStream(streamTypeService, propName, streamTypeService.hasPropertyAgnosticType(), false);
            } catch (ExprValidationPropertyException ex) {
                // fine
            }
            if (propertyInfoPair != null) {
                if (specAfterStreamName.getParameters().size() != 1) {
                    throw handleNotFound(specAfterStreamName.getName());
                }
                forge = getPropertyPairEvaluator(specAfterStreamName.getParameters().get(0).getForge(), propertyInfoPair, validationContext);
                return null;
            }

            // Attempt to resolve as event-underlying object instance method
            EventType eventType = validationContext.getStreamTypeService().getEventTypes()[prefixedStreamNumber];
            Class type = eventType.getUnderlyingType();

            List<ExprChainedSpec> remainderChain = new ArrayList<ExprChainedSpec>(chainSpec);
            remainderChain.remove(0);

            ExprValidationException methodEx = null;
            ExprDotForge[] underlyingMethodChain = null;
            try {
                EPType typeInfo = EPTypeHelper.singleValue(type);
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
                if (ExprDotNodeUtility.isDatetimeOrEnumMethod(remainderChain.get(0).getName())) {
                    prefixedStreamNumException = enumDatetimeEx;
                } else {
                    prefixedStreamNumException = new ExprValidationException("Failed to solve '" + remainderChain.get(0).getName() + "' to either an date-time or enumeration method, an event property or a method on the event underlying object: " + methodEx.getMessage(), methodEx);
                }
            }
        }

        // There no root node, in this case the classname or property name is provided as part of the chain.
        // Such as "MyClass.myStaticLib(...)" or "mycollectionproperty.doIt(...)"
        //
        List<ExprChainedSpec> modifiedChain = new ArrayList<ExprChainedSpec>(chainSpec);
        ExprChainedSpec firstItem = modifiedChain.remove(0);

        Pair<PropertyResolutionDescriptor, String> propertyInfoPair = null;
        try {
            propertyInfoPair = ExprIdentNodeUtil.getTypeFromStream(streamTypeService, firstItem.getName(), streamTypeService.hasPropertyAgnosticType(), true);
        } catch (ExprValidationPropertyException ex) {
            // not a property
        }

        // If property then treat it as such
        if (propertyInfoPair != null) {

            String propertyName = propertyInfoPair.getFirst().getPropertyName();
            int streamId = propertyInfoPair.getFirst().getStreamNum();
            EventType streamType = streamTypeService.getEventTypes()[streamId];
            EPType typeInfo;
            ExprEnumerationForge enumerationForge = null;
            EPType inputType;
            ExprForge rootNodeForge = null;
            EventPropertyGetterSPI getter;

            if (firstItem.getParameters().isEmpty()) {
                getter = ((EventTypeSPI) streamType).getGetterSPI(propertyInfoPair.getFirst().getPropertyName());

                ExprDotEnumerationSourceForgeForProps propertyEval = ExprDotNodeUtility.getPropertyEnumerationSource(propertyInfoPair.getFirst().getPropertyName(), streamId, streamType, hasEnumerationMethod, validationContext.isDisablePropertyExpressionEventCollCache());
                typeInfo = propertyEval.getReturnType();
                enumerationForge = propertyEval.getEnumeration();
                inputType = propertyEval.getReturnType();
                rootNodeForge = new PropertyDotNonLambdaForge(streamId, getter, JavaClassHelper.getBoxedType(propertyInfoPair.getFirst().getPropertyType()));
            } else {
                // property with parameter - mapped or indexed property
                EventPropertyDescriptor desc = EventTypeUtility.getNestablePropertyDescriptor(streamTypeService.getEventTypes()[propertyInfoPair.getFirst().getStreamNum()], firstItem.getName());
                if (firstItem.getParameters().size() > 1) {
                    throw new ExprValidationException("Property '" + firstItem.getName() + "' may not be accessed passing 2 or more parameters");
                }
                ExprForge paramEval = firstItem.getParameters().get(0).getForge();
                typeInfo = EPTypeHelper.singleValue(desc.getPropertyComponentType());
                inputType = typeInfo;
                getter = null;
                if (desc.isMapped()) {
                    if (paramEval.getEvaluationType() != String.class) {
                        throw new ExprValidationException("Parameter expression to mapped property '" + propertyName + "' is expected to return a string-type value but returns " + JavaClassHelper.getClassNameFullyQualPretty(paramEval.getEvaluationType()));
                    }
                    EventPropertyGetterMappedSPI mappedGetter = ((EventTypeSPI) propertyInfoPair.getFirst().getStreamEventType()).getGetterMappedSPI(propertyInfoPair.getFirst().getPropertyName());
                    if (mappedGetter == null) {
                        throw new ExprValidationException("Mapped property named '" + propertyName + "' failed to obtain getter-object");
                    }
                    rootNodeForge = new PropertyDotNonLambdaMappedForge(streamId, mappedGetter, paramEval, desc.getPropertyComponentType());
                }
                if (desc.isIndexed()) {
                    if (JavaClassHelper.getBoxedType(paramEval.getEvaluationType()) != Integer.class) {
                        throw new ExprValidationException("Parameter expression to mapped property '" + propertyName + "' is expected to return a Integer-type value but returns " + JavaClassHelper.getClassNameFullyQualPretty(paramEval.getEvaluationType()));
                    }
                    EventPropertyGetterIndexedSPI indexedGetter = ((EventTypeSPI) propertyInfoPair.getFirst().getStreamEventType()).getGetterIndexedSPI(propertyInfoPair.getFirst().getPropertyName());
                    if (indexedGetter == null) {
                        throw new ExprValidationException("Mapped property named '" + propertyName + "' failed to obtain getter-object");
                    }
                    rootNodeForge = new PropertyDotNonLambdaIndexedForge(streamId, indexedGetter, paramEval, desc.getPropertyComponentType());
                }
            }
            if (typeInfo == null) {
                throw new ExprValidationException("Property '" + propertyName + "' is not a mapped or indexed property");
            }

            // try to build chain based on the input (non-fragment)
            ExprDotNodeRealizedChain evals;
            ExprDotNodeFilterAnalyzerInputProp filterAnalyzerInputProp = new ExprDotNodeFilterAnalyzerInputProp(propertyInfoPair.getFirst().getStreamNum(), propertyInfoPair.getFirst().getPropertyName());
            boolean rootIsEventBean = false;
            try {
                evals = ExprDotNodeUtility.getChainEvaluators(streamId, inputType, modifiedChain, validationContext, isDuckTyping, filterAnalyzerInputProp);
            } catch (ExprValidationException ex) {

                // try building the chain based on the fragment event type (i.e. A.after(B) based on A-configured start time where A is a fragment)
                FragmentEventType fragment = propertyInfoPair.getFirst().getFragmentEventType();
                if (fragment == null) {
                    throw ex;
                }

                EPType fragmentTypeInfo;
                if (fragment.isIndexed()) {
                    fragmentTypeInfo = EPTypeHelper.collectionOfEvents(fragment.getFragmentType());
                } else {
                    fragmentTypeInfo = EPTypeHelper.singleEvent(fragment.getFragmentType());
                }

                rootIsEventBean = true;
                evals = ExprDotNodeUtility.getChainEvaluators(propertyInfoPair.getFirst().getStreamNum(), fragmentTypeInfo, modifiedChain, validationContext, isDuckTyping, filterAnalyzerInputProp);
                rootNodeForge = new PropertyDotNonLambdaFragmentForge(streamId, getter);
            }

            FilterExprAnalyzerAffector filterExprAnalyzerAffector = evals.getFilterAnalyzerDesc();
            int streamNumReferenced = propertyInfoPair.getFirst().getStreamNum();
            String rootPropertyName = propertyInfoPair.getFirst().getPropertyName();
            forge = new ExprDotNodeForgeRootChild(this, filterExprAnalyzerAffector, streamNumReferenced, rootPropertyName, hasEnumerationMethod, rootNodeForge, enumerationForge, inputType, evals.getChain(), evals.getChainWithUnpack(), !rootIsEventBean);
            return null;
        }

        // If variable then resolve as such
        String contextNameVariable = validationContext.getVariableService().isContextVariable(firstItem.getName());
        if (contextNameVariable != null) {
            throw new ExprValidationException("Method invocation on context-specific variable is not supported");
        }
        VariableReader variableReader = validationContext.getVariableService().getReader(firstItem.getName(), EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID);
        if (variableReader != null) {
            EPType typeInfo;
            ExprDotStaticMethodWrap wrap;
            if (variableReader.getVariableMetaData().getType().isArray()) {
                typeInfo = EPTypeHelper.collectionOfSingleValue(variableReader.getVariableMetaData().getType().getComponentType());
                wrap = new ExprDotStaticMethodWrapArrayScalar(variableReader.getVariableMetaData().getVariableName(), variableReader.getVariableMetaData().getType());
            } else if (variableReader.getVariableMetaData().getEventType() != null) {
                typeInfo = EPTypeHelper.singleEvent(variableReader.getVariableMetaData().getEventType());
                wrap = null;
            } else {
                typeInfo = EPTypeHelper.singleValue(variableReader.getVariableMetaData().getType());
                wrap = null;
            }

            ExprDotNodeRealizedChain evals = ExprDotNodeUtility.getChainEvaluators(null, typeInfo, modifiedChain, validationContext, false, new ExprDotNodeFilterAnalyzerInputStatic());
            forge = new ExprDotNodeForgeVariable(this, variableReader, wrap, evals.getChainWithUnpack());
            return null;
        }

        // try resolve as enumeration class with value
        Object enumconstant = EngineImportUtil.resolveIdentAsEnumConst(firstItem.getName(), validationContext.getEngineImportService(), false);
        if (enumconstant != null) {

            // try resolve method
            final ExprChainedSpec methodSpec = modifiedChain.get(0);
            final String enumvalue = firstItem.getName();
            ExprNodeUtilResolveExceptionHandler handler = new ExprNodeUtilResolveExceptionHandler() {
                public ExprValidationException handle(Exception ex) {
                    return new ExprValidationException("Failed to resolve method '" + methodSpec.getName() + "' on enumeration value '" + enumvalue + "': " + ex.getMessage());
                }
            };
            EventType wildcardType = validationContext.getStreamTypeService().getEventTypes().length != 1 ? null : validationContext.getStreamTypeService().getEventTypes()[0];
            ExprNodeUtilMethodDesc methodDesc = ExprNodeUtilityRich.resolveMethodAllowWildcardAndStream(enumconstant.getClass().getName(), enumconstant.getClass(), methodSpec.getName(), methodSpec.getParameters(), validationContext.getEngineImportService(), validationContext.getEventAdapterService(), validationContext.getStatementId(), wildcardType != null, wildcardType, handler, methodSpec.getName(), validationContext.getTableService(), streamTypeService.getEngineURIQualifier());

            // method resolved, hook up
            modifiedChain.remove(0);    // we identified this piece
            ExprDotStaticMethodWrap optionalLambdaWrap = ExprDotStaticMethodWrapFactory.make(methodDesc.getReflectionMethod(), validationContext.getEventAdapterService(), modifiedChain, null);
            EPType typeInfo = optionalLambdaWrap != null ? optionalLambdaWrap.getTypeInfo() : EPTypeHelper.singleValue(methodDesc.getFastMethod().getReturnType());

            ExprDotNodeRealizedChain evals = ExprDotNodeUtility.getChainEvaluators(null, typeInfo, modifiedChain, validationContext, false, new ExprDotNodeFilterAnalyzerInputStatic());
            forge = new ExprDotNodeForgeStaticMethod(this, false, validationContext.getStatementName(), firstItem.getName(), methodDesc.getFastMethod(),
                    methodDesc.getChildForges(), false, evals.getChainWithUnpack(), optionalLambdaWrap, false, enumconstant);
            return null;
        }

        // if prefixed by a stream name, we are giving up
        if (prefixedStreamNumException != null) {
            throw prefixedStreamNumException;
        }

        // If class then resolve as class
        ExprChainedSpec secondItem = modifiedChain.remove(0);

        boolean allowWildcard = validationContext.getStreamTypeService().getEventTypes().length == 1;
        EventType streamZeroType = null;
        if (validationContext.getStreamTypeService().getEventTypes().length > 0) {
            streamZeroType = validationContext.getStreamTypeService().getEventTypes()[0];
        }

        ExprNodeUtilMethodDesc method = ExprNodeUtilityRich.resolveMethodAllowWildcardAndStream(firstItem.getName(), null, secondItem.getName(), secondItem.getParameters(), validationContext.getEngineImportService(), validationContext.getEventAdapterService(), validationContext.getStatementId(), allowWildcard, streamZeroType, new ExprNodeUtilResolveExceptionHandlerDefault(firstItem.getName() + "." + secondItem.getName(), false), secondItem.getName(), validationContext.getTableService(), streamTypeService.getEngineURIQualifier());

        boolean isConstantParameters = method.isAllConstants() && isUDFCache;
        boolean isReturnsConstantResult = isConstantParameters && modifiedChain.isEmpty();

        // this may return a pair of null if there is no lambda or the result cannot be wrapped for lambda-function use
        ExprDotStaticMethodWrap optionalLambdaWrap = ExprDotStaticMethodWrapFactory.make(method.getReflectionMethod(), validationContext.getEventAdapterService(), modifiedChain, null);
        EPType typeInfo = optionalLambdaWrap != null ? optionalLambdaWrap.getTypeInfo() : EPTypeHelper.singleValue(method.getReflectionMethod().getReturnType());

        ExprDotNodeRealizedChain evals = ExprDotNodeUtility.getChainEvaluators(null, typeInfo, modifiedChain, validationContext, false, new ExprDotNodeFilterAnalyzerInputStatic());
        forge = new ExprDotNodeForgeStaticMethod(this, isReturnsConstantResult, validationContext.getStatementName(), firstItem.getName(), method.getFastMethod(), method.getChildForges(), isConstantParameters, evals.getChainWithUnpack(), optionalLambdaWrap, false, null);

        return null;
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

    private int prefixedStreamName(List<ExprChainedSpec> chainSpec, StreamTypeService streamTypeService) {
        if (chainSpec.size() < 1) {
            return -1;
        }
        ExprChainedSpec spec = chainSpec.get(0);
        if (spec.getParameters().size() > 0 && !spec.isProperty()) {
            return -1;
        }
        return streamTypeService.getStreamNumForStreamName(spec.getName());
    }

    public void accept(ExprNodeVisitor visitor) {
        super.accept(visitor);
        ExprNodeUtilityRich.acceptChain(visitor, chainSpec);
    }

    public void accept(ExprNodeVisitorWithParent visitor) {
        super.accept(visitor);
        ExprNodeUtilityRich.acceptChain(visitor, chainSpec);
    }

    public void acceptChildnodes(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        super.acceptChildnodes(visitor, parent);
        ExprNodeUtilityRich.acceptChain(visitor, chainSpec, this);
    }

    public void replaceUnlistedChildNode(ExprNode nodeToReplace, ExprNode newNode) {
        ExprNodeUtilityRich.replaceChainChildNode(nodeToReplace, newNode, chainSpec);
    }

    public List<ExprChainedSpec> getChainSpec() {
        return chainSpec;
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
            writer.append(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(this.getChildNodes()[0]));
        }
        ExprNodeUtilityRich.toExpressionString(chainSpec, writer, this.getChildNodes().length != 0, null);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.MINIMUM;
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
        return ExprNodeUtilityRich.collectChainParameters(chainSpec);
    }

    public String isVariableOpGetName(VariableService variableService) {
        VariableMetaData metaData = null;
        if (chainSpec.size() > 0 && chainSpec.get(0).isProperty()) {
            metaData = variableService.getVariableMetaData(chainSpec.get(0).getName());
        }
        return metaData == null ? null : metaData.getVariableName();
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
        String lhsName = chainSpec.get(0).getName().toLowerCase(Locale.ENGLISH);
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
        String lhsName = chainSpec.get(0).getName().toLowerCase(Locale.ENGLISH);
        String operationName = chainSpec.get(1).getName().toLowerCase(Locale.ENGLISH);
        boolean pointInside = lhsName.equals("point") && operationName.equals("inside");
        boolean rectangleIntersects = lhsName.equals("rectangle") && operationName.equals("intersects");
        if (!pointInside && !rectangleIntersects) {
            return null;
        }
        if (chainSpec.get(1).getParameters().size() != 1) {
            throw getAppDocMethodException(lhsName, operationName);
        }
        ExprNode param = chainSpec.get(1).getParameters().get(0);
        if (!(param instanceof ExprDotNode)) {
            throw getAppDocMethodException(lhsName, operationName);
        }
        ExprDotNode compared = (ExprDotNode) chainSpec.get(1).getParameters().get(0);
        if (compared.getChainSpec().size() != 1) {
            throw getAppDocMethodException(lhsName, operationName);
        }
        String rhsName = compared.getChainSpec().get(0).getName().toLowerCase(Locale.ENGLISH);
        boolean pointInsideRectangle = pointInside && rhsName.equals("rectangle");
        boolean rectangleIntersectsRectangle = rectangleIntersects && rhsName.equals("rectangle");
        if (!pointInsideRectangle && !rectangleIntersectsRectangle) {
            throw getAppDocMethodException(lhsName, operationName);
        }

        List<ExprNode> lhsExpressions = chainSpec.get(0).getParameters();
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

        ExprNode[] lhs = ExprNodeUtilityCore.toArray(lhsExpressionsValues);
        ExprNode[] rhs = ExprNodeUtilityCore.toArray(compared.getChainSpec().get(0).getParameters());

        EngineImportApplicationDotMethod predefined;
        if (pointInsideRectangle) {
            predefined = new EngineImportApplicationDotMethodPointInsideRectange(this, lhsName, lhs, operationName, rhsName, rhs, indexNamedParameter);
        } else {
            predefined = new EngineImportApplicationDotMethodRectangeIntersectsRectangle(this, lhsName, lhs, operationName, rhsName, rhs, indexNamedParameter);
        }
        return new ExprAppDotMethodImpl(predefined);
    }

    private ExprValidationException getAppDocMethodException(String lhsName, String operationName) {
        return new ExprValidationException(lhsName + "." + operationName + " requires a single rectangle as parameter");
    }
}

