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

#include "ignite/transactions/transaction.h"

using namespace ignite::common::concurrent;
using namespace ignite::impl::transactions;

namespace ignite
{
    namespace transactions
    {
        Transaction::Transaction(SharedPointer<TransactionImpl> impl) :
            impl(impl)
        {
            // No-op.
        }

        Transaction::Transaction(const Transaction & other) :
            impl(other.impl)
        {
            // No-op.
        }

        Transaction& Transaction::operator=(const Transaction& other)
        {
            impl = other.impl;

            return *this;
        }

        Transaction::~Transaction()
        {
            // No-op.
        }

        void Transaction::Commit()
        {
            IgniteError err;

            Commit(err);

            IgniteError::ThrowIfNeeded(err);
        }

        void Transaction::Commit(IgniteError & err)
        {
            err = IgniteError();

            TransactionImpl* txImpl = impl.Get();

            if (txImpl)
                txImpl->Commit(err);
            else
            {
                err = IgniteError(IgniteError::IGNITE_ERR_GENERIC,
                    "Instance is not usable (did you check for error?).");
            }
        }

        void Transaction::Rollback()
        {
            IgniteError err;

            Rollback(err);

            IgniteError::ThrowIfNeeded(err);
        }

        void Transaction::Rollback(IgniteError & err)
        {
            err = IgniteError();

            TransactionImpl* txImpl = impl.Get();

            if (txImpl)
                txImpl->Rollback(err);
            else
            {
                err = IgniteError(IgniteError::IGNITE_ERR_GENERIC,
                    "Instance is not usable (did you check for error?).");
            }
        }

        void Transaction::Close()
        {
            IgniteError err;

            Close(err);

            IgniteError::ThrowIfNeeded(err);
        }

        void Transaction::Close(IgniteError & err)
        {
            err = IgniteError();

            TransactionImpl* txImpl = impl.Get();

            if (txImpl)
                txImpl->Close(err);
            else
            {
                err = IgniteError(IgniteError::IGNITE_ERR_GENERIC,
                    "Instance is not usable (did you check for error?).");
            }
        }

        void Transaction::SetRollbackOnly()
        {
            IgniteError err;

            SetRollbackOnly(err);

            IgniteError::ThrowIfNeeded(err);
        }

        void Transaction::SetRollbackOnly(IgniteError & err)
        {
            err = IgniteError();

            TransactionImpl* txImpl = impl.Get();

            if (txImpl)
                txImpl->SetRollbackOnly(err);
            else
            {
                err = IgniteError(IgniteError::IGNITE_ERR_GENERIC,
                    "Instance is not usable (did you check for error?).");
            }
        }

        bool Transaction::IsRollbackOnly()
        {
            IgniteError err;

            bool res = IsRollbackOnly(err);

            IgniteError::ThrowIfNeeded(err);

            return res;
        }

        bool Transaction::IsRollbackOnly(IgniteError& err)
        {
            err = IgniteError();

            TransactionImpl* txImpl = impl.Get();

            if (txImpl)
                return txImpl->IsRollbackOnly(err);
            else
            {
                err = IgniteError(IgniteError::IGNITE_ERR_GENERIC,
                    "Instance is not usable (did you check for error?).");
            }

            return false;
        }

        TransactionState::Type Transaction::GetState()
        {
            IgniteError err;

            TransactionState::Type res = GetState(err);

            IgniteError::ThrowIfNeeded(err);

            return res;
        }

        TransactionState::Type Transaction::GetState(IgniteError& err)
        {
            err = IgniteError();

            TransactionImpl* txImpl = impl.Get();

            if (txImpl)
                return txImpl->GetState(err);
            else
            {
                err = IgniteError(IgniteError::IGNITE_ERR_GENERIC,
                    "Instance is not usable (did you check for error?).");
            }

            return TransactionState::UNKNOWN;
        }
    }
}

