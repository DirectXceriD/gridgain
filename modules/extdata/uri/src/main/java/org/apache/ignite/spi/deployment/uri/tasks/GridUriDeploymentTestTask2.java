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

package org.apache.ignite.spi.deployment.uri.tasks;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * URI deployment test task which loads Spring bean definitions from spring2.xml configuration file.
 */
public class GridUriDeploymentTestTask2 extends ComputeTaskSplitAdapter<Object, Object> {
    /** */
    private static final long serialVersionUID = 172455091783232848L;

    /** */
    @SuppressWarnings("unchecked")
    public GridUriDeploymentTestTask2() {
        XmlBeanFactory factory = new XmlBeanFactory(
            new ClassPathResource("org/apache/ignite/spi/deployment/uri/tasks/spring2.xml",
                getClass().getClassLoader()));

        factory.setBeanClassLoader(getClass().getClassLoader());

        Map map = (Map)factory.getBean("task.cfg");

        System.out.println("Loaded data from spring2.xml [map=" + map + ']');

        assert map != null;

        GridUriDeploymentDependency2 depend = new GridUriDeploymentDependency2();

        System.out.println("GridUriDeploymentTestTask2 dependency resolved [msg=" + depend.getMessage() + ']');
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<? extends ComputeJob> split(int gridSize, Object arg) {
        System.out.println("Split is called: " + this);

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object reduce(List<ComputeJobResult> results) {
        System.out.println("Reduce is called.");

        return null;
    }
}
