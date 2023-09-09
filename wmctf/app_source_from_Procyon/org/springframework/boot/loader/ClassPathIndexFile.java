// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader;

import java.net.URISyntaxException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.net.URL;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.List;
import java.io.File;

final class ClassPathIndexFile
{
    private final File root;
    private final List<String> lines;
    
    private ClassPathIndexFile(final File root, final List<String> lines) {
        this.root = root;
        this.lines = lines.stream().map((Function<? super Object, ?>)this::extractName).collect((Collector<? super Object, ?, List<String>>)Collectors.toList());
    }
    
    private String extractName(final String line) {
        if (line.startsWith("- \"") && line.endsWith("\"")) {
            return line.substring(3, line.length() - 1);
        }
        throw new IllegalStateException("Malformed classpath index line [" + line + "]");
    }
    
    int size() {
        return this.lines.size();
    }
    
    boolean containsEntry(final String name) {
        return name != null && !name.isEmpty() && this.lines.contains(name);
    }
    
    List<URL> getUrls() {
        return Collections.unmodifiableList((List<? extends URL>)this.lines.stream().map((Function<? super Object, ?>)this::asUrl).collect((Collector<? super Object, ?, List<? extends T>>)Collectors.toList()));
    }
    
    private URL asUrl(final String line) {
        try {
            return new File(this.root, line).toURI().toURL();
        }
        catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    static ClassPathIndexFile loadIfPossible(final URL root, final String location) throws IOException {
        return loadIfPossible(asFile(root), location);
    }
    
    private static ClassPathIndexFile loadIfPossible(final File root, final String location) throws IOException {
        return loadIfPossible(root, new File(root, location));
    }
    
    private static ClassPathIndexFile loadIfPossible(final File root, final File indexFile) throws IOException {
        if (indexFile.exists() && indexFile.isFile()) {
            try (final InputStream inputStream = new FileInputStream(indexFile)) {
                return new ClassPathIndexFile(root, loadLines(inputStream));
            }
        }
        return null;
    }
    
    private static List<String> loadLines(final InputStream inputStream) throws IOException {
        final List<String> lines = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (!line.trim().isEmpty()) {
                lines.add(line);
            }
        }
        return Collections.unmodifiableList((List<? extends String>)lines);
    }
    
    private static File asFile(final URL url) {
        if (!"file".equals(url.getProtocol())) {
            throw new IllegalArgumentException("URL does not reference a file");
        }
        try {
            return new File(url.toURI());
        }
        catch (URISyntaxException ex) {
            return new File(url.getPath());
        }
    }
}
