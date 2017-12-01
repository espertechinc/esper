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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.core.ExprVariableNodeImpl;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableServiceImpl;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import junit.framework.TestCase;

public class TestExprVariableNode extends TestCase {
    private ExprVariableNodeImpl varNode;
    private VariableService variableService;

    public void setUp() throws Exception {
        EngineImportService engineImportService = SupportEngineImportServiceFactory.make();
        variableService = new VariableServiceImpl(100, null, null, null);
        variableService.createNewVariable(null, "var1", "string", true, false, false, null, engineImportService);
        variableService.createNewVariable(null, "dummy", "string", true, false, false, null, engineImportService);
        variableService.createNewVariable(null, "intPrimitive", "int", true, false, false, null, engineImportService);
        varNode = new ExprVariableNodeImpl(variableService.getVariableMetaData("var1"), null);
    }

    public void testGetType() throws Exception {
        SupportExprNodeFactory.validate3Stream(varNode);
        assertEquals(String.class, varNode.getConstantType());
    }

    public void testEvaluate() throws Exception {
        SupportExprNodeFactory.validate3Stream(varNode);
        assertEquals("my_variable_value", varNode.evaluate(null, true, null));
    }

    public void testEquals() throws Exception {
        ExprInNode otherInNode = SupportExprNodeFactory.makeInSetNode(false);
        ExprVariableNodeImpl otherVarOne = new ExprVariableNodeImpl(variableService.getVariableMetaData("dummy"), null);
        ExprVariableNodeImpl otherVarTwo = new ExprVariableNodeImpl(variableService.getVariableMetaData("var1"), null);
        ExprVariableNodeImpl otherVarThree = new ExprVariableNodeImpl(variableService.getVariableMetaData("var1"), "abc");

        assertTrue(varNode.equalsNode(varNode, false));
        assertTrue(varNode.equalsNode(otherVarTwo, false));
        assertFalse(varNode.equalsNode(otherVarOne, false));
        assertFalse(varNode.equalsNode(otherInNode, false));
        assertFalse(otherVarTwo.equalsNode(otherVarThree, false));
    }

    public void testToExpressionString() throws Exception {
        assertEquals("var1", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(varNode));
    }

    private void tryInvalidValidate(ExprVariableNodeImpl varNode) throws Exception {
        try {
            SupportExprNodeFactory.validate3Stream(varNode);
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }
    }
}
