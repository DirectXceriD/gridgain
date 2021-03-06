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

import debounce from 'lodash/debounce';

export default class ItemsTableController {
    static $inject = ['$scope', 'gridUtil', '$timeout', 'uiGridSelectionService'];

    constructor($scope, gridUtil, $timeout, uiGridSelectionService) {
        Object.assign(this, {$scope, gridUtil, $timeout, uiGridSelectionService});
        this.rowIdentityKey = '_id';
    }

    $onInit() {
        this.grid = {
            data: this.items,
            columnDefs: this.columnDefs,
            rowHeight: 46,
            enableColumnMenus: false,
            enableFullRowSelection: true,
            enableSelectionBatchEvent: true,
            selectionRowHeaderWidth: 52,
            enableColumnCategories: true,
            flatEntityAccess: true,
            headerRowHeight: 70,
            modifierKeysToMultiSelect: true,
            enableFiltering: true,
            rowIdentity: (row) => {
                return row[this.rowIdentityKey];
            },
            onRegisterApi: (api) => {
                this.gridAPI = api;

                api.selection.on.rowSelectionChanged(this.$scope, (row, e) => {
                    this.onRowsSelectionChange([row], e);
                });

                api.selection.on.rowSelectionChangedBatch(this.$scope, (rows, e) => {
                    this.onRowsSelectionChange(rows, e);
                });

                api.core.on.rowsVisibleChanged(this.$scope, () => {
                    const visibleRows = api.core.getVisibleRows();
                    if (this.onVisibleRowsChange) this.onVisibleRowsChange({$event: visibleRows});
                    this.adjustHeight(api, visibleRows.length);
                    this.showFilterNotification = this.grid.data.length && visibleRows.length === 0;
                });

                if (this.onFilterChanged) {
                    api.core.on.filterChanged(this.$scope, () => {
                        this.onFilterChanged();
                    });
                }

                this.$timeout(() => {
                    if (this.selectedRowId) this.applyIncomingSelection(this.selectedRowId);
                });
            },
            appScopeProvider: this.$scope.$parent
        };
        this.actionsMenu = this.makeActionsMenu(this.incomingActionsMenu);
    }

    oneWaySelection = false;

    onRowsSelectionChange = debounce((rows, e = {}) => {
        if (e.ignore)
            return;

        const selected = this.gridAPI.selection.legacyGetSelectedRows();

        if (this.oneWaySelection)
            rows.forEach((r) => r.isSelected = false);

        if (this.onSelectionChange)
            this.onSelectionChange({$event: selected});
    });

    makeActionsMenu(incomingActionsMenu = []) {
        return incomingActionsMenu;
    }

    $onChanges(changes) {
        const hasChanged = (binding) => binding in changes && changes[binding].currentValue !== changes[binding].previousValue;

        if (hasChanged('items') && this.grid) {
            this.grid.data = changes.items.currentValue;
            this.gridAPI.grid.modifyRows(this.grid.data);
            this.adjustHeight(this.gridAPI, this.grid.data.length);

            // Without property existence check non-set selectedRowId binding might cause
            // unwanted behavior, like unchecking rows during any items change, even if
            // nothing really changed.
            if ('selectedRowId' in this)
                this.applyIncomingSelection(this.selectedRowId);
        }

        if (hasChanged('selectedRowId') && this.grid && this.grid.data)
            this.applyIncomingSelection(changes.selectedRowId.currentValue);

        if ('incomingActionsMenu' in changes)
            this.actionsMenu = this.makeActionsMenu(changes.incomingActionsMenu.currentValue);
    }

    applyIncomingSelection(selected = []) {
        this.gridAPI.selection.clearSelectedRows({ignore: true});
        const rows = this.grid.data.filter((r) => selected.includes(r[this.rowIdentityKey]));

        rows.forEach((r) => {
            this.gridAPI.selection.selectRow(r, {ignore: true});
        });

        if (rows.length === 1) {
            this.$timeout(() => {
                this.gridAPI.grid.scrollToIfNecessary(this.gridAPI.grid.getRow(rows[0]), null);
            });
        }
    }

    adjustHeight(api, rows) {
        const maxRowsToShow = this.maxRowsToShow || 5;
        const headerBorder = 1;
        const header = this.grid.headerRowHeight + headerBorder;
        const optionalScroll = (rows ? this.gridUtil.getScrollbarWidth() : 0);
        const height = Math.min(rows, maxRowsToShow) * this.grid.rowHeight + header + optionalScroll;
        api.grid.element.css('height', height + 'px');
        api.core.handleWindowResize();
    }
}
