package com.ptah.bundle;

import com.ptah.config.UniversalEmotesPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class BundleDiscovery {
    private BundleDiscovery() { }
    public static Path directory() { return UniversalEmotesPaths.bundles(); }
    public static List<Path> discover() throws IOException {
        Path directory = directory();
        Files.createDirectories(directory);
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }
}
