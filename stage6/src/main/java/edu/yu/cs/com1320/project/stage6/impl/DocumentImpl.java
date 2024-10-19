package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.stage6.Document;

import java.net.URI;
import java.util.*;

public class DocumentImpl implements Document, Comparable<Document> {
    private final URI uri;
    private final String text;
    private final byte[] binaryData;
    private final Map<String, String> metadata;
    private long lastUseTime; // last use time
    private Map<String, Integer> wordMap;

    // Constructor for text documents
    public DocumentImpl(URI uri, String txt, Map<String, Integer> wordMap) {
        if (uri == null || txt == null || txt.isBlank()) {
            throw new IllegalArgumentException("URI and text must not be null or empty");
        }
        this.uri = uri;
        this.text = txt;
        this.binaryData = null;
        this.metadata = new HashMap<>();
        this.lastUseTime = System.nanoTime(); // Initialize last use time
        this.wordMap = (wordMap != null) ? wordMap : generateWordMap(txt);
    }

    // Constructor for binary documents
    public DocumentImpl(URI uri, byte[] binaryData) {
        if (uri == null) {
            throw new IllegalArgumentException("URI must not be null");
        }
        this.uri = uri;
        this.text = null;
        this.binaryData = binaryData != null ? binaryData.clone() : null;
        this.metadata = new HashMap<>();
        this.lastUseTime = System.nanoTime(); // Initialize last use time
        this.wordMap = new HashMap<>();
    }

    private Map<String, Integer> generateWordMap(String text) {
        Map<String, Integer> map = new HashMap<>();
        String[] words = text.toLowerCase().split("\\W+");
        for (String word : words) {
            map.put(word, map.getOrDefault(word, 0) + 1);
        }
        return map;
    }

    @Override
    public String setMetadataValue(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Metadata key must not be null or empty");
        }
        return this.metadata.put(key, value);
    }

    @Override
    public String getMetadataValue(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Metadata key must not be null or empty");
        }
        return this.metadata.get(key);
    }

    @Override
    public HashMap<String, String> getMetadata() {
        return new HashMap<>(this.metadata);
    }

    @Override
    public void setMetadata(HashMap<String, String> metadata) {
        this.metadata.clear();
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }

    @Override
    public String getDocumentTxt() {
        if (this.binaryData != null) {
            return null; // Return null for binary documents
        }
        return this.text;
    }

    @Override
    public byte[] getDocumentBinaryData() {
        if (this.binaryData != null) {
            return this.binaryData.clone();
        }
        return null;
    }

    @Override
    public URI getKey() {
        return this.uri;
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public int hashCode() {
        if (this.binaryData != null) {
            return Arrays.hashCode(this.binaryData);
        } else if (this.text != null) {
            return this.text.hashCode();
        } else {
            return 0;
        }
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
@Override
public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
        return false;
    }
    DocumentImpl document = (DocumentImpl) obj;
    return uri.equals(document.uri) && text.equals(document.text);
}


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public int wordCount(String word) {
        if (word == null || word.isBlank()) {
            throw new IllegalArgumentException("Word must not be null or empty");
        }

        if (this.binaryData != null) {
            return 0; // Binary document, return 0
        }

        // Word count
        int count = 0;
        String[] words = this.text.split("\\s+"); // Split text into words
        for (String w : words) {
            if (w.equals(word)) { //case-sensitive match
                count++;
            }
        }

        return count;
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public Set<String> getWords() {
        if (this.binaryData != null) {
            return Collections.emptySet(); // Return empty set for binary documents
        }
        String[] words = this.text.toLowerCase().split("\\W+");  //
        return new HashSet<>(this.wordMap.keySet());
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public long getLastUseTime() {
        return this.lastUseTime;
    }

    @Override
    public void setLastUseTime(long timeInNanoseconds) {
        this.lastUseTime = timeInNanoseconds;
    }

    @Override
    public int compareTo(Document o) {
        // Compare based on last use time
        return Long.compare(this.lastUseTime, o.getLastUseTime());
    }

    @Override
    public HashMap<String, Integer> getWordMap() {
        return new HashMap<>(this.wordMap);
    }

    @Override
    public void setWordMap(HashMap<String, Integer> wordMap) {
        this.wordMap = new HashMap<>(wordMap);
    }
}
