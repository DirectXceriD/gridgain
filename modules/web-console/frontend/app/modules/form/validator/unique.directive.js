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

import {ListEditableTransclude} from 'app/components/list-editable/components/list-editable-transclude/directive';
import isNumber from 'lodash/fp/isNumber';
import get from 'lodash/fp/get';

class Controller {
    /** @type {ng.INgModelController} */
    ngModel;
    /** @type {ListEditableTransclude} */
    listEditableTransclude;
    /** @type {Array} */
    items;
    /** @type {string?} */
    key;
    /** @type {Array<string>} */
    skip;

    static $inject = ['$scope'];

    /**
     * @param {ng.IScope} $scope
     */
    constructor($scope) {
        this.$scope = $scope;
    }

    $onInit() {
        const isNew = this.key && this.key.startsWith('new');
        const shouldNotSkip = (item) => get(this.skip[0], item) !== get(...this.skip);

        this.ngModel.$validators.igniteUnique = (value) => {
            const matches = (item) => (this.key ? item[this.key] : item) === value;

            if (!this.skip) {
                // Return true in case if array not exist, array empty.
                if (!this.items || !this.items.length)
                    return true;

                const idx = this.items.findIndex(matches);

                // In case of new element check all items.
                if (isNew)
                    return idx < 0;

                // Case for new component list editable.
                const $index = this.listEditableTransclude
                    ? this.listEditableTransclude.$index
                    : isNumber(this.$scope.$index) ? this.$scope.$index : void 0;

                // Check for $index in case of editing in-place.
                return (isNumber($index) && (idx < 0 || $index === idx));
            }
            // TODO: converge both branches, use $index as idKey
            return !(this.items || []).filter(shouldNotSkip).some(matches);
        };
    }

    $onChanges(changes) {
        this.ngModel.$validate();
    }
}

export default () => {
    return {
        controller: Controller,
        require: {
            ngModel: 'ngModel',
            listEditableTransclude: '?^listEditableTransclude'
        },
        bindToController: {
            items: '<igniteUnique',
            key: '@?igniteUniqueProperty',
            skip: '<?igniteUniqueSkip'
        }
    };
};
