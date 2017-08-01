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
package com.espertech.esper.regression.script;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.util.ClassForNameProviderDefault;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.JavaClassHelper;

public class ExecScriptSandboxMVEL implements RegressionExecution {

    // Comment-in for MVEL support custom testing.
    public void run(EPServiceProvider epService) throws Exception {

        if (JavaClassHelper.getClassInClasspath("org.mvel2.MVEL", ClassForNameProviderDefault.INSTANCE) == null) {
            return;
        }

        /* Comment-me-in:
        String expression = "new MyImportedClass()";

        Map<String, Class> inputs = new HashMap<String, Class>();
        inputs.put("epl", MyEPLContext.class);

        // analysis
        ParserContext analysisResult = new ParserContext();
        analysisResult.setStrongTyping(true);
        analysisResult.setInputs(inputs);
        analysisResult.addImport(MyImportedClass.class);
        MVEL.analysisCompile(expression, analysisResult);
        System.out.println(inputs);

        // compile
        ParserContext compileResult = new ParserContext();
        compileResult.setStrongTyping(true);
        compileResult.addImport(MyImportedClass.class);
        ExecutableStatement ce = (ExecutableStatement) MVEL.compileExpression(expression, compileResult);

        // execute
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("epl", new MyEPLContext());
        System.out.println(MVEL.executeExpression(ce, params));

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            MVEL.executeExpression(ce, params);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        System.out.println("delta=" + delta);
         */
    }

    public static class MyEPLContext {
        public Long getVariable(String name) {
            return 50L;
        }
    }
}
