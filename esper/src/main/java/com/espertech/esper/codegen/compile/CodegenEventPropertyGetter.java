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
package com.espertech.esper.codegen.compile;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.util.ClassLoaderProvider;
import com.espertech.esper.codegen.core.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventPropertyGetterSPI;

import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class CodegenEventPropertyGetter {
    public static EventPropertyGetter compile(String engineURI, ClassLoaderProvider classLoaderProvider, EventPropertyGetterSPI getterSPI, String propertyExpression) {
        CodegenContext codegenContext = new CodegenContext();
        CodegenExpression get = getterSPI.codegenEventBeanGet(ref("bean"), codegenContext);
        CodegenExpression exists = getterSPI.codegenEventBeanExists(ref("bean"), codegenContext);
        CodegenExpression fragment = getterSPI.codegenEventBeanFragment(ref("bean"), codegenContext);

        List<CodegenNamedParam> singleBeanParam = CodegenNamedParam.from(EventBean.class, "bean");

        // For: public Object get(EventBean eventBean) throws PropertyAccessException;
        // For: public boolean isExistsProperty(EventBean eventBean);
        // For: public Object getFragment(EventBean eventBean) throws PropertyAccessException;
        CodegenMethod getMethod = new CodegenMethod(Object.class, "get", singleBeanParam, null);
        getMethod.statements().methodReturn(get);
        CodegenMethod isExistsPropertyMethod = new CodegenMethod(boolean.class, "isExistsProperty", singleBeanParam, null);
        isExistsPropertyMethod.statements().methodReturn(exists);
        CodegenMethod fragmentMethod = new CodegenMethod(Object.class, "getFragment", singleBeanParam, null);
        fragmentMethod.statements().methodReturn(fragment);

        CodegenClass clazz = new CodegenClass(
                "com.espertech.esper.codegen.uri_" + engineURI,
                EventPropertyGetter.class.getSimpleName() + "_" + CodeGenerationIDGenerator.generateClass(),
                EventPropertyGetter.class,
                codegenContext.getMembers(),
                Arrays.asList(getMethod, isExistsPropertyMethod, fragmentMethod),
                codegenContext.getMethods()
        );

        String debugInfo = null;
        if (CodegenCompiler.DEBUG) {
            debugInfo = getterSPI.getClass().getName() + " for property '" + propertyExpression + "'";
        }
        return CodegenCompiler.compile(clazz, classLoaderProvider, EventPropertyGetter.class, debugInfo);
    }
}
