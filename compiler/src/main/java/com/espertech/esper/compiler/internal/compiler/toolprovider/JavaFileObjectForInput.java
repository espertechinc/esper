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

import com.espertech.esper.common.internal.serde.serdeset.builtin.FastByteArrayInputStream;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class JavaFileObjectForInput extends SimpleJavaFileObject {
    private final String classNameFull;
    private final FastByteArrayInputStream fis;

    public JavaFileObjectForInput(URI uri, Kind kind, String classNameFull, FastByteArrayInputStream fis) {
        super(uri, kind);
        this.classNameFull = classNameFull;
        this.fis = fis;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return fis;
    }

    public String getClassNameFull() {
        return classNameFull;
    }
}
