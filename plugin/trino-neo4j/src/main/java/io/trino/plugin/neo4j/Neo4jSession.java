/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.neo4j;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import io.airlift.log.Logger;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.type.BigintType;
import io.trino.spi.type.BooleanType;
import io.trino.spi.type.DoubleType;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.Node;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class Neo4jSession
        implements Closeable
{
    private static final Logger log = Logger.get(Neo4jSession.class);

    private final Driver driver;
    private final String database;
    private final boolean caseInsensitiveNameMatching;

    @Inject
    public Neo4jSession(Neo4jConfig config)
    {
        requireNonNull(config, "config is null");

        Config.ConfigBuilder configBuilder = Config.builder()
                .withMaxConnectionPoolSize(config.getMaxConnectionPoolSize())
                .withConnectionTimeout(config.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .withMaxConnectionLifetime(config.getMaxConnectionLifetime().toMillis(), TimeUnit.MILLISECONDS)
                .withConnectionLivenessCheckTimeout(config.getMaxConnectionIdleTime().toMillis(), TimeUnit.MILLISECONDS);

        if (config.getUsername() != null && config.getPassword() != null) {
            this.driver = GraphDatabase.driver(
                    config.getUri(),
                    AuthTokens.basic(config.getUsername(), config.getPassword()),
                    configBuilder.build());
        }
        else {
            this.driver = GraphDatabase.driver(config.getUri(), configBuilder.build());
        }

        this.database = config.getDatabase();
        this.caseInsensitiveNameMatching = config.isCaseInsensitiveNameMatching();
    }

    public List<String> getAllSchemas()
    {
        // In Neo4j, we'll use the database as schema
        return ImmutableList.of(database);
    }

    public Set<String> getAllTables(String schemaName)
    {
        ImmutableSet.Builder<String> tables = ImmutableSet.builder();

        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            // Get all node labels (which we'll treat as tables)
            Result result = session.run("CALL db.labels()");
            while (result.hasNext()) {
                Record record = result.next();
                String label = record.get("label").asString();
                tables.add(label);
            }
        }
        catch (Exception e) {
            log.error(e, "Failed to get all tables");
        }

        return tables.build();
    }

    public Neo4jTableHandle getTableHandle(SchemaTableName tableName)
    {
        requireNonNull(tableName, "tableName is null");

        if (!database.equals(tableName.getSchemaName())) {
            return null;
        }

        return new Neo4jTableHandle(tableName.getSchemaName(), tableName.getTableName());
    }

    public Neo4jTableMetadata getTableMetadata(Neo4jTableHandle tableHandle)
    {
        requireNonNull(tableHandle, "tableHandle is null");

        ImmutableList.Builder<ColumnMetadata> columns = ImmutableList.builder();

        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            // Sample a node to infer schema
            String query = String.format("MATCH (n:%s) RETURN n LIMIT 1", tableHandle.getTableName());
            log.info("Querying schema for table %s: %s", tableHandle.getTableName(), query);
            Result result = session.run(query);

            if (result.hasNext()) {
                Record record = result.next();
                Node node = record.get("n").asNode();

                // Add internal ID column
                columns.add(new ColumnMetadata("id", BigintType.BIGINT));

                // Add property columns
                Map<String, Object> properties = node.asMap();
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    Type type = inferType(entry.getValue());
                    columns.add(new ColumnMetadata(entry.getKey(), type));
                }
            }
            else {
                // No nodes found, return minimal schema
                columns.add(new ColumnMetadata("id", BigintType.BIGINT));
            }
        }
        catch (Exception e) {
            log.error(e, "Failed to get table metadata for %s", tableHandle.getTableName());
            // Return minimal schema on error
            columns.add(new ColumnMetadata("id", BigintType.BIGINT));
        }

        SchemaTableName schemaTableName = new SchemaTableName(tableHandle.getSchemaName(), tableHandle.getTableName());
        return new Neo4jTableMetadata(schemaTableName, columns.build());
    }

    public List<Record> executeQuery(String query)
    {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            Result result = session.run(query);
            return result.list();
        }
    }

    public List<Record> executeQueryWithLimit(String label, int limit)
    {
        String query = String.format("MATCH (n:%s) RETURN n LIMIT %d", label, limit);
        return executeQuery(query);
    }

    private Type inferType(Object value)
    {
        if (value == null) {
            return VarcharType.VARCHAR;
        }

        if (value instanceof String) {
            return VarcharType.VARCHAR;
        }
        if (value instanceof Long || value instanceof Integer) {
            return BigintType.BIGINT;
        }
        if (value instanceof Double || value instanceof Float) {
            return DoubleType.DOUBLE;
        }
        if (value instanceof Boolean) {
            return BooleanType.BOOLEAN;
        }

        // Default to VARCHAR for unknown types
        return VarcharType.VARCHAR;
    }

    @Override
    public void close()
    {
        driver.close();
    }

    public Driver getDriver()
    {
        return driver;
    }

    public String getDatabase()
    {
        return database;
    }
}
