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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import org.apache.ignite.IgniteException;

/**
 * Class with various utility methods.
 */
public class Utils {
    /**
     * Perform deep copy of an object.
     *
     * @param orig Original object.
     * @param <T> Class of original object;
     * @return Deep copy of original object.
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T copy(T orig) {
        Object obj;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);

            out.writeObject(orig);
            out.flush();
            out.close();

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));

            obj = in.readObject();
        }
        catch (IOException | ClassNotFoundException e) {
            throw new IgniteException("Couldn't copy the object.", e);
        }

        return (T)obj;
    }

    /**
     * Select k distinct integers from range [0, n) with reservoir sampling:
     * https://en.wikipedia.org/wiki/Reservoir_sampling.
     *
     * @param n Number specifying left end of range of integers to pick values from.
     * @param k Count specifying how many integers should be picked.
     * @param rand RNG.
     * @return Array containing k distinct integers from range [0, n);
     */
    public static int[] selectKDistinct(int n, int k, Random rand) {
        int i;
        Random r = rand != null ? rand : new Random();

        int res[] = new int[k];
        for (i = 0; i < k; i++)
            res[i] = i;

        for (; i < n; i++) {
            int j = r.nextInt(i + 1);

            if (j < k)
                res[j] = i;
        }

        return res;
    }

    /**
     * Select k distinct integers from range [0, n) with reservoir sampling:
     * https://en.wikipedia.org/wiki/Reservoir_sampling.
     * Equivalent to {@code selectKDistinct(n, k, new Random())}.
     *
     * @param n Number specifying left end of range of integers to pick values from.
     * @param k Count specifying how many integers should be picked.
     * @return Array containing k distinct integers from range [0, n);
     */
    public static int[] selectKDistinct(int n, int k) {
        return selectKDistinct(n, k, new Random());
    }
}
