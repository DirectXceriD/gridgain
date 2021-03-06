#                   GridGain Community Edition Licensing
#                   Copyright 2019 GridGain Systems, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
# Restriction; you may not use this file except in compliance with the License. You may obtain a
# copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the
# License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the specific language governing permissions
# and limitations under the License.
#
# Commons Clause Restriction
#
# The Software is provided to you by the Licensor under the License, as defined below, subject to
# the following condition.
#
# Without limiting other conditions in the License, the grant of rights under the License will not
# include, and the License does not grant to you, the right to Sell the Software.
# For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
# under the License to provide to third parties, for a fee or other consideration (including without
# limitation fees for hosting or consulting/ support services related to the Software), a product or
# service whose value derives, entirely or substantially, from the functionality of the Software.
# Any license notice or attribution required by the License must also include this Commons Clause
# License Condition notice.
#
# For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
# the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
# Edition software provided with this notice.

"""
This module contains functions, that are (more or less) directly mapped to
Apache Ignite binary protocol operations. Read more:

https://apacheignite.readme.io/docs/binary-client-protocol#section-client-operations

When the binary client protocol changes, these functions also change. For
stable end user API see :mod:`pyignite.client` module.
"""

from .cache_config import (
    cache_create,
    cache_get_names,
    cache_get_or_create,
    cache_destroy,
    cache_get_configuration,
    cache_create_with_config,
    cache_get_or_create_with_config,
)
from .key_value import (
    cache_get,
    cache_put,
    cache_get_all,
    cache_put_all,
    cache_contains_key,
    cache_contains_keys,
    cache_get_and_put,
    cache_get_and_replace,
    cache_get_and_remove,
    cache_put_if_absent,
    cache_get_and_put_if_absent,
    cache_replace,
    cache_replace_if_equals,
    cache_clear,
    cache_clear_key,
    cache_clear_keys,
    cache_remove_key,
    cache_remove_if_equals,
    cache_remove_keys,
    cache_remove_all,
    cache_get_size,
)
from .sql import (
    scan,
    scan_cursor_get_page,
    sql,
    sql_cursor_get_page,
    sql_fields,
    sql_fields_cursor_get_page,
    resource_close,
)
from .binary import (
    get_binary_type,
    put_binary_type,
)
from .result import APIResult
