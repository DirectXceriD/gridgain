#!/usr/bin/env bash
set -o nounset
set -o errexit
set -o pipefail
set -o errtrace
set -o functrace

#
#                   GridGain Community Edition Licensing
#                   Copyright 2019 GridGain Systems, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
# Restriction; you may not use this file except in compliance with the License. You may obtain a
# copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the
# License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the specific language governing permissions
# and limitations under the License.
#
# Commons Clause Restriction
#
# The Software is provided to you by the Licensor under the License, as defined below, subject to
# the following condition.
#
# Without limiting other conditions in the License, the grant of rights under the License will not
# include, and the License does not grant to you, the right to Sell the Software.
# For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
# under the License to provide to third parties, for a fee or other consideration (including without
# limitation fees for hosting or consulting/ support services related to the Software), a product or
# service whose value derives, entirely or substantially, from the functionality of the Software.
# Any license notice or attribution required by the License must also include this Commons Clause
# License Condition notice.
#
# For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
# the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
# Edition software provided with this notice.
#

#
# Grid command line loader.
#

#
# Import common functions.
#
if [ "${IGNITE_HOME:-}" = "" ];
    then IGNITE_HOME_TMP="$(dirname "$(cd "$(dirname "$0")"; "pwd")")";
    else IGNITE_HOME_TMP=${IGNITE_HOME};
fi

#
# Set SCRIPTS_HOME - base path to scripts.
#
SCRIPTS_HOME="${IGNITE_HOME_TMP}/bin"

source "${SCRIPTS_HOME}"/include/functions.sh

#
# Discover path to Java executable and check it's version.
#
checkJava

#
# Discover IGNITE_HOME environment variable.
#
setIgniteHome

if [ "${DEFAULT_CONFIG:-}" == "" ]; then
    DEFAULT_CONFIG=config/default-config.xml
fi

#
# Parse command line parameters.
#
. "${SCRIPTS_HOME}"/include/parseargs.sh

#
# Set IGNITE_LIBS.
#
. "${SCRIPTS_HOME}"/include/setenv.sh
. "${SCRIPTS_HOME}"/include/build-classpath.sh # Will be removed in the binary release.
CP="${IGNITE_LIBS}"

RANDOM_NUMBER=$("$JAVA" -cp "${CP}" org.apache.ignite.startup.cmdline.CommandLineRandomNumberGenerator)

RESTART_SUCCESS_FILE="${IGNITE_HOME}/work/ignite_success_${RANDOM_NUMBER}"
RESTART_SUCCESS_OPT="-DIGNITE_SUCCESS_FILE=${RESTART_SUCCESS_FILE}"

#
# Find available port for JMX
#
# You can specify IGNITE_JMX_PORT environment variable for overriding automatically found JMX port
#
# This is executed when -nojmx is not specified
#
if [ "${NOJMX}" == "0" ] ; then
    findAvailableJmxPort
fi

# Mac OS specific support to display correct name in the dock.
osname=`uname`

if [ "${DOCK_OPTS:-}" == "" ]; then
    DOCK_OPTS="-Xdock:name=Ignite Node"
fi

#
# JVM options. See http://java.sun.com/javase/technologies/hotspot/vmoptions.jsp for more details.
#
# ADD YOUR/CHANGE ADDITIONAL OPTIONS HERE
#
if [ -z "$JVM_OPTS" ] ; then
    JVM_OPTS="-Xms1g -Xmx1g -server -XX:MaxMetaspaceSize=256m"
fi

#
# Uncomment the following GC settings if you see spikes in your throughput due to Garbage Collection.
#
# JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"

#
# Uncomment if you get StackOverflowError.
# On 64 bit systems this value can be larger, e.g. -Xss16m
#
# JVM_OPTS="${JVM_OPTS} -Xss4m"

#
# Uncomment to set preference for IPv4 stack.
#
# JVM_OPTS="${JVM_OPTS} -Djava.net.preferIPv4Stack=true"

#
# Assertions are disabled by default since version 3.5.
# If you want to enable them - set 'ENABLE_ASSERTIONS' flag to '1'.
#
ENABLE_ASSERTIONS="1"

#
# Set '-ea' options if assertions are enabled.
#
if [ "${ENABLE_ASSERTIONS}" = "1" ]; then
    JVM_OPTS="${JVM_OPTS} -ea"
fi

#
# If this is a Hadoop edition, and HADOOP_HOME set, add the native library location:
#
if [ -d "${IGNITE_HOME}/libs/ignite-hadoop/" ] && [ -n "${HADOOP_HOME}" ] && [ -d "${HADOOP_HOME}/lib/native/" ]; then
   if [[ "${JVM_OPTS}${JVM_XOPTS}" != *-Djava.library.path=* ]]; then
      JVM_OPTS="${JVM_OPTS} -Djava.library.path=${HADOOP_HOME}/lib/native/"
   fi
fi

#
# Set main class to start service (grid node by default).
#
if [ "${MAIN_CLASS:-}" = "" ]; then
    MAIN_CLASS=org.apache.ignite.startup.cmdline.CommandLineStartup
fi

#
# Remote debugging (JPDA).
# Uncomment and change if remote debugging is required.
#
# JVM_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8787 ${JVM_OPTS}"

#
# Final JVM_OPTS for Java 9+ compatibility
#
javaMajorVersion "${JAVA}"

if [ $version -eq 8 ] ; then
    JVM_OPTS="\
        -XX:+AggressiveOpts \
         ${JVM_OPTS}"

elif [ $version -gt 8 ] && [ $version -lt 11 ]; then
    JVM_OPTS="\
        -XX:+AggressiveOpts \
        --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
        --add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
        --add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED \
        --add-exports=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED \
        --add-exports=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED \
        --illegal-access=permit \
        --add-modules=java.transaction \
        --add-modules=java.xml.bind \
        ${JVM_OPTS}"

elif [ $version -eq 11 ] ; then
    JVM_OPTS="\
        --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
        --add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
        --add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED \
        --add-exports=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED \
        --add-exports=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED \
        --illegal-access=permit \
        ${JVM_OPTS}"
fi


ERRORCODE="-1"

while [ "${ERRORCODE}" -ne "130" ]
do
    if [ "${INTERACTIVE}" == "1" ] ; then
        case $osname in
            Darwin*)
                "$JAVA" ${JVM_OPTS} ${QUIET} "${DOCK_OPTS}" "${RESTART_SUCCESS_OPT}" ${JMX_MON} \
                -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_HOME="${IGNITE_HOME}" \
                -DIGNITE_PROG_NAME="$0" ${JVM_XOPTS} -cp "${CP}" ${MAIN_CLASS}
            ;;
            *)
                "$JAVA" ${JVM_OPTS} ${QUIET} "${RESTART_SUCCESS_OPT}" ${JMX_MON} \
                -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_HOME="${IGNITE_HOME}" \
                -DIGNITE_PROG_NAME="$0" ${JVM_XOPTS} -cp "${CP}" ${MAIN_CLASS}
            ;;
        esac
    else
        case $osname in
            Darwin*)
                "$JAVA" ${JVM_OPTS} ${QUIET} "${DOCK_OPTS}" "${RESTART_SUCCESS_OPT}" ${JMX_MON} \
                 -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_HOME="${IGNITE_HOME}" \
                 -DIGNITE_PROG_NAME="$0" ${JVM_XOPTS} -cp "${CP}" ${MAIN_CLASS} "${CONFIG}"
            ;;
            *)
                "$JAVA" ${JVM_OPTS} ${QUIET} "${RESTART_SUCCESS_OPT}" ${JMX_MON} \
                 -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_HOME="${IGNITE_HOME}" \
                 -DIGNITE_PROG_NAME="$0" ${JVM_XOPTS} -cp "${CP}" ${MAIN_CLASS} "${CONFIG}"
            ;;
        esac
    fi

    ERRORCODE="$?"

    if [ ! -f "${RESTART_SUCCESS_FILE}" ] ; then
        break
    else
        rm -f "${RESTART_SUCCESS_FILE}"
    fi
done

if [ -f "${RESTART_SUCCESS_FILE}" ] ; then
    rm -f "${RESTART_SUCCESS_FILE}"
fi
