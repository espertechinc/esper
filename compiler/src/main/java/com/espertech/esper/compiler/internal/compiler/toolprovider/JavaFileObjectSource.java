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
package com.espertech.esper.compiler.internal.compiler.toolprovider;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;

public class JavaFileObjectSource extends SimpleJavaFileObject {
    private final String code;

    public JavaFileObjectSource(URI uri, Kind kind, String code) {
        super(uri, kind);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return code;
    }

    public String getCode() {
        return code;
    }
}
