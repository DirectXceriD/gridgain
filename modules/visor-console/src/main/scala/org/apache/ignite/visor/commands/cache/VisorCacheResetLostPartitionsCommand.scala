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

package org.apache.ignite.visor.commands.cache

import java.util.Collections

import org.apache.ignite.cluster.{ClusterGroupEmptyException, ClusterNode}
import org.apache.ignite.internal.visor.cache.{VisorCacheResetLostPartitionsTask, VisorCacheResetLostPartitionsTaskArg}
import org.apache.ignite.internal.visor.util.VisorTaskUtils._
import org.apache.ignite.visor.visor._

import scala.language.reflectiveCalls

/**
 * ==Overview==
 * Visor 'lost partition reset' command implementation.
 *
 * ====Specification====
 * {{{
 *     cache -rlp -c=<cache name>
 * }}}
 *
 * ====Arguments====
 * {{{
 *     <cache-name>
 *         Name of the cache.
 * }}}
 *
 * ====Examples====
 * {{{
 *    cache -rlp -c=@c0
 *        Reset lost partition for cache with name taken from 'c0' memory variable.
 * }}}
 */
class VisorCacheResetLostPartitionsCommand {
    /**
     * Prints error message and advise.
     *
     * @param errMsgs Error messages.
     */
    private def scold(errMsgs: Any*) {
        assert(errMsgs != null)

        warn(errMsgs: _*)
        warn("Type 'help cache' to see how to use this command.")
    }

    private def error(e: Exception) {
        var cause: Throwable = e

        while (cause.getCause != null)
            cause = cause.getCause

        scold(cause.getMessage)
    }

    /**
     * ===Command===
     * Reset lost partitions for cache with specified name.
     *
     * ===Examples===
     * <ex>cache -c=cache -rlp</ex>
     * Reset lost partitions for cache with name 'cache'.
     *
     * @param argLst Command arguments.
     */
    def resetLostPartitions(argLst: ArgList, node: Option[ClusterNode]) {
        val cacheArg = argValue("c", argLst)

        val cacheName = cacheArg match {
            case None => null // default cache.

            case Some(s) if s.startsWith("@") =>
                warn("Can't find cache variable with specified name: " + s,
                    "Type 'cache' to see available cache variables."
                )

                return

            case Some(name) => name
        }

        val grp = try {
            groupForDataNode(node, cacheName)
        }
        catch {
            case _: ClusterGroupEmptyException =>
                scold(messageNodeNotFound(node, cacheName))

                return
        }

        try {
            executeRandom(grp, classOf[VisorCacheResetLostPartitionsTask],
                new VisorCacheResetLostPartitionsTaskArg(Collections.singletonList(cacheName)))

            println("Visor successfully reset lost partitions for cache: " + escapeName(cacheName))
        }
        catch {
            case _: ClusterGroupEmptyException => scold(messageNodeNotFound(node, cacheName))
            case e: Exception => error(e)
        }
    }
}

/**
 * Companion object that does initialization of the command.
 */
object VisorCacheResetLostPartitionsCommand {
    /** Singleton command. */
    private val cmd = new VisorCacheResetLostPartitionsCommand

    /**
      * Singleton.
      */
    def apply() = cmd
}
