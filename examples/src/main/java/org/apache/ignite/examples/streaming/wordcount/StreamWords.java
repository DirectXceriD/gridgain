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

package org.apache.ignite.examples.streaming.wordcount;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.affinity.AffinityUuid;
import org.apache.ignite.examples.ExampleNodeStartup;
import org.apache.ignite.examples.ExamplesUtils;

/**
 * Stream words into Ignite cache.
 * To start the example, you should:
 * <ul>
 *     <li>Start a few nodes using {@link ExampleNodeStartup}.</li>
 *     <li>Start streaming using {@link StreamWords}.</li>
 *     <li>Start querying popular words using {@link QueryWords}.</li>
 * </ul>
 */
public class StreamWords {
    /**
     * Starts words streaming.
     *
     * @param args Command line arguments (none required).
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        // Mark this cluster member as client.
        Ignition.setClientMode(true);

        try (Ignite ignite = Ignition.start("examples/config/example-ignite.xml")) {
            if (!ExamplesUtils.hasServerNodes(ignite))
                return;

            // The cache is configured with sliding window holding 1 second of the streaming data.
            IgniteCache<AffinityUuid, String> stmCache = ignite.getOrCreateCache(CacheConfig.wordCache());

            try (IgniteDataStreamer<AffinityUuid, String> stmr = ignite.dataStreamer(stmCache.getName())) {
                // Stream words from "alice-in-wonderland" book.
                while (true) {
                    InputStream in = StreamWords.class.getResourceAsStream("alice-in-wonderland.txt");

                    try (LineNumberReader rdr = new LineNumberReader(new InputStreamReader(in))) {
                        for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
                            for (String word : line.split(" "))
                                if (!word.isEmpty())
                                    // Stream words into Ignite.
                                    // By using AffinityUuid we ensure that identical
                                    // words are processed on the same cluster node.
                                    stmr.addData(new AffinityUuid(word), word);
                        }
                    }
                }
            }
        }
    }
}