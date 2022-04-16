package mipt.bit.utils;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Record;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Value;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.AccessMode;

import org.neo4j.driver.types.Node;

import static org.neo4j.driver.SessionConfig.builder;
import static org.neo4j.driver.Values.parameters;

public class TransactionsExecute {
    private final Driver driver;
    
    ArrayList<Long> readValues;
    Boolean nodeExists;
    Long nodesCount;

    public TransactionsExecute(Driver driver) {
        this.driver = driver;
    }

    public void wipeData() {
        try (Session session0 = driver.session(builder().withDefaultAccessMode(AccessMode.WRITE).build())) {
            session0.writeTransaction(tx -> wipeData(tx).consume());
        }
        // try (Session count = driver.session(builder().withDefaultAccessMode(AccessMode.WRITE).build())) {
        //     count.writeTransaction(tx -> nodesCount(tx).consume());
        //     System.out.printf("Nodes number: %s\n", nodesCount);
        // }
    }

    public List<JSONObject> execute (List<Operation> operations) {
        List<JSONObject> result = new ArrayList<>();
        
        for (Operation op : operations) {
            // try (Session count = driver.session(builder().withDefaultAccessMode(AccessMode.WRITE).build())) {
            //     count.writeTransaction(tx -> nodesCount(tx).consume());
            //     System.out.printf("In operation with type: %s, key: %s, value: %s\n", op.type, op.key, op.value);
            //     System.out.printf("Nodes number: %s\n", nodesCount);
            // }
            nodeExists = false;
            try (Session session = driver.session(builder().withDefaultAccessMode(AccessMode.READ).build())) {
                session.readTransaction(tx -> checkNodeExists(tx, op.key).consume());
            }
            if (op.type == "r") {
                String rv = "";
                if (!nodeExists) {
                    rv = "[]";
                } else {
                    readValues = new ArrayList<>();
                    try (Session session = driver.session(builder().withDefaultAccessMode(AccessMode.READ).build())) {
                        session.readTransaction(tx -> readAllList(tx, op.key).consume());
                    }
                    rv = readValues.subList(1, readValues.size()).toString();
                }
                String res = String.format("{type: r, key: %s, value: %s, readResult: %s}", op.key, op.value, rv);
                JSONObject obj = new JSONObject(res);
                result.add(obj);
            } else {
                if (!nodeExists) {
                    try (Session session = driver.session(builder().withDefaultAccessMode(AccessMode.WRITE).build())) {
                        session.writeTransaction(tx -> createNode(tx, op.key).consume());
                    }
                }
                try (Session session = driver.session(builder().withDefaultAccessMode(AccessMode.WRITE).build())) {
                    session.writeTransaction(tx -> appendToNode(tx, op.key, op.value).consume());
                }
                String res = String.format("{type: append, key: %s, value: %s}", op.key, op.value);
                JSONObject obj = new JSONObject(res);
                result.add(obj);

                // todo: remove
                // readValues = new ArrayList<>();
                // System.out.printf("After append. Got readValues before reading: %s\n", readValues.toString());
                // try (Session session = driver.session(builder().withDefaultAccessMode(AccessMode.READ).build())) {
                //     session.readTransaction(tx -> readAllList(tx, op.key).consume());
                // }
                // System.out.printf("Got readValues after reading: %s\n", readValues.toString());
            }
        }
        return result;
    }

    private Result createNode(final Transaction tx, final Long id) {
        return tx.run("CREATE (:headNode {id: $id})", parameters("id", id));
    }

    private Result checkNodeExists(final Transaction tx, final Long id) {
        String q = "MATCH (n {id: $id})\n" +
                "                WITH COUNT(n) > 0  as node_exists\n" +
                "                RETURN node_exists";
        Result result = tx.run(q, parameters("id", id));
        while (result.hasNext()) {
            Record record = result.next();
            nodeExists = record.get("node_exists").asBoolean();
        }
        return result;
    }

    private Result appendToNode(final Transaction tx, final Long id, final Long value) {
        String q = "MATCH (n {id: $id})-[:NEXT*0..]->(end)\n" +
                "                WHERE NOT (end)-[:NEXT]->()\n" +
                "                CREATE (end)-[:NEXT]->(new:Append {value: $value})";
        return tx.run(q, parameters("id", id, "value", value));
    }

    private Result readAllList(final Transaction tx, final Long id) {
        String q = "MATCH head = (n {id: $id})-[:NEXT*0..]->(end)\n" +
                "                WITH max(length(head)) as maxlen\n" +
                "                MATCH head = (n {id: $id})-[:NEXT*0..]->(end)\n" +
                "                WHERE length(head) = maxlen\n" +
                "                RETURN nodes(head)";
        Result result = tx.run(q, parameters("id", id));
        while (result.hasNext()) {
            Record record = result.next();
            List<String> keys = record.keys();
            // System.out.println("Printing keys");
            // for (String key : keys) {
            //     System.out.printf("Another value: %s\n", key);
            // }
            List<Value> listValues = record.values();
            List<Object> values = listValues.get(0).asList();
            // System.out.println("\nPrinting values");
            // System.out.println(values);
            for (Object obj : values) {
                Node node = (Node) obj;
                Iterable<Value> props = node.values();
                for (Value prop : props) {
                    Long val = prop.asLong();
                    // System.out.printf("Got another value: %s\n", val);
                    readValues.add(Long.valueOf(prop.toString()));
                }
            }
        }
        return result;
    }

    private Result wipeData(final Transaction tx) {
        return tx.run("MATCH (n) DETACH DELETE n");
    }

    private Result nodesCount(final Transaction tx) {
        String q = "MATCH (n)\n" +
                "                WITH COUNT(n) as node_count\n" +
                "                RETURN node_count";
        Result result = tx.run(q);
        while (result.hasNext()) {
            Record record = result.next();
            nodesCount = record.get("node_count").asLong();
        }
        return result;
    }
}