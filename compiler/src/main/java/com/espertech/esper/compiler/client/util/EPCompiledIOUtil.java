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
import com.espertech.esper.common.client.EPCompiledManifest;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.*;

/**
 * IO-related utilized for {@link EPCompiled}
 */
public class EPCompiledIOUtil {
    /**
     * Name of the attribute providing the compiler version.
     */
    public final static String MANIFEST_COMPILER_VERSION = "Esper-CompilerVersion";

    /**
     * Name of the attribute providing the module provider class name.
     */
    public final static String MANIFEST_MODULEPROVIDERCLASSNAME = "Esper-ModuleProvider";

    /**
     * Name of the attribute providing the fire-and-forget query provider class name.
     */
    public final static String MANIFEST_QUERYPROVIDERCLASSNAME = "Esper-QueryProvider";

    /**
     * Name of the attribute providing the flag whether the compiler targets high-availability.
     */
    public final static String MANIFEST_TARGETHA = "Esper-TargetHA";

    /**
     * Write the compiled to a jar file. Overwrites the existing jar file.
     *
     * @param compiled compiled
     * @param file     the target file
     * @throws IOException when the write failed
     */
    public static void write(EPCompiled compiled, File file) throws IOException {

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name(MANIFEST_COMPILER_VERSION), compiled.getManifest().getCompilerVersion());
        manifest.getMainAttributes().put(new Attributes.Name(MANIFEST_MODULEPROVIDERCLASSNAME), compiled.getManifest().getModuleProviderClassName());
        manifest.getMainAttributes().put(new Attributes.Name(MANIFEST_QUERYPROVIDERCLASSNAME), compiled.getManifest().getQueryProviderClassName());
        if (compiled.getManifest().isTargetHA()) {
            manifest.getMainAttributes().put(new Attributes.Name(MANIFEST_TARGETHA), "true");
        }

        JarOutputStream target = new JarOutputStream(new FileOutputStream(file), manifest);

        try {
            for (Map.Entry<String, byte[]> entry : compiled.getClasses().entrySet()) {
                write(entry.getKey(), entry.getValue(), target);
            }
        } finally {
            target.close();
        }
    }

    /**
     * Reads the jar file into an {@link EPCompiled} compiled for deployment into a runtime.
     *
     * @param file is the source jar file
     * @return compiled
     * @throws IOException when the read failed
     */
    public static EPCompiled read(File file) throws IOException {
        JarFile jarFile = new JarFile(file);

        Attributes attributes = jarFile.getManifest().getMainAttributes();
        String compilerVersion = getAttribute(attributes, MANIFEST_COMPILER_VERSION);
        if (compilerVersion == null) {
            throw new IOException("Manifest is missing " + MANIFEST_COMPILER_VERSION);
        }
        String moduleProvider = getAttribute(attributes, MANIFEST_MODULEPROVIDERCLASSNAME);
        String queryProvider = getAttribute(attributes, MANIFEST_QUERYPROVIDERCLASSNAME);
        if (moduleProvider == null && queryProvider == null) {
            throw new IOException("Manifest is missing both " + MANIFEST_MODULEPROVIDERCLASSNAME + " and " + MANIFEST_QUERYPROVIDERCLASSNAME);
        }
        String targetHAStr = getAttribute(attributes, MANIFEST_TARGETHA);
        boolean targetHA = targetHAStr != null && Boolean.parseBoolean(targetHAStr);

        Map<String, byte[]> classes = new HashMap<>();
        try {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                read(jarFile, entries.nextElement(), classes);
            }

        } finally {
            jarFile.close();
        }

        return new EPCompiled(classes, new EPCompiledManifest(compilerVersion, moduleProvider, queryProvider, targetHA));
    }

    private static String getAttribute(Attributes attributes, String name) {
        Attributes.Name attr = new Attributes.Name(name);
        String value = attributes.getValue(attr);
        if (value == null || value.equals("null")) {
            return null;
        }
        return value;
    }

    private static void write(String name, byte[] value, JarOutputStream target) throws IOException {
        name = name.replace(".", "/") + ".class";
        JarEntry entry = new JarEntry(name);
        entry.setTime(System.currentTimeMillis());
        target.putNextEntry(entry);
        target.write(value, 0, value.length);
        target.closeEntry();
    }

    private static void read(JarFile jarFile, JarEntry jarEntry, Map<String, byte[]> classes) throws IOException {
        if (jarEntry.isDirectory() || jarEntry.getName().equals("META-INF/MANIFEST.MF")) {
            return;
        }

        long size = jarEntry.getSize();
        if (size > Integer.MAX_VALUE - 1) {
            throw new IOException("Encountered jar entry with size " + size + " greater than max integer size");
        }

        InputStream in = jarFile.getInputStream(jarEntry);
        byte[] bytes;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            bytes = os.toByteArray();
        } finally {
            in.close();
        }

        String className = jarEntry.getName().replace("/", ".");
        if (className.endsWith(".class")) {
            className = className.substring(0, className.length() - 6);
        }
        classes.put(className, bytes);
    }
}
