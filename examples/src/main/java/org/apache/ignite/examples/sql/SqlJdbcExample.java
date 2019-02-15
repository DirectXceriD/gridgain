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

package org.apache.ignite.examples.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.ignite.examples.ExampleNodeStartup;

/**
 * This example demonstrates usage of Ignite JDBC driver.
 * <p>
 * Ignite nodes must be started in separate process using {@link ExampleNodeStartup} before running this example.
 */
public class SqlJdbcExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws Exception If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        print("JDBC example started.");

        // Open JDBC connection
        try (Connection conn = DriverManager.getConnection("jdbc:ignite:thin://127.0.0.1/")) {
            print("Connected to server.");

            // Create database objects.
            try (Statement stmt = conn.createStatement()) {
                // Create reference City table based on REPLICATED template.
                stmt.executeUpdate("CREATE TABLE city (id LONG PRIMARY KEY, name VARCHAR) " +
                    "WITH \"template=replicated\"");

                // Create table based on PARTITIONED template with one backup.
                stmt.executeUpdate("CREATE TABLE person (id LONG, name VARCHAR, city_id LONG, " +
                    "PRIMARY KEY (id, city_id)) WITH \"backups=1, affinity_key=city_id\"");

                // Create an index.
                stmt.executeUpdate("CREATE INDEX on Person (city_id)");
            }

            print("Created database objects.");

            // Populate City table with PreparedStatement.
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO city (id, name) VALUES (?, ?)")) {
                stmt.setLong(1, 1L);
                stmt.setString(2, "Forest Hill");
                stmt.executeUpdate();

                stmt.setLong(1, 2L);
                stmt.setString(2, "Denver");
                stmt.executeUpdate();

                stmt.setLong(1, 3L);
                stmt.setString(2, "St. Petersburg");
                stmt.executeUpdate();
            }

            // Populate Person table with PreparedStatement.
            try (PreparedStatement stmt =
                conn.prepareStatement("INSERT INTO person (id, name, city_id) values (?, ?, ?)")) {
                stmt.setLong(1, 1L);
                stmt.setString(2, "John Doe");
                stmt.setLong(3, 3L);
                stmt.executeUpdate();

                stmt.setLong(1, 2L);
                stmt.setString(2, "Jane Roe");
                stmt.setLong(3, 2L);
                stmt.executeUpdate();

                stmt.setLong(1, 3L);
                stmt.setString(2, "Mary Major");
                stmt.setLong(3, 1L);
                stmt.executeUpdate();

                stmt.setLong(1, 4L);
                stmt.setString(2, "Richard Miles");
                stmt.setLong(3, 2L);
                stmt.executeUpdate();
            }

            print("Populated data.");

            // Get data.
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs =
                    stmt.executeQuery("SELECT p.name, c.name FROM Person p INNER JOIN City c on c.id = p.city_id")) {
                    print("Query results:");

                    while (rs.next())
                        System.out.println(">>>    " + rs.getString(1) + ", " + rs.getString(2));
                }
            }

            // Drop database objects.
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP TABLE Person");
                stmt.executeUpdate("DROP TABLE City");
            }

            print("Dropped database objects.");
        }

        print("JDBC example finished.");
    }

    /**
     * Prints message.
     *
     * @param msg Message to print before all objects are printed.
     */
    private static void print(String msg) {
        System.out.println();
        System.out.println(">>> " + msg);
    }
}