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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.datetime.eval.ExprDotDTFactory;
import com.espertech.esper.epl.datetime.eval.ExprDotDTMethodDesc;
import com.espertech.esper.epl.enummethod.dot.*;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;
import com.espertech.esper.epl.rettype.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.*;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastMethod;

import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class ExprDotNodeUtility {
    public static boolean isDatetimeOrEnumMethod(String name) {
        return EnumMethodEnum.isEnumerationMethod(name) || DatetimeMethodEnum.isDateTimeMethod(name);
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
        EnumMethodEnum lastLambdaFunc = null;
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

            // resolve lambda
            if (EnumMethodEnum.isEnumerationMethod(chainElement.getName()) && (!matchingMethod || methodTarget.isArray() || JavaClassHelper.isImplementsInterface(methodTarget, Collection.class))) {
                EnumMethodEnum enumerationMethod = EnumMethodEnum.fromName(chainElement.getName());
                ExprDotForgeEnumMethod eval = (ExprDotForgeEnumMethod) JavaClassHelper.instantiate(ExprDotForgeEnumMethod.class, enumerationMethod.getImplementation());
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
            if (DatetimeMethodEnum.isDateTimeMethod(chainElement.getName()) && (!matchingMethod || methodTarget == Calendar.class || methodTarget == Date.class)) {
                DatetimeMethodEnum datetimeMethod = DatetimeMethodEnum.fromName(chainElement.getName());
                ExprDotDTMethodDesc datetimeImpl = ExprDotDTFactory.validateMake(validationContext.getStreamTypeService(), chainSpecStack, datetimeMethod, chainElement.getName(), currentInputType, chainElement.getParameters(), inputDesc, validationContext.getEngineImportService().getTimeZone(), validationContext.getEngineImportService().getTimeAbacus(), validationContext.getExprEvaluatorContext());
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
                    FastMethod fastMethod = desc.getFastMethod();
                    paramForges = desc.getChildForges();

                    ExprDotForge forge;
                    if (currentInputType instanceof ClassEPType) {
                        // if followed by an enumeration method, convert array to collection
                        if (fastMethod.getReturnType().isArray() && !chainSpecStack.isEmpty() && EnumMethodEnum.isEnumerationMethod(chainSpecStack.getFirst().getName())) {
                            forge = new ExprDotMethodForgeNoDuck(validationContext.getStatementName(), fastMethod, paramForges, ExprDotMethodForgeNoDuck.Type.WRAPARRAY);
                        } else {
                            forge = new ExprDotMethodForgeNoDuck(validationContext.getStatementName(), fastMethod, paramForges, ExprDotMethodForgeNoDuck.Type.PLAIN);
                        }
                    } else {
                        forge = new ExprDotMethodForgeNoDuck(validationContext.getStatementName(), fastMethod, paramForges, ExprDotMethodForgeNoDuck.Type.UNDERLYING);
                    }
                    methodForges.add(forge);
                    currentInputType = forge.getTypeInfo();
                } catch (Exception e) {
                    if (!isDuckTyping) {
                        throw new ExprValidationException(e.getMessage(), e);
                    } else {
                        ExprDotMethodForgeDuck duck = new ExprDotMethodForgeDuck(validationContext.getStatementName(), validationContext.getEngineImportService(), chainElement.getName(), paramTypes, paramForges);
                        methodForges.add(duck);
                        currentInputType = duck.getTypeInfo();
                    }
                }
                continue;
            }

            String message = "Could not find event property, enumeration method or instance method named '" +
                    chainElement.getName() + "' in " + EPTypeHelper.toTypeDescriptive(currentInputType);
            throw new ExprValidationException(message);
        }

        ExprDotForge[] intermediateEvals = methodForges.toArray(new ExprDotForge[methodForges.size()]);

        if (lastLambdaFunc != null) {
            ExprDotForge finalEval = null;
            if (currentInputType instanceof EventMultiValuedEPType) {
                EventMultiValuedEPType mvType = (EventMultiValuedEPType) currentInputType;
                TableMetadata tableMetadata = validationContext.getTableService().getTableMetadataFromEventType(mvType.getComponent());
                if (tableMetadata != null) {
                    finalEval = new ExprDotForgeUnpackCollEventBeanTable(mvType.getComponent(), tableMetadata);
                } else {
                    finalEval = new ExprDotForgeUnpackCollEventBean(mvType.getComponent());
                }
            } else if (currentInputType instanceof EventEPType) {
                EventEPType epType = (EventEPType) currentInputType;
                TableMetadata tableMetadata = validationContext.getTableService().getTableMetadataFromEventType(epType.getType());
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

    public static ObjectArrayEventType makeTransientOAType(String enumMethod, String propertyName, Class type, EventAdapterService eventAdapterService) {
        Map<String, Object> propsResult = new HashMap<String, Object>();
        propsResult.put(propertyName, JavaClassHelper.getBoxedType(type));
        String typeName = enumMethod + "__" + propertyName;
        return new ObjectArrayEventType(EventTypeMetadata.createAnonymous(typeName, EventTypeMetadata.ApplicationType.OBJECTARR), typeName, 0, eventAdapterService, propsResult, null, null, null);
    }

    public static EventType[] getSingleLambdaParamEventType(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, EventAdapterService eventAdapterService) {
        if (inputEventType != null) {
            return new EventType[]{inputEventType};
        } else {
            return new EventType[]{ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, goesToNames.get(0), collectionComponentType, eventAdapterService)};
        }
    }

    public static Object evaluateChain(ExprDotForge[] forges, ExprDotEval[] evaluators, Object inner, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            int i = -1;
            for (ExprDotEval methodEval : evaluators) {
                i++;
                InstrumentationHelper.get().qExprDotChainElement(i, methodEval);
                inner = methodEval.evaluate(inner, eventsPerStream, isNewData, context);
                InstrumentationHelper.get().aExprDotChainElement(forges[i].getTypeInfo(), inner);
                if (inner == null) {
                    break;
                }
            }
            return inner;
        } else {
            for (ExprDotEval methodEval : evaluators) {
                inner = methodEval.evaluate(inner, eventsPerStream, isNewData, context);
                if (inner == null) {
                    break;
                }
            }
            return inner;
        }
    }

    public static CodegenExpression evaluateChainCodegen(CodegenMethodNode parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, CodegenExpression inner, Class innerType, ExprDotForge[] forges, ExprDotStaticMethodWrap optionalResultWrapLambda) {
        if (forges.length == 0) {
            return inner;
        }
        ExprDotForge last = forges[forges.length - 1];
        Class lastType = EPTypeHelper.getCodegenReturnType(last.getTypeInfo());
        CodegenMethodNode methodNode = parent.makeChild(lastType, ExprDotNodeUtility.class, codegenClassScope).addParam(innerType, "inner");

        CodegenBlock block = methodNode.getBlock();
        String currentTarget = "wrapped";
        Class currentTargetType;
        if (optionalResultWrapLambda != null) {
            currentTargetType = EPTypeHelper.getCodegenReturnType(optionalResultWrapLambda.getTypeInfo());
            block.ifRefNullReturnNull("inner")
                    .declareVar(currentTargetType, "wrapped", optionalResultWrapLambda.codegenConvertNonNull(ref("inner"), methodNode, codegenClassScope));
        } else {
            block.declareVar(innerType, "wrapped", ref("inner"));
            currentTargetType = innerType;
        }

        String refname = null;
        for (int i = 0; i < forges.length; i++) {
            refname = "r" + i;
            Class reftype = EPTypeHelper.getCodegenReturnType(forges[i].getTypeInfo());
            if (reftype == void.class) {
                block.expression(forges[i].codegen(ref(currentTarget), currentTargetType, methodNode, exprSymbol, codegenClassScope));
            } else {
                block.declareVar(reftype, refname, forges[i].codegen(ref(currentTarget), currentTargetType, methodNode, exprSymbol, codegenClassScope));
                currentTarget = refname;
                currentTargetType = reftype;
                if (!reftype.isPrimitive()) {
                    block.ifRefNullReturnNull(refname);
                }
            }
        }
        if (lastType == void.class) {
            block.methodEnd();
        } else {
            block.methodReturn(ref(refname));
        }
        return localMethod(methodNode, inner);
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

        if (InstrumentationHelper.ENABLED) {
            EPType typeInfo;
            if (resultWrapLambda != null) {
                typeInfo = resultWrapLambda.getTypeInfo();
            } else {
                if (optionalResultSingleEventType != null) {
                    typeInfo = EPTypeHelper.singleEvent(optionalResultSingleEventType);
                } else {
                    typeInfo = EPTypeHelper.singleValue(resultType);
                }
            }
            InstrumentationHelper.get().qExprDotChain(typeInfo, result, chainEval);

            int i = -1;
            for (ExprDotEval aChainEval : chainEval) {
                i++;
                InstrumentationHelper.get().qExprDotChainElement(i, aChainEval);
                result = aChainEval.evaluate(result, eventsPerStream, newData, exprEvaluatorContext);
                InstrumentationHelper.get().aExprDotChainElement(chainForges[i].getTypeInfo(), result);
                if (result == null) {
                    break;
                }
            }

            InstrumentationHelper.get().aExprDotChain();
            return result;
        }

        for (ExprDotEval aChainEval : chainEval) {
            result = aChainEval.evaluate(result, eventsPerStream, newData, exprEvaluatorContext);
            if (result == null) {
                return result;
            }
        }
        return result;
    }

    public static ExprDotEnumerationSourceForge getEnumerationSource(ExprNode inputExpression, StreamTypeService streamTypeService, EventAdapterService eventAdapterService, int statementId, boolean hasEnumerationMethod, boolean disablePropertyExpressionEventCollCache) throws ExprValidationException {
        ExprForge rootNodeForge = inputExpression.getForge();
        ExprEnumerationForge rootLambdaForge = null;
        EPType info = null;

        if (rootNodeForge instanceof ExprEnumerationForge) {
            rootLambdaForge = (ExprEnumerationForge) rootNodeForge;

            if (rootLambdaForge.getEventTypeCollection(eventAdapterService, statementId) != null) {
                info = EPTypeHelper.collectionOfEvents(rootLambdaForge.getEventTypeCollection(eventAdapterService, statementId));
            } else if (rootLambdaForge.getEventTypeSingle(eventAdapterService, statementId) != null) {
                info = EPTypeHelper.singleEvent(rootLambdaForge.getEventTypeSingle(eventAdapterService, statementId));
            } else if (rootLambdaForge.getComponentTypeCollection() != null) {
                info = EPTypeHelper.collectionOfSingleValue(rootLambdaForge.getComponentTypeCollection());
            } else {
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
        ExprEnumerationGivenEvent enumEvaluatorGivenEvent = (ExprEnumerationGivenEvent) enumEvaluator;
        return new ExprDotEnumerationSourceForgeForProps(enumEvaluator, typeInfo, streamId, enumEvaluatorGivenEvent);
    }

    public static ExprDotEval[] getEvaluators(ExprDotForge[] forges) {
        ExprDotEval[] evals = new ExprDotEval[forges.length];
        for (int i = 0; i < forges.length; i++) {
            evals[i] = forges[i].getDotEvaluator();
        }
        return evals;
    }

    private static ExprNodeUtilMethodDesc getValidateMethodDescriptor(Class methodTarget, final String methodName, List<ExprNode> parameters, ExprValidationContext validationContext)
            throws ExprValidationException {
        ExprNodeUtilResolveExceptionHandler exceptionHandler = new ExprNodeUtilResolveExceptionHandler() {
            public ExprValidationException handle(Exception e) {
                return new ExprValidationException("Failed to resolve method '" + methodName + "': " + e.getMessage(), e);
            }
        };
        EventType wildcardType = validationContext.getStreamTypeService().getEventTypes().length != 1 ? null : validationContext.getStreamTypeService().getEventTypes()[0];
        return ExprNodeUtilityRich.resolveMethodAllowWildcardAndStream(methodTarget.getName(), methodTarget, methodName, parameters, validationContext.getEngineImportService(), validationContext.getEventAdapterService(), validationContext.getStatementId(), wildcardType != null, wildcardType, exceptionHandler, methodName, validationContext.getTableService(), validationContext.getStreamTypeService().getEngineURIQualifier());
    }
}
