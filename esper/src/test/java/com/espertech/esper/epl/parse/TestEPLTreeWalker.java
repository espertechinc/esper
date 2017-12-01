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
package com.espertech.esper.epl.parse;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.funcs.ExprMinMaxRowNode;
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;
import com.espertech.esper.epl.expression.ops.*;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableServiceImpl;
import com.espertech.esper.pattern.*;
import com.espertech.esper.rowregex.RowRegexExprNodePrecedenceEnum;
import com.espertech.esper.schedule.SchedulingServiceImpl;
import com.espertech.esper.supportunit.bean.*;
import com.espertech.esper.supportunit.epl.parse.SupportParserHelper;
import com.espertech.esper.timer.TimeSourceServiceImpl;
import com.espertech.esper.type.OuterJoinType;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;

public class TestEPLTreeWalker extends TestCase {
    private static String CLASSNAME = SupportBean.class.getName();
    private static String EXPRESSION = "select * from " +
            CLASSNAME + "(string='a')#length(10)#lastevent as win1," +
            CLASSNAME + "(string='b')#length(10)#lastevent as win2 ";

    public void testWalkGraph() throws Exception {
        String expression = "create dataflow MyGraph MyOp((s0, s1) as ST1, s2) -> out1, out2 {}";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        CreateDataFlowDesc graph = walker.getStatementSpec().getCreateDataFlowDesc();
        assertEquals("MyGraph", graph.getGraphName());
        assertEquals(1, graph.getOperators().size());
        GraphOperatorSpec op = graph.getOperators().get(0);
        assertEquals("MyOp", op.getOperatorName());

        // assert input
        assertEquals(2, op.getInput().getStreamNamesAndAliases().size());
        GraphOperatorInputNamesAlias in1 = op.getInput().getStreamNamesAndAliases().get(0);
        EPAssertionUtil.assertEqualsExactOrder("s0,s1".split(","), in1.getInputStreamNames());
        assertEquals("ST1", in1.getOptionalAsName());
        GraphOperatorInputNamesAlias in2 = op.getInput().getStreamNamesAndAliases().get(1);
        EPAssertionUtil.assertEqualsExactOrder("s2".split(","), in2.getInputStreamNames());
        assertNull(in2.getOptionalAsName());

        // assert output
        assertEquals(2, op.getOutput().getItems().size());
        GraphOperatorOutputItem out1 = op.getOutput().getItems().get(0);
        assertEquals("out1", out1.getStreamName());
        assertEquals(0, out1.getTypeInfo().size());
        GraphOperatorOutputItem out2 = op.getOutput().getItems().get(1);
        assertEquals("out2", out2.getStreamName());
        assertEquals(0, out1.getTypeInfo().size());

        GraphOperatorOutputItemType type;

        type = tryWalkGraphTypes("out<?>");
        assertTrue(type.isWildcard());
        assertNull(type.getTypeOrClassname());
        assertNull(type.getTypeParameters());

        type = tryWalkGraphTypes("out<eventbean<?>>");
        assertFalse(type.isWildcard());
        assertEquals("eventbean", type.getTypeOrClassname());
        assertEquals(1, type.getTypeParameters().size());
        assertTrue(type.getTypeParameters().get(0).isWildcard());
        assertNull(type.getTypeParameters().get(0).getTypeOrClassname());
        assertNull(type.getTypeParameters().get(0).getTypeParameters());

        type = tryWalkGraphTypes("out<eventbean<someschema>>");
        assertFalse(type.isWildcard());
        assertEquals("eventbean", type.getTypeOrClassname());
        assertEquals(1, type.getTypeParameters().size());
        assertFalse(type.getTypeParameters().get(0).isWildcard());
        assertEquals("someschema", type.getTypeParameters().get(0).getTypeOrClassname());
        assertEquals(0, type.getTypeParameters().get(0).getTypeParameters().size());

        type = tryWalkGraphTypes("out<Map<String, Integer>>");
        assertFalse(type.isWildcard());
        assertEquals("Map", type.getTypeOrClassname());
        assertEquals(2, type.getTypeParameters().size());
        assertEquals("String", type.getTypeParameters().get(0).getTypeOrClassname());
        assertEquals("Integer", type.getTypeParameters().get(1).getTypeOrClassname());
    }

    private GraphOperatorOutputItemType tryWalkGraphTypes(String outstream) throws Exception {
        String expression = "create dataflow MyGraph MyOp((s0, s1) as ST1, s2) -> " + outstream + " {}";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        CreateDataFlowDesc graph = walker.getStatementSpec().getCreateDataFlowDesc();
        return graph.getOperators().get(0).getOutput().getItems().get(0).getTypeInfo().get(0);
    }

    public void testWalkCreateSchema() throws Exception {
        String expression = "create schema MyName as com.company.SupportBean";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        CreateSchemaDesc schema = walker.getStatementSpec().getCreateSchemaDesc();
        assertEquals("MyName", schema.getSchemaName());
        EPAssertionUtil.assertEqualsExactOrder("com.company.SupportBean".split(","), schema.getTypes().toArray());
        assertTrue(schema.getInherits().isEmpty());
        assertTrue(schema.getColumns().isEmpty());
        assertEquals(CreateSchemaDesc.AssignedType.NONE, schema.getAssignedType());

        expression = "create schema MyName (col1 string, col2 int, col3 Type[]) inherits InheritedType";
        walker = SupportParserHelper.parseAndWalkEPL(expression);
        schema = walker.getStatementSpec().getCreateSchemaDesc();
        assertEquals("MyName", schema.getSchemaName());
        assertTrue(schema.getTypes().isEmpty());
        EPAssertionUtil.assertEqualsExactOrder("InheritedType".split(","), schema.getInherits().toArray());
        assertSchema(schema.getColumns().get(0), "col1", "string", false);
        assertSchema(schema.getColumns().get(1), "col2", "int", false);
        assertSchema(schema.getColumns().get(2), "col3", "Type", true);

        expression = "create variant schema MyName as MyNameTwo,MyNameThree";
        walker = SupportParserHelper.parseAndWalkEPL(expression);
        schema = walker.getStatementSpec().getCreateSchemaDesc();
        assertEquals("MyName", schema.getSchemaName());
        EPAssertionUtil.assertEqualsExactOrder("MyNameTwo,MyNameThree".split(","), schema.getTypes().toArray());
        assertTrue(schema.getInherits().isEmpty());
        assertTrue(schema.getColumns().isEmpty());
        assertEquals(CreateSchemaDesc.AssignedType.VARIANT, schema.getAssignedType());
    }

    private void assertSchema(ColumnDesc element, String name, String type, boolean isArray) {
        assertEquals(name, element.getName());
        assertEquals(type, element.getType());
        assertEquals(isArray, element.isArray());
    }

    public void testWalkCreateIndex() throws Exception {
        String expression = "create index A_INDEX on B_NAMEDWIN (c, d btree)";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        CreateIndexDesc createIndex = walker.getStatementSpec().getCreateIndexDesc();
        assertEquals("A_INDEX", createIndex.getIndexName());
        assertEquals("B_NAMEDWIN", createIndex.getWindowName());
        assertEquals(2, createIndex.getColumns().size());
        assertEquals("c", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(createIndex.getColumns().get(0).getExpressions().get(0)));
        assertEquals(CreateIndexType.HASH.getNameLower(), createIndex.getColumns().get(0).getType());
        assertEquals("d", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(createIndex.getColumns().get(1).getExpressions().get(0)));
        assertEquals(CreateIndexType.BTREE.getNameLower(), createIndex.getColumns().get(1).getType());
    }

    public void testWalkViewExpressions() throws Exception {
        String className = SupportBean.class.getName();
        String expression = "select * from " + className + ".win:x(intPrimitive, a.nested)";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        ViewSpec[] viewSpecs = walker.getStatementSpec().getStreamSpecs().get(0).getViewSpecs();
        List<ExprNode> parameters = viewSpecs[0].getObjectParameters();
        assertEquals("intPrimitive", ((ExprIdentNode) parameters.get(0)).getFullUnresolvedName());
        assertEquals("a.nested", ((ExprIdentNode) parameters.get(1)).getFullUnresolvedName());
    }

    public void testWalkJoinMethodStatement() throws Exception {
        String className = SupportBean.class.getName();
        String expression = "select distinct * from " + className + " unidirectional, method:com.MyClass.myMethod(string, 2*intPrimitive) as s0";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        StatementSpecRaw statementSpec = walker.getStatementSpec();
        assertTrue(statementSpec.getSelectClauseSpec().isDistinct());
        assertEquals(2, statementSpec.getStreamSpecs().size());
        assertTrue(statementSpec.getStreamSpecs().get(0).getOptions().isUnidirectional());
        assertFalse(statementSpec.getStreamSpecs().get(0).getOptions().isRetainUnion());
        assertFalse(statementSpec.getStreamSpecs().get(0).getOptions().isRetainIntersection());

        MethodStreamSpec methodSpec = (MethodStreamSpec) statementSpec.getStreamSpecs().get(1);
        assertEquals("method", methodSpec.getIdent());
        assertEquals("com.MyClass", methodSpec.getClassName());
        assertEquals("myMethod", methodSpec.getMethodName());
        assertEquals(2, methodSpec.getExpressions().size());
        assertTrue(methodSpec.getExpressions().get(0) instanceof ExprIdentNode);
        assertTrue(methodSpec.getExpressions().get(1) instanceof ExprMathNode);
    }

    public void testWalkRetainKeywords() throws Exception {
        String className = SupportBean.class.getName();
        String expression = "select * from " + className + " retain-union";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        StatementSpecRaw statementSpec = walker.getStatementSpec();
        assertEquals(1, statementSpec.getStreamSpecs().size());
        assertTrue(statementSpec.getStreamSpecs().get(0).getOptions().isRetainUnion());
        assertFalse(statementSpec.getStreamSpecs().get(0).getOptions().isRetainIntersection());

        expression = "select * from " + className + " retain-intersection";

        walker = SupportParserHelper.parseAndWalkEPL(expression);
        statementSpec = walker.getStatementSpec();
        assertEquals(1, statementSpec.getStreamSpecs().size());
        assertFalse(statementSpec.getStreamSpecs().get(0).getOptions().isRetainUnion());
        assertTrue(statementSpec.getStreamSpecs().get(0).getOptions().isRetainIntersection());
    }

    public void testWalkCreateVariable() throws Exception {
        String expression = "create constant variable sometype var1 = 1";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        StatementSpecRaw raw = walker.getStatementSpec();

        CreateVariableDesc createVarDesc = raw.getCreateVariableDesc();
        assertEquals("sometype", createVarDesc.getVariableType());
        assertEquals("var1", createVarDesc.getVariableName());
        assertTrue(createVarDesc.getAssignment() instanceof ExprConstantNode);
        assertTrue(createVarDesc.isConstant());
    }

    public void testWalkOnSet() throws Exception {
        VariableService variableService = new VariableServiceImpl(0, new SchedulingServiceImpl(new TimeSourceServiceImpl()), SupportEventAdapterService.getService(), null);
        variableService.createNewVariable(null, "var1", Long.class.getName(), false, false, false, 100L, SupportEngineImportServiceFactory.make());
        variableService.allocateVariableState("var1", 0, null, false);

        String expression = "on com.MyClass as myevent set var1 = 'a', var2 = 2*3, var3 = var1";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression, null, variableService);
        StatementSpecRaw raw = walker.getStatementSpec();

        FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) raw.getStreamSpecs().get(0);
        assertEquals("com.MyClass", streamSpec.getRawFilterSpec().getEventTypeName());
        assertEquals(0, streamSpec.getRawFilterSpec().getFilterExpressions().size());
        assertEquals("myevent", streamSpec.getOptionalStreamName());

        OnTriggerSetDesc setDesc = (OnTriggerSetDesc) raw.getOnTriggerDesc();
        assertTrue(setDesc.getOnTriggerType() == OnTriggerType.ON_SET);
        assertEquals(3, setDesc.getAssignments().size());

        OnTriggerSetAssignment assign = setDesc.getAssignments().get(0);
        assertEquals("var1", ((ExprVariableNode) (assign.getExpression().getChildNodes()[0])).getVariableName());
        assertTrue(assign.getExpression() instanceof ExprEqualsNode);
        assertTrue(assign.getExpression().getChildNodes()[1] instanceof ExprConstantNode);

        assign = setDesc.getAssignments().get(1);
        assertEquals("var2", ((ExprIdentNode) (assign.getExpression().getChildNodes()[0])).getFullUnresolvedName());
        assertTrue(assign.getExpression() instanceof ExprEqualsNode);
        assertTrue(assign.getExpression().getChildNodes()[1] instanceof ExprMathNode);

        assign = setDesc.getAssignments().get(2);
        assertEquals("var3", ((ExprIdentNode) (assign.getExpression().getChildNodes()[0])).getFullUnresolvedName());
        ExprVariableNode varNode = (ExprVariableNode) assign.getExpression().getChildNodes()[1];
        assertEquals("var1", varNode.getVariableName());

        assertTrue(raw.isHasVariables());

        // try a subquery
        expression = "select (select var1 from MyEvent) from MyEvent2";
        walker = SupportParserHelper.parseAndWalkEPL(expression, null, variableService);
        raw = walker.getStatementSpec();
        assertTrue(raw.isHasVariables());
    }

    public void testWalkOnUpdate() throws Exception {
        String expression = "on com.MyClass as myevent update MyWindow as mw set prop1 = 'a', prop2=a.b*c where a=b";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        StatementSpecRaw raw = walker.getStatementSpec();

        FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) raw.getStreamSpecs().get(0);
        assertEquals("com.MyClass", streamSpec.getRawFilterSpec().getEventTypeName());
        assertEquals(0, streamSpec.getRawFilterSpec().getFilterExpressions().size());
        assertEquals("myevent", streamSpec.getOptionalStreamName());

        OnTriggerWindowUpdateDesc setDesc = (OnTriggerWindowUpdateDesc) raw.getOnTriggerDesc();
        assertTrue(setDesc.getOnTriggerType() == OnTriggerType.ON_UPDATE);
        assertEquals(2, setDesc.getAssignments().size());

        OnTriggerSetAssignment assign = setDesc.getAssignments().get(0);
        assertEquals("prop1", ((ExprIdentNode) (assign.getExpression().getChildNodes()[0])).getFullUnresolvedName());
        assertTrue(assign.getExpression().getChildNodes()[1] instanceof ExprConstantNode);

        assertEquals("a=b", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(raw.getFilterExprRootNode()));
    }

    public void testWalkOnSelectNoInsert() throws Exception {
        String expression = "on com.MyClass(myval != 0) as myevent select *, mywin.* as abc, myevent.* from MyNamedWindow as mywin where a=b";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        StatementSpecRaw raw = walker.getStatementSpec();

        FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) raw.getStreamSpecs().get(0);
        assertEquals("com.MyClass", streamSpec.getRawFilterSpec().getEventTypeName());
        assertEquals(1, streamSpec.getRawFilterSpec().getFilterExpressions().size());
        assertEquals("myevent", streamSpec.getOptionalStreamName());

        OnTriggerWindowDesc windowDesc = (OnTriggerWindowDesc) raw.getOnTriggerDesc();
        assertEquals("MyNamedWindow", windowDesc.getWindowName());
        assertEquals("mywin", windowDesc.getOptionalAsName());
        assertEquals(OnTriggerType.ON_SELECT, windowDesc.getOnTriggerType());

        assertNull(raw.getInsertIntoDesc());
        assertTrue(raw.getSelectClauseSpec().isUsingWildcard());
        assertEquals(3, raw.getSelectClauseSpec().getSelectExprList().size());
        assertTrue(raw.getSelectClauseSpec().getSelectExprList().get(0) instanceof SelectClauseElementWildcard);
        assertEquals("mywin", ((SelectClauseStreamRawSpec) raw.getSelectClauseSpec().getSelectExprList().get(1)).getStreamName());
        assertEquals("mywin", ((SelectClauseStreamRawSpec) raw.getSelectClauseSpec().getSelectExprList().get(1)).getStreamName());
        assertEquals("abc", ((SelectClauseStreamRawSpec) raw.getSelectClauseSpec().getSelectExprList().get(1)).getOptionalAsName());
        assertEquals("myevent", (((SelectClauseStreamRawSpec) raw.getSelectClauseSpec().getSelectExprList().get(2)).getStreamName()));
        assertNull(((SelectClauseStreamRawSpec) raw.getSelectClauseSpec().getSelectExprList().get(2)).getOptionalAsName());
        assertTrue(raw.getFilterRootNode() instanceof ExprEqualsNode);
    }

    public void testWalkOnSelectInsert() throws Exception {
        String expression = "on pattern [com.MyClass] as pat insert into MyStream(a, b) select c, d from MyNamedWindow as mywin " +
                " where a=b group by symbol having c=d order by e";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        StatementSpecRaw raw = walker.getStatementSpec();

        PatternStreamSpecRaw streamSpec = (PatternStreamSpecRaw) raw.getStreamSpecs().get(0);
        assertTrue(streamSpec.getEvalFactoryNode() instanceof EvalFilterFactoryNode);
        assertEquals("pat", streamSpec.getOptionalStreamName());

        OnTriggerWindowDesc windowDesc = (OnTriggerWindowDesc) raw.getOnTriggerDesc();
        assertEquals("MyNamedWindow", windowDesc.getWindowName());
        assertEquals("mywin", windowDesc.getOptionalAsName());
        assertEquals(OnTriggerType.ON_SELECT, windowDesc.getOnTriggerType());
        assertTrue(raw.getFilterRootNode() instanceof ExprEqualsNode);

        assertEquals("MyStream", raw.getInsertIntoDesc().getEventTypeName());
        assertEquals(2, raw.getInsertIntoDesc().getColumnNames().size());
        assertEquals("a", raw.getInsertIntoDesc().getColumnNames().get(0));
        assertEquals("b", raw.getInsertIntoDesc().getColumnNames().get(1));

        assertFalse(raw.getSelectClauseSpec().isUsingWildcard());
        assertEquals(2, raw.getSelectClauseSpec().getSelectExprList().size());

        assertEquals(1, raw.getGroupByExpressions().size());
        assertTrue(raw.getHavingExprRootNode() instanceof ExprEqualsNode);
        assertEquals(1, raw.getOrderByList().size());
    }

    public void testWalkOnSelectMultiInsert() throws Exception {
        String expression = "on Bean as pat " +
                " insert into MyStream select * where 1>2" +
                " insert into BStream(a, b) select * where 1=2" +
                " insert into CStream select a,b";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        StatementSpecRaw raw = walker.getStatementSpec();

        FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) raw.getStreamSpecs().get(0);
        assertEquals("pat", streamSpec.getOptionalStreamName());

        OnTriggerSplitStreamDesc triggerDesc = (OnTriggerSplitStreamDesc) raw.getOnTriggerDesc();
        assertEquals(OnTriggerType.ON_SPLITSTREAM, triggerDesc.getOnTriggerType());
        assertEquals(2, triggerDesc.getSplitStreams().size());

        assertEquals("MyStream", raw.getInsertIntoDesc().getEventTypeName());
        assertTrue(raw.getSelectClauseSpec().isUsingWildcard());
        assertEquals(1, raw.getSelectClauseSpec().getSelectExprList().size());
        assertNotNull((ExprRelationalOpNode) raw.getFilterRootNode());

        OnTriggerSplitStream splitStream = triggerDesc.getSplitStreams().get(0);
        assertEquals("BStream", splitStream.getInsertInto().getEventTypeName());
        assertEquals(2, splitStream.getInsertInto().getColumnNames().size());
        assertEquals("a", splitStream.getInsertInto().getColumnNames().get(0));
        assertEquals("b", splitStream.getInsertInto().getColumnNames().get(1));
        assertTrue(splitStream.getSelectClause().isUsingWildcard());
        assertEquals(1, splitStream.getSelectClause().getSelectExprList().size());
        assertNotNull((ExprEqualsNode) splitStream.getWhereClause());

        splitStream = triggerDesc.getSplitStreams().get(1);
        assertEquals("CStream", splitStream.getInsertInto().getEventTypeName());
        assertEquals(0, splitStream.getInsertInto().getColumnNames().size());
        assertFalse(splitStream.getSelectClause().isUsingWildcard());
        assertEquals(2, splitStream.getSelectClause().getSelectExprList().size());
        assertNull(splitStream.getWhereClause());
    }

    public void testWalkOnDelete() throws Exception {
        // try a filter
        String expression = "on com.MyClass(myval != 0) as myevent delete from MyNamedWindow as mywin where mywin.key = myevent.otherKey";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        StatementSpecRaw raw = walker.getStatementSpec();

        FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) raw.getStreamSpecs().get(0);
        assertEquals("com.MyClass", streamSpec.getRawFilterSpec().getEventTypeName());
        assertEquals(1, streamSpec.getRawFilterSpec().getFilterExpressions().size());
        assertEquals("myevent", streamSpec.getOptionalStreamName());

        OnTriggerWindowDesc windowDesc = (OnTriggerWindowDesc) raw.getOnTriggerDesc();
        assertEquals("MyNamedWindow", windowDesc.getWindowName());
        assertEquals("mywin", windowDesc.getOptionalAsName());
        assertEquals(OnTriggerType.ON_DELETE, windowDesc.getOnTriggerType());

        assertTrue(raw.getFilterRootNode() instanceof ExprEqualsNode);

        // try a pattern
        expression = "on pattern [every MyClass] as myevent delete from MyNamedWindow";
        walker = SupportParserHelper.parseAndWalkEPL(expression);
        raw = walker.getStatementSpec();

        PatternStreamSpecRaw patternSpec = (PatternStreamSpecRaw) raw.getStreamSpecs().get(0);
        assertTrue(patternSpec.getEvalFactoryNode() instanceof EvalEveryFactoryNode);
    }

    public void testWalkCreateWindow() throws Exception {
        String expression = "create window MyWindow#groupwin(symbol)#length(20) as select *, aprop, bprop as someval from com.MyClass insert where a=b";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        StatementSpecRaw raw = walker.getStatementSpec();

        // window name
        assertEquals("MyWindow", raw.getCreateWindowDesc().getWindowName());
        assertTrue(raw.getCreateWindowDesc().isInsert());
        assertTrue(raw.getCreateWindowDesc().getInsertFilter() instanceof ExprEqualsNode);

        // select clause
        assertTrue(raw.getSelectClauseSpec().isUsingWildcard());
        List<SelectClauseElementRaw> selectSpec = raw.getSelectClauseSpec().getSelectExprList();
        assertEquals(3, selectSpec.size());
        assertTrue(raw.getSelectClauseSpec().getSelectExprList().get(0) instanceof SelectClauseElementWildcard);
        SelectClauseExprRawSpec rawSpec = (SelectClauseExprRawSpec) selectSpec.get(1);
        assertEquals("aprop", ((ExprIdentNode) rawSpec.getSelectExpression()).getUnresolvedPropertyName());
        rawSpec = (SelectClauseExprRawSpec) selectSpec.get(2);
        assertEquals("bprop", ((ExprIdentNode) rawSpec.getSelectExpression()).getUnresolvedPropertyName());
        assertEquals("someval", rawSpec.getOptionalAsName());

        // filter is the event type
        FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) raw.getStreamSpecs().get(0);
        assertEquals("com.MyClass", streamSpec.getRawFilterSpec().getEventTypeName());

        // 2 views
        assertEquals(2, raw.getCreateWindowDesc().getViewSpecs().size());
        assertEquals("groupwin", raw.getCreateWindowDesc().getViewSpecs().get(0).getObjectName());
        assertEquals(null, raw.getCreateWindowDesc().getViewSpecs().get(0).getObjectNamespace());
        assertEquals("length", raw.getCreateWindowDesc().getViewSpecs().get(1).getObjectName());
    }

    public void testWalkMatchRecognize() throws Exception {
        String[] patternTests = new String[]{
                "A", "A B", "A? B*", "(A|B)+", "A C|B C", "(G1|H1) (I1|J1)", "(G1*|H1)? (I1+|J1?)", "A B G (H H|(I P)?) K?"};

        for (int i = 0; i < patternTests.length; i++) {
            String expression = "select * from MyEvent#keepall match_recognize (" +
                    "  partition by string measures A.string as a_string pattern ( " + patternTests[i] + ") define A as (A.value = 1) )";

            EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
            StatementSpecRaw raw = walker.getStatementSpec();

            assertEquals(1, raw.getMatchRecognizeSpec().getMeasures().size());
            assertEquals(1, raw.getMatchRecognizeSpec().getDefines().size());
            assertEquals(1, raw.getMatchRecognizeSpec().getPartitionByExpressions().size());

            StringWriter writer = new StringWriter();
            raw.getMatchRecognizeSpec().getPattern().toEPL(writer, RowRegexExprNodePrecedenceEnum.MINIMUM);
            String received = writer.toString();
            assertEquals(patternTests[i], received);
        }
    }

    public void testWalkSubstitutionParams() throws Exception {
        // try EPL
        String expression = "select * from " + CLASSNAME + "(string=?, value=?)";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        walker.end();
        StatementSpecRaw raw = walker.getStatementSpec();
        assertEquals(2, raw.getSubstitutionParameters().size());

        FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) raw.getStreamSpecs().get(0);
        ExprEqualsNode equalsFilter = (ExprEqualsNode) streamSpec.getRawFilterSpec().getFilterExpressions().get(0);
        assertEquals(1, (int) ((ExprSubstitutionNode) equalsFilter.getChildNodes()[1]).getIndex());
        equalsFilter = (ExprEqualsNode) streamSpec.getRawFilterSpec().getFilterExpressions().get(1);
        assertEquals(2, (int) ((ExprSubstitutionNode) equalsFilter.getChildNodes()[1]).getIndex());

        // try pattern
        expression = CLASSNAME + "(string=?, value=?)";
        walker = SupportParserHelper.parseAndWalkPattern(expression);
        raw = walker.getStatementSpec();
        assertEquals(2, raw.getSubstitutionParameters().size());
    }

    public void testWalkPatternMatchUntil() throws Exception {
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkPattern("A until (B or C)");
        StatementSpecRaw raw = walker.getStatementSpec();
        PatternStreamSpecRaw a = (PatternStreamSpecRaw) raw.getStreamSpecs().get(0);
        EvalMatchUntilFactoryNode matchNode = (EvalMatchUntilFactoryNode) a.getEvalFactoryNode();
        assertEquals(2, matchNode.getChildNodes().size());
        assertTrue(matchNode.getChildNodes().get(0) instanceof EvalFilterFactoryNode);
        assertTrue(matchNode.getChildNodes().get(1) instanceof EvalOrFactoryNode);

        EvalMatchUntilFactoryNode spec = getMatchUntilSpec("A until (B or C)");
        assertNull(spec.getLowerBounds());
        assertNull(spec.getUpperBounds());

        spec = getMatchUntilSpec("[1:10] A until (B or C)");
        assertEquals(1, spec.getLowerBounds().getForge().getExprEvaluator().evaluate(null, true, null));
        assertEquals(10, spec.getUpperBounds().getForge().getExprEvaluator().evaluate(null, true, null));

        spec = getMatchUntilSpec("[1 : 10] A until (B or C)");
        assertEquals(1, spec.getLowerBounds().getForge().getExprEvaluator().evaluate(null, true, null));
        assertEquals(10, spec.getUpperBounds().getForge().getExprEvaluator().evaluate(null, true, null));

        spec = getMatchUntilSpec("[1:10] A until (B or C)");
        assertEquals(1, spec.getLowerBounds().getForge().getExprEvaluator().evaluate(null, true, null));
        assertEquals(10, spec.getUpperBounds().getForge().getExprEvaluator().evaluate(null, true, null));

        spec = getMatchUntilSpec("[1:] A until (B or C)");
        assertEquals(1, spec.getLowerBounds().getForge().getExprEvaluator().evaluate(null, true, null));
        assertEquals(null, spec.getUpperBounds());

        spec = getMatchUntilSpec("[1 :] A until (B or C)");
        assertEquals(1, spec.getLowerBounds().getForge().getExprEvaluator().evaluate(null, true, null));
        assertEquals(null, spec.getUpperBounds());
        assertEquals(null, spec.getSingleBound());

        spec = getMatchUntilSpec("[:2] A until (B or C)");
        assertEquals(null, spec.getLowerBounds());
        assertEquals(null, spec.getSingleBound());
        assertEquals(2, spec.getUpperBounds().getForge().getExprEvaluator().evaluate(null, true, null));

        spec = getMatchUntilSpec("[: 2] A until (B or C)");
        assertEquals(null, spec.getLowerBounds());
        assertEquals(null, spec.getSingleBound());
        assertEquals(2, spec.getUpperBounds().getForge().getExprEvaluator().evaluate(null, true, null));

        spec = getMatchUntilSpec("[2] A until (B or C)");
        assertEquals(2, spec.getSingleBound().getForge().getExprEvaluator().evaluate(null, true, null));
    }

    private EvalMatchUntilFactoryNode getMatchUntilSpec(String text) throws Exception {
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkPattern(text);
        StatementSpecRaw raw = walker.getStatementSpec();
        PatternStreamSpecRaw a = (PatternStreamSpecRaw) raw.getStreamSpecs().get(0);
        return (EvalMatchUntilFactoryNode) a.getEvalFactoryNode();
    }

    public void testWalkSimpleWhere() throws Exception {
        String expression = EXPRESSION + "where win1.f1=win2.f2";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);

        assertEquals(2, walker.getStatementSpec().getStreamSpecs().size());

        FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);
        assertEquals(2, streamSpec.getViewSpecs().length);
        assertEquals(SupportBean.class.getName(), streamSpec.getRawFilterSpec().getEventTypeName());
        assertEquals("length", streamSpec.getViewSpecs()[0].getObjectName());
        assertEquals("lastevent", streamSpec.getViewSpecs()[1].getObjectName());
        assertEquals("win1", streamSpec.getOptionalStreamName());

        streamSpec = (FilterStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(1);
        assertEquals("win2", streamSpec.getOptionalStreamName());

        // Join expression tree validation
        assertTrue(walker.getStatementSpec().getFilterRootNode() instanceof ExprEqualsNode);
        ExprNode equalsNode = (walker.getStatementSpec().getFilterRootNode());
        assertEquals(2, equalsNode.getChildNodes().length);

        ExprIdentNode identNode = (ExprIdentNode) equalsNode.getChildNodes()[0];
        assertEquals("win1", identNode.getStreamOrPropertyName());
        assertEquals("f1", identNode.getUnresolvedPropertyName());
        identNode = (ExprIdentNode) equalsNode.getChildNodes()[1];
        assertEquals("win2", identNode.getStreamOrPropertyName());
        assertEquals("f2", identNode.getUnresolvedPropertyName());
    }

    public void testWalkWhereWithAnd() throws Exception {
        String expression = "select * from " +
                CLASSNAME + "(string='a')#length(10)#lastevent as win1," +
                CLASSNAME + "(string='b')#length(9)#lastevent as win2, " +
                CLASSNAME + "(string='c')#length(3)#lastevent as win3 " +
                "where win1.f1=win2.f2 and win3.f3=f4 limit 5 offset 10";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);

        // ProjectedStream spec validation
        assertEquals(3, walker.getStatementSpec().getStreamSpecs().size());
        assertEquals("win1", walker.getStatementSpec().getStreamSpecs().get(0).getOptionalStreamName());
        assertEquals("win2", walker.getStatementSpec().getStreamSpecs().get(1).getOptionalStreamName());
        assertEquals("win3", walker.getStatementSpec().getStreamSpecs().get(2).getOptionalStreamName());

        FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(2);
        assertEquals(2, streamSpec.getViewSpecs().length);
        assertEquals(SupportBean.class.getName(), streamSpec.getRawFilterSpec().getEventTypeName());
        assertEquals("length", streamSpec.getViewSpecs()[0].getObjectName());
        assertEquals("lastevent", streamSpec.getViewSpecs()[1].getObjectName());

        // Join expression tree validation
        assertTrue(walker.getStatementSpec().getFilterRootNode() instanceof ExprAndNode);
        assertEquals(2, walker.getStatementSpec().getFilterRootNode().getChildNodes().length);
        ExprNode equalsNode = (walker.getStatementSpec().getFilterRootNode().getChildNodes()[0]);
        assertEquals(2, equalsNode.getChildNodes().length);

        ExprIdentNode identNode = (ExprIdentNode) equalsNode.getChildNodes()[0];
        assertEquals("win1", identNode.getStreamOrPropertyName());
        assertEquals("f1", identNode.getUnresolvedPropertyName());
        identNode = (ExprIdentNode) equalsNode.getChildNodes()[1];
        assertEquals("win2", identNode.getStreamOrPropertyName());
        assertEquals("f2", identNode.getUnresolvedPropertyName());

        equalsNode = (walker.getStatementSpec().getFilterRootNode().getChildNodes()[1]);
        identNode = (ExprIdentNode) equalsNode.getChildNodes()[0];
        assertEquals("win3", identNode.getStreamOrPropertyName());
        assertEquals("f3", identNode.getUnresolvedPropertyName());
        identNode = (ExprIdentNode) equalsNode.getChildNodes()[1];
        assertNull(identNode.getStreamOrPropertyName());
        assertEquals("f4", identNode.getUnresolvedPropertyName());

        assertEquals(5, (int) walker.getStatementSpec().getRowLimitSpec().getNumRows());
        assertEquals(10, (int) walker.getStatementSpec().getRowLimitSpec().getOptionalOffset());
    }

    public void testWalkPerRowFunctions() throws Exception {
        assertEquals(9, tryExpression("max(6, 9)"));
        assertEquals(6.11, tryExpression("min(6.11, 6.12)"));
        assertEquals(6.10, tryExpression("min(6.11, 6.12, 6.1)"));
        assertEquals("ab", tryExpression("'a'||'b'"));
        assertEquals(null, tryExpression("coalesce(null, null)"));
        assertEquals(1, tryExpression("coalesce(null, 1)"));
        assertEquals(1l, tryExpression("coalesce(null, 1l)"));
        assertEquals("a", tryExpression("coalesce(null, 'a', 'b')"));
        assertEquals(13.5d, tryExpression("coalesce(null, null, 3*4.5)"));
        assertEquals(true, tryExpression("coalesce(null, true)"));
        assertEquals(5, tryExpression("coalesce(5, null, 6)"));
        assertEquals(2, tryExpression("(case 1 when 1 then 2 end)"));
    }

    public void testWalkMath() throws Exception {
        assertEquals(32.0, tryExpression("5*6-3+15/3"));
        assertEquals(-5, tryExpression("1-1-1-2-1-1"));
        assertEquals(2.8d, tryExpression("1.4 + 1.4"));
        assertEquals(1d, tryExpression("55.5/5/11.1"));
        assertEquals(2 / 3d, tryExpression("2/3"));
        assertEquals(2 / 3d, tryExpression("2.0/3"));
        assertEquals(10, tryExpression("(1+4)*2"));
        assertEquals(12, tryExpression("(3*(6-4))*2"));
        assertEquals(8.5, tryExpression("(1+(4*3)+2)/2+1"));
        assertEquals(1, tryExpression("10%3"));
        assertEquals(10.1 % 3, tryExpression("10.1%3"));
    }

    public void testWalkRelationalOp() throws Exception {
        assertEquals(true, tryRelationalOp("3>2"));
        assertEquals(true, tryRelationalOp("3*5/2 >= 7.5"));
        assertEquals(true, tryRelationalOp("3*5/2.0 >= 7.5"));
        assertEquals(false, tryRelationalOp("1.1 + 2.2 < 3.2"));
        assertEquals(false, tryRelationalOp("3<=2"));
        assertEquals(true, tryRelationalOp("4*(3+1)>=16"));

        assertEquals(false, tryRelationalOp("(4>2) and (2>3)"));
        assertEquals(true, tryRelationalOp("(4>2) or (2>3)"));

        assertEquals(false, tryRelationalOp("not 3>2"));
        assertEquals(true, tryRelationalOp("not (not 3>2)"));
    }

    public void testWalkInsertInto() throws Exception {
        String expression = "insert into MyAlias select * from " +
                CLASSNAME + "()#length(10)#lastevent as win1," +
                CLASSNAME + "(string='b')#length(9)#lastevent as win2";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);

        InsertIntoDesc desc = walker.getStatementSpec().getInsertIntoDesc();
        assertEquals(SelectClauseStreamSelectorEnum.ISTREAM_ONLY, desc.getStreamSelector());
        assertEquals("MyAlias", desc.getEventTypeName());
        assertEquals(0, desc.getColumnNames().size());

        expression = "insert rstream into MyAlias(a, b, c) select * from " +
                CLASSNAME + "()#length(10)#lastevent as win1," +
                CLASSNAME + "(string='b')#length(9)#lastevent as win2";

        walker = SupportParserHelper.parseAndWalkEPL(expression);

        desc = walker.getStatementSpec().getInsertIntoDesc();
        assertEquals(SelectClauseStreamSelectorEnum.RSTREAM_ONLY, desc.getStreamSelector());
        assertEquals("MyAlias", desc.getEventTypeName());
        assertEquals(3, desc.getColumnNames().size());
        assertEquals("a", desc.getColumnNames().get(0));
        assertEquals("b", desc.getColumnNames().get(1));
        assertEquals("c", desc.getColumnNames().get(2));

        expression = "insert irstream into Test2 select * from " + CLASSNAME + "()#length(10)";
        walker = SupportParserHelper.parseAndWalkEPL(expression);
        desc = walker.getStatementSpec().getInsertIntoDesc();
        assertEquals(SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH, desc.getStreamSelector());
        assertEquals("Test2", desc.getEventTypeName());
        assertEquals(0, desc.getColumnNames().size());
    }

    public void testWalkView() throws Exception {
        String text = "select * from " + SupportBean.class.getName() + "(string=\"IBM\").win:lenght(10, 1.1, \"a\").stat:uni(price, false)";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);
        FilterSpecRaw filterSpec = ((FilterStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0)).getRawFilterSpec();

        // Check filter spec properties
        assertEquals(SupportBean.class.getName(), filterSpec.getEventTypeName());
        assertEquals(1, filterSpec.getFilterExpressions().size());

        // Check views
        ViewSpec[] viewSpecs = walker.getStatementSpec().getStreamSpecs().get(0).getViewSpecs();
        assertEquals(2, viewSpecs.length);

        ViewSpec specOne = viewSpecs[0];
        assertEquals("win", specOne.getObjectNamespace());
        assertEquals("lenght", specOne.getObjectName());
        assertEquals(3, specOne.getObjectParameters().size());
        assertEquals(10, ((ExprConstantNode) specOne.getObjectParameters().get(0)).getConstantValue(null));
        assertEquals(1.1d, ((ExprConstantNode) specOne.getObjectParameters().get(1)).getConstantValue(null));
        assertEquals("a", ((ExprConstantNode) specOne.getObjectParameters().get(2)).getConstantValue(null));

        ViewSpec specTwo = viewSpecs[1];
        assertEquals("stat", specTwo.getObjectNamespace());
        assertEquals("uni", specTwo.getObjectName());
        assertEquals(2, specTwo.getObjectParameters().size());
        assertEquals("price", ((ExprIdentNode) specTwo.getObjectParameters().get(0)).getFullUnresolvedName());
        assertEquals(false, ((ExprConstantNode) specTwo.getObjectParameters().get(1)).getConstantValue(null));
    }

    public void testWalkPropertyExpr() throws Exception {
        String text = "select * from " + SupportBean.class.getName() + "[a.b][select c,d.*,* from e as f where g]";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);
        FilterSpecRaw filterSpec = ((FilterStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0)).getRawFilterSpec();
        assertEquals(2, filterSpec.getOptionalPropertyEvalSpec().getAtoms().size());
        assertEquals("a.b", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(filterSpec.getOptionalPropertyEvalSpec().getAtoms().get(0).getSplitterExpression()));
        assertEquals(0, filterSpec.getOptionalPropertyEvalSpec().getAtoms().get(0).getOptionalSelectClause().getSelectExprList().size());

        PropertyEvalAtom atomTwo = filterSpec.getOptionalPropertyEvalSpec().getAtoms().get(1);
        assertEquals("e", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(atomTwo.getSplitterExpression()));
        assertEquals("f", atomTwo.getOptionalAsName());
        assertNotNull(atomTwo.getOptionalWhereClause());
        List<SelectClauseElementRaw> list = atomTwo.getOptionalSelectClause().getSelectExprList();
        assertEquals(3, list.size());
        assertTrue(list.get(0) instanceof SelectClauseExprRawSpec);
        assertTrue(list.get(1) instanceof SelectClauseStreamRawSpec);
        assertTrue(list.get(2) instanceof SelectClauseElementWildcard);
    }

    public void testSelectList() throws Exception {
        String text = "select intPrimitive, 2 * intBoxed, 5 as myConst, stream0.string as theString from " + SupportBean.class.getName() + "().win:lenght(10) as stream0";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);
        List<SelectClauseElementRaw> selectExpressions = walker.getStatementSpec().getSelectClauseSpec().getSelectExprList();
        assertEquals(4, selectExpressions.size());

        SelectClauseExprRawSpec rawSpec = (SelectClauseExprRawSpec) selectExpressions.get(0);
        assertTrue(rawSpec.getSelectExpression() instanceof ExprIdentNode);

        rawSpec = (SelectClauseExprRawSpec) selectExpressions.get(1);
        assertTrue(rawSpec.getSelectExpression() instanceof ExprMathNode);

        rawSpec = (SelectClauseExprRawSpec) selectExpressions.get(2);
        assertTrue(rawSpec.getSelectExpression() instanceof ExprConstantNode);
        assertEquals("myConst", rawSpec.getOptionalAsName());

        rawSpec = (SelectClauseExprRawSpec) selectExpressions.get(3);
        assertTrue(rawSpec.getSelectExpression() instanceof ExprIdentNode);
        assertEquals("theString", rawSpec.getOptionalAsName());
        assertNull(walker.getStatementSpec().getInsertIntoDesc());

        text = "select * from " + SupportBean.class.getName() + "().win:lenght(10)";
        walker = SupportParserHelper.parseAndWalkEPL(text);
        assertEquals(1, walker.getStatementSpec().getSelectClauseSpec().getSelectExprList().size());
    }

    public void testArrayViewParams() throws Exception {
        // Check a list of integer as a view parameter
        String text = "select * from " + SupportBean.class.getName() + "().win:lenght({10, 11, 12})";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);

        ViewSpec[] viewSpecs = walker.getStatementSpec().getStreamSpecs().get(0).getViewSpecs();
        ExprNode node = viewSpecs[0].getObjectParameters().get(0);
        node.validate(SupportExprValidationContextFactory.makeEmpty());
        Object[] intParams = (Object[]) ((ExprArrayNode) node).getForge().getExprEvaluator().evaluate(null, true, null);
        assertEquals(10, intParams[0]);
        assertEquals(11, intParams[1]);
        assertEquals(12, intParams[2]);

        // Check a list of objects
        text = "select * from " + SupportBean.class.getName() + "().win:lenght({false, 11.2, 's'})";
        walker = SupportParserHelper.parseAndWalkEPL(text);
        viewSpecs = walker.getStatementSpec().getStreamSpecs().get(0).getViewSpecs();
        ExprNode param = viewSpecs[0].getObjectParameters().get(0);
        param.validate(SupportExprValidationContextFactory.makeEmpty());
        Object[] objParams = (Object[]) ((ExprArrayNode) param).getForge().getExprEvaluator().evaluate(null, true, null);
        assertEquals(false, objParams[0]);
        assertEquals(11.2, objParams[1]);
        assertEquals("s", objParams[2]);
    }

    public void testOuterJoin() throws Exception {
        tryOuterJoin("left", OuterJoinType.LEFT);
        tryOuterJoin("right", OuterJoinType.RIGHT);
        tryOuterJoin("full", OuterJoinType.FULL);
    }

    public void testNoPackageName() throws Exception {
        String text = "select intPrimitive from SupportBean_N().win:lenght(10) as win1";
        SupportParserHelper.parseAndWalkEPL(text);
    }

    public void testAggregateFunction() throws Exception {
        String fromClause = "from " + SupportBean_N.class.getName() + "().win:lenght(10) as win1";
        String text = "select max(distinct intPrimitive) " + fromClause;
        SupportParserHelper.parseAndWalkEPL(text);

        text = "select sum(intPrimitive)," +
                "sum(distinct doubleBoxed)," +
                "avg(doubleBoxed)," +
                "avg(distinct doubleBoxed)," +
                "count(*)," +
                "count(intPrimitive)," +
                "count(distinct intPrimitive)," +
                "max(distinct intPrimitive)," +
                "min(distinct intPrimitive)," +
                "max(intPrimitive)," +
                "min(intPrimitive), " +
                "median(intPrimitive), " +
                "median(distinct intPrimitive)," +
                "stddev(intPrimitive), " +
                "stddev(distinct intPrimitive)," +
                "avedev(intPrimitive)," +
                "avedev(distinct intPrimitive) " +
                fromClause;
        SupportParserHelper.parseAndWalkEPL(text);

        // try min-max aggregate versus row functions
        text = "select max(intPrimitive), min(intPrimitive)," +
                "max(intPrimitive,intBoxed), min(intPrimitive,intBoxed)," +
                "max(distinct intPrimitive), min(distinct intPrimitive)" +
                fromClause;
        SupportParserHelper.parseAndWalkEPL(text);
    }

    public void testGroupBy() throws Exception {
        String text = "select sum(intPrimitive) from SupportBean_N().win:lenght(10) as win1 where intBoxed > 5 " +
                "group by intBoxed, 3 * doubleBoxed, max(2, doublePrimitive)";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);

        List<GroupByClauseElement> groupByList = walker.getStatementSpec().getGroupByExpressions();
        assertEquals(3, groupByList.size());

        ExprNode node = ((GroupByClauseElementExpr) groupByList.get(0)).getExpr();
        assertTrue(node instanceof ExprIdentNode);

        node = ((GroupByClauseElementExpr) groupByList.get(1)).getExpr();
        assertTrue(node instanceof ExprMathNode);
        assertTrue(node.getChildNodes()[0] instanceof ExprConstantNode);
        assertTrue(node.getChildNodes()[1] instanceof ExprIdentNode);

        node = ((GroupByClauseElementExpr) groupByList.get(2)).getExpr();
        assertTrue(node instanceof ExprMinMaxRowNode);
    }

    public void testHaving() throws Exception {
        String text = "select sum(intPrimitive) from SupportBean_N().win:lenght(10) as win1 where intBoxed > 5 " +
                "group by intBoxed having sum(intPrimitive) > 5";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);

        ExprNode havingNode = walker.getStatementSpec().getHavingExprRootNode();

        assertTrue(havingNode instanceof ExprRelationalOpNode);
        assertTrue(havingNode.getChildNodes()[0] instanceof ExprSumNode);
        assertTrue(havingNode.getChildNodes()[1] instanceof ExprConstantNode);

        text = "select sum(intPrimitive) from SupportBean_N().win:lenght(10) as win1 where intBoxed > 5 " +
                "having intPrimitive < avg(intPrimitive)";
        walker = SupportParserHelper.parseAndWalkEPL(text);

        havingNode = walker.getStatementSpec().getHavingExprRootNode();
        assertTrue(havingNode instanceof ExprRelationalOpNode);
    }

    public void testDistinct() throws Exception {
        String text = "select sum(distinct intPrimitive) from SupportBean_N().win:lenght(10) as win1";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);

        SelectClauseElementRaw rawElement = walker.getStatementSpec().getSelectClauseSpec().getSelectExprList().get(0);
        SelectClauseExprRawSpec exprSpec = (SelectClauseExprRawSpec) rawElement;
        ExprAggregateNodeBase aggrNode = (ExprAggregateNodeBase) exprSpec.getSelectExpression();
        assertTrue(aggrNode.isDistinct());
    }

    public void testComplexProperty() throws Exception {
        String text = "select array [ 1 ],s0.map('a'),nested.nested2, a[1].b as x, nested.abcdef? " +
                " from SupportBean_N().win:lenght(10) as win1 " +
                " where a[1].b('a').nested.c[0] = 4";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);

        ExprIdentNode identNode = (ExprIdentNode) getSelectExprSpec(walker.getStatementSpec(), 0).getSelectExpression();
        assertEquals("array[1]", identNode.getUnresolvedPropertyName());
        assertNull(identNode.getStreamOrPropertyName());

        identNode = (ExprIdentNode) getSelectExprSpec(walker.getStatementSpec(), 1).getSelectExpression();
        assertEquals("map('a')", identNode.getUnresolvedPropertyName());
        assertEquals("s0", identNode.getStreamOrPropertyName());

        identNode = (ExprIdentNode) getSelectExprSpec(walker.getStatementSpec(), 2).getSelectExpression();
        assertEquals("nested2", identNode.getUnresolvedPropertyName());
        assertEquals("nested", identNode.getStreamOrPropertyName());

        identNode = (ExprIdentNode) getSelectExprSpec(walker.getStatementSpec(), 3).getSelectExpression();
        assertEquals("a[1].b", identNode.getUnresolvedPropertyName());
        assertEquals(null, identNode.getStreamOrPropertyName());

        identNode = (ExprIdentNode) getSelectExprSpec(walker.getStatementSpec(), 4).getSelectExpression();
        assertEquals("abcdef?", identNode.getUnresolvedPropertyName());
        assertEquals("nested", identNode.getStreamOrPropertyName());

        identNode = (ExprIdentNode) walker.getStatementSpec().getFilterRootNode().getChildNodes()[0];
        assertEquals("a[1].b('a').nested.c[0]", identNode.getUnresolvedPropertyName());
        assertEquals(null, identNode.getStreamOrPropertyName());
    }

    public void testBitWise() throws Exception {
        String text = "select intPrimitive & intBoxed from " + SupportBean.class.getName() + "().win:lenght(10) as stream0";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);
        List<SelectClauseElementRaw> selectExpressions = walker.getStatementSpec().getSelectClauseSpec().getSelectExprList();
        assertEquals(1, selectExpressions.size());
        assertTrue(getSelectExprSpec(walker.getStatementSpec(), 0).getSelectExpression() instanceof ExprBitWiseNode);

        assertEquals(0, tryBitWise("1&2"));
        assertEquals(3, tryBitWise("1|2"));
        assertEquals(8, tryBitWise("10^2"));
    }

    public void testPatternsOnly() throws Exception {
        String patternOne = "a=" + SupportBean.class.getName() + " -> b=" + SupportBean.class.getName();

        // Test simple case, one pattern and no "as streamName"
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL("select * from pattern [" + patternOne + "]");
        assertEquals(1, walker.getStatementSpec().getStreamSpecs().size());
        PatternStreamSpecRaw patternStreamSpec = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);

        assertEquals(EvalFollowedByFactoryNode.class, patternStreamSpec.getEvalFactoryNode().getClass());
        assertNull(patternStreamSpec.getOptionalStreamName());

        // Test case with "as s0"
        walker = SupportParserHelper.parseAndWalkEPL("select * from pattern [" + patternOne + "] as s0");
        patternStreamSpec = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);
        assertEquals("s0", patternStreamSpec.getOptionalStreamName());

        // Test case with multiple patterns
        String patternTwo = "c=" + SupportBean.class.getName() + " or " + SupportBean.class.getName();
        walker = SupportParserHelper.parseAndWalkEPL("select * from pattern [" + patternOne + "] as s0, pattern [" + patternTwo + "] as s1");
        assertEquals(2, walker.getStatementSpec().getStreamSpecs().size());
        patternStreamSpec = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);
        assertEquals("s0", patternStreamSpec.getOptionalStreamName());
        assertEquals(EvalFollowedByFactoryNode.class, patternStreamSpec.getEvalFactoryNode().getClass());

        patternStreamSpec = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(1);
        assertEquals("s1", patternStreamSpec.getOptionalStreamName());
        assertEquals(EvalOrFactoryNode.class, patternStreamSpec.getEvalFactoryNode().getClass());

        // Test 3 patterns
        walker = SupportParserHelper.parseAndWalkEPL("select * from pattern [" + patternOne + "], pattern [" + patternTwo + "] as s1," +
                "pattern[x=" + SupportBean_S2.class.getName() + "] as s2");
        assertEquals(3, walker.getStatementSpec().getStreamSpecs().size());
        patternStreamSpec = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(2);
        assertEquals("s2", patternStreamSpec.getOptionalStreamName());

        // Test patterns with views
        walker = SupportParserHelper.parseAndWalkEPL("select * from pattern [" + patternOne + "]#time(1), pattern [" + patternTwo + "]#length(1)#lastevent as s1");
        assertEquals(2, walker.getStatementSpec().getStreamSpecs().size());
        patternStreamSpec = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);
        assertEquals(1, patternStreamSpec.getViewSpecs().length);
        assertEquals("time", patternStreamSpec.getViewSpecs()[0].getObjectName());
        patternStreamSpec = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(1);
        assertEquals(2, patternStreamSpec.getViewSpecs().length);
        assertEquals("length", patternStreamSpec.getViewSpecs()[0].getObjectName());
        assertEquals("lastevent", patternStreamSpec.getViewSpecs()[1].getObjectName());
    }

    public void testIfThenElseCase() throws Exception {
        String text;
        text = "select case when intPrimitive > shortPrimitive then count(intPrimitive) end from " + SupportBean.class.getName() + "().win:lenght(10) as win";
        SupportParserHelper.parseAndWalkEPL(text);
        text = "select case when intPrimitive > shortPrimitive then count(intPrimitive) end as p1 from " + SupportBean.class.getName() + "().win:lenght(10) as win";
        SupportParserHelper.parseAndWalkEPL(text);
        text = "select case when intPrimitive > shortPrimitive then count(intPrimitive) else shortPrimitive end from " + SupportBean.class.getName() + "().win:lenght(10) as win";
        SupportParserHelper.parseAndWalkEPL(text);
        text = "select case when intPrimitive > shortPrimitive then count(intPrimitive) when longPrimitive > intPrimitive then count(longPrimitive) else shortPrimitive end from " + SupportBean.class.getName() + "().win:lenght(10) as win";
        SupportParserHelper.parseAndWalkEPL(text);
        text = "select case intPrimitive  when 1 then count(intPrimitive) end from " + SupportBean.class.getName() + "().win:lenght(10) as win";
        SupportParserHelper.parseAndWalkEPL(text);
        text = "select case intPrimitive when longPrimitive then (intPrimitive + longPrimitive) end" +
                " from " + SupportBean.class.getName() + "#length(3)";
        SupportParserHelper.parseAndWalkEPL(text);
    }

    private void tryOuterJoin(String outerType, OuterJoinType typeExpected) throws Exception {
        String text = "select intPrimitive from " +
                SupportBean_A.class.getName() + "().win:lenght(10) as win1 " +
                outerType + " outer join " +
                SupportBean_A.class.getName() + "().win:lenght(10) as win2 " +
                "on win1.f1 = win2.f2[1]";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);

        List<OuterJoinDesc> descList = walker.getStatementSpec().getOuterJoinDescList();
        assertEquals(1, descList.size());
        OuterJoinDesc desc = descList.get(0);
        assertEquals(typeExpected, desc.getOuterJoinType());
        assertEquals("f1", desc.getOptLeftNode().getUnresolvedPropertyName());
        assertEquals("win1", desc.getOptLeftNode().getStreamOrPropertyName());
        assertEquals("f2[1]", desc.getOptRightNode().getUnresolvedPropertyName());
        assertEquals("win2", desc.getOptRightNode().getStreamOrPropertyName());

        text = "select intPrimitive from " +
                SupportBean_A.class.getName() + "().win:lenght(10) as win1 " +
                outerType + " outer join " +
                SupportBean_A.class.getName() + "().win:lenght(10) as win2 " +
                "on win1.f1 = win2.f2 " +
                outerType + " outer join " +
                SupportBean_A.class.getName() + "().win:lenght(10) as win3 " +
                "on win1.f1 = win3.f3 and win1.f11 = win3.f31";
        walker = SupportParserHelper.parseAndWalkEPL(text);

        descList = walker.getStatementSpec().getOuterJoinDescList();
        assertEquals(2, descList.size());

        desc = descList.get(0);
        assertEquals(typeExpected, desc.getOuterJoinType());
        assertEquals("f1", desc.getOptLeftNode().getUnresolvedPropertyName());
        assertEquals("win1", desc.getOptLeftNode().getStreamOrPropertyName());
        assertEquals("f2", desc.getOptRightNode().getUnresolvedPropertyName());
        assertEquals("win2", desc.getOptRightNode().getStreamOrPropertyName());

        desc = descList.get(1);
        assertEquals(typeExpected, desc.getOuterJoinType());
        assertEquals("f1", desc.getOptLeftNode().getUnresolvedPropertyName());
        assertEquals("win1", desc.getOptLeftNode().getStreamOrPropertyName());
        assertEquals("f3", desc.getOptRightNode().getUnresolvedPropertyName());
        assertEquals("win3", desc.getOptRightNode().getStreamOrPropertyName());

        assertEquals(1, desc.getAdditionalLeftNodes().length);
        assertEquals("f11", desc.getAdditionalLeftNodes()[0].getUnresolvedPropertyName());
        assertEquals("win1", desc.getAdditionalLeftNodes()[0].getStreamOrPropertyName());
        assertEquals(1, desc.getAdditionalRightNodes().length);
        assertEquals("f31", desc.getAdditionalRightNodes()[0].getUnresolvedPropertyName());
        assertEquals("win3", desc.getAdditionalRightNodes()[0].getStreamOrPropertyName());
    }

    public void testOnMerge() throws Exception {
        String text = "on MyEvent ev " +
                "merge MyWindow " +
                "where a not in (b) " +
                "when matched and y=100 " +
                "  then insert into xyz1 select g1,g2 where u>2" +
                "  then update set a=b where e like '%a' " +
                "  then delete where myvar " +
                "  then delete " +
                "when not matched and y=2 " +
                "  then insert into xyz select * where e=4" +
                "  then insert select * where t=2";

        StatementSpecRaw spec = SupportParserHelper.parseAndWalkEPL(text).getStatementSpec();
        OnTriggerMergeDesc merge = (OnTriggerMergeDesc) spec.getOnTriggerDesc();
        assertEquals(2, merge.getItems().size());
        assertTrue(spec.getFilterExprRootNode() instanceof ExprInNode);

        OnTriggerMergeMatched first = merge.getItems().get(0);
        assertEquals(4, first.getActions().size());
        assertTrue(first.isMatchedUnmatched());
        assertTrue(first.getOptionalMatchCond() instanceof ExprEqualsNode);

        OnTriggerMergeActionInsert insertOne = (OnTriggerMergeActionInsert) first.getActions().get(0);
        assertEquals("xyz1", insertOne.getOptionalStreamName());
        assertEquals(0, insertOne.getColumns().size());
        assertEquals(2, insertOne.getSelectClause().size());
        assertTrue(insertOne.getOptionalWhereClause() instanceof ExprRelationalOpNode);

        OnTriggerMergeActionUpdate updateOne = (OnTriggerMergeActionUpdate) first.getActions().get(1);
        assertEquals(1, updateOne.getAssignments().size());
        assertTrue(updateOne.getOptionalWhereClause() instanceof ExprLikeNode);

        OnTriggerMergeActionDelete delOne = (OnTriggerMergeActionDelete) first.getActions().get(2);
        assertTrue(delOne.getOptionalWhereClause() instanceof ExprIdentNode);

        OnTriggerMergeActionDelete delTwo = (OnTriggerMergeActionDelete) first.getActions().get(3);
        assertNull(delTwo.getOptionalWhereClause());

        OnTriggerMergeMatched second = merge.getItems().get(1);
        assertFalse(second.isMatchedUnmatched());
        assertTrue(second.getOptionalMatchCond() instanceof ExprEqualsNode);
        assertEquals(2, second.getActions().size());
    }

    public void testWalkPattern() throws Exception {
        String text = "every g=" + SupportBean.class.getName() + "(string=\"IBM\", intPrimitive != 1) where timer:within(20)";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkPattern(text);

        assertEquals(1, walker.getStatementSpec().getStreamSpecs().size());
        PatternStreamSpecRaw patternStreamSpec = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);

        EvalFactoryNode rootNode = patternStreamSpec.getEvalFactoryNode();

        EvalEveryFactoryNode everyNode = (EvalEveryFactoryNode) rootNode;

        assertEquals(1, everyNode.getChildNodes().size());
        assertTrue(everyNode.getChildNodes().get(0) instanceof EvalGuardFactoryNode);
        EvalGuardFactoryNode guardNode = (EvalGuardFactoryNode) everyNode.getChildNodes().get(0);

        assertEquals(1, guardNode.getChildNodes().size());
        assertTrue(guardNode.getChildNodes().get(0) instanceof EvalFilterFactoryNode);
        EvalFilterFactoryNode filterNode = (EvalFilterFactoryNode) guardNode.getChildNodes().get(0);

        assertEquals("g", filterNode.getEventAsName());
        assertEquals(0, filterNode.getChildNodes().size());
        assertEquals(2, filterNode.getRawFilterSpec().getFilterExpressions().size());
        ExprEqualsNode equalsNode = (ExprEqualsNode) filterNode.getRawFilterSpec().getFilterExpressions().get(1);
        assertEquals(2, equalsNode.getChildNodes().length);
    }

    public void testWalkPropertyPatternCombination() throws Exception {
        final String EVENT = SupportBeanComplexProps.class.getName();
        String property = tryWalkGetPropertyPattern(EVENT + "(mapped ( 'key' )  = 'value')");
        assertEquals("mapped('key')", property);

        property = tryWalkGetPropertyPattern(EVENT + "(indexed [ 1 ]  = 1)");
        assertEquals("indexed[1]", property);
        property = tryWalkGetPropertyPattern(EVENT + "(nested . nestedValue  = 'value')");
        assertEquals("nestedValue", property);
    }

    public void testWalkPatternUseResult() throws Exception {
        final String EVENT = SupportBean_N.class.getName();
        String text = "na=" + EVENT + "() -> every nb=" + EVENT + "(doublePrimitive in [0:na.doublePrimitive])";
        SupportParserHelper.parseAndWalkPattern(text);
    }

    public void testWalkIStreamRStreamSelect() throws Exception {
        String text = "select rstream 'a' from " + SupportBean_N.class.getName();
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);
        assertEquals(SelectClauseStreamSelectorEnum.RSTREAM_ONLY, walker.getStatementSpec().getSelectStreamSelectorEnum());

        text = "select istream 'a' from " + SupportBean_N.class.getName();
        walker = SupportParserHelper.parseAndWalkEPL(text);
        assertEquals(SelectClauseStreamSelectorEnum.ISTREAM_ONLY, walker.getStatementSpec().getSelectStreamSelectorEnum());

        text = "select 'a' from " + SupportBean_N.class.getName();
        walker = SupportParserHelper.parseAndWalkEPL(text);
        assertEquals(SelectClauseStreamSelectorEnum.ISTREAM_ONLY, walker.getStatementSpec().getSelectStreamSelectorEnum());

        text = "select irstream 'a' from " + SupportBean_N.class.getName();
        walker = SupportParserHelper.parseAndWalkEPL(text);
        assertEquals(SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH, walker.getStatementSpec().getSelectStreamSelectorEnum());
    }

    public void testWalkPatternNoPackage() throws Exception {
        SupportEventAdapterService.getService().addBeanType("SupportBean_N", SupportBean_N.class, true, true, true);
        String text = "na=SupportBean_N()";
        SupportParserHelper.parseAndWalkPattern(text);
    }

    public void testWalkPatternTypesValid() throws Exception {
        String text = SupportBean.class.getName();
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkPattern(text);
        assertEquals(1, walker.getStatementSpec().getStreamSpecs().size());
    }

    public void testWalkPatternIntervals() throws Exception {
        Object[][] intervals = {
                {"1E2 milliseconds", 0.1d},
                {"11 millisecond", 11 / 1000d},
                {"1.1 msec", 1.1 / 1000d},
                {"1 usec", 1 / 1000000d},
                {"1.1 microsecond", 1.1 / 1000000d},
                {"1E3 microsecond", 1000 / 1000000d},
                {"5 seconds 1 microsecond", 5d + 1 / 1000000d},
                {"5 seconds", 5d},
                {"0.1 second", 0.1d},
                {"135L sec", 135d},
                {"1.4 minutes", 1.4 * 60d},
                {"11 minute", 11 * 60d},
                {"123.2 min", 123.2 * 60d},
                {".2 hour", .2 * 60 * 60d},
                {"11.2 hours", 11.2 * 60 * 60d},
                {"2 day", 2 * 24 * 60 * 60d},
                {"11.2 days", 11.2 * 24 * 60 * 60d},
                {"0.2 day 3.3 hour 1E3 minute 0.33 second 10000 millisecond",
                        0.2d * 24 * 60 * 60 + 3.3d * 60 * 60 + 1E3 * 60 + 0.33 + 10000 / 1000},
                {"0.2 day 3.3 hour 1E3 min 0.33 sec 10000 msec",
                        0.2d * 24 * 60 * 60 + 3.3d * 60 * 60 + 1E3 * 60 + 0.33 + 10000 / 1000},
                {"1.01 hour 2 sec", 1.01d * 60 * 60 + 2},
                {"0.02 day 5 msec", 0.02d * 24 * 60 * 60 + 5 / 1000d},
                {"66 min 4 sec", 66 * 60 + 4d},
                {"1 days 6 hours 2 minutes 4 seconds 3 milliseconds",
                        1 * 24 * 60 * 60 + 6 * 60 * 60 + 2 * 60 + 4 + 3 / 1000d},
                {"1 year", 365 * 24 * 60 * 60d},
                {"1 month", 30 * 24 * 60 * 60d},
                {"1 week", 7 * 24 * 60 * 60d},
                {"2 years 3 month 10 week 2 days 6 hours 2 minutes 4 seconds 3 milliseconds 7 microseconds",
                        2 * 365 * 24 * 60 * 60d + 3 * 30 * 24 * 60 * 60d + 10 * 7 * 24 * 60 * 60d + 2 * 24 * 60 * 60 + 6 * 60 * 60 + 2 * 60 + 4 + 3 / 1000d + 7 / 1000000d},
        };

        for (int i = 0; i < intervals.length; i++) {
            String interval = (String) intervals[i][0];
            double result = tryInterval(interval);
            double expected = (Double) intervals[i][1];
            double delta = result - expected;
            assertTrue("Interval '" + interval + "' expected=" + expected + " actual=" + result, Math.abs(delta) < 0.0000001);
        }

        tryIntervalInvalid("1.5 month",
                "Time period expressions with month or year component require integer values, received a double value");
    }

    public void testWalkInAndBetween() throws Exception {
        assertTrue((Boolean) tryRelationalOp("1 between 0 and 2"));
        assertFalse((Boolean) tryRelationalOp("-1 between 0 and 2"));
        assertFalse((Boolean) tryRelationalOp("1 not between 0 and 2"));
        assertTrue((Boolean) tryRelationalOp("-1 not between 0 and 2"));

        assertFalse((Boolean) tryRelationalOp("1 in (2,3)"));
        assertTrue((Boolean) tryRelationalOp("1 in (2,3,1)"));
        assertTrue((Boolean) tryRelationalOp("1 not in (2,3)"));
    }

    public void testWalkLikeRegex() throws Exception {
        assertTrue((Boolean) tryRelationalOp("'abc' like 'a__'"));
        assertFalse((Boolean) tryRelationalOp("'abcd' like 'a__'"));

        assertFalse((Boolean) tryRelationalOp("'abcde' not like 'a%'"));
        assertTrue((Boolean) tryRelationalOp("'bcde' not like 'a%'"));

        assertTrue((Boolean) tryRelationalOp("'a_' like 'a!_' escape '!'"));
        assertFalse((Boolean) tryRelationalOp("'ab' like 'a!_' escape '!'"));

        assertFalse((Boolean) tryRelationalOp("'a' not like 'a'"));
        assertTrue((Boolean) tryRelationalOp("'a' not like 'ab'"));
    }

    public void testWalkDBJoinStatement() throws Exception {
        String className = SupportBean.class.getName();
        String sql = "select a from b where $x.id=c.d";
        String expression = "select * from " + className + ", sql:mydb ['" + sql + "']";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        StatementSpecRaw statementSpec = walker.getStatementSpec();
        assertEquals(2, statementSpec.getStreamSpecs().size());
        DBStatementStreamSpec dbSpec = (DBStatementStreamSpec) statementSpec.getStreamSpecs().get(1);
        assertEquals("mydb", dbSpec.getDatabaseName());
        assertEquals(sql, dbSpec.getSqlWithSubsParams());

        expression = "select * from " + className + ", sql:mydb ['" + sql + "' metadatasql 'select * from B']";

        walker = SupportParserHelper.parseAndWalkEPL(expression);
        statementSpec = walker.getStatementSpec();
        assertEquals(2, statementSpec.getStreamSpecs().size());
        dbSpec = (DBStatementStreamSpec) statementSpec.getStreamSpecs().get(1);
        assertEquals("mydb", dbSpec.getDatabaseName());
        assertEquals(sql, dbSpec.getSqlWithSubsParams());
        assertEquals("select * from B", dbSpec.getMetadataSQL());
    }

    public void testRangeBetweenAndIn() throws Exception {
        String className = SupportBean.class.getName();
        String expression = "select * from " + className + "(intPrimitive in [1:2], intBoxed in (1,2), doubleBoxed between 2 and 3)";
        SupportParserHelper.parseAndWalkEPL(expression);

        expression = "select * from " + className + "(intPrimitive not in [1:2], intBoxed not in (1,2), doubleBoxed not between 2 and 3)";
        SupportParserHelper.parseAndWalkEPL(expression);
    }

    public void testSubselect() throws Exception {
        String expression = "select (select a from B(id=1) where cox=mox) from C";
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        SelectClauseExprRawSpec element = getSelectExprSpec(walker.getStatementSpec(), 0);
        ExprSubselectNode exprNode = (ExprSubselectNode) element.getSelectExpression();

        // check select expressions
        StatementSpecRaw spec = exprNode.getStatementSpecRaw();
        assertEquals(1, spec.getSelectClauseSpec().getSelectExprList().size());

        // check filter
        assertEquals(1, spec.getStreamSpecs().size());
        FilterStreamSpecRaw filter = (FilterStreamSpecRaw) spec.getStreamSpecs().get(0);
        assertEquals("B", filter.getRawFilterSpec().getEventTypeName());
        assertEquals(1, filter.getRawFilterSpec().getFilterExpressions().size());

        // check where clause
        assertTrue(spec.getFilterRootNode() instanceof ExprEqualsNode);
    }

    public void testWalkPatternObject() throws Exception {
        String expression = "select * from pattern [" + SupportBean.class.getName() + " -> timer:interval(100)]";
        SupportParserHelper.parseAndWalkEPL(expression);

        expression = "select * from pattern [" + SupportBean.class.getName() + " where timer:within(100)]";
        SupportParserHelper.parseAndWalkEPL(expression);
    }

    private void tryIntervalInvalid(String interval, String message) {
        try {
            tryInterval(interval);
            fail();
        } catch (Exception ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private double tryInterval(String interval) throws Exception {
        String text = "select * from " + SupportBean.class.getName() + "#win:time(" + interval + ")";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(text);
        ViewSpec viewSpec = walker.getStatementSpec().getStreamSpecs().get(0).getViewSpecs()[0];
        assertEquals("win", viewSpec.getObjectNamespace());
        assertEquals("time", viewSpec.getObjectName());
        assertEquals(1, viewSpec.getObjectParameters().size());
        ExprTimePeriod exprNode = (ExprTimePeriod) viewSpec.getObjectParameters().get(0);
        exprNode.validate(SupportExprValidationContextFactory.makeEmpty());
        return exprNode.evaluateAsSeconds(null, true, null);
    }

    private String tryWalkGetPropertyPattern(String stmt) throws Exception {
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkPattern(stmt);

        assertEquals(1, walker.getStatementSpec().getStreamSpecs().size());
        PatternStreamSpecRaw patternStreamSpec = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);

        EvalFilterFactoryNode filterNode = (EvalFilterFactoryNode) patternStreamSpec.getEvalFactoryNode();
        assertEquals(1, filterNode.getRawFilterSpec().getFilterExpressions().size());
        ExprNode node = filterNode.getRawFilterSpec().getFilterExpressions().get(0);
        ExprIdentNode identNode = (ExprIdentNode) node.getChildNodes()[0];
        return identNode.getUnresolvedPropertyName();
    }

    private Object tryBitWise(String equation) throws Exception {
        String expression = EXPRESSION + "where (" + equation + ")=win2.f2";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        ExprNode exprNode = walker.getStatementSpec().getFilterRootNode().getChildNodes()[0];
        ExprBitWiseNode bitWiseNode = (ExprBitWiseNode) (exprNode);
        ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, bitWiseNode, SupportExprValidationContextFactory.makeEmpty());
        return bitWiseNode.getForge().getExprEvaluator().evaluate(null, false, null);
    }

    private Object tryExpression(String equation) throws Exception {
        String expression = EXPRESSION + "where " + equation + "=win2.f2";

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        ExprNode exprNode = (walker.getStatementSpec().getFilterRootNode().getChildNodes()[0]);
        exprNode = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, exprNode, SupportExprValidationContextFactory.makeEmpty());
        return exprNode.getForge().getExprEvaluator().evaluate(null, false, null);
    }

    private Object tryRelationalOp(String subExpr) throws Exception {
        String expression = EXPRESSION + "where " + subExpr;

        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        ExprNode filterExprNode = walker.getStatementSpec().getFilterRootNode();
        ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, filterExprNode, SupportExprValidationContextFactory.makeEmpty());
        return filterExprNode.getForge().getExprEvaluator().evaluate(null, false, null);
    }

    private SelectClauseExprRawSpec getSelectExprSpec(StatementSpecRaw statementSpec, int index) {
        SelectClauseElementRaw raw = statementSpec.getSelectClauseSpec().getSelectExprList().get(index);
        return (SelectClauseExprRawSpec) raw;
    }

    private static final Logger log = LoggerFactory.getLogger(TestEPLTreeWalker.class);
}
