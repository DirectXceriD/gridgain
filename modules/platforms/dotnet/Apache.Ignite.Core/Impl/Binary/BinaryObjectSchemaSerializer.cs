﻿/*
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

namespace Apache.Ignite.Core.Impl.Binary
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.IO;
    using Apache.Ignite.Core.Binary;
    using Apache.Ignite.Core.Impl.Binary.IO;

    /// <summary>
    /// Schema reader/writer.
    /// </summary>
    internal static class BinaryObjectSchemaSerializer
    {
        /// <summary>
        /// Converts schema fields to dictionary.
        /// </summary>
        /// <param name="fields">The fields.</param>
        /// <returns>Fields as dictionary.</returns>
        public static Dictionary<int, int> ToDictionary(this BinaryObjectSchemaField[] fields)
        {
            if (fields == null)
                return null;

            var res = new Dictionary<int, int>(fields.Length);

            foreach (var field in fields)
                res[field.Id] = field.Offset;

            return res;
        }

        /// <summary>
        /// Reads the schema according to this header data.
        /// </summary>
        /// <param name="stream">The stream.</param>
        /// <param name="position">The position.</param>
        /// <param name="hdr">The header.</param>
        /// <param name="schema">The schema.</param>
        /// <param name="ignite">The ignite.</param>
        /// <returns>
        /// Schema.
        /// </returns>
        public static BinaryObjectSchemaField[] ReadSchema(IBinaryStream stream, int position, BinaryObjectHeader hdr, 
            BinaryObjectSchema schema, IIgniteInternal ignite)
        {
            Debug.Assert(stream != null);
            Debug.Assert(schema != null);

            return ReadSchema(stream, position, hdr, () => GetFieldIds(hdr, schema, ignite));
        }

        /// <summary>
        /// Reads the schema according to this header data.
        /// </summary>
        /// <param name="stream">The stream.</param>
        /// <param name="position">The position.</param>
        /// <param name="hdr">The header.</param>
        /// <param name="fieldIdsFunc">The field ids function.</param>
        /// <returns>
        /// Schema.
        /// </returns>
        public static unsafe BinaryObjectSchemaField[] ReadSchema(IBinaryStream stream, int position, 
            BinaryObjectHeader hdr, Func<int[]> fieldIdsFunc)
        {
            Debug.Assert(stream != null);
            Debug.Assert(fieldIdsFunc != null);

            var schemaSize = hdr.SchemaFieldCount;

            if (schemaSize == 0)
                return null;

            stream.Seek(position + hdr.SchemaOffset, SeekOrigin.Begin);

            var res = new BinaryObjectSchemaField[schemaSize];

            var offsetSize = hdr.SchemaFieldOffsetSize;

            if (hdr.IsCompactFooter)
            {
                var fieldIds = fieldIdsFunc();

                Debug.Assert(fieldIds.Length == schemaSize);

                if (offsetSize == 1)
                {
                    for (var i = 0; i < schemaSize; i++)
                        res[i] = new BinaryObjectSchemaField(fieldIds[i], stream.ReadByte());

                }
                else if (offsetSize == 2)
                {
                    for (var i = 0; i < schemaSize; i++)
                        res[i] = new BinaryObjectSchemaField(fieldIds[i], (ushort) stream.ReadShort());
                }
                else
                {
                    for (var i = 0; i < schemaSize; i++)
                        res[i] = new BinaryObjectSchemaField(fieldIds[i], stream.ReadInt());
                }
            }
            else
            {
                if (offsetSize == 1)
                {
                    for (var i = 0; i < schemaSize; i++)
                        res[i] = new BinaryObjectSchemaField(stream.ReadInt(), stream.ReadByte());
                }
                else if (offsetSize == 2)
                {
                    for (var i = 0; i < schemaSize; i++)
                        res[i] = new BinaryObjectSchemaField(stream.ReadInt(), (ushort) stream.ReadShort());
                }
                else
                {
                    if (BitConverter.IsLittleEndian)
                    {
                        fixed (BinaryObjectSchemaField* ptr = &res[0])
                        {
                            stream.Read((byte*) ptr, schemaSize * BinaryObjectSchemaField.Size);
                        }
                    }
                    else
                    {
                        for (var i = 0; i < schemaSize; i++)
                            res[i] = new BinaryObjectSchemaField(stream.ReadInt(), stream.ReadInt());
                    }
                }
            }

            return res;
        }

        /// <summary>
        /// Writes an array of fields to a stream.
        /// </summary>
        /// <param name="fields">Fields.</param>
        /// <param name="stream">Stream.</param>
        /// <param name="offset">Offset in the array.</param>
        /// <param name="count">Field count to write.</param>
        /// <param name="compact">Compact mode without field ids.</param>
        /// <returns>
        /// Flags according to offset sizes: <see cref="BinaryObjectHeader.Flag.OffsetOneByte" />,
        /// <see cref="BinaryObjectHeader.Flag.OffsetTwoBytes" />, or 0.
        /// </returns>
        public static unsafe BinaryObjectHeader.Flag WriteSchema(BinaryObjectSchemaField[] fields, IBinaryStream stream, int offset,
            int count, bool compact)
        {
            Debug.Assert(fields != null);
            Debug.Assert(stream != null);
            Debug.Assert(count > 0);
            Debug.Assert(offset >= 0);
            Debug.Assert(offset < fields.Length);

            unchecked
            {
                // Last field is the farthest in the stream
                var maxFieldOffset = fields[offset + count - 1].Offset;

                if (compact)
                {
                    if (maxFieldOffset <= byte.MaxValue)
                    {
                        for (int i = offset; i < count + offset; i++)
                            stream.WriteByte((byte)fields[i].Offset);

                        return BinaryObjectHeader.Flag.OffsetOneByte;
                    }

                    if (maxFieldOffset <= ushort.MaxValue)
                    {
                        for (int i = offset; i < count + offset; i++)
                            stream.WriteShort((short)fields[i].Offset);

                        return BinaryObjectHeader.Flag.OffsetTwoBytes;
                    }

                    for (int i = offset; i < count + offset; i++)
                        stream.WriteInt(fields[i].Offset);
                }
                else
                {
                    if (maxFieldOffset <= byte.MaxValue)
                    {
                        for (int i = offset; i < count + offset; i++)
                        {
                            var field = fields[i];

                            stream.WriteInt(field.Id);
                            stream.WriteByte((byte)field.Offset);
                        }

                        return BinaryObjectHeader.Flag.OffsetOneByte;
                    }

                    if (maxFieldOffset <= ushort.MaxValue)
                    {
                        for (int i = offset; i < count + offset; i++)
                        {
                            var field = fields[i];

                            stream.WriteInt(field.Id);

                            stream.WriteShort((short)field.Offset);
                        }

                        return BinaryObjectHeader.Flag.OffsetTwoBytes;
                    }

                    if (BitConverter.IsLittleEndian)
                    {
                        fixed (BinaryObjectSchemaField* ptr = &fields[offset])
                        {
                            stream.Write((byte*)ptr, count * BinaryObjectSchemaField.Size);
                        }
                    }
                    else
                    {
                        for (int i = offset; i < count + offset; i++)
                        {
                            var field = fields[i];

                            stream.WriteInt(field.Id);
                            stream.WriteInt(field.Offset);
                        }
                    }
                }


                return BinaryObjectHeader.Flag.None;
            }
        }

        /// <summary>
        /// Gets the field ids.
        /// </summary>
        private static int[] GetFieldIds(BinaryObjectHeader hdr, IIgniteInternal ignite)
        {
            Debug.Assert(hdr.TypeId != BinaryTypeId.Unregistered);

            int[] fieldIds = null;

            if (ignite != null)
            {
                fieldIds = ignite.BinaryProcessor.GetSchema(hdr.TypeId, hdr.SchemaId);
            }

            if (fieldIds == null)
            {
                throw new BinaryObjectException("Cannot find schema for object with compact footer [" +
                                                "typeId=" + hdr.TypeId + ", schemaId=" + hdr.SchemaId + ']');
            }

            return fieldIds;
        }

        /// <summary>
        /// Reads the schema, maintains stream position.
        /// </summary>
        public static int[] GetFieldIds(BinaryObjectHeader hdr, IIgniteInternal ignite, IBinaryStream stream, 
            int objectPos)
        {
            Debug.Assert(stream != null);

            if (hdr.IsCompactFooter)
            {
                // Get schema from Java
                return GetFieldIds(hdr, ignite);
            }

            var pos = stream.Position;

            stream.Seek(objectPos + hdr.SchemaOffset, SeekOrigin.Begin);

            var count = hdr.SchemaFieldCount;

            var offsetSize = hdr.SchemaFieldOffsetSize;

            var res = new int[count];

            for (var i = 0; i < count; i++)
            {
                res[i] = stream.ReadInt();
                stream.Seek(offsetSize, SeekOrigin.Current);  // Skip offsets.
            }

            stream.Seek(pos, SeekOrigin.Begin);

            return res;
        }


        /// <summary>
        /// Gets the field ids.
        /// </summary>
        private static int[] GetFieldIds(BinaryObjectHeader hdr, BinaryObjectSchema schema, IIgniteInternal ignite)
        {
            return schema.Get(hdr.SchemaId) ?? GetFieldIds(hdr, ignite);
        }
    }
}
