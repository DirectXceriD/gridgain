/*
 *                   GridGain Community Edition Licensing
 *                   Copyright 2019 GridGain Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
 * Restriction; you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * Commons Clause Restriction
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including without
 * limitation fees for hosting or consulting/ support services related to the Software), a product or
 * service whose value derives, entirely or substantially, from the functionality of the Software.
 * Any license notice or attribution required by the License must also include this Commons Clause
 * License Condition notice.
 *
 * For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
 * the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
 * Edition software provided with this notice.
 */

package org.apache.ignite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.compute.ComputeJobSibling;
import org.apache.ignite.compute.ComputeTaskSession;
import org.apache.ignite.compute.ComputeTaskSessionAttributeListener;
import org.apache.ignite.compute.ComputeTaskSessionScope;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteUuid;
import org.jetbrains.annotations.Nullable;

/**
 * Test task session.
 */
public class GridTestTaskSession implements ComputeTaskSession {
    /** */
    private String taskName;

    /** */
    private String jobTypeId;

    /** */
    private IgniteUuid sesId;

    /** */
    private UUID taskNodeId = UUID.randomUUID();

    /** */
    private Map<Object, Object> attrs = new HashMap<>();

    /** */
    private Collection<ComputeTaskSessionAttributeListener> lsnrs = new ArrayList<>();

    /** */
    private ClassLoader clsLdr = getClass().getClassLoader();

    /** */
    public GridTestTaskSession() {
        /* No-op. */
    }

    /**
     * @param sesId Session ID.
     */
    public GridTestTaskSession(IgniteUuid sesId) {
        this.sesId = sesId;
    }

    /**
     * @param taskName Task name.
     * @param jobTypeId Job type ID.
     * @param sesId Session ID.
     */
    public GridTestTaskSession(String taskName, String jobTypeId, IgniteUuid sesId) {
        this.taskName = taskName;
        this.jobTypeId = jobTypeId;
        this.sesId = sesId;
    }

    /** {@inheritDoc} */
    @Override public UUID getTaskNodeId() {
        return taskNodeId;
    }

    /** {@inheritDoc} */
    @Override public <K, V> V waitForAttribute(K key, long timeout) {
        assert false : "Not implemented";

        return null;
    }

    /** {@inheritDoc} */
    @Override public <K, V> boolean waitForAttribute(K key, @Nullable V val, long timeout) throws InterruptedException {
        assert false : "Not implemented";

        return false;
    }

    /** {@inheritDoc} */
    @Override public Map<?, ?> waitForAttributes(Collection<?> keys, long timeout) {
        assert false : "Not implemented";

        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean waitForAttributes(Map<?, ?> attrs, long timeout) throws InterruptedException {
        assert false : "Not implemented";

        return false;
    }

    /** {@inheritDoc} */
    @Override public String getTaskName() {
        return taskName;
    }

    /** {@inheritDoc} */
    @Override public IgniteUuid getId() {
        return sesId;
    }

    /** {@inheritDoc} */
    @Override public long getEndTime() {
        return Long.MAX_VALUE;
    }

    /** {@inheritDoc} */
    @Override public ClassLoader getClassLoader() {
        return clsLdr;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Collection<ComputeJobSibling> getJobSiblings() {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Collection<ComputeJobSibling> refreshJobSiblings() {
        return getJobSiblings();
    }

    /** {@inheritDoc} */
    @Override public ComputeJobSibling getJobSibling(IgniteUuid jobId) {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void setAttribute(Object key, Object val) {
        attrs.put(key, val);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public <K, V> V getAttribute(K key) {
        return (V)attrs.get(key);
    }

    /** {@inheritDoc} */
    @Override public void setAttributes(Map<?, ?> attrs) {
        this.attrs.putAll(attrs);
    }

    /** {@inheritDoc} */
    @Override public Map<Object, Object> getAttributes() {
        return Collections.unmodifiableMap(attrs);
    }

    /** {@inheritDoc} */
    @Override public void addAttributeListener(ComputeTaskSessionAttributeListener lsnr, boolean rewind) {
        lsnrs.add(lsnr);
    }

    /** {@inheritDoc} */
    @Override public boolean removeAttributeListener(ComputeTaskSessionAttributeListener lsnr) {
        return lsnrs.remove(lsnr);
    }

    /** {@inheritDoc} */
    @Override public void saveCheckpoint(String key, Object state) {
        assert false : "Not implemented";
    }

    /** {@inheritDoc} */
    @Override public void saveCheckpoint(String key, Object state, ComputeTaskSessionScope scope, long timeout) {
        assert false : "Not implemented";
    }

    /** {@inheritDoc} */
    @Override public void saveCheckpoint(String key, Object state, ComputeTaskSessionScope scope, long timeout,
        boolean overwrite) {
        assert false : "Not implemented";
    }

    /** {@inheritDoc} */
    @Override public <T> T loadCheckpoint(String key) {
        assert false : "Not implemented";

        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean removeCheckpoint(String key) {
        assert false : "Not implemented";

        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Collection<UUID> getTopology() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public long getStartTime() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public IgniteFuture<?> mapFuture() {
        assert false : "Not implemented";

        return null;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(getClass().getName());
        buf.append(" [taskName='").append(taskName).append('\'');
        buf.append(", jobTypeId='").append(jobTypeId).append('\'');
        buf.append(", sesId=").append(sesId);
        buf.append(", clsLdr=").append(clsLdr);
        buf.append(']');

        return buf.toString();
    }
}