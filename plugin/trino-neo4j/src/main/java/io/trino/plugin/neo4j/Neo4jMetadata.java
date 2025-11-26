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
import com.google.common.collect.ImmutableMap;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.ConnectorTableVersion;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.SchemaTablePrefix;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class Neo4jMetadata
        implements ConnectorMetadata
{
    private final Neo4jSession session;

    public Neo4jMetadata(Neo4jSession session)
    {
        this.session = requireNonNull(session, "session is null");
    }

    @Override
    public List<String> listSchemaNames(ConnectorSession connectorSession)
    {
        return session.getAllSchemas();
    }

    @Override
    public ConnectorTableHandle getTableHandle(
            ConnectorSession connectorSession,
            SchemaTableName tableName,
            Optional<ConnectorTableVersion> startVersion,
            Optional<ConnectorTableVersion> endVersion)
    {
        requireNonNull(tableName, "tableName is null");
        return session.getTableHandle(tableName);
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(
            ConnectorSession connectorSession,
            ConnectorTableHandle tableHandle)
    {
        requireNonNull(tableHandle, "tableHandle is null");
        Neo4jTableHandle neo4jTableHandle = (Neo4jTableHandle) tableHandle;

        Neo4jTableMetadata tableMetadata = session.getTableMetadata(neo4jTableHandle);
        return new ConnectorTableMetadata(
                tableMetadata.getTableName(),
                tableMetadata.getColumns());
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> schemaName)
    {
        ImmutableList.Builder<SchemaTableName> tables = ImmutableList.builder();

        List<String> schemaNames = schemaName
                .<List<String>>map(ImmutableList::of)
                .orElseGet(() -> listSchemaNames(session));

        for (String schema : schemaNames) {
            for (String tableName : this.session.getAllTables(schema)) {
                tables.add(new SchemaTableName(schema, tableName));
            }
        }

        return tables.build();
    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(
            ConnectorSession connectorSession,
            ConnectorTableHandle tableHandle)
    {
        requireNonNull(tableHandle, "tableHandle is null");
        Neo4jTableHandle neo4jTableHandle = (Neo4jTableHandle) tableHandle;

        Neo4jTableMetadata tableMetadata = session.getTableMetadata(neo4jTableHandle);
        ImmutableMap.Builder<String, ColumnHandle> columnHandles = ImmutableMap.builder();

        int index = 0;
        for (ColumnMetadata column : tableMetadata.getColumns()) {
            columnHandles.put(column.getName(), new Neo4jColumnHandle(column.getName(), column.getType(), index));
            index++;
        }

        return columnHandles.buildOrThrow();
    }

    @Override
    public ColumnMetadata getColumnMetadata(
            ConnectorSession connectorSession,
            ConnectorTableHandle tableHandle,
            ColumnHandle columnHandle)
    {
        requireNonNull(tableHandle, "tableHandle is null");
        requireNonNull(columnHandle, "columnHandle is null");

        Neo4jColumnHandle neo4jColumnHandle = (Neo4jColumnHandle) columnHandle;
        return new ColumnMetadata(neo4jColumnHandle.getName(), neo4jColumnHandle.getType());
    }

    @Override
    public Map<SchemaTableName, List<ColumnMetadata>> listTableColumns(
            ConnectorSession session,
            SchemaTablePrefix prefix)
    {
        requireNonNull(prefix, "prefix is null");

        ImmutableMap.Builder<SchemaTableName, List<ColumnMetadata>> columns = ImmutableMap.builder();

        for (SchemaTableName tableName : listTables(session, prefix.getSchema())) {
            if (prefix.getTable().isEmpty() || tableName.getTableName().equals(prefix.getTable().get())) {
                ConnectorTableHandle tableHandle = getTableHandle(session, tableName, Optional.empty(), Optional.empty());
                if (tableHandle != null) {
                    ConnectorTableMetadata tableMetadata = getTableMetadata(session, tableHandle);
                    columns.put(tableName, tableMetadata.getColumns());
                }
            }
        }

        return columns.buildOrThrow();
    }
}
