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

import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigDescription;
import io.airlift.configuration.ConfigSecuritySensitive;
import io.airlift.units.Duration;
import io.airlift.units.MinDuration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Neo4jConfig
{
    private String uri = "bolt://localhost:7687";
    private String username;
    private String password;
    private String database = "neo4j";
    private int maxConnectionPoolSize = 100;
    private Duration connectionTimeout = new Duration(30, SECONDS);
    private Duration maxConnectionLifetime = new Duration(3600, SECONDS);
    private Duration maxConnectionIdleTime = new Duration(600, SECONDS);
    private boolean caseInsensitiveNameMatching;
    private Duration dynamicFilteringWaitTimeout = new Duration(5, SECONDS);

    @NotNull
    public @Pattern(message = "Invalid URI. Expected bolt://, bolt+s://, bolt+ssc://, or neo4j://", regexp = "^(bolt|bolt\\+s|bolt\\+ssc|neo4j)(\\+s|\\+ssc)?://.*") String getUri()
    {
        return uri;
    }

    @Config("neo4j.uri")
    @ConfigDescription("Neo4j connection URI")
    public Neo4jConfig setUri(String uri)
    {
        this.uri = uri;
        return this;
    }

    public String getUsername()
    {
        return username;
    }

    @Config("neo4j.username")
    @ConfigDescription("Username for Neo4j authentication")
    public Neo4jConfig setUsername(String username)
    {
        this.username = username;
        return this;
    }

    public String getPassword()
    {
        return password;
    }

    @Config("neo4j.password")
    @ConfigSecuritySensitive
    @ConfigDescription("Password for Neo4j authentication")
    public Neo4jConfig setPassword(String password)
    {
        this.password = password;
        return this;
    }

    @NotNull
    public String getDatabase()
    {
        return database;
    }

    @Config("neo4j.database")
    @ConfigDescription("Neo4j database name")
    public Neo4jConfig setDatabase(String database)
    {
        this.database = database;
        return this;
    }

    @Min(1)
    public int getMaxConnectionPoolSize()
    {
        return maxConnectionPoolSize;
    }

    @Config("neo4j.max-connection-pool-size")
    @ConfigDescription("Maximum number of connections in the connection pool")
    public Neo4jConfig setMaxConnectionPoolSize(int maxConnectionPoolSize)
    {
        this.maxConnectionPoolSize = maxConnectionPoolSize;
        return this;
    }

    @NotNull
    public Duration getConnectionTimeout()
    {
        return connectionTimeout;
    }

    @Config("neo4j.connection-timeout")
    @ConfigDescription("Connection timeout duration")
    public Neo4jConfig setConnectionTimeout(Duration connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    @NotNull
    public Duration getMaxConnectionLifetime()
    {
        return maxConnectionLifetime;
    }

    @Config("neo4j.max-connection-lifetime")
    @ConfigDescription("Maximum lifetime of a pooled connection")
    public Neo4jConfig setMaxConnectionLifetime(Duration maxConnectionLifetime)
    {
        this.maxConnectionLifetime = maxConnectionLifetime;
        return this;
    }

    @NotNull
    public Duration getMaxConnectionIdleTime()
    {
        return maxConnectionIdleTime;
    }

    @Config("neo4j.max-connection-idle-time")
    @ConfigDescription("Maximum idle time for a pooled connection")
    public Neo4jConfig setMaxConnectionIdleTime(Duration maxConnectionIdleTime)
    {
        this.maxConnectionIdleTime = maxConnectionIdleTime;
        return this;
    }

    public boolean isCaseInsensitiveNameMatching()
    {
        return caseInsensitiveNameMatching;
    }

    @Config("neo4j.case-insensitive-name-matching")
    @ConfigDescription("Match schema, table, and column names case-insensitively")
    public Neo4jConfig setCaseInsensitiveNameMatching(boolean caseInsensitiveNameMatching)
    {
        this.caseInsensitiveNameMatching = caseInsensitiveNameMatching;
        return this;
    }

    @MinDuration("0ms")
    @NotNull
    public Duration getDynamicFilteringWaitTimeout()
    {
        return dynamicFilteringWaitTimeout;
    }

    @Config("neo4j.dynamic-filtering.wait-timeout")
    @ConfigDescription("Duration to wait for completion of dynamic filters during split generation")
    public Neo4jConfig setDynamicFilteringWaitTimeout(Duration dynamicFilteringWaitTimeout)
    {
        this.dynamicFilteringWaitTimeout = dynamicFilteringWaitTimeout;
        return this;
    }
}
