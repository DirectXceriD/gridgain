##
##                   GridGain Community Edition Licensing
##                   Copyright 2019 GridGain Systems, Inc.
##
## Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
## Restriction;  you may not use this file except in compliance with the License. You may obtain a
## copy of the License at
##
## http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software distributed under the
## License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
## KIND, either express or implied. See the License for the specific language governing permissions
## and limitations under the License.
##
## Commons Clause Restriction
##
## The Software is provided to you by the Licensor under the License, as defined below, subject to
## the following condition.
##
## Without limiting other conditions in the License, the grant of rights under the License will not
## include, and the License does not grant to you, the right to Sell the Software.
## For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
## under the License to provide to third parties, for a fee or other consideration (including without
## limitation fees for hosting or consulting/ support services related to the Software), a product or
## service whose value derives, entirely or substantially, from the functionality of the Software.
## Any license notice or attribution required by the License must also include this Commons Clause
## License Condition notice.
##
## For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
## the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
## Edition software provided with this notice.
##

ACLOCAL_AMFLAGS =-I m4

lib_LTLIBRARIES = libignite-odbc.la

SUBDIRS = \
    include

AM_CPPFLAGS = \
    -I$(srcdir)/include \
    -I@top_srcdir@/common/include \
    -I@top_srcdir@/common/os/linux/include \
    -I@top_srcdir@/binary/include \
    -DIGNITE_IMPL \
    -D__STDC_LIMIT_MACROS \
    -D__STDC_CONSTANT_MACROS

AM_CXXFLAGS = \
    -Wall \
    -std=c++03

libignite_odbc_la_LIBADD = \
    @top_srcdir@/binary/libignite-binary.la \
    -lodbcinst

libignite_odbc_la_LDFLAGS = \
    -no-undefined \
    -ldl \
    -version-info 0:0:0 \
    -release $(PACKAGE_VERSION)

libignite_odbc_la_DEPENDENCIES = \
    @top_srcdir@/binary/libignite-binary.la

libignite_odbc_la_SOURCES = \
    os/linux/src/system/tcp_socket_client.cpp \
    src/app/application_data_buffer.cpp \
    src/app/parameter.cpp \
    src/app/parameter_set.cpp \
    src/common_types.cpp \
    src/config/config_tools.cpp \
    src/config/configuration.cpp \
    src/config/connection_info.cpp \
    src/config/connection_string_parser.cpp \
    src/connection.cpp \
    src/cursor.cpp \
    src/diagnostic/diagnosable_adapter.cpp \
    src/diagnostic/diagnostic_record.cpp \
    src/diagnostic/diagnostic_record_storage.cpp \
    src/environment.cpp \
    src/meta/column_meta.cpp \
    src/meta/table_meta.cpp \
    src/odbc.cpp \
    src/entry_points.cpp \
    src/dsn_config.cpp \
    src/query/column_metadata_query.cpp \
    src/query/data_query.cpp \
    src/query/batch_query.cpp \
    src/query/foreign_keys_query.cpp \
    src/query/primary_keys_query.cpp \
    src/query/table_metadata_query.cpp \
    src/query/type_info_query.cpp \
    src/query/special_columns_query.cpp \
    src/query/streaming_query.cpp \
    src/ssl/ssl_gateway.cpp \
    src/ssl/secure_socket_client.cpp \
    src/ssl/ssl_mode.cpp \
    src/ssl/ssl_api.cpp \
    src/sql/sql_lexer.cpp \
    src/sql/sql_parser.cpp \
    src/sql/sql_set_streaming_command.cpp \
    src/sql/sql_utils.cpp \
    src/streaming/streaming_batch.cpp \
    src/streaming/streaming_context.cpp \
    src/protocol_version.cpp \
    src/result_page.cpp \
    src/row.cpp \
    src/nested_tx_mode.cpp \
    src/message.cpp \
    src/column.cpp \
    src/statement.cpp \
    src/type_traits.cpp \
    src/utility.cpp \
    src/log.cpp

clean-local:
	$(RM) *.gcno *.gcda

clean-docs:
	$(RM) $(DX_CLEANFILES)
