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

#include <algorithm>
#include <cassert>

#include "ignite/common/bits.h"

namespace ignite
{
    namespace common
    {
        namespace bits
        {
            int32_t NumberOfTrailingZerosI32(int32_t i)
            {
                int32_t y;

                if (i == 0) return 32;

                int32_t n = 31;

                y = i << 16;

                if (y != 0) {
                    n = n - 16;
                    i = y;
                }

                y = i << 8; 

                if (y != 0) {
                    n = n - 8;
                    i = y;
                }

                y = i << 4;

                if (y != 0) {
                    n = n - 4;
                    i = y;
                }

                y = i << 2;

                if (y != 0) {
                    n = n - 2;
                    i = y;
                }

                return n - static_cast<int32_t>(static_cast<uint32_t>(i << 1) >> 31);
            }

            int32_t NumberOfLeadingZerosI32(int32_t i)
            {
                return NumberOfLeadingZerosU32(static_cast<uint32_t>(i));
            }

            int32_t NumberOfLeadingZerosU32(uint32_t i)
            {
                if (i == 0)
                    return 32;

                int32_t n = 1;

                if (i >> 16 == 0) {
                    n += 16;
                    i <<= 16;
                }

                if (i >> 24 == 0) {
                    n += 8;
                    i <<= 8;
                }

                if (i >> 28 == 0) {
                    n += 4;
                    i <<= 4;
                }

                if (i >> 30 == 0) {
                    n += 2;
                    i <<= 2;
                }

                return n - static_cast<int32_t>(i >> 31);
            }

            int32_t NumberOfLeadingZerosI64(int64_t i)
            {
                return NumberOfLeadingZerosU64(static_cast<uint64_t>(i));
            }

            int32_t NumberOfLeadingZerosU64(uint64_t i)
            {
                if (i == 0)
                    return 64;

                int32_t n = 1;

                uint32_t x = static_cast<uint32_t>(i >> 32);

                if (x == 0) {
                    n += 32;
                    x = static_cast<uint32_t>(i);
                }

                if (x >> 16 == 0) {
                    n += 16;
                    x <<= 16;
                }

                if (x >> 24 == 0) {
                    n += 8;
                    x <<= 8;
                }

                if (x >> 28 == 0) {
                    n += 4;
                    x <<= 4;
                }

                if (x >> 30 == 0) {
                    n += 2;
                    x <<= 2;
                }

                n -= x >> 31;

                return n;
            }

            int32_t BitCountI32(int32_t i)
            {
                uint32_t ui = static_cast<uint32_t>(i);

                ui -= (ui >> 1) & 0x55555555;
                ui = (ui & 0x33333333) + ((ui >> 2) & 0x33333333);
                ui = (ui + (ui >> 4)) & 0x0f0f0f0f;
                ui += ui >> 8;
                ui += ui >> 16;

                return static_cast<int32_t>(ui & 0x3f);
            }

            int32_t BitLengthI32(int32_t i)
            {
                return 32 - NumberOfLeadingZerosI32(i);
            }

            int32_t BitLengthU32(uint32_t i)
            {
                return 32 - NumberOfLeadingZerosU32(i);
            }

            int32_t GetCapasityForSize(int32_t size)
            {
                assert(size > 0);

                if (size <= 8)
                    return 8;

                int32_t bl = BitLengthI32(size);

                if (bl > 30)
                    return INT32_MAX;

                int32_t res = 1 << bl;

                return size > res ? res << 1 : res;
            }

            int32_t DigitLength(uint64_t x)
            {
                // See http://graphics.stanford.edu/~seander/bithacks.html
                // for the details on the algorithm.

                if (x < 10)
                    return 1;

                int32_t r = ((64 - NumberOfLeadingZerosU64(x) + 1) * 1233) >> 12;

                assert(r <= UINT64_MAX_PRECISION);

                return (r == UINT64_MAX_PRECISION || x < TenPowerU64(r)) ? r : r + 1;
            }

            uint64_t TenPowerU64(int32_t n)
            {
                static const uint64_t TEN_POWERS_TABLE[UINT64_MAX_PRECISION] = {
                    1ULL,                     // 0  / 10^0
                    10ULL,                    // 1  / 10^1
                    100ULL,                   // 2  / 10^2
                    1000ULL,                  // 3  / 10^3
                    10000ULL,                 // 4  / 10^4
                    100000ULL,                // 5  / 10^5
                    1000000ULL,               // 6  / 10^6
                    10000000ULL,              // 7  / 10^7
                    100000000ULL,             // 8  / 10^8
                    1000000000ULL,            // 9  / 10^9
                    10000000000ULL,           // 10 / 10^10
                    100000000000ULL,          // 11 / 10^11
                    1000000000000ULL,         // 12 / 10^12
                    10000000000000ULL,        // 13 / 10^13
                    100000000000000ULL,       // 14 / 10^14
                    1000000000000000ULL,      // 15 / 10^15
                    10000000000000000ULL,     // 16 / 10^16
                    100000000000000000ULL,    // 17 / 10^17
                    1000000000000000000ULL,   // 18 / 10^18
                    10000000000000000000ULL   // 19 / 10^19
                };

                assert(n >= 0 && n < UINT64_MAX_PRECISION);

                return TEN_POWERS_TABLE[n];
            }
        }
    }
}
