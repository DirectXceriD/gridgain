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
const express = require('express');
const passport = require('passport');

// Fire me up!

module.exports = {
    implements: 'routes/public',
    inject: ['mongo', 'settings', 'services/users', 'services/auth', 'errors']
};

/**                             
 * @param mongo
 * @param settings
 * @param {UsersService} usersService
 * @param {AuthService} authService
 * @param errors
 * @returns {Promise}
 */
module.exports.factory = function(mongo, settings, usersService, authService, errors) {
    return new Promise((factoryResolve) => {
        const router = new express.Router();

        // GET user.
        router.post('/user', (req, res) => {
            usersService.get(req.user, req.session.viewedUser)
                .then(res.api.ok)
                .catch(res.api.error);
        });

        /**
         * Register new account.
         */
        router.post('/signup', (req, res) => {
            const createdByAdmin = _.get(req, 'user.admin', false);

            usersService.create(req.origin(), req.body, createdByAdmin)
                .then((user) => {
                    if (createdByAdmin)
                        return user;

                    return new Promise((resolve, reject) => {
                        req.logIn(user, {}, (err) => {
                            if (err)
                                reject(err);

                            resolve(user);
                        });
                    });
                })
                .then(res.api.ok)
                .catch(res.api.error);
        });

        /**
         * Sign in into exist account.
         */
        router.post('/signin', (req, res, next) => {
            passport.authenticate('local', (errAuth, user) => {
                if (errAuth)
                    return res.api.error(new errors.AuthFailedException(errAuth.message));

                if (!user)
                    return res.api.error(new errors.AuthFailedException('Invalid email or password'));

                if (settings.activation.enabled) {
                    const activationToken = req.body.activationToken;

                    const errToken = authService.validateActivationToken(user, activationToken);

                    if (errToken)
                        return res.api.error(errToken);

                    if (authService.isActivationTokenExpired(user, activationToken)) {
                        authService.resetActivationToken(req.origin(), user.email)
                            .catch((ignored) => {
                                // No-op.
                            });

                        return res.api.error(new errors.AuthFailedException('This activation link was expired. We resend a new one. Please open the most recent email and click on the activation link.'));
                    }

                    user.activated = true;
                }

                return req.logIn(user, {}, (errLogIn) => {
                    if (errLogIn)
                        return res.api.error(new errors.AuthFailedException(errLogIn.message));

                    return res.sendStatus(200);
                });
            })(req, res, next);
        });

        /**
         * Logout.
         */
        router.post('/logout', (req, res) => {
            req.logout();

            res.sendStatus(200);
        });

        /**
         * Send e-mail to user with reset token.
         */
        router.post('/password/forgot', (req, res) => {
            authService.resetPasswordToken(req.origin(), req.body.email)
                .then(() => res.api.ok('An email has been sent with further instructions.'))
                .catch(res.api.error);
        });

        /**
         * Change password with given token.
         */
        router.post('/password/reset', (req, res) => {
            const {token, password} = req.body;

            authService.resetPasswordByToken(req.origin(), token, password)
                .then(res.api.ok)
                .catch(res.api.error);
        });

        /* GET reset password page. */
        router.post('/password/validate/token', (req, res) => {
            const token = req.body.token;

            authService.validateResetToken(token)
                .then(res.api.ok)
                .catch(res.api.error);
        });

        /* Send e-mail to user with account confirmation token. */
        router.post('/activation/resend', (req, res) => {
            authService.resetActivationToken(req.origin(), req.body.email)
                .then(() => res.api.ok('An email has been sent with further instructions.'))
                .catch(res.api.error);
        });

        factoryResolve(router);
    });
};
