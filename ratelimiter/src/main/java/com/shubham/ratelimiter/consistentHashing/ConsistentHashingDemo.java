package com.shubham.ratelimiter.consistentHashing;

import java.util.List;

/**
 * Standalone demo (no Spring, no HTTP).
 *
 * Run this file's main() to SEE consistent hashing in action:
 *   1. Build a ring with 3 nodes.
 *   2. Assign a set of users -> nodes, print the mapping.
 *   3. Remove one node.
 *   4. Re-assign the SAME users, print the new mapping.
 *   5. Show that ONLY the users who lived on the removed node moved;
 *      everyone else stayed put (that's the whole point of consistent hashing).
 */
public class ConsistentHashingDemo {

    public static void main(String[] args) {
        ConsistentHashingRing ring = new ConsistentHashingRing();

        // --- 1. add nodes ---
        ring.addNode("node-A");
        ring.addNode("node-B");
        ring.addNode("node-C");

        String[] users = {
                "user-1", "user-2", "user-3", "user-4", "user-5",
                "user-6", "user-7", "user-8", "user-9", "user-10"
        };

        // --- 2. assignment BEFORE removal ---
        System.out.println("===== BEFORE removing node-B =====");
        java.util.Map<String, String> before = new java.util.LinkedHashMap<>();
        for (String user : users) {
            String node = ring.getNode(user);
            before.put(user, node);
            System.out.printf("%-8s -> %s   (replicas: %s)%n",
                    user, node, ring.getNodes(user, 2));
        }

        // --- 3. remove a node ---
        System.out.println("\n>>> removing node-B <<<\n");
        ring.deleteNode("node-B");

        // --- 4. assignment AFTER removal ---
        System.out.println("===== AFTER removing node-B =====");
        java.util.Map<String, String> after = new java.util.LinkedHashMap<>();
        for (String user : users) {
            String node = ring.getNode(user);
            after.put(user, node);
            System.out.printf("%-8s -> %s   (replicas: %s)%n",
                    user, node, ring.getNodes(user, 2));
        }

        // --- 5. who moved? ---
        System.out.println("\n===== WHAT CHANGED =====");
        int moved = 0;
        for (String user : users) {
            String from = before.get(user);
            String to   = after.get(user);
            if (!from.equals(to)) {
                System.out.printf("%-8s moved: %s -> %s%n", user, from, to);
                moved++;
            } else {
                System.out.printf("%-8s stayed on %s%n", user, to);
            }
        }
        System.out.printf("%n%d of %d users moved. "
                + "Only users that were on node-B should have moved.%n",
                moved, users.length);
    }
}
