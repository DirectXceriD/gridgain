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

package org.apache.ignite.ml.util;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.apache.ignite.IgniteException;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;

/**
 * Utility class for reading MNIST dataset.
 */
public class MnistUtils {
    /**
     * Read random {@code count} samples from MNIST dataset from two files (images and labels) into a stream of labeled
     * vectors.
     *
     * @param imagesPath Path to the file with images.
     * @param labelsPath Path to the file with labels.
     * @param rnd Random numbers generator.
     * @param cnt Count of samples to read.
     * @return Stream of MNIST samples.
     * @throws IgniteException In case of exception.
     */
    public static Stream<DenseVector> mnistAsStream(String imagesPath, String labelsPath, Random rnd, int cnt)
        throws IOException {
        FileInputStream isImages = new FileInputStream(imagesPath);
        FileInputStream isLabels = new FileInputStream(labelsPath);

        read4Bytes(isImages); // Skip magic number.
        int numOfImages = read4Bytes(isImages);
        int imgHeight = read4Bytes(isImages);
        int imgWidth = read4Bytes(isImages);

        read4Bytes(isLabels); // Skip magic number.
        read4Bytes(isLabels); // Skip number of labels.

        int numOfPixels = imgHeight * imgWidth;

        double[][] vecs = new double[numOfImages][numOfPixels + 1];

        for (int imgNum = 0; imgNum < numOfImages; imgNum++) {
            vecs[imgNum][numOfPixels] = isLabels.read();
            for (int p = 0; p < numOfPixels; p++) {
                int c = 128 - isImages.read();
                vecs[imgNum][p] = (double)c / 128;
            }
        }

        List<double[]> lst = Arrays.asList(vecs);
        Collections.shuffle(lst, rnd);

        isImages.close();
        isLabels.close();

        return lst.subList(0, cnt).stream().map(DenseVector::new);
    }

    /**
     * Read random {@code count} samples from MNIST dataset from two files (images and labels) into a stream of labeled
     * vectors.
     *
     * @param imagesPath Path to the file with images.
     * @param labelsPath Path to the file with labels.
     * @param rnd Random numbers generator.
     * @param cnt Count of samples to read.
     * @return List of MNIST samples.
     * @throws IOException In case of exception.
     */
    public static List<MnistLabeledImage> mnistAsList(String imagesPath, String labelsPath, Random rnd, int cnt) throws IOException {

        List<MnistLabeledImage> res = new ArrayList<>();

        try (
            FileInputStream isImages = new FileInputStream(imagesPath);
            FileInputStream isLabels = new FileInputStream(labelsPath)
        ) {
            read4Bytes(isImages); // Skip magic number.
            int numOfImages = read4Bytes(isImages);
            int imgHeight = read4Bytes(isImages);
            int imgWidth = read4Bytes(isImages);

            read4Bytes(isLabels); // Skip magic number.
            read4Bytes(isLabels); // Skip number of labels.

            int numOfPixels = imgHeight * imgWidth;

            for (int imgNum = 0; imgNum < numOfImages; imgNum++) {
                double[] pixels = new double[numOfPixels];
                for (int p = 0; p < numOfPixels; p++) {
                    int c = 128 - isImages.read();
                    pixels[p] = ((double)c) / 128;
                }
                res.add(new MnistLabeledImage(pixels, isLabels.read()));
            }
        }

        Collections.shuffle(res, rnd);

        return res.subList(0, cnt);
    }

    /**
     * Convert random {@code count} samples from MNIST dataset from two files (images and labels) into libsvm format.
     *
     * @param imagesPath Path to the file with images.
     * @param labelsPath Path to the file with labels.
     * @param outPath Path to output path.
     * @param rnd Random numbers generator.
     * @param cnt Count of samples to read.
     * @throws IgniteException In case of exception.
     */
    public static void asLIBSVM(String imagesPath, String labelsPath, String outPath, Random rnd, int cnt)
        throws IOException {

        try (FileWriter fos = new FileWriter(outPath)) {
            mnistAsStream(imagesPath, labelsPath, rnd, cnt).forEach(vec -> {
                try {
                    fos.write((int)vec.get(vec.size() - 1) + " ");

                    for (int i = 0; i < vec.size() - 1; i++) {
                        double val = vec.get(i);

                        if (val != 0)
                            fos.write((i + 1) + ":" + val + " ");
                    }

                    fos.write("\n");

                }
                catch (IOException e) {
                    throw new IgniteException("Error while converting to LIBSVM.");
                }
            });
        }
    }

    /**
     * Utility method for reading 4 bytes from input stream.
     *
     * @param is Input stream.
     * @throws IOException In case of exception.
     */
    private static int read4Bytes(FileInputStream is) throws IOException {
        return (is.read() << 24) | (is.read() << 16) | (is.read() << 8) | (is.read());
    }

    /**
     * MNIST image.
     */
    public static class MnistImage {
        /** Pixels. */
        private final double[] pixels;

        /**
         * Construct a new instance of MNIST image.
         *
         * @param pixels Pixels.
         */
        public MnistImage(double[] pixels) {
            this.pixels = pixels;
        }

        /** */
        public double[] getPixels() {
            return pixels;
        }
    }

    /**
     * MNIST labeled image.
     */
    public static class MnistLabeledImage extends MnistImage {
        /** Label. */
        private final int lb;

        /**
         * Constructs a new instance of MNIST labeled image.
         *
         * @param pixels Pixels.
         * @param lb Label.
         */
        public MnistLabeledImage(double[] pixels, int lb) {
            super(pixels);
            this.lb = lb;
        }

        /** */
        public int getLabel() {
            return lb;
        }
    }
}
