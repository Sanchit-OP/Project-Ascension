package com.ascension.compat.ftbquests.internal.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

/**
 * What both dev checks ({@link QuestChecks} on a server, the screen check on a client) share: being switched
 * on by a trigger file, and saying what they found in one format, {@code [name] PASS ...} or
 * {@code [name] FAIL ...} and a closing {@code RESULT} line, so a script can read either one the same way.
 */
public final class CheckReport {

    private final String tag;
    private int checks;
    private int failures;

    /** @param name the tag every line starts with, such as {@code quest-checks} */
    public CheckReport(String name) {
        this.tag = "[" + name + "] ";
    }

    /**
     * Whether the trigger file is in the game directory. It is deleted as it is read, so the next start of
     * the game is an ordinary one; if it cannot be deleted the check stays off rather than run on every start.
     */
    public static boolean consumeTrigger(String fileName) {
        Path trigger = FMLPaths.GAMEDIR.get().resolve(fileName);
        if (!Files.exists(trigger)) {
            return false;
        }
        try {
            Files.deleteIfExists(trigger);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void expect(String what, boolean ok) {
        checks++;
        if (ok) {
            print("PASS " + what);
        } else {
            failures++;
            print("FAIL " + what);
        }
    }

    public void fail(String what) {
        expect(what, false);
    }

    public void print(String line) {
        System.out.println(tag + line);
    }

    /** The closing line. */
    public void finish() {
        print("RESULT: " + checks + " checks, " + failures + " failures");
    }
}
