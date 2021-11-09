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

import com.espertech.esper.common.internal.serde.serdeset.builtin.FastByteArrayOutputStream;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class JavaFileObjectForOutput extends SimpleJavaFileObject {
    private final FastByteArrayOutputStream fos;

    public JavaFileObjectForOutput(URI uri, Kind kind, FastByteArrayOutputStream fos) {
        super(uri, kind);
        this.fos = fos;
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return fos;
    }
}
