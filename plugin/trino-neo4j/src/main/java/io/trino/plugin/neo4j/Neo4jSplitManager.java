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
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.FixedSplitSource;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class Neo4jSplitManager
        implements ConnectorSplitManager
{
    private static final int SPLIT_SIZE = 1000;
    private final Neo4jSession session;

    @Inject
    public Neo4jSplitManager(Neo4jSession session)
    {
        this.session = requireNonNull(session, "session is null");
    }

    @Override
    public ConnectorSplitSource getSplits(
            ConnectorTransactionHandle transaction,
            ConnectorSession connectorSession,
            ConnectorTableHandle table,
            DynamicFilter dynamicFilter,
            Constraint constraint)
    {
        Neo4jTableHandle tableHandle = (Neo4jTableHandle) table;

        List<Neo4jSplit> splits = new ArrayList<>();

        // Create splits with pagination
        // For simplicity, create one split initially
        // In a production implementation, you would query Neo4j for the count
        // and create multiple splits for parallel processing
        splits.add(new Neo4jSplit(tableHandle.getTableName(), 0, Integer.MAX_VALUE));

        return new FixedSplitSource(splits);
    }
}
