package com.wac.autocore.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Nothing in the production tree may build the service without the guard around it.
 *
 * WAC4_0 §5 says access control must not be possible to get around by taking another way into the
 * application. `GuardedGarageSystem` is that control, so a class that writes `new GarageSystem(...)`
 * is a way around it, whether or not anybody uses the class today.
 *
 * This is a test and not a note in a ticket because it happened: the classes were cleared out, and a
 * day later a console class was moved into the compiled tree and opened the door again, without
 * anything failing.
 */
class NoUnguardedConstructionTest {

    /** A construction of the service itself rather than of the guard around it. */
    private static final Pattern UNGUARDED = Pattern.compile("new\\s+GarageSystem\\s*\\(");

    @Test
    void noProductionClassBuildsTheServiceWithoutTheGuard() throws IOException {

        Path production = productionRoot();

        List<Path> sources;
        try (Stream<Path> files = Files.walk(production)) {
            sources = files.filter(p -> p.toString().endsWith(".java")).sorted()
                    .collect(Collectors.toList());
        }

        assertTrue(sources.size() > 100, "hittade bara " + sources.size() + " källfiler i " + production);

        List<String> offences = new ArrayList<String>();

        for (Path source : sources) {
            String[] lines = new String(Files.readAllBytes(source), StandardCharsets.UTF_8).split("\n");
            for (int i = 0; i < lines.length; i++) {
                Matcher found = UNGUARDED.matcher(lines[i]);
                if (found.find()) {
                    offences.add(production.relativize(source) + ":" + (i + 1) + "  " + lines[i].trim());
                }
            }
        }

        assertTrue(offences.isEmpty(),
                "produktionskoden får inte bygga GarageSystem förbi spärren, använd GuardedGarageSystem:\n"
                        + String.join("\n", offences));
    }

    /** src/main/java, counted from wherever the test is run from. */
    private static Path productionRoot() {
        Path candidate = Paths.get("src/main/java");
        if (Files.isDirectory(candidate)) {
            return candidate;
        }
        throw new IllegalStateException("kör provet från modulkatalogen, hittar inte " + candidate.toAbsolutePath());
    }
}
