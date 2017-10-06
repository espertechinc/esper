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
package com.espertech.esper.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class ObjectInputStreamWithTCCL extends ObjectInputStream {

    private final static Logger log = LoggerFactory.getLogger(ObjectInputStreamWithTCCL.class);

    public ObjectInputStreamWithTCCL(InputStream input) throws IOException {
        super(input);
    }

    public ObjectInputStreamWithTCCL() throws IOException, SecurityException {
    }

    @Override
    public Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {

        if (log.isDebugEnabled()) {
            log.debug("Resolving class " + desc.getName() + " id " + desc.getSerialVersionUID() + " classloader " + Thread.currentThread().getContextClassLoader().getClass());
        }

        ClassLoader currentTccl = null;
        try {
            currentTccl = Thread.currentThread().getContextClassLoader();
            if (currentTccl != null) {
                return currentTccl.loadClass(desc.getName());
            }
        } catch (Exception e) {
        }
        return super.resolveClass(desc);
    }
}
