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
package com.espertech.esper.common.internal.serde.compiletime.resolve;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

public class DataInputOutputSerdeForgeParameterizedVars {
    private final CodegenMethod method;
    private final CodegenClassScope scope;
    private final CodegenExpression optionalEventTypeResolver;

    public DataInputOutputSerdeForgeParameterizedVars(CodegenMethod method, CodegenClassScope scope, CodegenExpression optionalEventTypeResolver) {
        this.method = method;
        this.scope = scope;
        this.optionalEventTypeResolver = optionalEventTypeResolver;
    }

    public CodegenMethod getMethod() {
        return method;
    }

    public CodegenClassScope getScope() {
        return scope;
    }

    public CodegenExpression getOptionalEventTypeResolver() {
        return optionalEventTypeResolver;
    }
}
