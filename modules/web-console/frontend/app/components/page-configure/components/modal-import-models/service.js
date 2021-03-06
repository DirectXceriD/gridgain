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

export default class ModalImportModels {
    static $inject = ['$modal', '$q', '$uiRouter', 'AgentManager'];

    deferred;

    constructor($modal, $q, $uiRouter, AgentManager) {
        this.$modal = $modal;
        this.$q = $q;
        this.$uiRouter = $uiRouter;
        this.AgentManager = AgentManager;
    }

    _goToDynamicState() {
        if (this.deferred)
            return this.deferred.promise;

        this.deferred = this.$q.defer();

        if (this._state)
            this.$uiRouter.stateRegistry.deregister(this._state);

        this._state = this.$uiRouter.stateRegistry.register({
            name: 'importModels',
            parent: this.$uiRouter.stateService.current,
            onEnter: () => {
                this._open();
            },
            onExit: () => {
                this.AgentManager.stopWatch();
                this._modal && this._modal.hide();
            }
        });

        return this.$uiRouter.stateService.go(this._state, this.$uiRouter.stateService.params)
            .catch(() => {
                this.deferred.reject(false);
                this.deferred = null;
            });
    }

    _open() {
        const self = this;

        this._modal = this.$modal({
            template: `
                <modal-import-models
                    on-hide='$ctrl.onHide()'
                    cluster-id='$ctrl.$state.params.clusterID'
                ></modal-import-models>
            `,
            controller: ['$state', function($state) {
                this.$state = $state;

                this.onHide = () => {
                    self.deferred.resolve(true);

                    this.$state.go('^');
                };
            }],
            controllerAs: '$ctrl',
            backdrop: 'static',
            show: false
        });

        return this.AgentManager.startAgentWatch('Back', this.$uiRouter.globals.current.name)
            .then(() => this._modal.$promise)
            .then(() => this._modal.show())
            .then(() => this.deferred.promise)
            .finally(() => this.deferred = null);
    }

    open() {
        this._goToDynamicState();
    }
}
