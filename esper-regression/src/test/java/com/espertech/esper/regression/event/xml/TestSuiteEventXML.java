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
package com.espertech.esper.regression.event.xml;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteEventXML extends TestCase {
    public void testExecEventXMLNoSchemaEventTransposeXPathConfigured() {
        RegressionRunner.run(new ExecEventXMLNoSchemaEventTransposeXPathConfigured());
    }

    public void testExecEventXMLNoSchemaEventTransposeXPathGetter() {
        RegressionRunner.run(new ExecEventXMLNoSchemaEventTransposeXPathGetter());
    }

    public void testExecEventXMLNoSchemaEventTransposeDOM() {
        RegressionRunner.run(new ExecEventXMLNoSchemaEventTransposeDOM());
    }

    public void testExecEventXMLSchemaPropertyDynamicXPathGetter() {
        RegressionRunner.run(new ExecEventXMLSchemaPropertyDynamicXPathGetter());
    }

    public void testExecEventXMLSchemaEventObservationDOM() {
        RegressionRunner.run(new ExecEventXMLSchemaEventObservationDOM());
    }

    public void testExecEventXMLSchemaEventObservationXPath() {
        RegressionRunner.run(new ExecEventXMLSchemaEventObservationXPath());
    }

    public void testExecEventXMLSchemaEventReplace() {
        RegressionRunner.run(new ExecEventXMLSchemaEventReplace());
    }

    public void testExecEventXMLSchemaEventSender() {
        RegressionRunner.run(new ExecEventXMLSchemaEventSender());
    }

    public void testExecEventXMLSchemaEventTransposeDOMGetter() {
        RegressionRunner.run(new ExecEventXMLSchemaEventTransposeDOMGetter());
    }

    public void testExecEventXMLSchemaEventTransposeXPathConfigured() {
        RegressionRunner.run(new ExecEventXMLSchemaEventTransposeXPathConfigured());
    }

    public void testExecEventXMLSchemaEventTransposeXPathGetter() {
        RegressionRunner.run(new ExecEventXMLSchemaEventTransposeXPathGetter());
    }

    public void testExecEventXMLSchemaEventTransposePrimitiveArray() {
        RegressionRunner.run(new ExecEventXMLSchemaEventTransposePrimitiveArray());
    }

    public void testExecEventXMLSchemaEventTransposeNodeArray() {
        RegressionRunner.run(new ExecEventXMLSchemaEventTransposeNodeArray());
    }

    public void testExecEventXMLSchemaEventTypes() {
        RegressionRunner.run(new ExecEventXMLSchemaEventTypes());
    }

    public void testExecEventXMLSchemaWithRestriction() {
        RegressionRunner.run(new ExecEventXMLSchemaWithRestriction());
    }

    public void testExecEventXMLSchemaWithAll() {
        RegressionRunner.run(new ExecEventXMLSchemaWithAll());
    }

    public void testExecEventXMLSchemaDOMGetterBacked() {
        RegressionRunner.run(new ExecEventXMLSchemaDOMGetterBacked());
    }

    public void testExecEventXMLSchemaXPathBacked() {
        RegressionRunner.run(new ExecEventXMLSchemaXPathBacked());
    }

    public void testExecEventXMLSchemaAddRemoveType() {
        RegressionRunner.run(new ExecEventXMLSchemaAddRemoveType());
    }

    public void testExecEventXMLSchemaInvalid() {
        RegressionRunner.run(new ExecEventXMLSchemaInvalid());
    }

    public void testExecEventXMLNoSchemaVariableAndDotMethodResolution() {
        RegressionRunner.run(new ExecEventXMLNoSchemaVariableAndDotMethodResolution());
    }

    public void testExecEventXMLNoSchemaSimpleXMLXPathProperties() {
        RegressionRunner.run(new ExecEventXMLNoSchemaSimpleXMLXPathProperties());
    }

    public void testExecEventXMLNoSchemaSimpleXMLDOMGetter() {
        RegressionRunner.run(new ExecEventXMLNoSchemaSimpleXMLDOMGetter());
    }

    public void testExecEventXMLNoSchemaSimpleXMLXPathGetter() {
        RegressionRunner.run(new ExecEventXMLNoSchemaSimpleXMLXPathGetter());
    }

    public void testExecEventXMLNoSchemaNestedXMLDOMGetter() {
        RegressionRunner.run(new ExecEventXMLNoSchemaNestedXMLDOMGetter());
    }

    public void testExecEventXMLNoSchemaNestedXMLXPathGetter() {
        RegressionRunner.run(new ExecEventXMLNoSchemaNestedXMLXPathGetter());
    }

    public void testExecEventXMLNoSchemaDotEscape() {
        RegressionRunner.run(new ExecEventXMLNoSchemaDotEscape());
    }

    public void testExecEventXMLNoSchemaEventXML() {
        RegressionRunner.run(new ExecEventXMLNoSchemaEventXML());
    }

    public void testExecEventXMLNoSchemaElementNode() {
        RegressionRunner.run(new ExecEventXMLNoSchemaElementNode());
    }

    public void testExecEventXMLNoSchemaNamespaceXPathRelative() {
        RegressionRunner.run(new ExecEventXMLNoSchemaNamespaceXPathRelative());
    }

    public void testExecEventXMLNoSchemaNamespaceXPathAbsolute() {
        RegressionRunner.run(new ExecEventXMLNoSchemaNamespaceXPathAbsolute());
    }

    public void testExecEventXMLNoSchemaXPathArray() {
        RegressionRunner.run(new ExecEventXMLNoSchemaXPathArray());
    }

    public void testExecEventXMLNoSchemaPropertyDynamicDOMGetter() {
        RegressionRunner.run(new ExecEventXMLNoSchemaPropertyDynamicDOMGetter());
    }

    public void testExecEventXMLNoSchemaPropertyDynamicXPathGetter() {
        RegressionRunner.run(new ExecEventXMLNoSchemaPropertyDynamicXPathGetter());
    }

    public void testExecEventXMLSchemaPropertyDynamicDOMGetter() {
        RegressionRunner.run(new ExecEventXMLSchemaPropertyDynamicDOMGetter());
    }
}
