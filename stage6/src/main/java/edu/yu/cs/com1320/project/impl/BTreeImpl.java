package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.stage6.PersistenceManager;

import java.io.IOException;
import java.util.TreeMap;

public class BTreeImpl<Key extends Comparable<Key>, Value> implements BTree<Key, Value> {
    private final TreeMap<Key, Value> map = new TreeMap<>();
    private PersistenceManager<Key, Value> pm;

    @Override
    public Value get(Key k) {
        Value val = this.map.get(k);
        if (val == null) {
            try {
                val = this.pm.deserialize(k);
                if (val != null) {
                    this.map.put(k, val);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return val;
    }

    @Override
    public Value put(Key k, Value v) {
        Value existingVal = this.map.get(k);
        if (existingVal == null) {
            try {
                existingVal = this.pm.deserialize(k);
                if (existingVal != null) {
                    this.map.put(k, existingVal);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (v == null && existingVal != null) {
            try {
                this.pm.delete(k);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (existingVal != null && v != null) {
            try {
                this.pm.delete(k);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return this.map.put(k, v);
    }

    @Override
    public void moveToDisk(Key k) throws IOException {
        Value val = this.map.get(k);
        if (val != null) {
            this.pm.serialize(k, val);
            this.map.put(k, null); // Set the value to null to indicate it's on disk
        }
    }

    @Override
    public void setPersistenceManager(PersistenceManager<Key, Value> pm) {
        this.pm = pm;
    }
}
