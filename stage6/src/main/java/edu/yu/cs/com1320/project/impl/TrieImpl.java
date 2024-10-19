package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

import java.util.*;

public class TrieImpl<Value> implements Trie<Value> {
    private static final int ALPHABET_SIZE = 256;
    private Node<Value> root;
    private final StackImpl<Undoable> commandStack;

    public TrieImpl() {
        this.root = new Node<>();
        this.commandStack = new StackImpl<>();
    }

    private static class Node<Value> {
        private Set<Value> values = new HashSet<>();
        private final Node<Value>[] links = new Node[ALPHABET_SIZE];
    }

    @Override
    public void put(String key, Value val) {
        if (key == null || val == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        // Create a GenericCommand to encapsulate the put operation
        GenericCommand<Value> command = new GenericCommand<>(val, (v) -> delete(key, val));
        this.commandStack.push(command); // Push the command onto the undo stack

        // Continue with the put
        this.root = put(this.root, key, val, 0);
    }

    private Node<Value> put(Node<Value> x, String key, Value val, int d) {
        if (x == null) {
            x = new Node<>();
        }
        if (d == key.length()) {
            x.values.add(val);  // Adds the value to the set
            return x;
        }
        char c = key.charAt(d);
        x.links[c] = put(x.links[c], key, val, d + 1);
        return x;
    }

    @Override
    public List<Value> getSorted(String key, Comparator<Value> comparator) {
        if (key == null || comparator == null) {
            throw new IllegalArgumentException("Key and comparator cannot be null");
        }

        Node<Value> x = get(root, key, 0);
        if (x == null || x.values == null) {
            return List.of(); // if no matches, return empty list
        }

        List<Value> matches = new ArrayList<>(x.values);
        matches.sort(comparator.reversed()); // sorts in descending order
        return matches;
    }

    private Node<Value> get(Node<Value> x, String key, int d) {
        if (x == null) {
            return null;
        }

        if (d == key.length()) {
            return x;
        }

        char c = key.charAt(d);
        return get(x.links[c], key, d + 1);
    }

    @Override
    public Set<Value> get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        Node<Value> x = get(root, key, 0);
        if (x == null || x.values == null) {
            return Set.of(); // Return an empty set if no matches
        }
        return new HashSet<>(x.values);
    }

    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if (prefix == null || comparator == null) {
            throw new IllegalArgumentException("Prefix and comparator cannot be null");
        }

        Node<Value> x = get(root, prefix, 0);
        List<Value> matches = new ArrayList<>();
        collect(x, matches);

        matches.sort(comparator); // sort with comparator
        return matches;
    }

    private void collect(Node<Value> x, List<Value> matches) {
        if (x == null) {
            return;
        }
        if (x.values != null) {
            matches.addAll(x.values);
        }
        for (Node<Value> child : x.links) {
            collect(child, matches);
        }
    }

    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null");
        }

        Set<Value> deletedValues = new HashSet<>();
        Node<Value> x = get(root, prefix, 0);
        if (x != null) {
            collectAndDelete(x, deletedValues);

            // Create an undo command for the deleteAllWithPrefix operation
            GenericCommand<Set<Value>> deleteCommand = new GenericCommand<>(deletedValues, set -> {
                set.forEach(value -> put(prefix, value));
            });

            commandStack.push(deleteCommand); // Push to undo stack
        }
        return deletedValues;
    }

    private void collectAndDelete(Node<Value> x, Set<Value> deletedValues) {
        if (x == null) {
            return;
        }
        if (x.values != null && !x.values.isEmpty()) {
            deletedValues.addAll(x.values);  // Collect the values
            x.values.clear();  // Clear the values from the node
        }
        for (Node<Value> child : x.links) {
            collectAndDelete(child, deletedValues); // Recursively call child nodes
        }
    }

    @Override
    public Set<Value> deleteAll(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        Set<Value> deletedValues = new HashSet<>();
        root = deleteAll(root, key, 0, deletedValues);
        return deletedValues;
    }

    private Node<Value> deleteAll(Node<Value> x, String key, int d, Set<Value> deletedValues) {
        if (x == null) {
            return null;
        }
        if (d == key.length()) {
            deletedValues.addAll(x.values);  // Add all values to the deleted set
            x.values.clear();  // Clear all values from the node
            return x;
        }
        char c = key.charAt(d);
        x.links[c] = deleteAll(x.links[c], key, d + 1, deletedValues);
        return x;
    }

    @Override
    public Value delete(String key, Value val) {
        if (key == null || val == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        Node<Value> x = get(root, key, 0);
        if (x != null && x.values != null && x.values.contains(val)) {
            GenericCommand<Value> deleteCommand = new GenericCommand<>(val, v -> put(key, v));

            commandStack.push(deleteCommand); // Push to undo stack
            x.values.remove(val);  // Remove the value from the set
            return val; // Return the deleted value
        }
        return null; // Key not found or value not matched
    }
}
