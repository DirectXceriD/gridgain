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

import {suite, test} from 'mocha';
import {assert} from 'chai';
import {spy} from 'sinon';
import {of, throwError} from 'rxjs';
import {TestScheduler} from 'rxjs/testing';

const mocks = () => new Map([
    ['IgniteConfigurationResource', {}],
    ['$state', {}],
    ['ConfigureState', {}],
    ['Clusters', {}]
]);

import {REMOVE_CLUSTERS_LOCAL_REMOTE, CLONE_CLUSTERS} from './PageConfigure';
import PageConfigure from './PageConfigure';
import {REMOVE_CLUSTERS, LOAD_LIST, ADD_CLUSTERS, UPDATE_CLUSTER} from '../reducer';

suite.skip('PageConfigure service', () => {
    suite('cloneCluster$ effect', () => {
        test('successfull clusters clone', () => {
            const testScheduler = new TestScheduler((...args) => assert.deepEqual(...args));
            const values = {
                s: {
                    list: {
                        clusters: new Map([
                            [1, {_id: 1, name: 'Cluster 1'}],
                            [2, {_id: 2, name: 'Cluster 1 (clone)'}]
                        ])
                    }
                },
                a: {
                    type: CLONE_CLUSTERS,
                    clusters: [
                        {_id: 1, name: 'Cluster 1'},
                        {_id: 2, name: 'Cluster 1 (clone)'}
                    ]
                },
                b: {
                    type: ADD_CLUSTERS,
                    clusters: [
                        {_id: -1, name: 'Cluster 1 (clone) (1)'},
                        {_id: -2, name: 'Cluster 1 (clone) (clone)'}
                    ]
                },
                c: {
                    type: UPDATE_CLUSTER,
                    _id: -1,
                    cluster: {_id: 99}
                },
                d: {
                    type: UPDATE_CLUSTER,
                    _id: -2,
                    cluster: {_id: 99}
                }
            };
            const actions = '-a----';
            const state   = 's-----';
            const output  = '-(bcd)';

            const deps = mocks()
            .set('Clusters', {
                saveCluster$: (c) => of({data: 99})
            })
            .set('ConfigureState', {
                actions$: testScheduler.createHotObservable(actions, values),
                state$: testScheduler.createHotObservable(state, values),
                dispatchAction: spy()
            });

            const s = new PageConfigure(...deps.values());

            testScheduler.expectObservable(s.cloneClusters$).toBe(output, values);
            testScheduler.flush();
            assert.equal(s.ConfigureState.dispatchAction.callCount, 3);
        });
        test('some clusters clone failure', () => {
            const testScheduler = new TestScheduler((...args) => assert.deepEqual(...args));
            const values = {
                s: {
                    list: {
                        clusters: new Map([
                            [1, {_id: 1, name: 'Cluster 1'}],
                            [2, {_id: 2, name: 'Cluster 1 (clone)'}]
                        ])
                    }
                },
                a: {
                    type: CLONE_CLUSTERS,
                    clusters: [
                        {_id: 1, name: 'Cluster 1'},
                        {_id: 2, name: 'Cluster 1 (clone)'}
                    ]
                },
                b: {
                    type: ADD_CLUSTERS,
                    clusters: [
                        {_id: -1, name: 'Cluster 1 (clone) (1)'},
                        {_id: -2, name: 'Cluster 1 (clone) (clone)'}
                    ]
                },
                c: {
                    type: UPDATE_CLUSTER,
                    _id: -1,
                    cluster: {_id: 99}
                },
                d: {
                    type: REMOVE_CLUSTERS,
                    clusterIDs: [-2]
                }
            };
            const actions = '-a----';
            const state   = 's-----';
            const output  = '-(bcd)';

            const deps = mocks()
            .set('Clusters', {
                saveCluster$: (c) => c.name === values.b.clusters[0].name
                    ? of({data: 99})
                    : throwError()
            })
            .set('ConfigureState', {
                actions$: testScheduler.createHotObservable(actions, values),
                state$: testScheduler.createHotObservable(state, values),
                dispatchAction: spy()
            });

            const s = new PageConfigure(...deps.values());

            testScheduler.expectObservable(s.cloneClusters$).toBe(output, values);
            testScheduler.flush();
            assert.equal(s.ConfigureState.dispatchAction.callCount, 3);
        });
    });
    suite('removeCluster$ effect', () => {
        test('successfull clusters removal', () => {
            const testScheduler = new TestScheduler((...args) => assert.deepEqual(...args));

            const values = {
                a: {
                    type: REMOVE_CLUSTERS_LOCAL_REMOTE,
                    clusters: [1, 2, 3, 4, 5].map((i) => ({_id: i}))
                },
                b: {
                    type: REMOVE_CLUSTERS,
                    clusterIDs: [1, 2, 3, 4, 5]
                },
                c: {
                    type: LOAD_LIST,
                    list: []
                },
                d: {
                    type: REMOVE_CLUSTERS,
                    clusterIDs: [1, 2, 3, 4, 5]
                },
                s: {
                    list: []
                }
            };

            const actions = '-a';
            const state   = 's-';
            const output  = '-d';

            const deps = mocks()
            .set('ConfigureState', {
                actions$: testScheduler.createHotObservable(actions, values),
                state$: testScheduler.createHotObservable(state, values),
                dispatchAction: spy()
            })
            .set('Clusters', {
                removeCluster$: (v) => of(v)
            });
            const s = new PageConfigure(...deps.values());

            testScheduler.expectObservable(s.removeClusters$).toBe(output, values);
            testScheduler.flush();
            assert.equal(s.ConfigureState.dispatchAction.callCount, 1);
        });
        test('some clusters removal failure', () => {
            const testScheduler = new TestScheduler((...args) => assert.deepEqual(...args));

            const values = {
                a: {
                    type: REMOVE_CLUSTERS_LOCAL_REMOTE,
                    clusters: [1, 2, 3, 4, 5].map((i) => ({_id: i}))
                },
                b: {
                    type: REMOVE_CLUSTERS,
                    clusterIDs: [1, 2, 3, 4, 5]
                },
                c: {
                    type: LOAD_LIST,
                    list: []
                },
                d: {
                    type: REMOVE_CLUSTERS,
                    clusterIDs: [1, 3, 5]
                },
                s: {
                    list: []
                }
            };

            const actions = '-a----';
            const state   = 's-----';
            const output  = '-(bcd)';

            const deps = mocks()
            .set('ConfigureState', {
                actions$: testScheduler.createHotObservable(actions, values),
                state$: testScheduler.createHotObservable(state, values),
                dispatchAction: spy()
            })
            .set('Clusters', {
                removeCluster$: (v) => v._id % 2 ? of(v) : throwError()
            });
            const s = new PageConfigure(...deps.values());

            testScheduler.expectObservable(s.removeClusters$).toBe(output, values);
            testScheduler.flush();
            assert.equal(s.ConfigureState.dispatchAction.callCount, 3);
        });
    });
});
