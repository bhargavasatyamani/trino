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

import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.SchemaTableName;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class Neo4jTableMetadata
{
    private final SchemaTableName tableName;
    private final List<ColumnMetadata> columns;

    public Neo4jTableMetadata(SchemaTableName tableName, List<ColumnMetadata> columns)
    {
        this.tableName = requireNonNull(tableName, "tableName is null");
        this.columns = requireNonNull(columns, "columns is null");
    }

    public SchemaTableName getTableName()
    {
        return tableName;
    }

    public List<ColumnMetadata> getColumns()
    {
        return columns;
    }
}
