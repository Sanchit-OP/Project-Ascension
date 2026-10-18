package com.ascension.compat.ftbquests.client.dev;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Reaches FTB Quests' client side by name, for the screen check: find a quest by its page and title, open the
 * book at it, ask whether the player has finished it, and read the code a command needs to complete it.
 *
 * <p>Reflection, on purpose: this jar has no compile dependency on Quests, so a change inside Quests cannot
 * break the build, and a dev tool that names Quests' classes by string needs nothing from it but what it
 * already exposes. Every method throws {@link ReflectiveOperationException} for the caller to report.
 */
final class QuestBookDriver {

    private static final String CLIENT_FILE = "dev.ftb.mods.ftbquests.client.ClientQuestFile";
    private static final String QUEST = "dev.ftb.mods.ftbquests.quest.Quest";
    private static final String QUEST_OBJECT = "dev.ftb.mods.ftbquests.quest.QuestObject";

    private QuestBookDriver() {
    }

    private static Object file() throws ReflectiveOperationException {
        return Class.forName(CLIENT_FILE).getField("INSTANCE").get(null);
    }

    /** The quest on the page titled {@code chapterTitle} whose title is {@code title}, or null. */
    static Object quest(String chapterTitle, String title) throws ReflectiveOperationException {
        Object file = file();
        Collection<?> chapters = (Collection<?>) file.getClass().getMethod("getAllChapters").invoke(file);
        for (Object chapter : chapters) {
            if (!titled(chapter, chapterTitle)) {
                continue;
            }
            Collection<?> quests = (Collection<?>) chapter.getClass().getMethod("getQuests").invoke(chapter);
            for (Object quest : quests) {
                if (titled(quest, title)) {
                    return quest;
                }
            }
        }
        return null;
    }

    /** A title in the lang file comes back from getTitle(); the raw title is empty for ours, so both are tried. */
    private static boolean titled(Object object, String wanted) throws ReflectiveOperationException {
        Object component = object.getClass().getMethod("getTitle").invoke(object);
        String shown = component.getClass().getMethod("getString").invoke(component).toString();
        String raw = (String) object.getClass().getMethod("getRawTitle").invoke(object);
        return wanted.equals(shown) || wanted.equals(raw);
    }

    /** Opens the book on the page the quest is on, with that quest's panel showing. */
    static void open(Object quest) throws ReflectiveOperationException {
        Class<?> fileClass = Class.forName(CLIENT_FILE);
        Method open = fileClass.getMethod("openGui", Class.forName(QUEST), boolean.class);
        open.invoke(null, quest, false);
    }

    /** The code {@code /ftbquests change_progress} takes for this quest. */
    static String code(Object quest) throws ReflectiveOperationException {
        return (String) quest.getClass().getMethod("getCodeString").invoke(quest);
    }

    /** Whether the player's team has finished the quest. */
    static boolean isDone(Object quest) throws ReflectiveOperationException {
        Object file = file();
        Object team = file.getClass().getField("selfTeamData").get(file);
        Method completed = team.getClass().getMethod("isCompleted", Class.forName(QUEST_OBJECT));
        return quest != null && (boolean) completed.invoke(team, quest);
    }
}
