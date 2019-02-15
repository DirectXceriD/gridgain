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

package org.apache.ignite.internal.processors.hadoop.impl;

import com.google.common.base.Joiner;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.ignite.igfs.IgfsPath;
import org.apache.ignite.internal.processors.igfs.IgfsEx;

/**
 * Abstract class for tests based on WordCount test job.
 */
public abstract class HadoopAbstractWordCountTest extends HadoopAbstractSelfTest {
    /** Input path. */
    protected static final String PATH_INPUT = "/input";

    /** Output path. */
    protected static final String PATH_OUTPUT = "/output";

    /** IGFS instance. */
    protected IgfsEx igfs;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted0() throws Exception {
        Configuration cfg = new Configuration();

        setupFileSystems(cfg);

        // Init cache by correct LocalFileSystem implementation
        FileSystem.getLocal(cfg);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        igfs = (IgfsEx)startGrids(gridCount()).fileSystem(igfsName);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids(true);
    }

    /** {@inheritDoc} */
    @Override protected boolean igfsEnabled() {
        return true;
    }

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 1;
    }

    /**
     * Generates test file.
     *
     * @param path File name.
     * @param wordCounts Words and counts.
     * @throws Exception If failed.
     */
    protected void generateTestFile(String path, Object... wordCounts) throws Exception {
        List<String> wordsArr = new ArrayList<>();

        //Generating
        for (int i = 0; i < wordCounts.length; i += 2) {
            String word = (String) wordCounts[i];
            int cnt = (Integer) wordCounts[i + 1];

            while (cnt-- > 0)
                wordsArr.add(word);
        }

        //Shuffling
        for (int i = 0; i < wordsArr.size(); i++) {
            int j = (int)(Math.random() * wordsArr.size());

            Collections.swap(wordsArr, i, j);
        }

        //Input file preparing
        PrintWriter testInputFileWriter = new PrintWriter(igfs.create(new IgfsPath(path), true));

        int j = 0;

        while (j < wordsArr.size()) {
            int i = 5 + (int)(Math.random() * 5);

            List<String> subList = wordsArr.subList(j, Math.min(j + i, wordsArr.size()));
            j += i;

            testInputFileWriter.println(Joiner.on(' ').join(subList));
        }

        testInputFileWriter.close();
    }

    /**
     * Read w/o decoding (default).
     *
     * @param fileName The file.
     * @return The file contents, human-readable.
     * @throws Exception On error.
     */
    protected String readAndSortFile(String fileName) throws Exception {
        return readAndSortFile(fileName, null);
    }

    /**
     * Reads whole text file into String.
     *
     * @param fileName Name of the file to read.
     * @return Content of the file as String value.
     * @throws Exception If could not read the file.
     */
    protected String readAndSortFile(String fileName, Configuration conf) throws Exception {
        final List<String> list = new ArrayList<>();

        final boolean snappyDecode = conf != null && conf.getBoolean(FileOutputFormat.COMPRESS, false);

        if (snappyDecode) {
            try (SequenceFile.Reader reader = new SequenceFile.Reader(conf,
                    SequenceFile.Reader.file(new Path(fileName)))) {
                Text key = new Text();

                IntWritable val = new IntWritable();

                while (reader.next(key, val))
                    list.add(key + "\t" + val);
            }
        }
        else {
            try (InputStream is0 = igfs.open(new IgfsPath(fileName))) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is0));

                String line;

                while ((line = reader.readLine()) != null)
                    list.add(line);
            }
        }

        Collections.sort(list);

        return Joiner.on('\n').join(list) + "\n";
    }
}