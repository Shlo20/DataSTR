import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.DocumentStore;
import edu.yu.cs.com1320.project.stage6.impl.DocumentStoreImpl;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class Stage6Tests {
    private DocumentStoreImpl documentStore;

    @Before
    public void setUp() throws IOException {
        // Create a temporary directory for testing
        File baseDir = Files.createTempDirectory("stage6Test").toFile();
        documentStore = new DocumentStoreImpl(baseDir);
    }

    @Test
    public void testPutAndGetTextDocument() throws IOException {
        URI uri = URI.create("http://edu.yu.cs/com1320/project/stage6/testPutAndGetTextDocument");
        String text = "Hello, this is a test document.";

        documentStore.put(new ByteArrayInputStream(text.getBytes()), uri, DocumentStore.DocumentFormat.TXT);

        Document document = documentStore.get(uri);
        assertNotNull(document);
        assertEquals(text, document.getDocumentTxt());
    }

    @Test
    public void testPutAndGetBinaryDocument() throws IOException {
        URI uri = URI.create("http://edu.yu.cs/com1320/project/stage6/testPutAndGetBinaryDocument");
        byte[] binaryData = "Binary data".getBytes();

        documentStore.put(new ByteArrayInputStream(binaryData), uri, DocumentStore.DocumentFormat.BINARY);

        Document document = documentStore.get(uri);
        assertNotNull(document);
        assertArrayEquals(binaryData, document.getDocumentBinaryData());
    }

    @Test
    public void testMoveToDiskAndBack() throws IOException {
        URI uri = URI.create("http://edu.yu.cs/com1320/project/stage6/testMoveToDiskAndBack");
        String text = "This document will be moved to disk.";

        documentStore.put(new ByteArrayInputStream(text.getBytes()), uri, DocumentStore.DocumentFormat.TXT);

        // Move the document to disk by triggering memory limits
        documentStore.setMaxDocumentCount(1);
        documentStore.setMaxDocumentBytes(1);

        // Ensure the document is moved to disk
        Document documentOnDisk = documentStore.get(uri);
        assertNotNull(documentOnDisk);
        assertEquals(text, documentOnDisk.getDocumentTxt());
    }

    @Test
    public void testSetMetadata() throws IOException {
        URI uri = URI.create("http://edu.yu.cs/com1320/project/stage6/testSetMetadata");
        String text = "Document with metadata.";

        documentStore.put(new ByteArrayInputStream(text.getBytes()), uri, DocumentStore.DocumentFormat.TXT);
        documentStore.setMetadata(uri, "author", "testAuthor");

        Document document = documentStore.get(uri);
        assertEquals("testAuthor", document.getMetadataValue("author"));
    }

    @Test
    public void testDeleteDocument() throws IOException {
        URI uri = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteDocument");
        String text = "Document to be deleted.";

        documentStore.put(new ByteArrayInputStream(text.getBytes()), uri, DocumentStore.DocumentFormat.TXT);

        boolean deleted = documentStore.delete(uri);
        assertTrue(deleted);
        assertNull(documentStore.get(uri));
    }

    @Test
    public void testUndoPut() throws IOException {
        URI uri = URI.create("http://edu.yu.cs/com1320/project/stage6/testUndoPut");
        String text = "Document for undo put.";

        documentStore.put(new ByteArrayInputStream(text.getBytes()), uri, DocumentStore.DocumentFormat.TXT);

        documentStore.undo();

        assertNull(documentStore.get(uri));
    }

    @Test
    public void testUndoDelete() throws IOException {
        URI uri = URI.create("http://edu.yu.cs/com1320/project/stage6/testUndoDelete");
        String text = "Document for undo delete.";

        documentStore.put(new ByteArrayInputStream(text.getBytes()), uri, DocumentStore.DocumentFormat.TXT);

        documentStore.delete(uri);
        documentStore.undo();

        Document document = documentStore.get(uri);
        assertNotNull(document);
        assertEquals(text, document.getDocumentTxt());
    }

    @Test
    public void testSearchDocument() throws IOException {
        URI uri = URI.create("http://edu.yu.cs/com1320/project/stage6/testSearchDocument");
        String text = "Document for search functionality.";

        documentStore.put(new ByteArrayInputStream(text.getBytes()), uri, DocumentStore.DocumentFormat.TXT);

        var results = documentStore.search("search");
        assertFalse(results.isEmpty());
        assertEquals(text, results.get(0).getDocumentTxt());
    }

    @Test
    public void testSearchByPrefix() throws IOException {
        URI uri = URI.create("http://edu.yu.cs/com1320/project/stage6/testSearchByPrefix");
        String text = "Document for testing prefix search.";

        documentStore.put(new ByteArrayInputStream(text.getBytes()), uri, DocumentStore.DocumentFormat.TXT);

        var results = documentStore.searchByPrefix("test");
        assertFalse(results.isEmpty());
        assertEquals(text, results.get(0).getDocumentTxt());
    }

    @Test
    public void testDeleteAllWithPrefix() throws IOException {
        URI uri1 = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteAllWithPrefix1");
        URI uri2 = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteAllWithPrefix2");
        String text1 = "First document for delete all prefix.";
        String text2 = "Second document for delete all prefix.";

        documentStore.put(new ByteArrayInputStream(text1.getBytes()), uri1, DocumentStore.DocumentFormat.TXT);
        documentStore.put(new ByteArrayInputStream(text2.getBytes()), uri2, DocumentStore.DocumentFormat.TXT);

        var deletedURIs = documentStore.deleteAllWithPrefix("prefix");
        assertEquals(2, deletedURIs.size());
        assertNull(documentStore.get(uri1));
        assertNull(documentStore.get(uri2));
    }

    @Test
    public void testDeleteAllWithKeyword() throws IOException {
        URI uri1 = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteAllWithKeyword1");
        URI uri2 = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteAllWithKeyword2");
        String text1 = "First document with the keyword delete.";
        String text2 = "Second document with the keyword delete.";

        documentStore.put(new ByteArrayInputStream(text1.getBytes()), uri1, DocumentStore.DocumentFormat.TXT);
        documentStore.put(new ByteArrayInputStream(text2.getBytes()), uri2, DocumentStore.DocumentFormat.TXT);

        Set<URI> deletedURIs = documentStore.deleteAll("delete");
        assertEquals(2, deletedURIs.size());
        assertNull(documentStore.get(uri1));
        assertNull(documentStore.get(uri2));
    }

    @Test
    public void testDeleteAllWithMetadata() throws IOException {
        URI uri1 = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteAllWithMetadata1");
        URI uri2 = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteAllWithMetadata2");
        String text1 = "Document with metadata for delete.";
        String text2 = "Another document with metadata for delete.";

        documentStore.put(new ByteArrayInputStream(text1.getBytes()), uri1, DocumentStore.DocumentFormat.TXT);
        documentStore.put(new ByteArrayInputStream(text2.getBytes()), uri2, DocumentStore.DocumentFormat.TXT);

        documentStore.setMetadata(uri1, "author", "testAuthor");
        documentStore.setMetadata(uri2, "author", "testAuthor");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("author", "testAuthor");

        Set<URI> deletedURIs = documentStore.deleteAllWithMetadata(metadata);
        assertEquals(2, deletedURIs.size());
        assertNull(documentStore.get(uri1));
        assertNull(documentStore.get(uri2));
    }

    @Test
    public void testDeleteAllWithKeywordAndMetadata() throws IOException {
        URI uri1 = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteAllWithKeywordAndMetadata1");
        URI uri2 = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteAllWithKeywordAndMetadata2");
        String text1 = "Document with keyword and metadata for delete.";
        String text2 = "Another document with keyword and metadata for delete.";

        documentStore.put(new ByteArrayInputStream(text1.getBytes()), uri1, DocumentStore.DocumentFormat.TXT);
        documentStore.put(new ByteArrayInputStream(text2.getBytes()), uri2, DocumentStore.DocumentFormat.TXT);

        documentStore.setMetadata(uri1, "author", "testAuthor");
        documentStore.setMetadata(uri2, "author", "testAuthor");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("author", "testAuthor");

        Set<URI> deletedURIs = documentStore.deleteAllWithKeywordAndMetadata("delete", metadata);
        assertEquals(2, deletedURIs.size());
        assertNull(documentStore.get(uri1));
        assertNull(documentStore.get(uri2));
    }

    @Test
    public void testDeleteAllWithPrefixAndMetadata() throws IOException {
        URI uri1 = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteAllWithPrefixAndMetadata1");
        URI uri2 = URI.create("http://edu.yu.cs/com1320/project/stage6/testDeleteAllWithPrefixAndMetadata2");
        String text1 = "Document with prefix and metadata for delete.";
        String text2 = "Another document with prefix and metadata for delete.";

        documentStore.put(new ByteArrayInputStream(text1.getBytes()), uri1, DocumentStore.DocumentFormat.TXT);
        documentStore.put(new ByteArrayInputStream(text2.getBytes()), uri2, DocumentStore.DocumentFormat.TXT);

        documentStore.setMetadata(uri1, "author", "testAuthor");
        documentStore.setMetadata(uri2, "author", "testAuthor");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("author", "testAuthor");

        Set<URI> deletedURIs = documentStore.deleteAllWithPrefixAndMetadata("delete", metadata);
        assertEquals(2, deletedURIs.size());
        assertNull(documentStore.get(uri1));
        assertNull(documentStore.get(uri2));
    }

}
