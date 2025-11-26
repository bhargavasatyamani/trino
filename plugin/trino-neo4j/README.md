# Trino Neo4j Connector

The Neo4j connector allows querying data stored in Neo4j graph databases using Trino.

## Features

- Query Neo4j node labels as tables
- Read node properties as columns
- Support for basic Neo4j data types (String, Long, Double, Boolean)
- Connection pooling and timeout configuration
- Case-insensitive name matching support

## Configuration

To configure the Neo4j connector, create a catalog properties file in `etc/catalog/` named, for example, `neo4j.properties`, with the following contents:

```properties
connector.name=neo4j
neo4j.uri=bolt://localhost:7687
neo4j.username=neo4j
neo4j.password=your-password
neo4j.database=neo4j
```

### Configuration Properties

| Property Name | Description | Default | Required |
|--------------|-------------|---------|----------|
| `neo4j.uri` | Neo4j connection URI (bolt://, bolt+s://, or neo4j://) | `bolt://localhost:7687` | Yes |
| `neo4j.username` | Username for authentication | | No |
| `neo4j.password` | Password for authentication | | No |
| `neo4j.database` | Neo4j database name | `neo4j` | Yes |
| `neo4j.max-connection-pool-size` | Maximum number of connections in the pool | `100` | No |
| `neo4j.connection-timeout` | Connection timeout duration | `30s` | No |
| `neo4j.max-connection-lifetime` | Maximum lifetime of a pooled connection | `3600s` | No |
| `neo4j.max-connection-idle-time` | Maximum idle time for a pooled connection | `600s` | No |
| `neo4j.case-insensitive-name-matching` | Match names case-insensitively | `false` | No |
| `neo4j.dynamic-filtering.wait-timeout` | Dynamic filter wait timeout | `5s` | No |

## Usage

### Querying Neo4j Data

In Trino, Neo4j node labels are exposed as tables. For example, if you have a Neo4j database with a `Person` label:

```sql
-- List all schemas (databases)
SHOW SCHEMAS FROM neo4j;

-- List all tables (node labels)
SHOW TABLES FROM neo4j.neo4j;

-- Query data
SELECT * FROM neo4j.neo4j.Person;

-- Query specific columns
SELECT name, age FROM neo4j.neo4j.Person WHERE age > 30;
```

### Schema Mapping

- **Schema**: Neo4j database name
- **Table**: Neo4j node label
- **Column**: Node property
- **id**: Internal Neo4j node ID (automatically included)

### Supported Data Types

| Neo4j Type | Trino Type |
|-----------|-----------|
| String | VARCHAR |
| Long/Integer | BIGINT |
| Double/Float | DOUBLE |
| Boolean | BOOLEAN |

## Limitations

- Currently supports read-only operations
- No support for relationships (edges) as separate tables
- Limited to node properties (graph structure not fully exposed)
- Complex nested types are converted to VARCHAR
- No predicate pushdown optimization yet

## Example Setup

1. Start Neo4j database:
```bash
docker run -d \
  --name neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password \
  neo4j:5.15.0
```

2. Create catalog configuration file `etc/catalog/neo4j.properties`:
```properties
connector.name=neo4j
neo4j.uri=bolt://localhost:7687
neo4j.username=neo4j
neo4j.password=password
neo4j.database=neo4j
```

3. Load sample data in Neo4j:
```cypher
CREATE (p:Person {name: 'Alice', age: 30})
CREATE (p2:Person {name: 'Bob', age: 25})
CREATE (c:Company {name: 'Acme Corp', founded: 1990})
```

4. Query from Trino:
```sql
SELECT * FROM neo4j.neo4j.Person;
SELECT * FROM neo4j.neo4j.Company;
```

## Future Enhancements

- Support for relationship queries
- Write operations (INSERT, UPDATE, DELETE)
- Graph-specific functions
- Cypher query passthrough
- Predicate pushdown optimization
- Support for complex types (Lists, Maps)
- Multi-label support
