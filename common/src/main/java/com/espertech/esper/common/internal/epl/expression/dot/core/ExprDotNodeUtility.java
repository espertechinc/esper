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

    public static ObjectArrayEventType makeTransientOAType(String enumMethod, String propertyName, Class type, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
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
        EPType info = null;

        if (rootNodeForge instanceof ExprEnumerationForge) {
            rootLambdaForge = (ExprEnumerationForge) rootNodeForge;
            EventType eventTypeCollection = rootLambdaForge.getEventTypeCollection(statementRawInfo, compileTimeServices);
            if (eventTypeCollection != null) {
                info = EPTypeHelper.collectionOfEvents(eventTypeCollection);
            }
            if (info == null) {
                EventType eventTypeSingle = rootLambdaForge.getEventTypeSingle(statementRawInfo, compileTimeServices);
                if (eventTypeSingle != null) {
                    info = EPTypeHelper.singleEvent(eventTypeSingle);
                }
            }
            if (info == null) {
                Class componentType = rootLambdaForge.getComponentTypeCollection();
                if (componentType != null) {
                    info = EPTypeHelper.collectionOfSingleValue(rootLambdaForge.getComponentTypeCollection());
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

        Class propertyType = streamType.getPropertyType(propertyName);
        EPType typeInfo = EPTypeHelper.singleValue(propertyType);  // assume scalar for now

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
                typeInfo = EPTypeHelper.collectionOfEvents(fragmentEventType.getFragmentType());
            } else {   // we don't want native to require an eventbean instance
                enumEvaluator = new PropertyDotEventSingleForge(streamId, fragmentEventType.getFragmentType(), getter);
                typeInfo = EPTypeHelper.singleEvent(fragmentEventType.getFragmentType());
            }
        } else {
            EventPropertyDescriptor desc = EventTypeUtility.getNestablePropertyDescriptor(streamType, propertyName);
            if (desc != null && desc.isIndexed() && !desc.isRequiresIndex() && desc.getPropertyComponentType() != null) {
                if (JavaClassHelper.isImplementsInterface(propertyType, Collection.class)) {
                    enumEvaluator = new PropertyDotScalarCollection(propertyName, streamId, getter, desc.getPropertyComponentType());
                } else if (JavaClassHelper.isImplementsInterface(propertyType, Iterable.class)) {
                    enumEvaluator = new PropertyDotScalarIterable(propertyName, streamId, getter, desc.getPropertyComponentType(), propertyType);
                } else if (propertyType.isArray()) {
                    enumEvaluator = new PropertyDotScalarArrayForge(propertyName, streamId, getter, desc.getPropertyComponentType(), desc.getPropertyType());
                } else {
                    throw new IllegalStateException("Property indicated indexed-type but failed to find proper collection adapter for use with enumeration methods");
                }
                typeInfo = EPTypeHelper.collectionOfSingleValue(desc.getPropertyComponentType());
            }
        }
        return new ExprDotEnumerationSourceForgeForProps(enumEvaluator, typeInfo, streamId, (ExprEnumerationGivenEventForge) enumEvaluator);
    }

    public static EventType[] getSingleLambdaParamEventType(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        if (inputEventType != null) {
            return new EventType[]{inputEventType};
        } else {
            return new EventType[]{ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, goesToNames.get(0), collectionComponentType, statementRawInfo, services)};
        }
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
        EPType inputType,
        List<ExprChainedSpec> chainSpec,
        ExprValidationContext validationContext,
        boolean isDuckTyping,
        ExprDotNodeFilterAnalyzerInput inputDesc)
        throws ExprValidationException {
        List<ExprDotForge> methodForges = new ArrayList<>();
        EPType currentInputType = inputType;
        EnumMethodDesc lastLambdaFunc = null;
        ExprChainedSpec lastElement = chainSpec.isEmpty() ? null : chainSpec.get(chainSpec.size() - 1);
        FilterExprAnalyzerAffector filterAnalyzerDesc = null;

        Deque<ExprChainedSpec> chainSpecStack = new ArrayDeque<ExprChainedSpec>(chainSpec);
        while (!chainSpecStack.isEmpty()) {
            ExprChainedSpec chainElement = chainSpecStack.removeFirst();
            lastLambdaFunc = null;  // reset

            // compile parameters for chain element
            ExprForge[] paramForges = new ExprForge[chainElement.getParameters().size()];
            Class[] paramTypes = new Class[chainElement.getParameters().size()];
            for (int i = 0; i < chainElement.getParameters().size(); i++) {
                paramForges[i] = chainElement.getParameters().get(i).getForge();
                paramTypes[i] = paramForges[i].getEvaluationType();
            }

            // check if special 'size' method
            if (currentInputType instanceof ClassMultiValuedEPType) {
                ClassMultiValuedEPType type = (ClassMultiValuedEPType) currentInputType;
                if (chainElement.getName().toLowerCase(Locale.ENGLISH).equals("size") && paramTypes.length == 0 && lastElement == chainElement) {
                    ExprDotForgeArraySize sizeExpr = new ExprDotForgeArraySize();
                    methodForges.add(sizeExpr);
                    currentInputType = sizeExpr.getTypeInfo();
                    continue;
                }
                if (chainElement.getName().toLowerCase(Locale.ENGLISH).equals("get") && paramTypes.length == 1 && JavaClassHelper.getBoxedType(paramTypes[0]) == Integer.class) {
                    Class componentType = JavaClassHelper.getBoxedType(type.getComponent());
                    ExprDotForgeArrayGet get = new ExprDotForgeArrayGet(paramForges[0], componentType);
                    methodForges.add(get);
                    currentInputType = get.getTypeInfo();
                    continue;
                }
            }

            // determine if there is a matching method
            boolean matchingMethod = false;
            Class methodTarget = getMethodTarget(currentInputType);
            if (methodTarget != null) {
                try {
                    getValidateMethodDescriptor(methodTarget, chainElement.getName(), chainElement.getParameters(), validationContext);
                    matchingMethod = true;
                } catch (ExprValidationException ex) {
                    // expected
                }
            }

            if (EnumMethodResolver.isEnumerationMethod(chainElement.getName(), validationContext.getClasspathImportService()) && (!matchingMethod || methodTarget.isArray() || JavaClassHelper.isImplementsInterface(methodTarget, Collection.class))) {
                EnumMethodDesc enumerationMethod = EnumMethodResolver.fromName(chainElement.getName(), validationContext.getClasspathImportService());
                ExprDotForgeEnumMethod eval = enumerationMethod.getFactory().make();
                if (currentInputType instanceof ClassEPType && JavaClassHelper.isImplementsInterface(((ClassEPType) currentInputType).getType(), Collection.class)) {
                    currentInputType = EPTypeHelper.collectionOfSingleValue(Object.class);
                }
                eval.init(streamOfProviderIfApplicable, enumerationMethod, chainElement.getName(), currentInputType, chainElement.getParameters(), validationContext);
                currentInputType = eval.getTypeInfo();
                if (currentInputType == null) {
                    throw new IllegalStateException("Enumeration method '" + chainElement.getName() + "' has not returned type information");
                }
                methodForges.add(eval);
                lastLambdaFunc = enumerationMethod;
                continue;
            }

            // resolve datetime
            if (DatetimeMethodResolver.isDateTimeMethod(chainElement.getName(), validationContext.getClasspathImportService()) && (!matchingMethod || methodTarget == Calendar.class || methodTarget == Date.class)) {
                DatetimeMethodDesc datetimeMethod = DatetimeMethodResolver.fromName(chainElement.getName(), validationContext.getClasspathImportService());
                ExprDotDTMethodDesc datetimeImpl = ExprDotDTFactory.validateMake(validationContext.getStreamTypeService(), chainSpecStack, datetimeMethod, chainElement.getName(), currentInputType, chainElement.getParameters(), inputDesc, validationContext.getClasspathImportService().getTimeAbacus(), validationContext.getTableCompileTimeResolver(), validationContext.getClasspathImportService(), validationContext.getStatementRawInfo());
                currentInputType = datetimeImpl.getReturnType();
                if (currentInputType == null) {
                    throw new IllegalStateException("Date-time method '" + chainElement.getName() + "' has not returned type information");
                }
                methodForges.add(datetimeImpl.getForge());
                filterAnalyzerDesc = datetimeImpl.getIntervalFilterDesc();
                continue;
            }

            // try to resolve as property if the last method returned a type
            if (currentInputType instanceof EventEPType) {
                EventTypeSPI inputEventType = (EventTypeSPI) ((EventEPType) currentInputType).getType();
                Class type = inputEventType.getPropertyType(chainElement.getName());
                EventPropertyGetterSPI getter = inputEventType.getGetterSPI(chainElement.getName());
                if (type != null && getter != null) {
                    ExprDotForgeProperty noduck = new ExprDotForgeProperty(getter, EPTypeHelper.singleValue(JavaClassHelper.getBoxedType(type)));
                    methodForges.add(noduck);
                    currentInputType = EPTypeHelper.singleValue(EPTypeHelper.getClassSingleValued(noduck.getTypeInfo()));
                    continue;
                }
            }

            // Finally try to resolve the method
            if (methodTarget != null) {
                try {
                    // find descriptor again, allow for duck typing
                    ExprNodeUtilMethodDesc desc = getValidateMethodDescriptor(methodTarget, chainElement.getName(), chainElement.getParameters(), validationContext);
                    paramForges = desc.getChildForges();
                    ExprDotForge forge;
                    if (currentInputType instanceof ClassEPType) {
                        // if followed by an enumeration method, convert array to collection
                        if (desc.getReflectionMethod().getReturnType().isArray() && !chainSpecStack.isEmpty() && EnumMethodResolver.isEnumerationMethod(chainSpecStack.getFirst().getName(), validationContext.getClasspathImportService())) {
                            forge = new ExprDotMethodForgeNoDuck(validationContext.getStatementName(), desc.getReflectionMethod(), paramForges, ExprDotMethodForgeNoDuck.Type.WRAPARRAY);
                        } else {
                            forge = new ExprDotMethodForgeNoDuck(validationContext.getStatementName(), desc.getReflectionMethod(), paramForges, ExprDotMethodForgeNoDuck.Type.PLAIN);
                        }
                    } else {
                        forge = new ExprDotMethodForgeNoDuck(validationContext.getStatementName(), desc.getReflectionMethod(), paramForges, ExprDotMethodForgeNoDuck.Type.UNDERLYING);
                    }
                    methodForges.add(forge);
                    currentInputType = forge.getTypeInfo();
                } catch (Exception e) {
                    if (!isDuckTyping) {
                        throw new ExprValidationException(e.getMessage(), e);
                    } else {
                        ExprDotMethodForgeDuck duck = new ExprDotMethodForgeDuck(validationContext.getStatementName(), validationContext.getClasspathImportService(), chainElement.getName(), paramTypes, paramForges);
                        methodForges.add(duck);
                        currentInputType = duck.getTypeInfo();
                    }
                }
                continue;
            }

            String message = "Could not find event property or method named '" +
                chainElement.getName() + "' in " + EPTypeHelper.toTypeDescriptive(currentInputType);
            throw new ExprValidationException(message);
        }

        ExprDotForge[] intermediateEvals = methodForges.toArray(new ExprDotForge[methodForges.size()]);

        if (lastLambdaFunc != null) {
            ExprDotForge finalEval = null;
            if (currentInputType instanceof EventMultiValuedEPType) {
                EventMultiValuedEPType mvType = (EventMultiValuedEPType) currentInputType;
                TableMetaData tableMetadata = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(mvType.getComponent());
                if (tableMetadata != null) {
                    finalEval = new ExprDotForgeUnpackCollEventBeanTable(mvType.getComponent(), tableMetadata);
                } else {
                    finalEval = new ExprDotForgeUnpackCollEventBean(mvType.getComponent());
                }
            } else if (currentInputType instanceof EventEPType) {
                EventEPType epType = (EventEPType) currentInputType;
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

    private static Class getMethodTarget(EPType currentInputType) {
        if (currentInputType instanceof ClassEPType) {
            return ((ClassEPType) currentInputType).getType();
        } else if (currentInputType instanceof EventEPType) {
            return ((EventEPType) currentInputType).getType().getUnderlyingType();
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

    public static CodegenExpression evaluateChainCodegen(CodegenMethod parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, CodegenExpression inner, Class innerType, ExprDotForge[] forges, ExprDotStaticMethodWrap optionalResultWrapLambda) {
        if (forges.length == 0) {
            return inner;
        }
        ExprDotForge last = forges[forges.length - 1];
        Class lastType = EPTypeHelper.getCodegenReturnType(last.getTypeInfo());
        CodegenMethod methodNode = parent.makeChild(lastType, ExprDotNodeUtility.class, codegenClassScope).addParam(innerType, "inner");

        CodegenBlock block = methodNode.getBlock();
        String currentTarget = "wrapped";
        Class currentTargetType;
        if (optionalResultWrapLambda != null) {
            currentTargetType = EPTypeHelper.getCodegenReturnType(optionalResultWrapLambda.getTypeInfo());
            if (lastType != void.class) {
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
                typeInformation = codegenClassScope.addOrGetFieldSharable(new EPTypeCodegenSharable(forges[i].getTypeInfo(), codegenClassScope));
            }

            Class reftype = EPTypeHelper.getCodegenReturnType(forges[i].getTypeInfo());
            if (reftype == void.class) {
                block.expression(forges[i].codegen(ref(currentTarget), currentTargetType, methodNode, exprSymbol, codegenClassScope))
                    .apply(instblock(codegenClassScope, "aExprDotChainElement", typeInformation, constantNull()));
            } else {
                block.declareVar(reftype, refname, forges[i].codegen(ref(currentTarget), currentTargetType, methodNode, exprSymbol, codegenClassScope));
                currentTarget = refname;
                currentTargetType = reftype;
                if (!reftype.isPrimitive()) {
                    CodegenBlock ifBlock = block.ifRefNull(refname)
                        .apply(instblock(codegenClassScope, "aExprDotChainElement", typeInformation, constantNull()));
                    if (lastType != void.class) {
                        ifBlock.blockReturn(constantNull());
                    } else {
                        ifBlock.blockEnd();
                    }
                }
                block.apply(instblock(codegenClassScope, "aExprDotChainElement", typeInformation, ref(refname)));
            }
        }
        if (lastType == void.class) {
            block.methodEnd();
        } else {
            block.methodReturn(ref(refname));
        }
        return localMethod(methodNode, inner);
    }

    private static ExprNodeUtilMethodDesc getValidateMethodDescriptor(Class methodTarget, final String methodName, List<ExprNode> parameters, ExprValidationContext validationContext)
        throws ExprValidationException {
        ExprNodeUtilResolveExceptionHandler exceptionHandler = new ExprNodeUtilResolveExceptionHandler() {
            public ExprValidationException handle(Exception e) {
                return new ExprValidationException("Failed to resolve method '" + methodName + "': " + e.getMessage(), e);
            }
        };
        EventType wildcardType = validationContext.getStreamTypeService().getEventTypes().length != 1 ? null : validationContext.getStreamTypeService().getEventTypes()[0];
        return ExprNodeUtilityResolve.resolveMethodAllowWildcardAndStream(methodTarget.getName(), methodTarget, methodName, parameters,
            wildcardType != null, wildcardType, exceptionHandler, methodName, validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
    }
}
