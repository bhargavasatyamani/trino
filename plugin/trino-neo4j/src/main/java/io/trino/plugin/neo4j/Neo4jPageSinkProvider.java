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
import io.trino.spi.connector.ConnectorInsertTableHandle;
import io.trino.spi.connector.ConnectorOutputTableHandle;
import io.trino.spi.connector.ConnectorPageSink;
import io.trino.spi.connector.ConnectorPageSinkId;
import io.trino.spi.connector.ConnectorPageSinkProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTransactionHandle;

import static java.util.Objects.requireNonNull;

public class Neo4jPageSinkProvider
        implements ConnectorPageSinkProvider
{
    private final Neo4jSession neo4jSession;

    @Inject
    public Neo4jPageSinkProvider(Neo4jSession neo4jSession)
    {
        this.neo4jSession = requireNonNull(neo4jSession, "neo4jSession is null");
    }

    @Override
    public ConnectorPageSink createPageSink(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorOutputTableHandle outputTableHandle,
            ConnectorPageSinkId pageSinkId)
    {
        Neo4jOutputTableHandle tableHandle = (Neo4jOutputTableHandle) outputTableHandle;
        return new Neo4jPageSink(
                neo4jSession,
                tableHandle.getTableName(),
                tableHandle.getColumns());
    }

    @Override
    public ConnectorPageSink createPageSink(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorInsertTableHandle insertTableHandle,
            ConnectorPageSinkId pageSinkId)
    {
        Neo4jInsertTableHandle tableHandle = (Neo4jInsertTableHandle) insertTableHandle;
        return new Neo4jPageSink(
                neo4jSession,
                tableHandle.getTableName(),
                tableHandle.getColumns());
    }
}
