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

package org.apache.ignite.internal.sql.command;

import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.internal.sql.SqlKeyword;
import org.apache.ignite.internal.sql.SqlLexer;
import org.apache.ignite.internal.sql.SqlLexerTokenType;
import org.apache.ignite.internal.sql.SqlParseException;

import static org.apache.ignite.internal.sql.SqlParserUtils.error;
import static org.apache.ignite.internal.sql.SqlParserUtils.errorUnexpectedToken;
import static org.apache.ignite.internal.sql.SqlParserUtils.parseBoolean;
import static org.apache.ignite.internal.sql.SqlParserUtils.parseInt;

/**
 * SET STREAMING command.
 */
public class SqlSetStreamingCommand implements SqlCommand {
    /** Default batch size for driver. */
    private final static int DFLT_STREAM_BATCH_SIZE = IgniteDataStreamer.DFLT_PER_NODE_BUFFER_SIZE * 4;

    /** Whether streaming must be turned on or off by this command. */
    private boolean turnOn;

    /** Whether existing values should be overwritten on keys duplication. */
    private boolean allowOverwrite;

    /** Batch size for driver. */
    private int batchSize = DFLT_STREAM_BATCH_SIZE;

    /** Per node number of parallel operations. */
    private int perNodeParOps;

    /** Per node buffer size. */
    private int perNodeBufSize;

    /** Streamer flush timeout. */
    private long flushFreq;

    /** Ordered streamer. */
    private boolean ordered;

    /** {@inheritDoc} */
    @Override public SqlCommand parse(SqlLexer lex) {
        turnOn = parseBoolean(lex);

        while (lex.lookAhead().tokenType() == SqlLexerTokenType.DEFAULT) {
            switch (lex.lookAhead().token()) {
                case SqlKeyword.BATCH_SIZE:
                    lex.shift();

                    checkOffLast(lex);

                    batchSize = parseInt(lex);

                    if (batchSize <= 0)
                        throw error(lex, "Invalid batch size (must be positive).");

                    break;

                case SqlKeyword.PER_NODE_BUFFER_SIZE:
                    lex.shift();

                    checkOffLast(lex);

                    perNodeBufSize = parseInt(lex);

                    if (perNodeBufSize <= 0)
                        throw error(lex, "Invalid per node buffer size (must be positive).");

                    break;

                case SqlKeyword.PER_NODE_PARALLEL_OPERATIONS:
                    lex.shift();

                    checkOffLast(lex);

                    perNodeParOps = parseInt(lex);

                    if (perNodeParOps <= 0)
                        throw error(lex, "Invalid per node parallel operations number (must be positive).");

                    break;

                case SqlKeyword.ALLOW_OVERWRITE:
                    lex.shift();

                    checkOffLast(lex);

                    allowOverwrite = parseBoolean(lex);

                    break;

                case SqlKeyword.FLUSH_FREQUENCY:
                    lex.shift();

                    checkOffLast(lex);

                    flushFreq = parseInt(lex);

                    if (flushFreq <= 0)
                        throw error(lex, "Invalid flush frequency (must be positive).");

                    break;

                case SqlKeyword.ORDERED:
                    lex.shift();

                    checkOffLast(lex);

                    ordered = true;

                    break;

                default:
                    return this;
            }
        }

        return this;
    }

    /**
     * Throw an unexpected token exception if this command turns streaming off.
     * @param lex Lexer to take unexpected token from.
     * @throws SqlParseException if {@link #turnOn} is {@code false}.
     */
    private void checkOffLast(SqlLexer lex) throws SqlParseException {
        if (!turnOn) {
            assert lex.tokenType() == SqlLexerTokenType.DEFAULT;

            throw errorUnexpectedToken(lex);
        }
    }

    /**
     * @return Whether streaming must be turned on or off by this command.
     */
    public boolean isTurnOn() {
        return turnOn;
    }

    /**
     * @return Whether existing values should be overwritten on keys duplication.
     */
    public boolean allowOverwrite() {
        return allowOverwrite;
    }

    /**
     * @return Batch size for driver.
     */
    public int batchSize() {
        return batchSize;
    }

    /**
     * @return Per node number of parallel operations.
     */
    public int perNodeParallelOperations() {
        return perNodeParOps;
    }

    /**
     * @return Per node streamer buffer size.
     */
    public int perNodeBufferSize() {
        return perNodeBufSize;
    }

    /**
     * @return Streamer flush timeout
     */
    public long flushFrequency() {
        return flushFreq;
    }

    /**
     * @return {@code true} if the streamer keep the order of the statements. Otherwise returns {@code false}.
     */
    public boolean isOrdered() {
        return ordered;
    }

    /** {@inheritDoc} */
    @Override public String schemaName() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void schemaName(String schemaName) {
        // No-op.
    }
}
