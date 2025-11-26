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
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.transaction.IsolationLevel;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

public class Neo4jTransactionManager
{
    private final ConcurrentMap<ConnectorTransactionHandle, Neo4jMetadata> transactions = new ConcurrentHashMap<>();
    private final Neo4jSession session;

    @Inject
    public Neo4jTransactionManager(Neo4jSession session)
    {
        this.session = requireNonNull(session, "session is null");
    }

    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel)
    {
        Neo4jTransactionHandle transaction = new Neo4jTransactionHandle(UUID.randomUUID().toString());
        transactions.put(transaction, new Neo4jMetadata(session));
        return transaction;
    }

    public Neo4jMetadata getMetadata(ConnectorTransactionHandle transaction)
    {
        Neo4jMetadata metadata = transactions.get(transaction);
        if (metadata == null) {
            throw new IllegalStateException("Unknown transaction: " + transaction);
        }
        return metadata;
    }

    public void commit(ConnectorTransactionHandle transaction)
    {
        transactions.remove(transaction);
    }

    public void rollback(ConnectorTransactionHandle transaction)
    {
        transactions.remove(transaction);
    }
}
