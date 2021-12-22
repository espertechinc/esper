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

import com.espertech.esper.common.internal.compile.compiler.CompilerAbstractionClassCollection;
import com.espertech.esper.common.internal.serde.serdeset.builtin.FastByteArrayInputStream;
import com.espertech.esper.common.internal.serde.serdeset.builtin.FastByteArrayOutputStream;

import javax.tools.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class JavaFileManagerForwarding extends ForwardingJavaFileManager<JavaFileManager> {
    private final String generatedCodePackageName;
    private final Map<String, byte[]> classpath;
    private final Map<String, FastByteArrayOutputStream> classes = new HashMap<>();
    private final Map<String, List<String>> codeToClassNames = new LinkedHashMap<>();

    public JavaFileManagerForwarding(String generatedCodePackageName, JavaFileManager fileManager, Map<String, byte[]> classpath) {
        super(fileManager);
        this.generatedCodePackageName = generatedCodePackageName;
        this.classpath = classpath;
    }

    @Override
    /**
     * Note: classes in the default package cannot be imported/used by Java classes in a package.
     * Since all code generated lives in a preconfigured package, that package becomes part of classpath.
     */
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        if (!location.equals(StandardLocation.CLASS_PATH) || classpath.isEmpty() || !packageName.equals(generatedCodePackageName)) {
            return super.list(location, packageName, kinds, recurse);
        }
        List<JavaFileObject> cp = new ArrayList<>(classpath.size());
        String prefix = generatedCodePackageName + ".";
        for (Map.Entry<String, byte[]> entry : classpath.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            String filename = entry.getKey() + ".class";
            filename = filename.replace(prefix, "");
            URI fileURI = URI.create(filename);
            FastByteArrayInputStream fis = new FastByteArrayInputStream(entry.getValue());
            cp.add(new JavaFileObjectForInput(fileURI, JavaFileObject.Kind.CLASS, entry.getKey(), fis));
        }
        return cp;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof JavaFileObjectForInput) {
            JavaFileObjectForInput input = (JavaFileObjectForInput) file;
            return input.getClassNameFull();
        }
        return super.inferBinaryName(location, file);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        String filename = className + ".class";
        URI fileURI = URI.create(filename);
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        classes.put(className, fos);

        // keep track of code resulting in class name
        JavaFileObjectSource src = (JavaFileObjectSource) sibling;
        List<String> classNames = codeToClassNames.get(src.getCode());
        if (classNames == null) {
            classNames = new ArrayList<>(2);
            classNames.add(className);
            codeToClassNames.put(src.getCode(), classNames);
        } else {
            if (!classNames.contains(className)) {
                classNames.add(className);
            }
        }

        return new JavaFileObjectForOutput(fileURI, kind, fos);
    }

    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
        return super.getFileForOutput(location, packageName, relativeName, sibling);
    }

    public void addClasses(CompilerAbstractionClassCollection state) {
        for (Map.Entry<String, FastByteArrayOutputStream> entry : classes.entrySet()) {
            state.getClasses().put(entry.getKey(), entry.getValue().getByteArrayFast());
        }
    }

    public Map<String, List<String>> getClassNamesProduced() {
        return codeToClassNames;
    }
}
