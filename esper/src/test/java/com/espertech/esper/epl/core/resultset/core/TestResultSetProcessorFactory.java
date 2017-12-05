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
package com.espertech.esper.epl.core.resultset.core;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.core.context.mgr.ContextPropertyRegistryImpl;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.epl.SupportSelectExprFactory;
import com.espertech.esper.supportunit.epl.SupportStreamTypeSvc1Stream;
import com.espertech.esper.supportunit.epl.SupportStreamTypeSvc3Stream;
import com.espertech.esper.util.CollectionUtil;
import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

public class TestResultSetProcessorFactory extends TestCase {
    private StreamTypeService typeService1Stream;
    private StreamTypeService typeService3Stream;
    private List<ExprNode> groupByList;
    private List<OrderByItem> orderByList;
    private AgentInstanceContext agentInstanceContext;
    private StatementContext stmtContext;

    public void setUp() {
        typeService1Stream = new SupportStreamTypeSvc1Stream();
        typeService3Stream = new SupportStreamTypeSvc3Stream();
        groupByList = new LinkedList<ExprNode>();
        orderByList = new LinkedList<OrderByItem>();
        agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        stmtContext = agentInstanceContext.getStatementContext();
    }

    public void testGetProcessorNoProcessorRequired() throws Exception {
        // single stream, empty group-by and wildcard select, no having clause, no need for any output processing
        SelectClauseElementCompiled[] wildcardSelect = new SelectClauseElementCompiled[]{new SelectClauseElementWildcard()};
        StatementSpecCompiled spec = makeSpec(new SelectClauseSpecCompiled(wildcardSelect, false), null, groupByList, null, null, orderByList);
        ResultSetProcessorFactoryDesc processor = ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService1Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
        assertEquals(ResultSetProcessorType.HANDTHROUGH, processor.getResultSetProcessorType());
    }

    public void testGetProcessorSimpleSelect() throws Exception {
        // empty group-by and no event properties aggregated in select clause (wildcard), no having clause
        SelectClauseElementCompiled[] wildcardSelect = new SelectClauseElementCompiled[]{new SelectClauseElementWildcard()};
        StatementSpecCompiled spec = makeSpec(new SelectClauseSpecCompiled(wildcardSelect, false), null, groupByList, null, null, orderByList);
        ResultSetProcessorFactoryDesc processor = ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService3Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
        assertEquals(ResultSetProcessorType.HANDTHROUGH, processor.getResultSetProcessorType());

        // empty group-by with select clause elements
        SelectClauseElementCompiled[] selectList = SupportSelectExprFactory.makeNoAggregateSelectListUnnamed();
        spec = makeSpec(new SelectClauseSpecCompiled(selectList, false), null, groupByList, null, null, orderByList);
        processor = ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService1Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
        assertEquals(ResultSetProcessorType.HANDTHROUGH, processor.getResultSetProcessorType());

        // non-empty group-by and wildcard select, group by ignored
        groupByList.add(SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0"));
        spec = makeSpec(new SelectClauseSpecCompiled(wildcardSelect, false), null, groupByList, null, null, orderByList);
        processor = ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService1Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
        assertEquals(ResultSetProcessorType.UNAGGREGATED_UNGROUPED, processor.getResultSetProcessorType());
    }

    public void testGetProcessorAggregatingAll() throws Exception {
        // empty group-by but aggragating event properties in select clause (output per event), no having clause
        // and one or more properties in the select clause is not aggregated
        SelectClauseElementCompiled[] selectList = SupportSelectExprFactory.makeAggregateMixed();
        StatementSpecCompiled spec = makeSpec(new SelectClauseSpecCompiled(selectList, false), null, groupByList, null, null, orderByList);
        ResultSetProcessorFactoryDesc processor = ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService1Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
        assertEquals(ResultSetProcessorType.AGGREGATED_UNGROUPED, processor.getResultSetProcessorType());

        // test a case where a property is both aggregated and non-aggregated: select volume, sum(volume)
        selectList = SupportSelectExprFactory.makeAggregatePlusNoAggregate();
        spec = makeSpec(new SelectClauseSpecCompiled(selectList, false), null, groupByList, null, null, orderByList);
        processor = ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService1Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
        assertEquals(ResultSetProcessorType.AGGREGATED_UNGROUPED, processor.getResultSetProcessorType());
    }

    public void testGetProcessorRowForAll() throws Exception {
        // empty group-by but aggragating event properties in select clause (output per event), no having clause
        // and all properties in the select clause are aggregated
        SelectClauseElementCompiled[] selectList = SupportSelectExprFactory.makeAggregateSelectListWithProps();
        StatementSpecCompiled spec = makeSpec(new SelectClauseSpecCompiled(selectList, false), null, groupByList, null, null, orderByList);
        ResultSetProcessorFactoryDesc processor = ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService1Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
        assertEquals(ResultSetProcessorType.FULLYAGGREGATED_UNGROUPED, processor.getResultSetProcessorType());
    }

    public void testGetProcessorRowPerGroup() throws Exception {
        // with group-by and the non-aggregated event properties are all listed in the group by (output per group)
        // no having clause
        SelectClauseElementCompiled[] selectList = SupportSelectExprFactory.makeAggregateMixed();
        groupByList.add(SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0"));
        StatementSpecCompiled spec = makeSpec(new SelectClauseSpecCompiled(selectList, false), null, groupByList, null, null, orderByList);
        ResultSetProcessorFactoryDesc processor = ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService1Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
        assertEquals(ResultSetProcessorType.FULLYAGGREGATED_GROUPED, processor.getResultSetProcessorType());
    }

    public void testGetProcessorAggregatingGrouped() throws Exception {
        // with group-by but either
        //      wildcard
        //      or one or more non-aggregated event properties are not in the group by (output per event)
        SelectClauseElementCompiled[] selectList = SupportSelectExprFactory.makeAggregateMixed();
        ExprNode identNode = SupportExprNodeFactory.makeIdentNode("theString", "s0");
        selectList = (SelectClauseElementCompiled[]) CollectionUtil.arrayExpandAddSingle(selectList, new SelectClauseExprCompiledSpec(identNode, null, null, false));

        groupByList.add(SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0"));
        StatementSpecCompiled spec = makeSpec(new SelectClauseSpecCompiled(selectList, false), null, groupByList, null, null, orderByList);
        ResultSetProcessorFactoryDesc processor = ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService1Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
        assertEquals(ResultSetProcessorType.AGGREGATED_GROUPED, processor.getResultSetProcessorType());
    }

    public void testGetProcessorInvalid() throws Exception {
        StatementSpecCompiled spec = makeSpec(new SelectClauseSpecCompiled(SupportSelectExprFactory.makeInvalidSelectList(), false), null, groupByList, null, null, orderByList);
        // invalid select clause
        try {
            ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService3Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }

        // invalid group-by
        groupByList.add(new ExprIdentNodeImpl("xxxx", "s0"));
        try {
            spec = makeSpec(new SelectClauseSpecCompiled(SupportSelectExprFactory.makeNoAggregateSelectListUnnamed(), false), null, groupByList, null, null, orderByList);
            ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService3Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }

        // Test group by having properties that are aggregated in select clause, should fail
        groupByList.clear();
        groupByList.add(SupportExprNodeFactory.makeSumAggregateNode());

        SelectClauseElementCompiled[] selectList = new SelectClauseElementCompiled[]{
                new SelectClauseExprCompiledSpec(SupportExprNodeFactory.makeSumAggregateNode(), null, null, false)
        };

        try {
            spec = makeSpec(new SelectClauseSpecCompiled(selectList, false), null, groupByList, null, null, orderByList);
            ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, stmtContext, typeService3Stream, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, new Configuration(), null, false, false);
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }
    }

    private StatementSpecCompiled makeSpec(SelectClauseSpecCompiled selectClauseSpec,
                                           InsertIntoDesc insertIntoDesc,
                                           List<ExprNode> groupByNodes,
                                           ExprNode optionalHavingNode,
                                           OutputLimitSpec outputLimitSpec,
                                           List<OrderByItem> orderByList) {
        return new StatementSpecCompiled(null, // on trigger
                null,  // create win
                null,  // create index
                null,  // create var
                null,  // create agg var
                null,  // create schema
                insertIntoDesc,
                SelectClauseStreamSelectorEnum.ISTREAM_ONLY,
                selectClauseSpec,
                new StreamSpecCompiled[0],  // stream specs
                null,  // outer join
                null,
                optionalHavingNode,
                outputLimitSpec,
                OrderByItem.toArray(orderByList),
                null,
                ExprNodeUtilityRich.EMPTY_DECLARED_ARR,
                ExprNodeUtilityRich.EMPTY_SCRIPTS,
                null,
                null,
                CollectionUtil.STRINGARRAY_EMPTY,
                new Annotation[0],
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null, new GroupByClauseExpressions(ExprNodeUtilityCore.toArray(groupByNodes)), null, null);
    }
}
