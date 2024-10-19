package edu.yu.cs.com1320.project.stage6.impl;


import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.DocumentStore;
import edu.yu.cs.com1320.project.stage6.PersistenceManager;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;


public class DocumentStoreImpl implements DocumentStore {
    private final BTree<URI, Document> documentTree;
    private final Stack<Undoable> commandStack;
    private final TrieImpl<Document> trie;
    private final MinHeapImpl<Document> minHeap;
    private final PersistenceManager<URI, Document> pm;
    private final Set<URI> documentURIs;
    private int maxDocumentCount = Integer.MAX_VALUE;
    private int maxDocumentBytes = Integer.MAX_VALUE;
    private int currentDocumentBytes = 0;
    private int currentDocumentCount;

    public DocumentStoreImpl(File baseDir) {
        this.documentTree = new BTreeImpl<>();
        this.pm = new DocumentPersistenceManager(baseDir);
        this.documentTree.setPersistenceManager(this.pm);
        this.commandStack = new Stack<>();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.documentURIs = new HashSet<>();
        this.currentDocumentBytes = 0;
        this.currentDocumentCount = 0; // Initialize document count
    }

    public DocumentStoreImpl() {
        this(null);
    }

    @Override
    public String setMetadata(URI uri, String key, String value) throws IOException{
        if (uri == null || key == null || key.isBlank()) {
            throw new IllegalArgumentException("URI and metadata key must not be null or empty");
        }
        Document document = this.documentTree.get(uri);
        if (document == null) {
            throw new IllegalArgumentException("No document stored at URI: " + uri);
        }
        String oldValue = document.setMetadataValue(key, value);
        GenericCommand<URI> command = new GenericCommand<>(uri, uri1 -> document.setMetadataValue(key, oldValue));
        this.commandStack.push(command);
        return oldValue;
    }


    @Override
    public String getMetadata(URI uri, String key) throws IOException {
        if (uri == null || key == null || key.isBlank()) {
            throw new IllegalArgumentException("URI and metadata key must not be null or empty");
        }
        Document document = this.documentTree.get(uri);
        if (document == null) {
            throw new IllegalArgumentException("No document stored at URI: " + uri);
        }
        return document.getMetadataValue(key);
    }




    @Override
    public int put(InputStream input, URI uri, DocumentFormat format) throws IOException {
        if (uri == null || format == null) {
            throw new IllegalArgumentException("URI and format must not be null");
        }

        Document previousDocument = this.documentTree.get(uri);
        int previousDocumentSize = previousDocument != null ? getSizeInBytes(previousDocument) : 0;

        if (input == null) {
            delete(uri);
            return previousDocument != null ? previousDocument.hashCode() : 0;
        }

        byte[] data = input.readAllBytes();
        Document newDocument = createDocument(uri, format, data);

        newDocument.setLastUseTime(System.nanoTime());

        indexMetadata(newDocument);
        updateMemoryUsage(previousDocument, newDocument, previousDocumentSize);
        enforceMemoryLimits();

        GenericCommand<URI> command = new GenericCommand<>(uri, uri1 -> undoPut(uri1, previousDocument, newDocument, previousDocumentSize));
        this.commandStack.push(command);

        return previousDocument != null ? previousDocument.hashCode() : 0;
    }



    private Document createDocument(URI uri, DocumentFormat format, byte[] data) throws IOException {
        Document newDocument;
        if (format == DocumentFormat.TXT) {
            String text = new String(data);
            newDocument = new DocumentImpl(uri, text, buildWordMap(text));
            indexContent(text, newDocument);
        } else {
            newDocument = new DocumentImpl(uri, data);
        }
        return newDocument;
    }

    private void indexContent(String text, Document document) {
        String[] words = text.split("\\W+");
        for (String word : words) {
            this.trie.put(word.toLowerCase(), document);
        }
    }

    private void indexMetadata(Document document) {
        Map<String, String> metadata = document.getMetadata();
        if (metadata != null) {
            for (String key : metadata.keySet()) {
                String value = metadata.get(key);
                if (value != null) {
                    this.trie.put(key.toLowerCase() + ":" + value.toLowerCase(), document);
                }
            }
        }
    }

    private void updateMemoryUsage(Document previousDocument, Document newDocument, int previousDocumentSize) {
        if (previousDocument != null) {
            this.minHeap.remove(previousDocument);
            this.currentDocumentBytes -= previousDocumentSize;
        } else {
            this.currentDocumentCount++; // Increment count for new document
        }
        this.documentTree.put(newDocument.getKey(), newDocument);
        this.documentURIs.add(newDocument.getKey());
        this.minHeap.insert(newDocument);
        int sizeChange = getSizeInBytes(newDocument) - previousDocumentSize;
        this.currentDocumentBytes += sizeChange;
    }


    private void undoPut(URI uri, Document previousDocument, Document newDocument, int sizeChange) {
        this.documentTree.put(uri, previousDocument);
        if (previousDocument != null) {
            previousDocument.setLastUseTime(System.nanoTime());
            this.minHeap.insert(previousDocument);
        }
        this.currentDocumentBytes -= sizeChange;
        enforceMemoryLimits();
    }

    private HashMap<String, Integer> buildWordMap(String text) {
        HashMap<String, Integer> wordCountMap = new HashMap<>();
        String[] words = text.split("\\W+");
        for (String word : words) {
            word = word.toLowerCase();
            wordCountMap.put(word, wordCountMap.getOrDefault(word, 0) + 1);
        }
        return wordCountMap;
    }



    @Override
    public Document get(URI uri) {
        Document doc = this.documentTree.get(uri);
        if (doc == null) {  // Document might be on disk
            try {
                doc = this.pm.deserialize(uri);
                if (doc != null) {
                    this.documentTree.put(uri, doc);  // Reinsert into BTree
                    this.minHeap.insert(doc);
                    this.currentDocumentBytes += getSizeInBytes(doc);
                    this.currentDocumentCount++; // Increment count when document is brought back into memory
                    doc.setLastUseTime(System.nanoTime());
                    enforceMemoryLimits();  // Ensure memory limits are still respected
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (doc != null) {
            doc.setLastUseTime(System.nanoTime());
            this.minHeap.reHeapify(doc);
        }
        return doc;
    }





    @Override
    public boolean delete(URI uri) {
        if (uri == null || uri.toString().isBlank()) {
            throw new IllegalArgumentException("URI must not be null or empty");
        }

        Document documentToDelete = this.documentTree.get(uri);
        if (documentToDelete == null) {
            try {
                return this.pm.delete(uri);  // Try deleting from disk if not in memory
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        removeDocumentFromStore(documentToDelete);
        removeFromIndexes(documentToDelete);
        recordUndoOperation(documentToDelete, uri);

        return true;
    }



    private void removeDocumentFromStore(Document document) {
        URI uri = document.getKey();
        this.documentTree.put(uri, null);
        this.documentURIs.remove(uri);
        this.minHeap.remove(document);
        this.currentDocumentBytes -= getSizeInBytes(document);
        this.currentDocumentCount--; // Decrement count when document is deleted
    }


    private void removeFromIndexes(Document document) {
        removeTextFromTrie(document);
        removeMetadataFromTrie(document);
    }


    private void removeTextFromTrie(Document document) {
        if (document.getDocumentTxt() != null) {
            String[] words = document.getDocumentTxt().split("\\W+");
            for (String word : words) {
                this.trie.delete(word.toLowerCase(), document);
            }
        }
    }


    private void removeMetadataFromTrie(Document document) {
        Map<String, String> metadata = document.getMetadata();
        if (metadata != null) {
            for (String key : metadata.keySet()) {
                String value = metadata.get(key);
                if (value != null) {
                    this.trie.delete(key.toLowerCase() + ":" + value.toLowerCase(), document);
                }
            }
        }
    }


    private void recordUndoOperation(Document document, URI uri) {
        GenericCommand<URI> undoCommand = new GenericCommand<>(uri, uri1 -> {
            this.documentTree.put(uri1, document);
            this.documentURIs.add(uri1);
            this.minHeap.insert(document);
            this.currentDocumentBytes += getSizeInBytes(document);
            reAddToIndexes(document);
        });


        this.commandStack.push(undoCommand);
    }


    private void reAddToIndexes(Document document) {
        reAddTextToTrie(document);
        reAddMetadataToTrie(document);
    }


    private void reAddTextToTrie(Document document) {
        if (document.getDocumentTxt() != null) {
            String[] words = document.getDocumentTxt().split("\\W+");
            for (String word : words) {
                this.trie.put(word.toLowerCase(), document);
            }
        }
    }


    private void reAddMetadataToTrie(Document document) {
        Map<String, String> metadata = document.getMetadata();
        if (metadata != null) {
            for (String key : metadata.keySet()) {
                String value = metadata.get(key);
                if (value != null) {
                    this.trie.put(key.toLowerCase() + ":" + value.toLowerCase(), document);
                }
            }
        }
    }


    private int getSizeInBytes(Document document) {
        if (document.getDocumentTxt() != null) {
            return document.getDocumentTxt().getBytes().length;
        }
        return document.getDocumentBinaryData().length;
    }


    @Override
    public void undo() throws IllegalStateException {
        if (this.commandStack.isEmpty()) {
            throw new IllegalStateException("There are no actions to be undone");
        }
        Undoable command = this.commandStack.pop();
        if (command instanceof CommandSet) {
            ((CommandSet<?>) command).undo();
        } else {
            ((GenericCommand<?>) command).undo();
        }
        enforceMemoryLimits();
    }


    @Override
    public void undo(URI uri) throws IllegalStateException {
        if (this.commandStack.isEmpty()) {
            throw new IllegalStateException("There are no actions to be undone");
        }


        Stack<Undoable> tempStack = new Stack<>();
        boolean found = false;


        while (!this.commandStack.isEmpty() && !found) {
            Undoable command = this.commandStack.pop();
            if (command instanceof CommandSet<?> commandSet) {
                if (containsTarget(commandSet, uri)) {
                    undoCommandSet(commandSet, uri);
                    found = true;
                } else {
                    tempStack.push(commandSet);
                }
            } else if (command instanceof GenericCommand<?> genericCommand) {
                if (genericCommand.getTarget().equals(uri)) {
                    genericCommand.undo();
                    found = true;
                } else {
                    tempStack.push(genericCommand);
                }
            }
        }


        while (!tempStack.isEmpty()) {
            this.commandStack.push(tempStack.pop());
        }


        if (!found) {
            throw new IllegalStateException("There are no actions on the command stack for the given URI");
        }


        enforceMemoryLimits();
    }


    private boolean containsTarget(CommandSet<?> commandSet, URI uri) {
        for (GenericCommand<?> cmd : commandSet) {
            if (cmd.getTarget().equals(uri)) {
                return true;
            }
        }
        return false;
    }


    private void undoCommandSet(CommandSet<?> commandSet, URI uri) {
        for (GenericCommand<?> cmd : commandSet) {
            if (cmd.getTarget().equals(uri)) {
                cmd.undo();
            }
        }
    }


    @Override
    public List<Document> search(String keyword) throws IOException {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Keyword must not be null or empty");
        }


        keyword = keyword.toLowerCase();
        String finalKeyword = keyword;
        List<Document> searchResults = trie.getSorted(keyword, Comparator.comparingInt(doc -> doc.wordCount(finalKeyword)));


        if (!searchResults.isEmpty()) {
            for (Document doc : searchResults) {
                doc.setLastUseTime(System.nanoTime());
                this.minHeap.reHeapify(doc);
            }
        }


        return searchResults;
    }


    @Override
    public List<Document> searchByPrefix(String keywordPrefix) throws IOException {
        if (keywordPrefix == null || keywordPrefix.isBlank()) {
            throw new IllegalArgumentException("Keyword prefix must not be null or empty");
        }


        List<Document> searchResults = trie.getAllWithPrefixSorted(keywordPrefix, Comparator.comparingInt(doc -> doc.wordCount(keywordPrefix)));


        if (!searchResults.isEmpty()) {
            for (Document doc : searchResults) {
                doc.setLastUseTime(System.nanoTime());
                this.minHeap.reHeapify(doc);
            }
        }


        return searchResults;
    }


    @Override
    public Set<URI> deleteAll(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Keyword must not be null or blank");
        }


        Set<Document> documentsToDelete;
        try {
            documentsToDelete = new HashSet<>(search(keyword));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Set<URI> deletedURIs = new HashSet<>();


        if (documentsToDelete.isEmpty()) {
            return deletedURIs;
        }


        CommandSet<URI> commandSet = new CommandSet<>();


        for (Document doc : documentsToDelete) {
            URI uri = doc.getKey();
            if (delete(uri)) {
                deletedURIs.add(uri);
                GenericCommand<URI> command = new GenericCommand<>(uri, uri1 -> {
                    this.documentTree.put(uri1, doc);
                    this.minHeap.insert(doc);
                    this.currentDocumentBytes += getSizeInBytes(doc);
                    reAddToIndexes(doc);
                });
                commandSet.addCommand(command);
            }
        }


        if (!commandSet.isEmpty()) {
            this.commandStack.push(commandSet);
        }


        return deletedURIs;
    }


    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        if (keywordPrefix == null || keywordPrefix.isBlank()) {
            throw new IllegalArgumentException("Keyword prefix must not be null or empty");
        }


        Set<Document> documentsToDelete;
        try {
            documentsToDelete = new HashSet<>(searchByPrefix(keywordPrefix));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Set<URI> deletedURIs = new HashSet<>();


        if (documentsToDelete.isEmpty()) {
            return deletedURIs;
        }


        CommandSet<URI> commandSet = new CommandSet<>();


        for (Document doc : documentsToDelete) {
            URI uri = doc.getKey();
            if (delete(uri)) {
                deletedURIs.add(uri);
                GenericCommand<URI> genericCommand = new GenericCommand<>(uri, uri1 -> {
                    this.documentTree.put(uri1, doc);
                    this.minHeap.insert(doc);
                    this.currentDocumentBytes += getSizeInBytes(doc);
                    reAddToIndexes(doc);
                });
                commandSet.addCommand(genericCommand);
            }
        }


        if (!commandSet.isEmpty()) {
            this.commandStack.push(commandSet);
        }


        return deletedURIs;
    }


    @Override
    public List<Document> searchByMetadata(Map<String, String> keysValues) throws IOException {
        if (keysValues == null || keysValues.isEmpty()) {
            throw new IllegalArgumentException("Metadata map must not be null or empty");
        }


        List<Document> result = new ArrayList<>();
        for (URI uri : this.documentURIs) {
            Document doc = this.documentTree.get(uri);
            if (doc == null) {
                // Try to load document from disk if not in memory
                try {
                    doc = this.pm.deserialize(uri);
                    if (doc != null) {
                        this.documentTree.put(uri, doc);
                        this.minHeap.insert(doc);
                        this.currentDocumentBytes += getSizeInBytes(doc);
                        doc.setLastUseTime(System.nanoTime());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (doc == null) {
                continue; // If still null, skip this document
            }
            boolean match = true;
            for (Map.Entry<String, String> entry : keysValues.entrySet()) {
                String metadataKey = entry.getKey();
                String expectedValue = entry.getValue();
                String actualValue = doc.getMetadataValue(metadataKey);
                if (actualValue == null || !actualValue.equals(expectedValue)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                result.add(doc);
            }
        }
        return result;
    }






    @Override
    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String, String> keysValues) throws IOException {
        if (keyword == null || keysValues == null || keysValues.isEmpty()) {
            throw new IllegalArgumentException("Keyword and keysValues map must not be null or empty.");
        }


        List<Document> result = new ArrayList<>();
        for (Document doc : search(keyword)) {
            boolean match = true;
            for (Map.Entry<String, String> entry : keysValues.entrySet()) {
                String metadataKey = entry.getKey();
                String expectedValue = entry.getValue();


                if (metadataKey == null) {
                    throw new IllegalArgumentException("Metadata key must not be null.");
                }


                String actualValue = doc.getMetadataValue(metadataKey);
                if (actualValue == null || !actualValue.equalsIgnoreCase(expectedValue)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                result.add(doc);
            }
        }
        return result;
    }


    @Override
    public List<Document> searchByPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) throws IOException {
        if (keywordPrefix == null || keysValues == null || keysValues.isEmpty()) {
            throw new IllegalArgumentException("Keyword prefix and keysValues map must not be null or empty.");
        }


        keywordPrefix = keywordPrefix.toLowerCase();
        Map<String, String> formattedKeysValues = new HashMap<>();
        keysValues.forEach((k, v) -> formattedKeysValues.put(k.toLowerCase(), v.toLowerCase()));


        List<Document> documents = this.searchByPrefix(keywordPrefix);
        List<Document> filteredDocuments = new ArrayList<>();


        for (Document doc : documents) {
            if (matchesAllMetadata(doc, formattedKeysValues)) {
                filteredDocuments.add(doc);
            }
        }


        return filteredDocuments;
    }


    private boolean matchesAllMetadata(Document doc, Map<String, String> keysValues) {
        for (Map.Entry<String, String> entry : keysValues.entrySet()) {
            String metadataKey = entry.getKey();
            String expectedValue = entry.getValue();
            String actualValue = doc.getMetadataValue(metadataKey);
            if (actualValue == null || !actualValue.toLowerCase().equals(expectedValue)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public Set<URI> deleteAllWithMetadata(Map<String, String> keysValues)throws IOException {
        if (keysValues == null) {
            throw new IllegalArgumentException("Metadata map must not be null");
        }


        Set<Document> documentsToDelete = new HashSet<>(searchByMetadata(keysValues));
        Set<URI> deletedURIs = new HashSet<>();


        if (documentsToDelete.isEmpty()) {
            return deletedURIs;
        }


        CommandSet<URI> commandSet = new CommandSet<>();


        for (Document doc : documentsToDelete) {
            URI uri = doc.getKey();
            if (delete(uri)) {
                deletedURIs.add(uri);
                GenericCommand<URI> command = new GenericCommand<>(uri, uri1 -> {
                    this.documentTree.put(uri1, doc);
                    this.documentURIs.add(uri1);
                    this.minHeap.insert(doc);
                    this.currentDocumentBytes += getSizeInBytes(doc);
                    reAddToIndexes(doc);
                });
                commandSet.addCommand(command);
            }
        }


        if (!commandSet.isEmpty()) {
            this.commandStack.push(commandSet);
        }


        return deletedURIs;
    }


    @Override
    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword, Map<String, String> keysValues) throws IOException {
        Set<URI> deletedURIs = new HashSet<>();
        for (Document doc : searchByKeywordAndMetadata(keyword, keysValues)) {
            delete(doc.getKey());
            deletedURIs.add(doc.getKey());
        }
        return deletedURIs;
    }


    @Override
    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) throws IOException {
        Set<URI> deletedURIs = new HashSet<>();
        for (Document doc : searchByPrefixAndMetadata(keywordPrefix, keysValues)) {
            delete(doc.getKey());
            deletedURIs.add(doc.getKey());
        }
        return deletedURIs;
    }


    @Override
    public void setMaxDocumentCount(int limit) {
        this.maxDocumentCount = limit;
        enforceMemoryLimits();
    }


    @Override
    public void setMaxDocumentBytes(int limit) {
        this.maxDocumentBytes = limit;
        enforceMemoryLimits();
    }


    private void enforceMemoryLimits() {
        while ((this.maxDocumentCount > 0 && this.currentDocumentCount > this.maxDocumentCount) ||
                (this.maxDocumentBytes > 0 && this.currentDocumentBytes > this.maxDocumentBytes)) {
            Document docToSerialize = this.minHeap.remove();
            if (docToSerialize != null) {
                try {
                    this.pm.serialize(docToSerialize.getKey(), docToSerialize);
                    this.documentTree.put(docToSerialize.getKey(), null);
                    this.currentDocumentBytes -= getSizeInBytes(docToSerialize);
                    this.currentDocumentCount--; // Decrement count when document is moved to disk
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }




}
