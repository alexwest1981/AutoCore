package com.wac.autocore.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SCRUM-334: the flag for native access has to be set on every way in, not only on the one the
 * developer happens to use.
 *
 * The warning about restricted methods came back whenever the program was started in a way the pom
 * did not know about: the jar in its own right, the stand-alone program, the script. The ways in
 * are read from the files that decide them, so a path that loses the flag fails here instead of
 * turning up as a warning in someone's terminal.
 */
class BuildFlagsTest {

    private static final String FLAG = "ALL-UNNAMED";

    @Test
    void theJarCarriesTheFlagInItsManifest() throws IOException {

        String pom = read("pom.xml");

        assertTrue(pom.contains("<Enable-Native-Access>" + FLAG + "</Enable-Native-Access>"),
                "java -jar läser manifestet, så flaggan måste stå där och inte bara på kommandoraden");
    }

    @Test
    void mavenRunAndTheStartScriptPassTheFlagOnTheCommandLine() throws IOException {

        String pom = read("pom.xml");
        assertEquals(1, count(pom, "--enable-native-access=" + FLAG),
                "spring-boot:run får flaggan genom pomens jvmArguments");

        Path script = Paths.get("..", "..", "start.sh").normalize();
        assertTrue(Files.isRegularFile(script), "hittar inte start.sh på " + script.toAbsolutePath());
        assertTrue(Files.readString(script, StandardCharsets.UTF_8).contains("--enable-native-access=" + FLAG),
                "start.sh ska sätta samma flagga som pom.xml");
    }

    @Test
    void theStandaloneProgramIsGivenTheFlagWhereItIsDocumented() throws IOException {

        Path readme = Paths.get("..", "..", "README.md").normalize();
        assertTrue(Files.isRegularFile(readme), "hittar inte README på " + readme.toAbsolutePath());

        assertTrue(Files.readString(readme, StandardCharsets.UTF_8).contains("java --enable-native-access=" + FLAG),
                "kommandot för det fristående programmet i README ska bära flaggan");
    }

    private static String read(String name) throws IOException {
        Path path = Paths.get(name);
        assertTrue(Files.isRegularFile(path), "kör provet från modulkatalogen, hittar inte " + path.toAbsolutePath());
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static int count(String text, String needle) {
        int found = 0;
        int at = text.indexOf(needle);
        while (at >= 0) {
            found++;
            at = text.indexOf(needle, at + needle.length());
        }
        return found;
    }
}
