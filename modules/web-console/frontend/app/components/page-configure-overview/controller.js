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

import {Subject} from 'rxjs';
import {map} from 'rxjs/operators';
import naturalCompare from 'natural-compare-lite';

const cellTemplate = (state) => `
    <div class="ui-grid-cell-contents">
        <a
            class="link-success"
            ui-sref="${state}({clusterID: row.entity._id})"
            title='Click to edit'
        >{{ row.entity[col.field] }}</a>
    </div>
`;

import {default as ConfigureState} from 'app/components/page-configure/services/ConfigureState';
import {default as ConfigSelectors} from 'app/components/page-configure/store/selectors';
import {default as Clusters} from 'app/services/Clusters';
import {default as ModalPreviewProject} from 'app/components/page-configure/components/modal-preview-project/service';
import {default as ConfigurationDownload} from 'app/components/page-configure/services/ConfigurationDownload';

import {confirmClustersRemoval} from '../page-configure/store/actionCreators';

export default class PageConfigureOverviewController {
    static $inject = [
        '$uiRouter',
        'ModalPreviewProject',
        'Clusters',
        'ConfigureState',
        'ConfigSelectors',
        'ConfigurationDownload'
    ];

    /**
     * @param {uirouter.UIRouter} $uiRouter
     * @param {ModalPreviewProject} ModalPreviewProject
     * @param {Clusters} Clusters
     * @param {ConfigureState} ConfigureState
     * @param {ConfigSelectors} ConfigSelectors
     * @param {ConfigurationDownload} ConfigurationDownload
     */
    constructor($uiRouter, ModalPreviewProject, Clusters, ConfigureState, ConfigSelectors, ConfigurationDownload) {
        this.$uiRouter = $uiRouter;
        this.ModalPreviewProject = ModalPreviewProject;
        this.Clusters = Clusters;
        this.ConfigureState = ConfigureState;
        this.ConfigSelectors = ConfigSelectors;
        this.ConfigurationDownload = ConfigurationDownload;
    }

    $onDestroy() {
        this.selectedRows$.complete();
    }

    /** @param {Array<ig.config.cluster.ShortCluster>} clusters */
    removeClusters(clusters) {
        this.ConfigureState.dispatchAction(confirmClustersRemoval(clusters.map((c) => c._id)));

        // TODO: Implement storing selected rows in store to share this data between other components.
        this.selectedRows$.next([]);
    }

    /** @param {ig.config.cluster.ShortCluster} cluster */
    editCluster(cluster) {
        return this.$uiRouter.stateService.go('^.edit', {clusterID: cluster._id});
    }

    $onInit() {
        this.shortClusters$ = this.ConfigureState.state$.pipe(this.ConfigSelectors.selectShortClustersValue());

        /** @type {Array<uiGrid.IColumnDefOf<ig.config.cluster.ShortCluster>>} */
        this.clustersColumnDefs = [
            {
                name: 'name',
                displayName: 'Name',
                field: 'name',
                enableHiding: false,
                filter: {
                    placeholder: 'Filter by name…'
                },
                sort: {direction: 'asc', priority: 0},
                sortingAlgorithm: naturalCompare,
                cellTemplate: cellTemplate('base.configuration.edit'),
                minWidth: 165
            },
            {
                name: 'discovery',
                displayName: 'Discovery',
                field: 'discovery',
                multiselectFilterOptions: this.Clusters.discoveries,
                width: 150
            },
            {
                name: 'caches',
                displayName: 'Caches',
                field: 'cachesCount',
                cellClass: 'ui-grid-number-cell',
                cellTemplate: cellTemplate('base.configuration.edit.advanced.caches'),
                enableFiltering: false,
                type: 'number',
                width: 95
            },
            {
                name: 'models',
                displayName: 'Models',
                field: 'modelsCount',
                cellClass: 'ui-grid-number-cell',
                cellTemplate: cellTemplate('base.configuration.edit.advanced.models'),
                enableFiltering: false,
                type: 'number',
                width: 95
            },
            {
                name: 'igfs',
                displayName: 'IGFS',
                field: 'igfsCount',
                cellClass: 'ui-grid-number-cell',
                cellTemplate: cellTemplate('base.configuration.edit.advanced.igfs'),
                enableFiltering: false,
                type: 'number',
                width: 80
            }
        ];

        /** @type {Subject<Array<ig.config.cluster.ShortCluster>>} */
        this.selectedRows$ = new Subject();

        this.selectedRowsIDs$ = this.selectedRows$.pipe(map((selectedClusters) => selectedClusters.map((cluster) => cluster._id)));

        this.actions$ = this.selectedRows$.pipe(map((selectedClusters) => [
            {
                action: 'Edit',
                click: () => this.editCluster(selectedClusters[0]),
                available: selectedClusters.length === 1
            },
            {
                action: 'See project structure',
                click: () => this.ModalPreviewProject.open(selectedClusters[0]),
                available: selectedClusters.length === 1
            },
            {
                action: 'Download project',
                click: () => this.ConfigurationDownload.downloadClusterConfiguration(selectedClusters[0]),
                available: selectedClusters.length === 1
            },
            {
                action: 'Delete',
                click: () => this.removeClusters(selectedClusters),
                available: true
            }
        ]));
    }
}
