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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodDesc;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodResolver;
import com.espertech.esper.common.internal.epl.datetime.eval.ExprDotDTFactory;
import com.espertech.esper.common.internal.epl.datetime.eval.ExprDotDTMethodDesc;
import com.espertech.esper.common.internal.epl.enummethod.dot.*;
import com.espertech.esper.common.internal.epl.expression.chain.Chainable;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableArray;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableName;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.rettype.*;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

public class ExprDotNodeUtility {

    public static ObjectArrayEventType makeTransientOAType(String enumMethod, String propertyName, EPType type, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        Map<String, Object> propsResult = new HashMap<>();
        propsResult.put(propertyName, JavaClassHelper.getBoxedType(type));
        return makeTransientOATypeInternal(enumMethod, propsResult, propertyName, statementRawInfo, services);
    }

    public static ObjectArrayEventType makeTransientOAType(String enumMethod, Map<String, Object> boxedPropertyTypes, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        return makeTransientOATypeInternal(enumMethod, boxedPropertyTypes, CodeGenerationIDGenerator.generateClassNameUUID(), statementRawInfo, services);
    }

    private static ObjectArrayEventType makeTransientOATypeInternal(String enumMethod, Map<String, Object> boxedPropertyTypes, String eventTypeNameUUid, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        String eventTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousTypeNameEnumMethod(enumMethod, eventTypeNameUUid);
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, statementRawInfo.getModuleName(), EventTypeTypeClass.ENUMDERIVED, EventTypeApplicationType.OBJECTARR, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        ObjectArrayEventType oatype = BaseNestableEventUtil.makeOATypeCompileTime(metadata, boxedPropertyTypes, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(oatype);
        return oatype;
    }

    public static boolean isDatetimeOrEnumMethod(String name, ClasspathImportServiceCompileTime classpathImportService) throws ExprValidationException {
        return EnumMethodResolver.isEnumerationMethod(name, classpathImportService) || DatetimeMethodResolver.isDateTimeMethod(name, classpathImportService);
    }

    public static ExprDotEnumerationSourceForge getEnumerationSource(ExprNode inputExpression, StreamTypeService streamTypeService, boolean hasEnumerationMethod, boolean disablePropertyExpressionEventCollCache, StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        ExprForge rootNodeForge = inputExpression.getForge();
        ExprEnumerationForge rootLambdaForge = null;
        EPChainableType info = null;

        if (rootNodeForge instanceof ExprEnumerationForge) {
            rootLambdaForge = (ExprEnumerationForge) rootNodeForge;
            EventType eventTypeCollection = rootLambdaForge.getEventTypeCollection(statementRawInfo, compileTimeServices);
            if (eventTypeCollection != null) {
                info = EPChainableTypeHelper.collectionOfEvents(eventTypeCollection);
            }
            if (info == null) {
                EventType eventTypeSingle = rootLambdaForge.getEventTypeSingle(statementRawInfo, compileTimeServices);
                if (eventTypeSingle != null) {
                    info = EPChainableTypeHelper.singleEvent(eventTypeSingle);
                }
            }
            if (info == null) {
                Class componentType = rootLambdaForge.getComponentTypeCollection() == null ? null : rootLambdaForge.getComponentTypeCollection().getType();
                if (componentType != null) {
                    info = EPChainableTypeHelper.collectionOfSingleValue(rootLambdaForge.getComponentTypeCollection());
                }
            }
            if (info == null) {
                rootLambdaForge = null; // not a lambda evaluator
            }
        } else if (inputExpression instanceof ExprIdentNode) {
            ExprIdentNode identNode = (ExprIdentNode) inputExpression;
            int streamId = identNode.getStreamId();
            EventType streamType = streamTypeService.getEventTypes()[streamId];
            return getPropertyEnumerationSource(identNode.getResolvedPropertyName(), streamId, streamType, hasEnumerationMethod, disablePropertyExpressionEventCollCache);
        }
        return new ExprDotEnumerationSourceForge(info, null, rootLambdaForge);
    }

    public static ExprDotEnumerationSourceForgeForProps getPropertyEnumerationSource(String propertyName, int streamId, EventType streamType, boolean allowEnumType, boolean disablePropertyExpressionEventCollCache) {

        EPType propertyType = streamType.getPropertyEPType(propertyName);
        EPChainableType typeInfo = EPChainableTypeHelper.singleValue(propertyType);  // assume scalar for now

        // no enumeration methods, no need to expose as an enumeration
        if (!allowEnumType) {
            return new ExprDotEnumerationSourceForgeForProps(null, typeInfo, streamId, null);
        }

        FragmentEventType fragmentEventType = streamType.getFragmentType(propertyName);
        EventPropertyGetterSPI getter = ((EventTypeSPI) streamType).getGetterSPI(propertyName);

        ExprEnumerationForge enumEvaluator = null;
        if (getter != null && fragmentEventType != null) {
            if (fragmentEventType.isIndexed()) {
                enumEvaluator = new PropertyDotEventCollectionForge(propertyName, streamId, fragmentEventType.getFragmentType(), getter, disablePropertyExpressionEventCollCache);
                typeInfo = EPChainableTypeHelper.collectionOfEvents(fragmentEventType.getFragmentType());
            } else {   // we don't want native to require an eventbean instance
                enumEvaluator = new PropertyDotEventSingleForge(streamId, fragmentEventType.getFragmentType(), getter);
                typeInfo = EPChainableTypeHelper.singleEvent(fragmentEventType.getFragmentType());
            }
        } else {
            EventPropertyDescriptor desc = EventTypeUtility.getNestablePropertyDescriptor(streamType, propertyName);
            if (desc != null && desc.isIndexed() && !desc.isRequiresIndex() && desc.getPropertyComponentType() != null) {
                if (JavaClassHelper.isImplementsInterface(propertyType, Collection.class)) {
                    enumEvaluator = new PropertyDotScalarCollection(propertyName, streamId, getter, desc.getPropertyComponentEPType());
                } else if (JavaClassHelper.isImplementsInterface(propertyType, Iterable.class)) {
                    enumEvaluator = new PropertyDotScalarIterable(propertyName, streamId, getter, desc.getPropertyComponentEPType(), (EPTypeClass) propertyType);
                } else if (((EPTypeClass) propertyType).getType().isArray()) {
                    enumEvaluator = new PropertyDotScalarArrayForge(propertyName, streamId, getter, desc.getPropertyComponentEPType(), (EPTypeClass) desc.getPropertyEPType());
                } else {
                    throw new IllegalStateException("Property indicated indexed-type but failed to find proper collection adapter for use with enumeration methods");
                }
                typeInfo = EPChainableTypeHelper.collectionOfSingleValue(desc.getPropertyComponentEPType());
            }
        }
        return new ExprDotEnumerationSourceForgeForProps(enumEvaluator, typeInfo, streamId, (ExprEnumerationGivenEventForge) enumEvaluator);
    }

    public static ExprDotEval[] getEvaluators(ExprDotForge[] forges) {
        ExprDotEval[] evals = new ExprDotEval[forges.length];
        for (int i = 0; i < forges.length; i++) {
            evals[i] = forges[i].getDotEvaluator();
        }
        return evals;
    }

    public static Object evaluateChain(ExprDotForge[] forges, ExprDotEval[] evaluators, Object inner, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        for (ExprDotEval methodEval : evaluators) {
            inner = methodEval.evaluate(inner, eventsPerStream, isNewData, context);
            if (inner == null) {
                break;
            }
        }
        return inner;
    }

    public static ExprDotNodeRealizedChain getChainEvaluators(
        Integer streamOfProviderIfApplicable,
        EPChainableType inputType,
        List<Chainable> chainSpec,
        ExprValidationContext validationContext,
        boolean isDuckTyping,
        ExprDotNodeFilterAnalyzerInput inputDesc)
        throws ExprValidationException {
        List<ExprDotForge> methodForges = new ArrayList<>();
        EPChainableType currentInputType = inputType;
        EnumMethodDesc lastLambdaFunc = null;
        Chainable lastElement = chainSpec.isEmpty() ? null : chainSpec.get(chainSpec.size() - 1);
        FilterExprAnalyzerAffector filterAnalyzerDesc = null;

        Deque<Chainable> chainSpecStack = new ArrayDeque<Chainable>(chainSpec);
        while (!chainSpecStack.isEmpty()) {
            Chainable chainElement = chainSpecStack.removeFirst();
            List<ExprNode> parameters = chainElement.getParametersOrEmpty();
            String chainElementName = chainElement.getRootNameOrEmptyString();
            boolean last = chainSpecStack.isEmpty();
            lastLambdaFunc = null;  // reset

            // compile parameters for chain element
            ExprForge[] paramForges = new ExprForge[parameters.size()];
            EPType[] paramTypes = new EPType[parameters.size()];
            for (int i = 0; i < parameters.size(); i++) {
                paramForges[i] = parameters.get(i).getForge();
                paramTypes[i] = paramForges[i].getEvaluationType();
            }

            // check if special 'size' method
            if (currentInputType instanceof EPChainableTypeClass) {
                EPChainableTypeClass type = (EPChainableTypeClass) currentInputType;
                boolean array = type.getType().getType().isArray();
                boolean collection = JavaClassHelper.isSubclassOrImplementsInterface(type.getType(), EPTypePremade.COLLECTION.getEPType());
                if ((array || collection) && chainElementName.toLowerCase(Locale.ENGLISH).equals("size") && paramTypes.length == 0 && lastElement == chainElement) {
                    ExprDotForge size;
                    if (array) {
                        size = new ExprDotForgeSizeArray();
                    } else {
                        size = new ExprDotForgeSizeCollection();
                    }
                    methodForges.add(size);
                    currentInputType = size.getTypeInfo();
                    continue;
                }
                if ((array || collection) && chainElementName.toLowerCase(Locale.ENGLISH).equals("get") && paramTypes.length == 1 && JavaClassHelper.isTypeInteger(paramTypes[0])) {
                    ExprDotForge get;
                    EPTypeClass component = array ? JavaClassHelper.getArrayComponentType(type.getType()) : JavaClassHelper.getSingleParameterTypeOrObject(type.getType());
                    EPTypeClass componentBoxed = JavaClassHelper.getBoxedType(component);
                    if (array) {
                        get = new ExprDotForgeGetArray(paramForges[0], componentBoxed);
                    } else {
                        get = new ExprDotForgeGetCollection(paramForges[0], componentBoxed);
                    }
                    methodForges.add(get);
                    currentInputType = get.getTypeInfo();
                    continue;
                }
                if (chainElement instanceof ChainableArray && array) {
                    ChainableArray chainableArray = (ChainableArray) chainElement;
                    final EPChainableType typeInfo = currentInputType;
                    ExprNode indexExpr = ChainableArray.validateSingleIndexExpr(chainableArray.getIndexes(), () -> "operation on type " + EPChainableTypeHelper.toTypeDescriptive(typeInfo));
                    EPTypeClass componentType = JavaClassHelper.getBoxedType(JavaClassHelper.getArrayComponentType(type.getType()));
                    ExprDotForgeGetArray get = new ExprDotForgeGetArray(indexExpr.getForge(), componentType);
                    methodForges.add(get);
                    currentInputType = get.getTypeInfo();
                    continue;
                }
            }

            // determine if there is a matching method
            boolean matchingMethod = false;
            EPTypeClass optionalMethodTarget = getMethodTarget(currentInputType);
            if (optionalMethodTarget != null && (!(chainElement instanceof ChainableArray))) {
                try {
                    getValidateMethodDescriptor(optionalMethodTarget, chainElementName, parameters, validationContext);
                    matchingMethod = true;
                } catch (ExprValidationException ex) {
                    // expected
                }
            }

            if (EnumMethodResolver.isEnumerationMethod(chainElementName, validationContext.getClasspathImportService()) && (!matchingMethod || (optionalMethodTarget != null && (optionalMethodTarget.getType().isArray() || JavaClassHelper.isImplementsInterface(optionalMethodTarget, Collection.class))))) {
                EnumMethodDesc enumerationMethod = EnumMethodResolver.fromName(chainElementName, validationContext.getClasspathImportService());
                ExprDotForgeEnumMethod eval = enumerationMethod.getFactory().make(chainElement.getParametersOrEmpty().size());
                eval.init(streamOfProviderIfApplicable, enumerationMethod, chainElementName, currentInputType, parameters, validationContext);
                currentInputType = eval.getTypeInfo();
                if (currentInputType == null) {
                    throw new IllegalStateException("Enumeration method '" + chainElementName + "' has not returned type information");
                }
                methodForges.add(eval);
                lastLambdaFunc = enumerationMethod;
                continue;
            }

            // resolve datetime
            if (DatetimeMethodResolver.isDateTimeMethod(chainElementName, validationContext.getClasspathImportService()) && (!matchingMethod || (optionalMethodTarget != null && (optionalMethodTarget.getType() == Calendar.class || optionalMethodTarget.getType() == Date.class)))) {
                DatetimeMethodDesc datetimeMethod = DatetimeMethodResolver.fromName(chainElementName, validationContext.getClasspathImportService());
                try {
                    ExprDotDTMethodDesc datetimeImpl = ExprDotDTFactory.validateMake(validationContext.getStreamTypeService(), chainSpecStack, datetimeMethod, chainElementName, currentInputType, parameters, inputDesc, validationContext.getClasspathImportService().getTimeAbacus(), validationContext.getTableCompileTimeResolver(), validationContext.getClasspathImportService(), validationContext.getStatementRawInfo());
                    currentInputType = datetimeImpl.getReturnType();
                    if (currentInputType == null) {
                        throw new IllegalStateException("Date-time method '" + chainElementName + "' has not returned type information");
                    }
                    methodForges.add(datetimeImpl.getForge());
                    filterAnalyzerDesc = datetimeImpl.getIntervalFilterDesc();
                    continue;
                } catch (ExprValidationException ex) {
                    if (!chainElementName.toLowerCase(Locale.ENGLISH).equals("get")) {
                        throw ex;
                    }
                }
            }

            // try to resolve as property if the last method returned a type
            if (currentInputType instanceof EPChainableTypeEventSingle) {
                if (chainElement instanceof ChainableArray) {
                    throw new ExprValidationException("Could not perform array operation on type " + EPChainableTypeHelper.toTypeDescriptive(currentInputType));
                }
                EventTypeSPI inputEventType = (EventTypeSPI) ((EPChainableTypeEventSingle) currentInputType).getType();
                EPType type = inputEventType.getPropertyEPType(chainElementName);
                EventPropertyGetterSPI getter = inputEventType.getGetterSPI(chainElementName);
                FragmentEventType fragmentType = inputEventType.getFragmentType(chainElementName);
                ExprDotForge forge;
                if (type != null && getter != null) {
                    if (fragmentType == null || last) {
                        forge = new ExprDotForgeProperty(getter, EPChainableTypeHelper.singleValue(JavaClassHelper.getBoxedType(type)));
                        currentInputType = forge.getTypeInfo();
                    } else {
                        if (!fragmentType.isIndexed()) {
                            currentInputType = EPChainableTypeHelper.singleEvent(fragmentType.getFragmentType());
                        } else {
                            currentInputType = EPChainableTypeHelper.arrayOfEvents(fragmentType.getFragmentType());
                        }
                        forge = new ExprDotForgePropertyFragment(getter, currentInputType);
                    }
                    methodForges.add(forge);
                    continue;
                }
            }

            if (currentInputType instanceof EPChainableTypeEventMulti && chainElement instanceof ChainableArray) {
                EventTypeSPI inputEventType = (EventTypeSPI) ((EPChainableTypeEventMulti) currentInputType).getComponent();
                ChainableArray array = (ChainableArray) chainElement;
                EPChainableType typeInfo = currentInputType;
                ExprNode indexExpr = ChainableArray.validateSingleIndexExpr(array.getIndexes(), () -> "operation on type " + EPChainableTypeHelper.toTypeDescriptive(typeInfo));
                currentInputType = EPChainableTypeHelper.singleEvent(inputEventType);
                ExprDotForgeEventArrayAtIndex forge = new ExprDotForgeEventArrayAtIndex(currentInputType, indexExpr);
                methodForges.add(forge);
                continue;
            }

            // Finally try to resolve the method
            if (optionalMethodTarget != null && !(chainElement instanceof ChainableArray)) {
                try {
                    // find descriptor again, allow for duck typing
                    ExprNodeUtilMethodDesc desc = getValidateMethodDescriptor(optionalMethodTarget, chainElementName, parameters, validationContext);
                    paramForges = desc.getChildForges();
                    ExprDotForge forge = getDotChainMethodCallForge(currentInputType, validationContext, chainSpecStack, desc, paramForges);
                    methodForges.add(forge);
                    currentInputType = forge.getTypeInfo();
                } catch (Exception e) {
                    if (!isDuckTyping) {
                        if (chainElement instanceof ChainableName) {
                            // try "something.property" -> getProperty()
                            try {
                                String methodName = "get" + Character.toUpperCase(chainElementName.charAt(0)) + chainElementName.substring(1);
                                ExprNodeUtilMethodDesc desc = getValidateMethodDescriptor(optionalMethodTarget, methodName, parameters, validationContext);
                                ExprDotForge forge = getDotChainMethodCallForge(currentInputType, validationContext, chainSpecStack, desc, paramForges);
                                methodForges.add(forge);
                                currentInputType = forge.getTypeInfo();
                                continue;
                            } catch (Exception e2) {
                                throw new ExprValidationException(e.getMessage(), e);
                            }
                        }
                        throw new ExprValidationException(e.getMessage(), e);
                    } else {
                        ExprDotMethodForgeDuck duck = new ExprDotMethodForgeDuck(validationContext.getStatementName(), validationContext.getClasspathImportService(), chainElementName, paramTypes, paramForges);
                        methodForges.add(duck);
                        currentInputType = duck.getTypeInfo();
                    }
                }
                continue;
            }

            String message;
            if (!(chainElement instanceof ChainableArray)) {
                message = "Could not find event property or method named '" + chainElementName + "' in " + EPChainableTypeHelper.toTypeDescriptive(currentInputType);
            } else {
                message = "Could not perform array operation on type " + EPChainableTypeHelper.toTypeDescriptive(currentInputType);
            }
            throw new ExprValidationException(message);
        }

        ExprDotForge[] intermediateEvals = methodForges.toArray(new ExprDotForge[methodForges.size()]);

        if (lastLambdaFunc != null) {
            ExprDotForge finalEval = null;
            if (currentInputType instanceof EPChainableTypeEventMulti) {
                EPChainableTypeEventMulti mvType = (EPChainableTypeEventMulti) currentInputType;
                TableMetaData tableMetadata = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(mvType.getComponent());
                if (tableMetadata != null) {
                    finalEval = new ExprDotForgeUnpackCollEventBeanTable(mvType.getComponent(), tableMetadata);
                } else {
                    finalEval = new ExprDotForgeUnpackCollEventBean(mvType.getComponent());
                }
            } else if (currentInputType instanceof EPChainableTypeEventSingle) {
                EPChainableTypeEventSingle epType = (EPChainableTypeEventSingle) currentInputType;
                TableMetaData tableMetadata = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(epType.getType());
                if (tableMetadata != null) {
                    finalEval = new ExprDotForgeUnpackBeanTable(epType.getType(), tableMetadata);
                } else {
                    finalEval = new ExprDotForgeUnpackBean(epType.getType());
                }
            }
            if (finalEval != null) {
                methodForges.add(finalEval);
            }
        }

        ExprDotForge[] unpackingForges = methodForges.toArray(new ExprDotForge[methodForges.size()]);
        return new ExprDotNodeRealizedChain(intermediateEvals, unpackingForges, filterAnalyzerDesc);
    }

    private static ExprDotForge getDotChainMethodCallForge(EPChainableType currentInputType, ExprValidationContext validationContext, Deque<Chainable> chainSpecStack, ExprNodeUtilMethodDesc desc, ExprForge[] paramForges) throws ExprValidationException {
        if (currentInputType instanceof EPChainableTypeClass) {
            // if followed by an enumeration method, convert array to collection
            if (desc.getReflectionMethod().getReturnType().isArray() && !chainSpecStack.isEmpty() && EnumMethodResolver.isEnumerationMethod(chainSpecStack.getFirst().getRootNameOrEmptyString(), validationContext.getClasspathImportService())) {
                return new ExprDotMethodForgeNoDuck(validationContext.getStatementName(), desc.getReflectionMethod(), desc.getMethodTargetType(), paramForges, ExprDotMethodForgeNoDuck.WrapType.WRAPARRAY);
            } else {
                return new ExprDotMethodForgeNoDuck(validationContext.getStatementName(), desc.getReflectionMethod(), desc.getMethodTargetType(), paramForges, ExprDotMethodForgeNoDuck.WrapType.PLAIN);
            }
        } else {
            return new ExprDotMethodForgeNoDuck(validationContext.getStatementName(), desc.getReflectionMethod(), desc.getMethodTargetType(), paramForges, ExprDotMethodForgeNoDuck.WrapType.UNDERLYING);
        }
    }

    private static EPTypeClass getMethodTarget(EPChainableType currentInputType) {
        if (currentInputType instanceof EPChainableTypeClass) {
            return ((EPChainableTypeClass) currentInputType).getType();
        } else if (currentInputType instanceof EPChainableTypeEventSingle) {
            return ((EPChainableTypeEventSingle) currentInputType).getType().getUnderlyingEPType();
        }
        return null;
    }

    public static Object evaluateChainWithWrap(ExprDotStaticMethodWrap resultWrapLambda,
                                               Object result,
                                               EventType optionalResultSingleEventType,
                                               Class resultType,
                                               ExprDotEval[] chainEval,
                                               ExprDotForge[] chainForges,
                                               EventBean[] eventsPerStream,
                                               boolean newData,
                                               ExprEvaluatorContext exprEvaluatorContext) {
        if (result == null) {
            return null;
        }

        if (resultWrapLambda != null) {
            result = resultWrapLambda.convertNonNull(result);
        }

        for (ExprDotEval aChainEval : chainEval) {
            result = aChainEval.evaluate(result, eventsPerStream, newData, exprEvaluatorContext);
            if (result == null) {
                return result;
            }
        }
        return result;
    }

    public static CodegenExpression evaluateChainCodegen(CodegenMethod parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, CodegenExpression inner, EPTypeClass innerType, ExprDotForge[] forges, ExprDotStaticMethodWrap optionalResultWrapLambda) {
        if (forges.length == 0) {
            return inner;
        }
        ExprDotForge last = forges[forges.length - 1];
        EPTypeClass lastType = EPChainableTypeHelper.getCodegenReturnType(last.getTypeInfo());
        CodegenMethod methodNode = parent.makeChild(lastType, ExprDotNodeUtility.class, codegenClassScope).addParam(innerType, "inner");

        CodegenBlock block = methodNode.getBlock();
        String currentTarget = "wrapped";
        EPTypeClass currentTargetType;
        if (optionalResultWrapLambda != null) {
            currentTargetType = EPChainableTypeHelper.getCodegenReturnType(optionalResultWrapLambda.getTypeInfo());
            if (!JavaClassHelper.isTypeVoid(lastType)) {
                block.ifRefNullReturnNull("inner");
            }
            block.declareVar(currentTargetType, "wrapped", optionalResultWrapLambda.codegenConvertNonNull(ref("inner"), methodNode, codegenClassScope));
        } else {
            block.declareVar(innerType, "wrapped", ref("inner"));
            currentTargetType = innerType;
        }

        String refname = null;
        ExprDotEvalVisitorImpl instrumentationName = new ExprDotEvalVisitorImpl();
        for (int i = 0; i < forges.length; i++) {
            refname = "r" + i;
            forges[i].visit(instrumentationName);
            block.apply(instblock(codegenClassScope, "qExprDotChainElement", constant(i), constant(instrumentationName.getMethodType()), constant(instrumentationName.getMethodName())));

            CodegenExpression typeInformation = constantNull();
            if (codegenClassScope.isInstrumented()) {
                typeInformation = codegenClassScope.addOrGetFieldSharable(new EPChainableTypeCodegenSharable(forges[i].getTypeInfo(), codegenClassScope));
            }

            EPTypeClass reftype = EPChainableTypeHelper.getCodegenReturnType(forges[i].getTypeInfo());
            if (JavaClassHelper.isTypeVoid(reftype)) {
                block.expression(forges[i].codegen(ref(currentTarget), currentTargetType, methodNode, exprSymbol, codegenClassScope))
                    .apply(instblock(codegenClassScope, "aExprDotChainElement", typeInformation, constantNull()));
            } else {
                block.declareVar(reftype, refname, forges[i].codegen(ref(currentTarget), currentTargetType, methodNode, exprSymbol, codegenClassScope));
                currentTarget = refname;
                currentTargetType = reftype;
                if (!reftype.getType().isPrimitive()) {
                    CodegenBlock ifBlock = block.ifRefNull(refname)
                        .apply(instblock(codegenClassScope, "aExprDotChainElement", typeInformation, constantNull()));
                    if (!JavaClassHelper.isTypeVoid(lastType)) {
                        ifBlock.blockReturn(constantNull());
                    } else {
                        ifBlock.blockEnd();
                    }
                }
                block.apply(instblock(codegenClassScope, "aExprDotChainElement", typeInformation, ref(refname)));
            }
        }
        if (JavaClassHelper.isTypeVoid(lastType)) {
            block.methodEnd();
        } else {
            block.methodReturn(ref(refname));
        }
        return localMethod(methodNode, inner);
    }

    private static ExprNodeUtilMethodDesc getValidateMethodDescriptor(EPTypeClass methodTarget, final String methodName, List<ExprNode> parameters, ExprValidationContext validationContext)
        throws ExprValidationException {
        ExprNodeUtilResolveExceptionHandler exceptionHandler = new ExprNodeUtilResolveExceptionHandler() {
            public ExprValidationException handle(Exception e) {
                return new ExprValidationException("Failed to resolve method '" + methodName + "': " + e.getMessage(), e);
            }
        };
        EventType wildcardType = validationContext.getStreamTypeService().getEventTypes().length != 1 ? null : validationContext.getStreamTypeService().getEventTypes()[0];
        return ExprNodeUtilityResolve.resolveMethodAllowWildcardAndStream(methodTarget.getTypeName(), methodTarget, methodName, parameters,
            wildcardType != null, wildcardType, exceptionHandler, methodName, validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
    }
}
