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

import com.google.inject.Inject;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.connector.ConnectorPageSourceProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.DynamicFilter;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class Neo4jPageSourceProvider
        implements ConnectorPageSourceProvider
{
    private final Neo4jSession session;

    @Inject
    public Neo4jPageSourceProvider(Neo4jSession session)
    {
        this.session = requireNonNull(session, "session is null");
    }

    @Override
    public ConnectorPageSource createPageSource(
            ConnectorTransactionHandle transaction,
            ConnectorSession connectorSession,
            ConnectorSplit split,
            ConnectorTableHandle table,
            List<ColumnHandle> columns,
            DynamicFilter dynamicFilter)
    {
        Neo4jSplit neo4jSplit = (Neo4jSplit) split;
        Neo4jTableHandle tableHandle = (Neo4jTableHandle) table;

        List<Neo4jColumnHandle> neo4jColumns = columns.stream()
                .map(Neo4jColumnHandle.class::cast)
                .toList();

        return new Neo4jPageSource(session, neo4jSplit, tableHandle, neo4jColumns);
    }
}
