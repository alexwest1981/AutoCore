package com.wac.autocore.theme;

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
import java.util.stream.Stream;

/**
 * The interface must not move when the pointer moves over it.
 *
 * A border in JavaFX is part of a control's insets, so a rule that widens the border on
 * {@code :focused} or {@code :hover} makes the control bigger and pushes everything beside it.
 * That is what made buttons and forms look like they were bouncing. A rule for a state may change
 * a colour, a radius or an effect; it may not change anything that is measured during layout.
 *
 * The scan reads the stylesheets from the classpath, the same files the app loads, and parses them
 * the way a browser would: selector, then block, with comments removed.
 */
class LayoutStillnessTest {

    /** Properties that are read during layout, so changing one in a state rule moves the window. */
    private static final String[] GEOMETRY = {
        "-fx-border-width", "-fx-border-insets",
        "-fx-padding",
        "-fx-font-size", "-fx-font-family", "-fx-font-weight",
        "-fx-min-width", "-fx-min-height",
        "-fx-pref-width", "-fx-pref-height",
        "-fx-max-width", "-fx-max-height",
    };

    /** The pseudo-classes a control can be in while the user is working. */
    private static final Pattern STATE = Pattern.compile(
            ":(pressed|focused|hover|armed|selected|disabled|showing|contains-focus)\\b");

    private static final Pattern BLOCK = Pattern.compile("([^{}]+)\\{([^{}]*)\\}", Pattern.DOTALL);

    @Test
    void noStateRuleChangesTheSizeOfAControl() throws IOException {

        Path themeDir = themeDir();

        List<Path> sheets;
        try (Stream<Path> files = Files.walk(themeDir)) {
            sheets = files.filter(p -> p.toString().endsWith(".css")).sorted()
                    .collect(java.util.stream.Collectors.toList());
        }

        assertTrue(sheets.size() >= 10, "hittade bara " + sheets.size() + " stilmallar");

        List<String> offences = new ArrayList<String>();

        for (Path sheet : sheets) {
            String text = stripComments(new String(Files.readAllBytes(sheet), StandardCharsets.UTF_8));
            Matcher block = BLOCK.matcher(text);

            while (block.find()) {
                String selector = oneLine(block.group(1));
                String body = block.group(2);

                if (!STATE.matcher(selector).find()) {
                    continue;
                }
                for (String property : GEOMETRY) {
                    if (body.contains(property + ":")) {
                        offences.add(themeDir.relativize(sheet) + " " + selector + " sätter " + property);
                    }
                }
            }
        }

        assertTrue(offences.isEmpty(),
                "en tillståndsregel får ändra färg och effekt, inte storlek:\n"
                        + String.join("\n", offences));
    }

    /**
     * The stylesheets as they lie in the repository.
     *
     * The classpath is not enough: the test's own package sits at the same path in target/test-classes,
     * and that folder holds classes and no CSS, so a classpath lookup finds it first and comes up
     * empty. The source tree is complete, and it is the file a person edits.
     */
    private static Path themeDir() {
        Path fromSource = Paths.get("src/main/resources/com/wac/autocore/theme");
        if (Files.isDirectory(fromSource)) {
            return fromSource;
        }
        Path working = Paths.get("").toAbsolutePath();
        throw new IllegalStateException(
                "hittar inte src/main/resources/com/wac/autocore/theme från " + working
                        + "; provet måste köras från modulkatalogen");
    }

    private static String stripComments(String css) {
        return css.replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private static String oneLine(String selector) {
        return selector.trim().replaceAll("\\s+", " ");
    }
}
