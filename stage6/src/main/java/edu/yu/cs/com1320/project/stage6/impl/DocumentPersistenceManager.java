package edu.yu.cs.com1320.project.stage6.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.PersistenceManager;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;

public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private final File baseDir;

    public DocumentPersistenceManager(File baseDir) {
        this.baseDir = baseDir != null ? baseDir : new File(System.getProperty("user.dir"));
    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(val);
        File file = new File(this.baseDir, uri.getHost() + uri.getPath().replace("/", File.separator) + ".json");
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directories for path: " + parentDir.getAbsolutePath());
            }
        }
        try (Writer writer = new FileWriter(file)) {
            writer.write(json);
        }
        System.out.println("Serialized document with key: " + uri + " to path: " + file.getAbsolutePath());
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        File file = new File(this.baseDir, uri.getHost() + uri.getPath().replace("/", File.separator) + ".json");
        if (!file.exists()) {
            System.out.println("File not found for deserialization: " + file.getAbsolutePath());
            return null;
        }
        Gson gson = new Gson();
        try (Reader reader = new FileReader(file)) {
            Document document = gson.fromJson(reader, DocumentImpl.class);
            System.out.println("Deserialized document with key: " + uri + " from " + file.getAbsolutePath());
            return document;
        } catch (FileNotFoundException e) {
            System.out.println("File not found for deserialization: " + file.getAbsolutePath());
            return null;
        }
    }

    @Override
    public boolean delete(URI uri) throws IOException {
        File file = new File(this.baseDir, uri.getHost() + uri.getPath().replace("/", File.separator) + ".json");
        boolean result = Files.deleteIfExists(file.toPath());
        System.out.println("Delete operation for key: " + uri + " from path: " + file.getAbsolutePath() + " was " + (result ? "successful" : "unsuccessful"));
        return result;
    }
}
