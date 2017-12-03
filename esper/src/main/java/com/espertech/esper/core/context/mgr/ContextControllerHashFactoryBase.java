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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.context.ContextPartitionIdentifier;
import com.espertech.esper.client.context.ContextPartitionIdentifierHash;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.stmt.*;
import com.espertech.esper.epl.core.engineimport.EngineImportSingleRowDesc;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.ContextDetail;
import com.espertech.esper.epl.spec.ContextDetailHash;
import com.espertech.esper.epl.spec.ContextDetailHashItem;
import com.espertech.esper.epl.spec.ContextDetailPartitionItem;
import com.espertech.esper.epl.util.StatementSpecCompiledAnalyzer;
import com.espertech.esper.epl.util.StatementSpecCompiledAnalyzerResult;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.filterspec.*;

import java.io.StringWriter;
import java.util.*;

public abstract class ContextControllerHashFactoryBase extends ContextControllerFactoryBase implements ContextControllerFactory {

    private final ContextDetailHash hashedSpec;
    private final List<FilterSpecCompiled> filtersSpecsNestedContexts;
    private Map<String, Object> contextBuiltinProps;
    private Map<EventType, ExprFilterSpecLookupable> nonPropertyExpressions = new HashMap<EventType, ExprFilterSpecLookupable>();

    public ContextControllerHashFactoryBase(ContextControllerFactoryContext factoryContext, ContextDetailHash hashedSpec, List<FilterSpecCompiled> filtersSpecsNestedContexts) {
        super(factoryContext);
        this.hashedSpec = hashedSpec;
        this.filtersSpecsNestedContexts = filtersSpecsNestedContexts;
    }

    public boolean hasFiltersSpecsNestedContexts() {
        return filtersSpecsNestedContexts != null && !filtersSpecsNestedContexts.isEmpty();
    }

    public void validateFactory() throws ExprValidationException {
        validatePopulateContextDesc();
        contextBuiltinProps = ContextPropertyEventType.getHashType();
    }

    public ContextControllerStatementCtxCache validateStatement(ContextControllerStatementBase statement) throws ExprValidationException {
        StatementSpecCompiledAnalyzerResult streamAnalysis = StatementSpecCompiledAnalyzer.analyzeFilters(statement.getStatementSpec());
        ContextControllerPartitionedUtil.validateStatementForContext(factoryContext.getContextName(), statement, streamAnalysis, getItemEventTypes(hashedSpec), factoryContext.getServicesContext().getNamedWindowMgmtService());
        // register non-property expression to be able to recreated indexes
        for (Map.Entry<EventType, ExprFilterSpecLookupable> entry : nonPropertyExpressions.entrySet()) {
            factoryContext.getServicesContext().getFilterNonPropertyRegisteryService().registerNonPropertyExpression(statement.getStatementContext().getStatementName(), entry.getKey(), entry.getValue());
        }
        return new ContextControllerStatementCtxCacheFilters(streamAnalysis.getFilters());
    }

    public void populateFilterAddendums(IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> filterAddendum, ContextControllerStatementDesc statement, Object key, int contextId) {
        ContextControllerStatementCtxCacheFilters statementInfo = (ContextControllerStatementCtxCacheFilters) statement.getCaches()[factoryContext.getNestingLevel() - 1];
        int assignedContextPartition = (Integer) key;
        int code = assignedContextPartition % hashedSpec.getGranularity();
        getAddendumFilters(filterAddendum, code, statementInfo.getFilterSpecs(), hashedSpec, statement);
    }

    public void populateContextInternalFilterAddendums(ContextInternalFilterAddendum filterAddendum, Object key) {
        int assignedContextPartition = (Integer) key;
        int code = assignedContextPartition % hashedSpec.getGranularity();
        getAddendumFilters(filterAddendum.getFilterAddendum(), code, filtersSpecsNestedContexts, hashedSpec, null);
    }

    public ExprFilterSpecLookupable getFilterLookupable(EventType eventType) {
        for (ContextDetailHashItem hashItem : hashedSpec.getItems()) {
            if (hashItem.getFilterSpecCompiled().getFilterForEventType() == eventType) {
                return hashItem.getLookupable();
            }
        }
        return null;
    }

    public boolean isSingleInstanceContext() {
        return false;
    }

    public StatementAIResourceRegistryFactory getStatementAIResourceRegistryFactory() {
        if (hashedSpec.getGranularity() <= 65536) {
            return new StatementAIResourceRegistryFactory() {
                public StatementAIResourceRegistry make() {
                    return new StatementAIResourceRegistry(new AIRegistryAggregationMultiPerm(), new AIRegistryExprMultiPerm());
                }
            };
        } else {
            return new StatementAIResourceRegistryFactory() {
                public StatementAIResourceRegistry make() {
                    return new StatementAIResourceRegistry(new AIRegistryAggregationMap(), new AIRegistryExprMap());
                }
            };
        }
    }

    public List<ContextDetailPartitionItem> getContextDetailPartitionItems() {
        return Collections.emptyList();
    }

    public ContextDetail getContextDetail() {
        return hashedSpec;
    }

    public ContextDetailHash getHashedSpec() {
        return hashedSpec;
    }

    public Map<String, Object> getContextBuiltinProps() {
        return contextBuiltinProps;
    }

    public ContextPartitionIdentifier keyPayloadToIdentifier(Object payload) {
        return new ContextPartitionIdentifierHash((Integer) payload);
    }

    private Collection<EventType> getItemEventTypes(ContextDetailHash hashedSpec) {
        List<EventType> itemEventTypes = new ArrayList<EventType>();
        for (ContextDetailHashItem item : hashedSpec.getItems()) {
            itemEventTypes.add(item.getFilterSpecCompiled().getFilterForEventType());
        }
        return itemEventTypes;
    }

    private void validatePopulateContextDesc() throws ExprValidationException {

        if (hashedSpec.getItems().isEmpty()) {
            throw new ExprValidationException("Empty list of hash items");
        }

        for (ContextDetailHashItem item : hashedSpec.getItems()) {
            if (item.getFunction().getParameters().isEmpty()) {
                throw new ExprValidationException("For context '" + factoryContext.getContextName() + "' expected one or more parameters to the hash function, but found no parameter list");
            }

            // determine type of hash to use
            String hashFuncName = item.getFunction().getName();
            HashFunctionEnum hashFunction = HashFunctionEnum.determine(factoryContext.getContextName(), hashFuncName);
            Pair<Class, EngineImportSingleRowDesc> hashSingleRowFunction = null;
            if (hashFunction == null) {
                try {
                    hashSingleRowFunction = factoryContext.getAgentInstanceContextCreate().getStatementContext().getEngineImportService().resolveSingleRow(hashFuncName);
                } catch (Exception e) {
                    // expected
                }

                if (hashSingleRowFunction == null) {
                    throw new ExprValidationException("For context '" + factoryContext.getContextName() + "' expected a hash function that is any of {" + HashFunctionEnum.getStringList() +
                            "} or a plug-in single-row function or script but received '" + hashFuncName + "'");
                }
            }

            // get first parameter
            ExprNode paramExpr = item.getFunction().getParameters().get(0);
            ExprEvaluator eval = ExprNodeCompiler.allocateEvaluator(paramExpr.getForge(), factoryContext.getServicesContext().getEngineImportService(), ContextControllerHashFactoryBase.class, false, factoryContext.getAgentInstanceContextCreate().getStatementName());
            Class paramType = paramExpr.getForge().getEvaluationType();
            EventPropertyGetter getter;

            if (hashFunction == HashFunctionEnum.CONSISTENT_HASH_CRC32) {
                if (item.getFunction().getParameters().size() > 1 || paramType != String.class) {
                    getter = new ContextControllerHashedGetterCRC32Serialized(factoryContext.getAgentInstanceContextCreate().getStatementContext().getStatementName(), item.getFunction().getParameters(), hashedSpec.getGranularity(), factoryContext.getServicesContext().getEngineImportService());
                } else {
                    getter = new ContextControllerHashedGetterCRC32Single(eval, hashedSpec.getGranularity());
                }
            } else if (hashFunction == HashFunctionEnum.HASH_CODE) {
                if (item.getFunction().getParameters().size() > 1) {
                    getter = new ContextControllerHashedGetterHashMultiple(item.getFunction().getParameters(), hashedSpec.getGranularity(), factoryContext.getServicesContext().getEngineImportService(), factoryContext.getAgentInstanceContextCreate().getStatementName());
                } else {
                    getter = new ContextControllerHashedGetterHashSingle(eval, hashedSpec.getGranularity());
                }
            } else if (hashSingleRowFunction != null) {
                getter = new ContextControllerHashedGetterSingleRow(factoryContext.getAgentInstanceContextCreate().getStatementContext().getStatementName(), hashFuncName, hashSingleRowFunction, item.getFunction().getParameters(), hashedSpec.getGranularity(),
                        factoryContext.getAgentInstanceContextCreate().getStatementContext().getEngineImportService(),
                        item.getFilterSpecCompiled().getFilterForEventType(),
                        factoryContext.getAgentInstanceContextCreate().getStatementContext().getEventAdapterService(),
                        factoryContext.getAgentInstanceContextCreate().getStatementId(),
                        factoryContext.getServicesContext().getTableService(),
                        factoryContext.getServicesContext().getEngineURI());
            } else {
                throw new IllegalArgumentException("Unrecognized hash code function '" + hashFuncName + "'");
            }

            // create and register expression
            String expression = item.getFunction().getName() + "(" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(paramExpr) + ")";
            ExprFilterSpecLookupable lookupable = new ExprFilterSpecLookupable(expression, getter, Integer.class, true);
            item.setLookupable(lookupable);
            factoryContext.getServicesContext().getFilterNonPropertyRegisteryService().registerNonPropertyExpression(factoryContext.getAgentInstanceContextCreate().getStatementName(), item.getFilterSpecCompiled().getFilterForEventType(), lookupable);
            nonPropertyExpressions.put(item.getFilterSpecCompiled().getFilterForEventType(), lookupable);
        }
    }

    // Compare filters in statement with filters in segmented context, addendum filter compilation
    public static void getAddendumFilters(IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> addendums, int hashCode, List<FilterSpecCompiled> filtersSpecs, ContextDetailHash hashSpec, ContextControllerStatementDesc statementDesc) {
        for (FilterSpecCompiled filtersSpec : filtersSpecs) {
            FilterValueSetParam[][] addendum = getAddendumFilters(filtersSpec, hashCode, hashSpec, statementDesc);
            if (addendum == null) {
                continue;
            }

            FilterValueSetParam[][] existing = addendums.get(filtersSpec);
            if (existing != null) {
                addendum = FilterAddendumUtil.multiplyAddendum(existing, addendum);
            }
            addendums.put(filtersSpec, addendum);
        }
    }

    public static FilterValueSetParam[][] getAddendumFilters(FilterSpecCompiled filterSpecCompiled, int hashCode, ContextDetailHash hashSpec, ContextControllerStatementDesc statementDesc) {

        // determine whether create-named-window
        boolean isCreateWindow = statementDesc != null && statementDesc.getStatement().getStatementSpec().getCreateWindowDesc() != null;
        ContextDetailHashItem foundPartition = null;

        if (!isCreateWindow) {
            foundPartition = findHashItemSpec(hashSpec, filterSpecCompiled);
        } else {
            String declaredAsName = statementDesc.getStatement().getStatementSpec().getCreateWindowDesc().getAsEventTypeName();
            for (ContextDetailHashItem partitionItem : hashSpec.getItems()) {
                if (partitionItem.getFilterSpecCompiled().getFilterForEventType().getName().equals(declaredAsName)) {
                    foundPartition = partitionItem;
                    break;
                }
            }
        }

        if (foundPartition == null) {
            return null;
        }

        FilterValueSetParam filter = new FilterValueSetParamImpl(foundPartition.getLookupable(), FilterOperator.EQUAL, hashCode);

        FilterValueSetParam[][] addendum = new FilterValueSetParam[1][];
        addendum[0] = new FilterValueSetParam[]{filter};

        FilterValueSetParam[][] partitionFilters = foundPartition.getParametersCompiled();
        if (partitionFilters != null) {
            addendum = FilterAddendumUtil.addAddendum(partitionFilters, filter);
        }
        return addendum;
    }

    public static ContextDetailHashItem findHashItemSpec(ContextDetailHash hashSpec, FilterSpecCompiled filterSpec) {
        ContextDetailHashItem foundPartition = null;
        for (ContextDetailHashItem partitionItem : hashSpec.getItems()) {
            boolean typeOrSubtype = EventTypeUtility.isTypeOrSubTypeOf(filterSpec.getFilterForEventType(), partitionItem.getFilterSpecCompiled().getFilterForEventType());
            if (typeOrSubtype) {
                foundPartition = partitionItem;
            }
        }

        return foundPartition;
    }

    public static enum HashFunctionEnum {
        CONSISTENT_HASH_CRC32,
        HASH_CODE;
        private static String stringList;

        public static HashFunctionEnum determine(String contextName, String name) throws ExprValidationException {
            String nameTrim = name.toLowerCase(Locale.ENGLISH).trim();
            for (HashFunctionEnum val : HashFunctionEnum.values()) {
                if (val.name().toLowerCase(Locale.ENGLISH).trim().equals(nameTrim)) {
                    return val;
                }
            }

            return null;
        }

        public static String getStringList() {
            StringWriter message = new StringWriter();
            String delimiter = "";
            for (HashFunctionEnum val : HashFunctionEnum.values()) {
                message.append(delimiter);
                message.append(val.name().toLowerCase(Locale.ENGLISH).trim());
                delimiter = ", ";
            }
            return message.toString();
        }
    }
}
