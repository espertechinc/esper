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
package com.espertech.esper.common.internal.event.bean.instantiator;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class BeanInstantiatorForgeByNewInstanceReflection implements BeanInstantiatorForge, BeanInstantiator {
    private final static Logger log = LoggerFactory.getLogger(BeanInstantiatorForgeByNewInstanceReflection.class);

    private final Class clazz;

    public BeanInstantiatorForgeByNewInstanceReflection(Class clazz) {
        this.clazz = clazz;
    }

    public Object instantiate() {
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException e) {
            return handle(e);
        } catch (InstantiationException e) {
            return handle(e);
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return newInstance(clazz);
    }

    public BeanInstantiator getBeanInstantiator() {
        return this;
    }

    private Object handle(Exception e) {
        String message = "Unexpected exception encountered invoking newInstance on class '" + clazz.getName() + "': " + e.getMessage();
        log.error(message, e);
        return null;
    }
}
