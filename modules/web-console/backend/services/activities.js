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

'use strict';

const _ = require('lodash');

// Fire me up!

module.exports = {
    implements: 'services/activities',
    inject: ['mongo']
};

/**
 * @param mongo
 * @returns {ActivitiesService}
 */
module.exports.factory = (mongo) => {
    class ActivitiesService {
        /**
         * Update page activities.
         *
         * @param {Object} user - User.
         * @param {String} action - Action string presentation.
         * @param {String} group - Action group string presentation.
         * @param {Date} [now] - Optional date to save in activity.
         * @returns {Promise.<mongo.ObjectId>} that resolve activity.
         */
        static merge(user, {action, group}, now = new Date()) {
            const owner = user._id;

            mongo.Account.findById(owner)
                .then((user) => {
                    user.lastActivity = new Date();

                    return user.save();
                });

            const date = Date.UTC(now.getFullYear(), now.getMonth(), 1);

            return mongo.Activities.findOneAndUpdate({owner, action, date},
                {$set: {owner, group, action, date}, $inc: {amount: 1}}, {new: true, upsert: true}).exec();
        }

        static total({startDate, endDate}) {
            const $match = {};

            if (startDate)
                $match.date = {$gte: new Date(startDate)};

            if (endDate) {
                $match.date = $match.date || {};
                $match.date.$lt = new Date(endDate);
            }

            return mongo.Activities.aggregate([
                {$match},
                {$group: {
                    _id: {owner: '$owner', group: '$group'},
                    amount: {$sum: '$amount'}
                }}
            ]).exec().then((data) => {
                return _.reduce(data, (acc, { _id, amount }) => {
                    const {owner, group} = _id;
                    acc[owner] = _.merge(acc[owner] || {}, { [group]: amount });
                    return acc;
                }, {});
            });
        }

        static detail({startDate, endDate}) {
            const $match = { };

            if (startDate)
                $match.date = {$gte: new Date(startDate)};

            if (endDate) {
                $match.date = $match.date || {};
                $match.date.$lt = new Date(endDate);
            }

            return mongo.Activities.aggregate([
                {$match},
                {$group: {_id: {owner: '$owner', action: '$action'}, total: {$sum: '$amount'}}}
            ]).exec().then((data) => {
                return _.reduce(data, (acc, { _id, total }) => {
                    const {owner, action} = _id;
                    acc[owner] = _.merge(acc[owner] || {}, { [action]: total });
                    return acc;
                }, {});
            });
        }
    }

    return ActivitiesService;
};
