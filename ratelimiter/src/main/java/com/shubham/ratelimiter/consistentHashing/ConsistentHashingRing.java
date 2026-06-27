package com.shubham.ratelimiter.consistentHashing;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConsistentHashingRing {
    private final Integer vNodes = 10;
    private NavigableMap<Long, String>  ring = new TreeMap<>();
    private final Map<String, List<Long>>  nodesToVnodes = new HashMap<>();
    private final HashFunction hashFunction = Hashing.murmur3_128();

    private Long hash(String s){
      return hashFunction.hashString(s, StandardCharsets.UTF_8).asLong();
    }

    public void addNode(String nodeId){

        List<Long> vNodesList = new ArrayList<>();

        if(nodesToVnodes.containsKey(nodeId)) {
            System.out.println("Node already exists");
            return;
        }

        for(int i=0;i<vNodes;i++){
            String vNodeId = nodeId + ":" +  i;
            Long vNodeHash = hash(vNodeId);
            ring.put(vNodeHash, vNodeId);
            vNodesList.add(vNodeHash);
        }
        nodesToVnodes.put(nodeId, vNodesList);
    }


    public String getNode(String key){
        if(ring.isEmpty()) return null;

        Long keyHash = hash(key);

        Map.Entry<Long , String> node  = ring.ceilingEntry(keyHash);
        if(node == null){
            node = ring.firstEntry();
        }

        String vNodeId = node.getValue();
        return vNodeId.substring(0, vNodeId.lastIndexOf(':'));
    }


    public void deleteNode(String key){
        if(ring.isEmpty()) return;
        if(!nodesToVnodes.containsKey(key)) return;

        List<Long> vNodesList = nodesToVnodes.get(key);
        if(vNodesList.isEmpty()) return;

        for(Long vNodeHash : vNodesList){
            ring.remove(vNodeHash);
        }
        nodesToVnodes.remove(key);
    }


    public List<String> getNodes(String key, int N ){
        if(ring.isEmpty() || N <= 0) return new ArrayList<>();

        Long keyHash = hash(key);

        // distinct physical nodes, kept in clockwise order (primary first)
        LinkedHashSet<String> nodes = new LinkedHashSet<>();

        // walk clockwise: from the key's position to the end of the ring,
        // then wrap around from the start, until we have N distinct nodes
        for(String vNodeId : ring.tailMap(keyHash, true).values()){
            nodes.add(vNodeId.substring(0, vNodeId.lastIndexOf(':')));
            if(nodes.size() == N) return new ArrayList<>(nodes);
        }
        for(String vNodeId : ring.values()){
            nodes.add(vNodeId.substring(0, vNodeId.lastIndexOf(':')));
            if(nodes.size() == N) break;
        }

        return new ArrayList<>(nodes);
    }

    

}
