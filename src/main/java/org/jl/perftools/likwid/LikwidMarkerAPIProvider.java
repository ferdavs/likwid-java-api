package org.jl.perftools.likwid;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.function.BiPredicate;

public class LikwidMarkerAPIProvider {
    private final static ILikwidMarker likwidMarker;

    static {
        ILikwidMarker _likwidMarker = DummyLikwidMarker.INSTANCE;
        try {
            if (System.getProperty("LIKWID_PERFMON") != null) {
                String path = extractLib();
                System.load(path);
                LikwidMarkerAPI api = new LikwidMarkerAPI();
                _likwidMarker = new NativeLikwidMarker(api);
                System.err.println("Likwid markers are enabled.");
            }
        } catch (UnsatisfiedLinkError e) {
            System.err.printf("Cannot load likwid-api library. Markers are disabled. (%s)%n", e);
        }
        likwidMarker = _likwidMarker;
    }

    public static ILikwidMarker getInstance() {
        return likwidMarker;
    }

    private static String extractLib() {
        try {
            var current = Paths.get("").toAbsolutePath();
            var libFile = Files
                    .find(current, 1000, (path, basicFileAttributes) -> path.getFileName().endsWith("liblikwid_api.so"))
                    .findFirst();
            if (libFile.isPresent()) {
                return libFile.get().toFile().getAbsolutePath();
            }
        } catch (IOException ignored) {

        }
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "likwid-api");
        tmpDir.mkdirs();
        File libFile = new File(tmpDir, "liblikwid-api.so");
        libFile.deleteOnExit();
        InputStream in = LikwidMarkerAPI.class.getResourceAsStream("/liblikwid-api.so");
        try {
            Files.copy(in, libFile.toPath());
        } catch (Exception e) {
            System.err.printf("Cannot extract library to: %s (%s)%n", libFile.getAbsolutePath(), e.getMessage());
        }

        return libFile.getAbsolutePath();
    }
}
