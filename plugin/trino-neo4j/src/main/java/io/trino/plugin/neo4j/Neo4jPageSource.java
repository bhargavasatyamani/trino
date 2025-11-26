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

import io.airlift.log.Logger;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.Page;
import io.trino.spi.PageBuilder;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.connector.SourcePage;
import io.trino.spi.type.Type;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class Neo4jPageSource
        implements ConnectorPageSource
{
    private static final Logger log = Logger.get(Neo4jPageSource.class);
    private static final int BATCH_SIZE = 1000;

    private final Neo4jSession neo4jSession;
    private final Neo4jSplit split;
    private final List<Neo4jColumnHandle> columns;
    private final PageBuilder pageBuilder;

    private Session session;
    private Result result;
    private boolean finished;
    private long completedBytes;

    public Neo4jPageSource(
            Neo4jSession neo4jSession,
            Neo4jSplit split,
            Neo4jTableHandle tableHandle,
            List<Neo4jColumnHandle> columns)
    {
        this.neo4jSession = requireNonNull(neo4jSession, "neo4jSession is null");
        this.split = requireNonNull(split, "split is null");
        this.columns = requireNonNull(columns, "columns is null");

        List<Type> types = columns.stream()
                .map(Neo4jColumnHandle::getType)
                .toList();

        this.pageBuilder = new PageBuilder(types);
        this.finished = false;

        // Initialize Neo4j session and query
        this.session = neo4jSession.getDriver().session(SessionConfig.forDatabase(neo4jSession.getDatabase()));
        String query = buildQuery(split.getLabel(), split.getSkip(), split.getLimit());
        log.info("Executing Neo4j query: %s", query);
        this.result = session.run(query);
    }

    private String buildQuery(String label, int skip, int limit)
    {
        StringBuilder query = new StringBuilder();
        query.append("MATCH (n:").append(label).append(") ");
        query.append("RETURN n");

        if (skip > 0) {
            query.append(" SKIP ").append(skip);
        }
        if (limit < Integer.MAX_VALUE) {
            query.append(" LIMIT ").append(limit);
        }

        return query.toString();
    }

    @Override
    public long getCompletedBytes()
    {
        return completedBytes;
    }

    @Override
    public long getReadTimeNanos()
    {
        return 0;
    }

    @Override
    public boolean isFinished()
    {
        return finished;
    }

    @Override
    public SourcePage getNextSourcePage()
    {
        if (finished) {
            return null;
        }

        int count = 0;
        while (!finished && count < BATCH_SIZE && result.hasNext()) {
            Record record = result.next();
            Node node = record.get("n").asNode();
            log.debug("Processing node: %s with properties: %s", node.elementId(), node.keys());

            pageBuilder.declarePosition();

            for (int i = 0; i < columns.size(); i++) {
                Neo4jColumnHandle column = columns.get(i);
                BlockBuilder blockBuilder = pageBuilder.getBlockBuilder(i);

                writeValue(blockBuilder, column, node);
            }

            count++;
            completedBytes += 100; // Approximate size
        }

        if (!result.hasNext()) {
            finished = true;
        }

        // Don't return empty pages
        if (count == 0) {
            return null;
        }

        Page page = pageBuilder.build();
        pageBuilder.reset();

        return SourcePage.create(page);
    }

    private void writeValue(BlockBuilder blockBuilder, Neo4jColumnHandle column, Node node)
    {
        String columnName = column.getName();
        Type type = column.getType();

        if (columnName.equals("id")) {
            // Write the internal Neo4j node ID
            type.writeLong(blockBuilder, node.id());
            return;
        }

        if (!node.containsKey(columnName)) {
            blockBuilder.appendNull();
            return;
        }

        Value value = node.get(columnName);

        if (value.isNull()) {
            blockBuilder.appendNull();
            return;
        }

        String typeName = type.getTypeSignature().getBase();
        switch (typeName) {
            case "bigint":
                type.writeLong(blockBuilder, value.asLong());
                break;
            case "double":
                type.writeDouble(blockBuilder, value.asDouble());
                break;
            case "boolean":
                type.writeBoolean(blockBuilder, value.asBoolean());
                break;
            case "varchar":
                Slice slice = Slices.utf8Slice(value.asString());
                type.writeSlice(blockBuilder, slice);
                break;
            default:
                // Default to string representation
                Slice defaultSlice = Slices.utf8Slice(value.toString());
                type.writeSlice(blockBuilder, defaultSlice);
                break;
        }
    }

    @Override
    public long getMemoryUsage()
    {
        return pageBuilder.getRetainedSizeInBytes();
    }

    @Override
    public void close()
    {
        if (session != null) {
            session.close();
        }
    }
}
