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

import com.espertech.esper.client.*;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.datetime.eval.ExprDotEvalDTFactory;
import com.espertech.esper.epl.datetime.eval.ExprDotEvalDTMethodDesc;
import com.espertech.esper.epl.datetime.eval.ExprDotNodeFilterAnalyzerDesc;
import com.espertech.esper.epl.enummethod.dot.*;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.rettype.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastMethod;

import java.util.*;

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
        List<ExprDotEval> methodEvals = new ArrayList<ExprDotEval>();
        EPType currentInputType = inputType;
        EnumMethodEnum lastLambdaFunc = null;
        ExprChainedSpec lastElement = chainSpec.isEmpty() ? null : chainSpec.get(chainSpec.size() - 1);
        ExprDotNodeFilterAnalyzerDesc filterAnalyzerDesc = null;

        Deque<ExprChainedSpec> chainSpecStack = new ArrayDeque<ExprChainedSpec>(chainSpec);
        while (!chainSpecStack.isEmpty()) {
            ExprChainedSpec chainElement = chainSpecStack.removeFirst();
            lastLambdaFunc = null;  // reset

            // compile parameters for chain element
            ExprEvaluator[] paramEvals = new ExprEvaluator[chainElement.getParameters().size()];
            Class[] paramTypes = new Class[chainElement.getParameters().size()];
            for (int i = 0; i < chainElement.getParameters().size(); i++) {
                paramEvals[i] = chainElement.getParameters().get(i).getExprEvaluator();
                paramTypes[i] = paramEvals[i].getType();
            }

            // check if special 'size' method
            if (currentInputType instanceof ClassMultiValuedEPType) {
                ClassMultiValuedEPType type = (ClassMultiValuedEPType) currentInputType;
                if (chainElement.getName().toLowerCase(Locale.ENGLISH).equals("size") && paramTypes.length == 0 && lastElement == chainElement) {
                    ExprDotEvalArraySize sizeExpr = new ExprDotEvalArraySize();
                    methodEvals.add(sizeExpr);
                    currentInputType = sizeExpr.getTypeInfo();
                    continue;
                }
                if (chainElement.getName().toLowerCase(Locale.ENGLISH).equals("get") && paramTypes.length == 1 && JavaClassHelper.getBoxedType(paramTypes[0]) == Integer.class) {
                    Class componentType = type.getComponent();
                    ExprDotEvalArrayGet get = new ExprDotEvalArrayGet(paramEvals[0], componentType);
                    methodEvals.add(get);
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
                ExprDotEvalEnumMethod eval = (ExprDotEvalEnumMethod) JavaClassHelper.instantiate(ExprDotEvalEnumMethod.class, enumerationMethod.getImplementation());
                eval.init(streamOfProviderIfApplicable, enumerationMethod, chainElement.getName(), currentInputType, chainElement.getParameters(), validationContext);
                currentInputType = eval.getTypeInfo();
                if (currentInputType == null) {
                    throw new IllegalStateException("Enumeration method '" + chainElement.getName() + "' has not returned type information");
                }
                methodEvals.add(eval);
                lastLambdaFunc = enumerationMethod;
                continue;
            }

            // resolve datetime
            if (DatetimeMethodEnum.isDateTimeMethod(chainElement.getName()) && (!matchingMethod || methodTarget == Calendar.class || methodTarget == Date.class)) {
                DatetimeMethodEnum datetimeMethod = DatetimeMethodEnum.fromName(chainElement.getName());
                ExprDotEvalDTMethodDesc datetimeImpl = ExprDotEvalDTFactory.validateMake(validationContext.getStreamTypeService(), chainSpecStack, datetimeMethod, chainElement.getName(), currentInputType, chainElement.getParameters(), inputDesc, validationContext.getEngineImportService().getTimeZone(), validationContext.getEngineImportService().getTimeAbacus(), validationContext.getExprEvaluatorContext());
                currentInputType = datetimeImpl.getReturnType();
                if (currentInputType == null) {
                    throw new IllegalStateException("Date-time method '" + chainElement.getName() + "' has not returned type information");
                }
                methodEvals.add(datetimeImpl.getEval());
                filterAnalyzerDesc = datetimeImpl.getIntervalFilterDesc();
                continue;
            }

            // try to resolve as property if the last method returned a type
            if (currentInputType instanceof EventEPType) {
                EventType inputEventType = ((EventEPType) currentInputType).getType();
                Class type = inputEventType.getPropertyType(chainElement.getName());
                EventPropertyGetter getter = inputEventType.getGetter(chainElement.getName());
                if (type != null && getter != null) {
                    ExprDotEvalProperty noduck = new ExprDotEvalProperty(getter, EPTypeHelper.singleValue(JavaClassHelper.getBoxedType(type)));
                    methodEvals.add(noduck);
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
                    paramEvals = desc.getChildEvals();

                    ExprDotEval eval;
                    if (currentInputType instanceof ClassEPType) {
                        // if followed by an enumeration method, convert array to collection
                        if (fastMethod.getReturnType().isArray() && !chainSpecStack.isEmpty() && EnumMethodEnum.isEnumerationMethod(chainSpecStack.getFirst().getName())) {
                            eval = new ExprDotMethodEvalNoDuckWrapArray(validationContext.getStatementName(), fastMethod, paramEvals);
                        } else {
                            eval = new ExprDotMethodEvalNoDuck(validationContext.getStatementName(), fastMethod, paramEvals);
                        }
                    } else {
                        eval = new ExprDotMethodEvalNoDuckUnderlying(validationContext.getStatementName(), fastMethod, paramEvals);
                    }
                    methodEvals.add(eval);
                    currentInputType = eval.getTypeInfo();
                } catch (Exception e) {
                    if (!isDuckTyping) {
                        throw new ExprValidationException(e.getMessage(), e);
                    } else {
                        ExprDotMethodEvalDuck duck = new ExprDotMethodEvalDuck(validationContext.getStatementName(), validationContext.getEngineImportService(), chainElement.getName(), paramTypes, paramEvals);
                        methodEvals.add(duck);
                        currentInputType = duck.getTypeInfo();
                    }
                }
                continue;
            }

            String message = "Could not find event property, enumeration method or instance method named '" +
                    chainElement.getName() + "' in " + EPTypeHelper.toTypeDescriptive(currentInputType);
            throw new ExprValidationException(message);
        }

        ExprDotEval[] intermediateEvals = methodEvals.toArray(new ExprDotEval[methodEvals.size()]);

        if (lastLambdaFunc != null) {
            ExprDotEval finalEval = null;
            if (currentInputType instanceof EventMultiValuedEPType) {
                EventMultiValuedEPType mvType = (EventMultiValuedEPType) currentInputType;
                TableMetadata tableMetadata = validationContext.getTableService().getTableMetadataFromEventType(mvType.getComponent());
                if (tableMetadata != null) {
                    finalEval = new ExprDotEvalUnpackCollEventBeanTable(mvType.getComponent(), tableMetadata);
                } else {
                    finalEval = new ExprDotEvalUnpackCollEventBean(mvType.getComponent());
                }
            } else if (currentInputType instanceof EventEPType) {
                EventEPType epType = (EventEPType) currentInputType;
                TableMetadata tableMetadata = validationContext.getTableService().getTableMetadataFromEventType(epType.getType());
                if (tableMetadata != null) {
                    finalEval = new ExprDotEvalUnpackBeanTable(epType.getType(), tableMetadata);
                } else {
                    finalEval = new ExprDotEvalUnpackBean(epType.getType());
                }
            }
            if (finalEval != null) {
                methodEvals.add(finalEval);
            }
        }

        ExprDotEval[] unpackingEvals = methodEvals.toArray(new ExprDotEval[methodEvals.size()]);
        return new ExprDotNodeRealizedChain(intermediateEvals, unpackingEvals, filterAnalyzerDesc);
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
        propsResult.put(propertyName, type);
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

    public static Object evaluateChain(ExprDotEval[] evaluators, Object inner, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            int i = -1;
            for (ExprDotEval methodEval : evaluators) {
                i++;
                InstrumentationHelper.get().qExprDotChainElement(i, methodEval);
                inner = methodEval.evaluate(inner, eventsPerStream, isNewData, context);
                InstrumentationHelper.get().aExprDotChainElement(methodEval.getTypeInfo(), inner);
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

    public static Object evaluateChainWithWrap(ExprDotStaticMethodWrap resultWrapLambda,
                                               Object result,
                                               EventType optionalResultSingleEventType,
                                               Class resultType,
                                               ExprDotEval[] chainEval,
                                               EventBean[] eventsPerStream,
                                               boolean newData,
                                               ExprEvaluatorContext exprEvaluatorContext) {
        if (result == null) {
            return null;
        }

        if (resultWrapLambda != null) {
            result = resultWrapLambda.convert(result);
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
                InstrumentationHelper.get().aExprDotChainElement(aChainEval.getTypeInfo(), result);
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

    public static ExprDotEnumerationSource getEnumerationSource(ExprNode inputExpression, StreamTypeService streamTypeService, EventAdapterService eventAdapterService, int statementId, boolean hasEnumerationMethod, boolean disablePropertyExpressionEventCollCache) throws ExprValidationException {
        ExprEvaluator rootNodeEvaluator = inputExpression.getExprEvaluator();
        ExprEvaluatorEnumeration rootLambdaEvaluator = null;
        EPType info = null;

        if (rootNodeEvaluator instanceof ExprEvaluatorEnumeration) {
            rootLambdaEvaluator = (ExprEvaluatorEnumeration) rootNodeEvaluator;

            if (rootLambdaEvaluator.getEventTypeCollection(eventAdapterService, statementId) != null) {
                info = EPTypeHelper.collectionOfEvents(rootLambdaEvaluator.getEventTypeCollection(eventAdapterService, statementId));
            } else if (rootLambdaEvaluator.getEventTypeSingle(eventAdapterService, statementId) != null) {
                info = EPTypeHelper.singleEvent(rootLambdaEvaluator.getEventTypeSingle(eventAdapterService, statementId));
            } else if (rootLambdaEvaluator.getComponentTypeCollection() != null) {
                info = EPTypeHelper.collectionOfSingleValue(rootLambdaEvaluator.getComponentTypeCollection());
            } else {
                rootLambdaEvaluator = null; // not a lambda evaluator
            }
        } else if (inputExpression instanceof ExprIdentNode) {
            ExprIdentNode identNode = (ExprIdentNode) inputExpression;
            int streamId = identNode.getStreamId();
            EventType streamType = streamTypeService.getEventTypes()[streamId];
            return getPropertyEnumerationSource(identNode.getResolvedPropertyName(), streamId, streamType, hasEnumerationMethod, disablePropertyExpressionEventCollCache);
        }
        return new ExprDotEnumerationSource(info, null, rootLambdaEvaluator);
    }

    public static ExprDotEnumerationSourceForProps getPropertyEnumerationSource(String propertyName, int streamId, EventType streamType, boolean allowEnumType, boolean disablePropertyExpressionEventCollCache) {

        Class propertyType = streamType.getPropertyType(propertyName);
        EPType typeInfo = EPTypeHelper.singleValue(propertyType);  // assume scalar for now

        // no enumeration methods, no need to expose as an enumeration
        if (!allowEnumType) {
            return new ExprDotEnumerationSourceForProps(null, typeInfo, streamId, null);
        }

        FragmentEventType fragmentEventType = streamType.getFragmentType(propertyName);
        EventPropertyGetter getter = streamType.getGetter(propertyName);

        ExprEvaluatorEnumeration enumEvaluator = null;
        if (getter != null && fragmentEventType != null) {
            if (fragmentEventType.isIndexed()) {
                enumEvaluator = new PropertyExprEvaluatorEventCollection(propertyName, streamId, fragmentEventType.getFragmentType(), getter, disablePropertyExpressionEventCollCache);
                typeInfo = EPTypeHelper.collectionOfEvents(fragmentEventType.getFragmentType());
            } else {   // we don't want native to require an eventbean instance
                enumEvaluator = new PropertyExprEvaluatorEventSingle(streamId, fragmentEventType.getFragmentType(), getter);
                typeInfo = EPTypeHelper.singleEvent(fragmentEventType.getFragmentType());
            }
        } else {
            EventPropertyDescriptor desc = EventTypeUtility.getNestablePropertyDescriptor(streamType, propertyName);
            if (desc != null && desc.isIndexed() && !desc.isRequiresIndex() && desc.getPropertyComponentType() != null) {
                if (JavaClassHelper.isImplementsInterface(propertyType, Collection.class)) {
                    enumEvaluator = new PropertyExprEvaluatorScalarCollection(propertyName, streamId, getter, desc.getPropertyComponentType());
                } else if (JavaClassHelper.isImplementsInterface(propertyType, Iterable.class)) {
                    enumEvaluator = new PropertyExprEvaluatorScalarIterable(propertyName, streamId, getter, desc.getPropertyComponentType());
                } else if (propertyType.isArray()) {
                    enumEvaluator = new PropertyExprEvaluatorScalarArray(propertyName, streamId, getter, desc.getPropertyComponentType());
                } else {
                    throw new IllegalStateException("Property indicated indexed-type but failed to find proper collection adapter for use with enumeration methods");
                }
                typeInfo = EPTypeHelper.collectionOfSingleValue(desc.getPropertyComponentType());
            }
        }
        ExprEvaluatorEnumerationGivenEvent enumEvaluatorGivenEvent = (ExprEvaluatorEnumerationGivenEvent) enumEvaluator;
        return new ExprDotEnumerationSourceForProps(enumEvaluator, typeInfo, streamId, enumEvaluatorGivenEvent);
    }

    private static ExprNodeUtilMethodDesc getValidateMethodDescriptor(Class methodTarget, final String methodName, List<ExprNode> parameters, ExprValidationContext validationContext)
            throws ExprValidationException {
        ExprNodeUtilResolveExceptionHandler exceptionHandler = new ExprNodeUtilResolveExceptionHandler() {
            public ExprValidationException handle(Exception e) {
                return new ExprValidationException("Failed to resolve method '" + methodName + "': " + e.getMessage(), e);
            }
        };
        EventType wildcardType = validationContext.getStreamTypeService().getEventTypes().length != 1 ? null : validationContext.getStreamTypeService().getEventTypes()[0];
        return ExprNodeUtility.resolveMethodAllowWildcardAndStream(methodTarget.getName(), methodTarget, methodName, parameters, validationContext.getEngineImportService(), validationContext.getEventAdapterService(), validationContext.getStatementId(), wildcardType != null, wildcardType, exceptionHandler, methodName, validationContext.getTableService(), validationContext.getStreamTypeService().getEngineURIQualifier());
    }
}
