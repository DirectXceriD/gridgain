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

package org.apache.ignite.internal.processors.igfs.meta;

import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryRawReader;
import org.apache.ignite.binary.BinaryRawWriter;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;
import org.apache.ignite.internal.processors.igfs.IgfsEntryInfo;
import org.apache.ignite.internal.processors.igfs.IgfsListingEntry;
import org.apache.ignite.internal.processors.igfs.IgfsUtils;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteUuid;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Map;

/**
 * Directory create processor.
 */
public class IgfsMetaDirectoryCreateProcessor implements EntryProcessor<IgniteUuid, IgfsEntryInfo, IgfsEntryInfo>,
    Externalizable, Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Access time. */
    private long accessTime;

    /** Modification time. */
    private long modificationTime;

    /** Properties. */
    private Map<String, String> props;

    /** Child name (optional). */
    private String childName;

    /** Child entry (optional. */
    private IgfsListingEntry childEntry;

    /**
     * Constructor.
     */
    public IgfsMetaDirectoryCreateProcessor() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param accessTime Create time.
     * @param modificationTime Modification time.
     * @param props Properties.
     */
    public IgfsMetaDirectoryCreateProcessor(long accessTime, long modificationTime, Map<String, String> props) {
        this(accessTime, modificationTime, props, null, null);
    }

    /**
     * Constructor.
     *
     * @param accessTime Create time.
     * @param modificationTime Modification time.
     * @param props Properties.
     * @param childName Child name.
     * @param childEntry Child entry.
     */
    public IgfsMetaDirectoryCreateProcessor(long accessTime, long modificationTime, Map<String, String> props,
        String childName, IgfsListingEntry childEntry) {
        this.accessTime = accessTime;
        this.modificationTime = modificationTime;
        this.props = props;
        this.childName = childName;
        this.childEntry = childEntry;
    }

    /** {@inheritDoc} */
    @Override public IgfsEntryInfo process(MutableEntry<IgniteUuid, IgfsEntryInfo> entry, Object... args)
        throws EntryProcessorException {

        IgfsEntryInfo info = IgfsUtils.createDirectory(
            entry.getKey(),
            null,
            props,
            accessTime,
            modificationTime
        );

        if (childName != null)
            info = info.listing(Collections.singletonMap(childName, childEntry));

        entry.setValue(info);

        return info;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(accessTime);
        out.writeLong(modificationTime);

        IgfsUtils.writeProperties(out, props);

        U.writeString(out, childName);

        if (childName != null)
            IgfsUtils.writeListingEntry(out, childEntry);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        accessTime = in.readLong();
        modificationTime = in.readLong();

        props = IgfsUtils.readProperties(in);

        childName = U.readString(in);

        if (childName != null)
            childEntry = IgfsUtils.readListingEntry(in);
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        BinaryRawWriter out = writer.rawWriter();

        out.writeLong(accessTime);
        out.writeLong(modificationTime);

        IgfsUtils.writeProperties(out, props);

        out.writeString(childName);

        if (childName != null)
            IgfsUtils.writeListingEntry(out, childEntry);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReader reader) throws BinaryObjectException {
        BinaryRawReader in = reader.rawReader();

        accessTime = in.readLong();
        modificationTime = in.readLong();

        props = IgfsUtils.readProperties(in);

        childName = in.readString();

        if (childName != null)
            childEntry = IgfsUtils.readListingEntry(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(IgfsMetaDirectoryCreateProcessor.class, this);
    }
}
