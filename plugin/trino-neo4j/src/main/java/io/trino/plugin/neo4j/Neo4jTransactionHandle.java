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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ConnectorTransactionHandle;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class Neo4jTransactionHandle
        implements ConnectorTransactionHandle
{
    private final String transactionId;

    @JsonCreator
    public Neo4jTransactionHandle(@JsonProperty("transactionId") String transactionId)
    {
        this.transactionId = requireNonNull(transactionId, "transactionId is null");
    }

    @JsonProperty
    public String getTransactionId()
    {
        return transactionId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Neo4jTransactionHandle that = (Neo4jTransactionHandle) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString()
    {
        return transactionId;
    }
}
