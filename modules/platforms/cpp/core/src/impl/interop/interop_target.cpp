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

#include "ignite/impl/interop/interop_target.h"
#include "ignite/impl/binary/binary_type_updater_impl.h"

using namespace ignite::common::concurrent;
using namespace ignite::jni::java;
using namespace ignite::java;
using namespace ignite::impl::interop;
using namespace ignite::impl::binary;
using namespace ignite::binary;

namespace ignite
{
    namespace impl
    {
        namespace interop
        {
            InteropTarget::InteropTarget(SharedPointer<IgniteEnvironment> env, jobject javaRef) :
                env(env), javaRef(javaRef), skipJavaRefRelease(false)
            {
                // No-op.
            }

            InteropTarget::InteropTarget(SharedPointer<IgniteEnvironment> env, jobject javaRef, 
                bool skipJavaRefRelease) :
                env(env), javaRef(javaRef), skipJavaRefRelease(skipJavaRefRelease)
            {
                // No-op.
            }

            InteropTarget::~InteropTarget()
            {
                if (!skipJavaRefRelease) 
                {
                    JniContext::Release(javaRef);
                }
            }

            int64_t InteropTarget::WriteTo(InteropMemory* mem, InputOperation& inOp, IgniteError& err)
            {
                BinaryTypeManager* metaMgr = env.Get()->GetTypeManager();

                int32_t metaVer = metaMgr->GetVersion();

                InteropOutputStream out(mem);
                BinaryWriterImpl writer(&out, metaMgr);
                
                inOp.ProcessInput(writer);

                out.Synchronize();

                if (metaMgr->IsUpdatedSince(metaVer))
                {
                    if (!metaMgr->ProcessPendingUpdates(err))
                        return 0;
                }

                return mem->PointerLong();
            }

            void InteropTarget::ReadFrom(InteropMemory* mem, OutputOperation& outOp)
            {
                InteropInputStream in(mem);
                BinaryReaderImpl reader(&in);

                outOp.ProcessOutput(reader);
            }

            void InteropTarget::ReadError(InteropMemory* mem, IgniteError& err)
            {
                InteropInputStream in(mem);
                BinaryReaderImpl reader(&in);

                // Reading and skipping error class name.
                reader.ReadObject<std::string>();

                std::string msg = reader.ReadObject<std::string>();

                err = IgniteError(IgniteError::IGNITE_ERR_GENERIC, msg.c_str());
            }

            bool InteropTarget::OutOp(int32_t opType, InputOperation& inOp, IgniteError& err)
            {
                JniErrorInfo jniErr;

                SharedPointer<InteropMemory> mem = env.Get()->AllocateMemory();

                int64_t outPtr = WriteTo(mem.Get(), inOp, err);

                if (outPtr)
                {
                    long long res = env.Get()->Context()->TargetInStreamOutLong(javaRef, opType, outPtr, &jniErr);

                    IgniteError::SetError(jniErr.code, jniErr.errCls, jniErr.errMsg, err);

                    if (jniErr.code == IGNITE_JNI_ERR_SUCCESS)
                        return res == 1;
                }

                return false;
            }

            bool InteropTarget::OutOp(int32_t opType, IgniteError& err)
            {
                JniErrorInfo jniErr;

                long long res = env.Get()->Context()->TargetInLongOutLong(javaRef, opType, 0, &jniErr);

                IgniteError::SetError(jniErr.code, jniErr.errCls, jniErr.errMsg, err);

                if (jniErr.code == IGNITE_JNI_ERR_SUCCESS)
                    return res == 1;

                return false;
            }

            bool InteropTarget::InOp(int32_t opType, OutputOperation& outOp, IgniteError& err)
            {
                JniErrorInfo jniErr;

                SharedPointer<InteropMemory> mem = env.Get()->AllocateMemory();

                env.Get()->Context()->TargetOutStream(javaRef, opType, mem.Get()->PointerLong(), &jniErr);

                IgniteError::SetError(jniErr.code, jniErr.errCls, jniErr.errMsg, err);

                if (jniErr.code == IGNITE_JNI_ERR_SUCCESS)
                {
                    ReadFrom(mem.Get(), outOp);

                    return true;
                }

                return false;
            }

            jobject InteropTarget::InOpObject(int32_t opType, IgniteError& err)
            {
                JniErrorInfo jniErr;

                jobject res = env.Get()->Context()->TargetOutObject(javaRef, opType, &jniErr);

                IgniteError::SetError(jniErr.code, jniErr.errCls, jniErr.errMsg, err);

                return res;
            }

            void InteropTarget::OutInOp(int32_t opType, InputOperation& inOp, OutputOperation& outOp, IgniteError& err)
            {
                JniErrorInfo jniErr;

                SharedPointer<InteropMemory> outMem = env.Get()->AllocateMemory();
                SharedPointer<InteropMemory> inMem = env.Get()->AllocateMemory();

                int64_t outPtr = WriteTo(outMem.Get(), inOp, err);

                if (outPtr)
                {
                    env.Get()->Context()->TargetInStreamOutStream(javaRef, opType, outPtr,
                        inMem.Get()->PointerLong(), &jniErr);

                    IgniteError::SetError(jniErr.code, jniErr.errCls, jniErr.errMsg, err);

                    if (jniErr.code == IGNITE_JNI_ERR_SUCCESS)
                        ReadFrom(inMem.Get(), outOp);
                }
            }

            void InteropTarget::OutInOpX(int32_t opType, InputOperation& inOp, OutputOperation& outOp, IgniteError& err)
            {
                JniErrorInfo jniErr;

                SharedPointer<InteropMemory> outInMem = env.Get()->AllocateMemory();

                int64_t outInPtr = WriteTo(outInMem.Get(), inOp, err);

                if (outInPtr)
                {
                    int64_t res = env.Get()->Context()->TargetInStreamOutLong(javaRef, opType, outInPtr, &jniErr);

                    IgniteError::SetError(jniErr.code, jniErr.errCls, jniErr.errMsg, err);

                    if (jniErr.code == IGNITE_JNI_ERR_SUCCESS && res == OperationResult::AI_SUCCESS)
                        ReadFrom(outInMem.Get(), outOp);
                    else if (res == OperationResult::AI_NULL)
                        outOp.SetNull();
                    else if (res == OperationResult::AI_ERROR)
                        ReadError(outInMem.Get(), err);
                    else
                        assert(false);
                }
            }

            InteropTarget::OperationResult::Type InteropTarget::InStreamOutLong(int32_t opType,
                InteropMemory& outInMem, IgniteError& err)
            {
                JniErrorInfo jniErr;

                int64_t outInPtr = outInMem.PointerLong();

                if (outInPtr)
                {
                    int64_t res = env.Get()->Context()->TargetInStreamOutLong(javaRef, opType, outInPtr, &jniErr);

                    IgniteError::SetError(jniErr.code, jniErr.errCls, jniErr.errMsg, err);

                    return static_cast<OperationResult::Type>(res);
                }

                return OperationResult::AI_ERROR;
            }

            jobject InteropTarget::InStreamOutObject(int32_t opType, InteropMemory& outInMem, IgniteError& err)
            {
                JniErrorInfo jniErr;

                int64_t outInPtr = outInMem.PointerLong();

                if (outInPtr)
                {
                    jobject res = env.Get()->Context()->TargetInStreamOutObject(javaRef, opType, outInPtr, &jniErr);

                    IgniteError::SetError(jniErr.code, jniErr.errCls, jniErr.errMsg, err);

                    return res;
                }

                return 0;
            }

            int64_t InteropTarget::OutInOpLong(int32_t opType, int64_t val, IgniteError& err)
            {
                JniErrorInfo jniErr;

                long long res = env.Get()->Context()->TargetInLongOutLong(javaRef, opType, val, &jniErr);

                IgniteError::SetError(jniErr.code, jniErr.errCls, jniErr.errMsg, err);

                return res;
            }
        }
    }
}
