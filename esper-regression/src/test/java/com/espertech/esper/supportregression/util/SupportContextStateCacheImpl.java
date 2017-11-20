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
package com.espertech.esper.supportregression.util;

import com.espertech.esper.client.context.ContextPartitionState;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.context.mgr.*;
import com.espertech.esper.epl.spec.ContextDetailInitiatedTerminated;
import com.espertech.esper.epl.spec.ContextDetailPartitioned;
import org.junit.Assert;

import java.util.*;

public class SupportContextStateCacheImpl implements ContextStateCache {

    private static Map<ContextStatePathKey, ContextStatePathValue> state = new HashMap<ContextStatePathKey, ContextStatePathValue>();
    private static Set<ContextStatePathKey> removedState = new HashSet<ContextStatePathKey>();

    public static void reset() {
        state.clear();
        removedState.clear();
    }

    public static void assertState(ContextState... descs) {
        Assert.assertEquals(descs.length, state.size());
        int count = -1;
        for (ContextState desc : descs) {
            count++;
            String text = "failed at descriptor " + count;
            ContextStatePathValue value = state.get(new ContextStatePathKey(desc.getLevel(), desc.getParentPath(), desc.getSubpath()));
            Assert.assertEquals(text, desc.getAgentInstanceId(), (int) value.getOptionalContextPartitionId());
            Assert.assertEquals(text, desc.isStarted() ? ContextPartitionState.STARTED : ContextPartitionState.STOPPED, value.getState());

            Object payloadReceived = ContextStateCacheNoSave.DEFAULT_SPI_TEST_BINDING.byteArrayToObject(value.getBlob(), null);
            if (desc.getPayload() == null) {
                Assert.assertNotNull(payloadReceived);
            } else {
                if (payloadReceived instanceof ContextControllerPartitionedState) {
                    payloadReceived = ((ContextControllerPartitionedState) payloadReceived).getPartitionKey();
                }
                EPAssertionUtil.assertEqualsAllowArray(text, desc.getPayload(), payloadReceived);
            }
        }
    }

    public static void assertRemovedState(ContextStatePathKey... keys) {
        Assert.assertEquals(keys.length, removedState.size());
        int count = -1;
        for (ContextStatePathKey key : keys) {
            count++;
            String text = "failed at descriptor " + count;
            Assert.assertTrue(text, removedState.contains(key));
        }
    }

    public ContextStatePathValueBinding getBinding(Object bindingInfo) {
        if (bindingInfo instanceof ContextDetailInitiatedTerminated) {
            return new ContextStateCacheNoSave.ContextStateCacheNoSaveInitTermBinding();
        }
        if (bindingInfo == ContextControllerPartitionedState.class) {
            return new ContextStateCacheNoSave.ContextStateCacheNoSavePartitionBinding();
        }
        return ContextStateCacheNoSave.DEFAULT_SPI_TEST_BINDING;
    }

    public void addContextPath(String contextName, int level, int parentPath, int subPath, Integer optionalContextPartitionId, Object additionalInfo, ContextStatePathValueBinding binding) {
        state.put(new ContextStatePathKey(level, parentPath, subPath), new ContextStatePathValue(optionalContextPartitionId, binding.toByteArray(additionalInfo), ContextPartitionState.STARTED));
    }

    public void updateContextPath(String contextName, ContextStatePathKey key, ContextStatePathValue value) {
        state.put(key, value);
    }

    public void removeContextParentPath(String contextName, int level, int parentPath) {

    }

    public void removeContextPath(String contextName, int level, int parentPath, int subPath) {
        ContextStatePathKey key = new ContextStatePathKey(level, parentPath, subPath);
        removedState.add(key);
        state.remove(key);
    }

    public void removeContext(String contextName) {

    }

    public TreeMap<ContextStatePathKey, ContextStatePathValue> getContextPaths(String contextName) {
        return null;
    }
}
