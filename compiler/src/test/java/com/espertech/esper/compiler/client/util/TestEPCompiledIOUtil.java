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
package com.espertech.esper.compiler.client.util;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.FileUtil;
import com.espertech.esper.common.internal.util.UuidGenerator;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.TreeSet;

public class TestEPCompiledIOUtil extends TestCase {
    private final static String IODIR = "regression/data";

    public void testWrite() {
        EPCompiled compiled = getCompiled();
        File jarFile = getJarFile();

        EPCompiled reread;
        boolean deleted = true;
        try {
            EPCompiledIOUtil.write(compiled, jarFile);

            reread = EPCompiledIOUtil.read(jarFile);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        } finally {
            deleted = jarFile.delete();
        }
        if (!deleted) {
            fail("File could not be deleted for file '" + jarFile + "'");
        }

        TreeSet<String> classNames = new TreeSet<>(compiled.getClasses().keySet());
        assertEquals(reread.getClasses().size(), compiled.getClasses().size());
        for (String name : classNames) {
            byte[] in = compiled.getClasses().get(name);
            byte[] back = reread.getClasses().get(name);
            assertTrue("Failed for " + name, Arrays.equals(in, back));
        }
        assertEquals(compiled.getManifest().getModuleProviderClassName(), reread.getManifest().getModuleProviderClassName());
        assertEquals(compiled.getManifest().getCompilerVersion(), reread.getManifest().getCompilerVersion());
        assertNull(reread.getManifest().getQueryProviderClassName());
    }


    private File getJarFile() {
        String path = FileUtil.findClasspathFile(IODIR);
        if (path == null) {
            fail("Failed to find file " + IODIR + " in classpath");
        }

        String filename = "compiled-" + UuidGenerator.generate() + ".jar";
        File file = new File(path, filename);
        if (file.exists()) {
            fail("File already exists for file '" + file + "'");
        }

        return file;
    }

    private EPCompiled getCompiled() {
        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(SupportBean.class);
        CompilerArguments args = new CompilerArguments(configuration);

        try {
            return EPCompilerProvider.getCompiler().compile("select * from SupportBean", args);
        } catch (EPCompileException ex) {
            throw new RuntimeException(ex);
        }
    }
}
