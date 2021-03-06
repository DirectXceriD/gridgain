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

import JSZip from 'jszip';

export default class ModalPreviewProjectController {
    static $inject = [
        'PageConfigure',
        'IgniteConfigurationResource',
        'IgniteSummaryZipper',
        'IgniteVersion',
        '$scope',
        'ConfigurationDownload',
        'IgniteLoading',
        'IgniteMessages'
    ];

    constructor(PageConfigure, IgniteConfigurationResource, summaryZipper, IgniteVersion, $scope, ConfigurationDownload, IgniteLoading, IgniteMessages) {
        Object.assign(this, {PageConfigure, IgniteConfigurationResource, summaryZipper, IgniteVersion, $scope, ConfigurationDownload, IgniteLoading, IgniteMessages});
    }

    $onInit() {
        this.treeOptions = {
            nodeChildren: 'children',
            dirSelectable: false,
            injectClasses: {
                iExpanded: 'fa fa-folder-open-o',
                iCollapsed: 'fa fa-folder-o'
            }
        };
        this.doStuff(this.cluster, this.isDemo);
    }

    showPreview(node) {
        this.fileText = '';

        if (!node)
            return;

        this.fileExt = node.file.name.split('.').reverse()[0].toLowerCase();

        if (node.file.dir)
            return;

        node.file.async('string').then((text) => {
            this.fileText = text;
            this.$scope.$applyAsync();
        });
    }

    doStuff(cluster, isDemo) {
        this.IgniteLoading.start('projectStructurePreview');
        return this.PageConfigure.getClusterConfiguration({clusterID: cluster._id, isDemo})
        .then((data) => {
            return this.IgniteConfigurationResource.populate(data);
        })
        .then(({clusters}) => {
            return clusters.find(({_id}) => _id === cluster._id);
        })
        .then((cluster) => {
            return this.summaryZipper({
                cluster,
                data: {},
                IgniteDemoMode: isDemo,
                targetVer: this.IgniteVersion.currentSbj.getValue()
            });
        })
        .then(JSZip.loadAsync)
        .then((val) => {
            const convert = (files) => {
                return Object.keys(files)
                .map((path, i, paths) => ({
                    fullPath: path,
                    path: path.replace(/\/$/, ''),
                    file: files[path],
                    parent: files[paths.filter((p) => path.startsWith(p) && p !== path).sort((a, b) => b.length - a.length)[0]]
                }))
                .map((node, i, nodes) => Object.assign(node, {
                    path: node.parent ? node.path.replace(node.parent.name, '') : node.path,
                    children: nodes.filter((n) => n.parent && n.parent.name === node.file.name)
                }));
            };

            const nodes = convert(val.files);

            this.data = [{
                path: this.ConfigurationDownload.nameFile(cluster),
                file: {dir: true},
                children: nodes.filter((n) => !n.parent)
            }];

            this.selectedNode = nodes.find((n) => n.path.includes('server.xml'));
            this.expandedNodes = [
                ...this.data,
                ...nodes.filter((n) => {
                    return !n.fullPath.startsWith('src/main/java/')
                        || /src\/main\/java(\/(config|load|startup))?\/$/.test(n.fullPath);
                })
            ];
            this.showPreview(this.selectedNode);
            this.IgniteLoading.finish('projectStructurePreview');
        })
        .catch((e) => {
            this.IgniteMessages.showError('Failed to generate project preview: ', e);
            this.onHide();
        });
    }

    orderBy() {
        return;
    }
}
