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

package org.apache.ignite.igfs;

import java.util.concurrent.Callable;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.lang.IgniteOutClosure;
import org.jetbrains.annotations.Nullable;

/**
 * Provides ability to execute IGFS code in a context of a specific user.
 */
public abstract class IgfsUserContext {
    /** Thread local to hold the current user context. */
    private static final ThreadLocal<String> userStackThreadLocal = new ThreadLocal<>();

    /**
     * Executes given callable in the given user context.
     * The main contract of this method is that {@link #currentUser()} method invoked
     * inside closure always returns 'user' this callable executed with.
     * @param user the user name to invoke closure on behalf of.
     * @param c the closure to execute
     * @param <T> The type of closure result.
     * @return the result of closure execution.
     * @throws IllegalArgumentException if user name is null or empty String or if the closure is null.
     */
    public static <T> T doAs(String user, final IgniteOutClosure<T> c) {
        if (F.isEmpty(user))
            throw new IllegalArgumentException("Failed to use null or empty user name.");

        final String ctxUser = userStackThreadLocal.get();

        if (F.eq(ctxUser, user))
            return c.apply(); // correct context is already there

        userStackThreadLocal.set(user);

        try {
            return c.apply();
        }
        finally {
            userStackThreadLocal.set(ctxUser);
        }
    }

    /**
     * Same contract that {@link #doAs(String, IgniteOutClosure)} has, but accepts
     * callable that throws checked Exception.
     * The Exception is not ever wrapped anyhow.
     * If your Callable throws Some specific checked Exceptions, the recommended usage pattern is:
     * <pre name="code" class="java">
     *  public Foo myOperation() throws MyCheckedException1, MyCheckedException2 {
     *      try {
     *          return IgfsUserContext.doAs(user, new Callable<Foo>() {
     *              &#64;Override public Foo call() throws MyCheckedException1, MyCheckedException2 {
     *                  return makeSomeFoo(); // do the job
     *              }
     *          });
     *      }
     *      catch (MyCheckedException1 | MyCheckedException2 | RuntimeException | Error e) {
     *          throw e;
     *      }
     *      catch (Exception e) {
     *          throw new AssertionError("Must never go there.");
     *      }
     *  }
     * </pre>
     * @param user the user name to invoke closure on behalf of.
     * @param c the Callable to execute
     * @param <T> The type of callable result.
     * @return the result of closure execution.
     * @throws IllegalArgumentException if user name is null or empty String or if the closure is null.
     */
    public static <T> T doAs(String user, final Callable<T> c) throws Exception {
        if (F.isEmpty(user))
            throw new IllegalArgumentException("Failed to use null or empty user name.");

        final String ctxUser = userStackThreadLocal.get();

        if (F.eq(ctxUser, user))
            return c.call(); // correct context is already there

        userStackThreadLocal.set(user);

        try {
            return c.call();
        }
        finally {
            userStackThreadLocal.set(ctxUser);
        }
    }

    /**
     * Gets the current context user.
     * If this method is invoked outside of any {@link #doAs(String, IgniteOutClosure)} on the call stack, it will
     * return null. Otherwise it will return the user name set in the most lower
     * {@link #doAs(String, IgniteOutClosure)} call on the call stack.
     * @return The current user, may be null.
     */
    @Nullable public static String currentUser() {
        return userStackThreadLocal.get();
    }
}