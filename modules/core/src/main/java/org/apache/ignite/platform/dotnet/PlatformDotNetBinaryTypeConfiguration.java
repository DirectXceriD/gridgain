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

package org.apache.ignite.platform.dotnet;

import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.Nullable;

/**
 * Mirror of .Net class BinaryTypeConfiguration.cs
 */
public class PlatformDotNetBinaryTypeConfiguration {
    /** Type name. */
    private String typeName;

    /** Name mapper. */
    private String nameMapper;

    /** Id mapper. */
    private String idMapper;

    /** Serializer. */
    private String serializer;

    /** Affinity key field name. */
    private String affinityKeyFieldName;

    /** Whether to cache deserialized value. */
    private Boolean keepDeserialized;

    /** Enum flag. */
    private boolean isEnum;

    /**
     * Default constructor.
     */
    public PlatformDotNetBinaryTypeConfiguration() {
        // No-op.
    }

    /**
     * Copy constructor.
     * @param cfg configuration to copy.
     */
    public PlatformDotNetBinaryTypeConfiguration(PlatformDotNetBinaryTypeConfiguration cfg) {
        typeName = cfg.getTypeName();
        nameMapper = cfg.getNameMapper();
        idMapper = cfg.getIdMapper();
        serializer = cfg.getSerializer();
        affinityKeyFieldName = cfg.getAffinityKeyFieldName();
        keepDeserialized = cfg.isKeepDeserialized();
        isEnum = cfg.isEnum();
    }

    /**
     * @return Type name.
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @param typeName New type name.
     * @return {@code this} for chaining.
     */
    public PlatformDotNetBinaryTypeConfiguration setTypeName(String typeName) {
        this.typeName = typeName;

        return this;
    }

    /**
     * @return Name mapper.
     */
    public String getNameMapper() {
        return nameMapper;
    }

    /**
     * @param nameMapper New name mapper.
     * @return {@code this} for chaining.
     */
    public PlatformDotNetBinaryTypeConfiguration setNameMapper(String nameMapper) {
        this.nameMapper = nameMapper;

        return this;
    }

    /**
     * @return Id mapper.
     */
    public String getIdMapper() {
        return idMapper;
    }

    /**
     * @param idMapper New id mapper.
     * @return {@code this} for chaining.
     */
    public PlatformDotNetBinaryTypeConfiguration setIdMapper(String idMapper) {
        this.idMapper = idMapper;

        return this;
    }

    /**
     * @return Serializer.
     */
    public String getSerializer() {
        return serializer;
    }

    /**
     * @param serializer New serializer.
     * @return {@code this} for chaining.
     */
    public PlatformDotNetBinaryTypeConfiguration setSerializer(String serializer) {
        this.serializer = serializer;

        return this;
    }

    /**
     * @return Affinity key field name.
     */
    public String getAffinityKeyFieldName() {
        return affinityKeyFieldName;
    }

    /**
     * @param affinityKeyFieldName Affinity key field name.
     * @return {@code this} for chaining.
     */
    public PlatformDotNetBinaryTypeConfiguration setAffinityKeyFieldName(String affinityKeyFieldName) {
        this.affinityKeyFieldName = affinityKeyFieldName;

        return this;
    }

    /**
     * Gets keep deserialized flag.
     *
     * @return Flag indicates whether to cache deserialized value.
     * @deprecated Use {@link #getKeepDeserialized()} instead.
     */
    @Deprecated
    @Nullable public Boolean isKeepDeserialized() {
        return keepDeserialized;
    }

    /**
     * Gets keep deserialized flag. See {@link #setKeepDeserialized(Boolean)} for more information.
     *
     * @return Flag indicates whether to cache deserialized value.
     */
    @Nullable public Boolean getKeepDeserialized() {
        return keepDeserialized;
    }

    /**
     * Sets keep deserialized flag.
     * <p />
     * When set to {@code null} default value taken from
     * {@link PlatformDotNetBinaryConfiguration#isDefaultKeepDeserialized()} will be used.
     *
     * @param keepDeserialized Keep deserialized flag.
     * @return {@code this} for chaining.
     */
    public PlatformDotNetBinaryTypeConfiguration setKeepDeserialized(@Nullable Boolean keepDeserialized) {
        this.keepDeserialized = keepDeserialized;

        return this;
    }

    /**
     * Gets whether this is enum type.
     *
     * @return {@code True} if enum.
     */
    public boolean isEnum() {
        return isEnum;
    }

    /**
     * Sets whether this is enum type.
     *
     * @param isEnum {@code True} if enum.
     * @return {@code this} for chaining.
     */
    public PlatformDotNetBinaryTypeConfiguration setEnum(boolean isEnum) {
        this.isEnum = isEnum;

        return this;
    }


    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(PlatformDotNetBinaryTypeConfiguration.class, this);
    }
}
