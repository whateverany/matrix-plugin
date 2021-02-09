package dev.dhdf.polo.types;

import org.json.JSONArray;
import org.json.JSONObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

import java.lang.IllegalArgumentException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * This represents intermediate JSON formatted message data
 */
public class IntermediateJSON {
    // Intermediate JSON data
    private final Object json;
    // UUIDs of players who've been explicitly mentioned
    private Set<UUID> mentions;
    // Whether the whole room has been legitimately mentioned
    private boolean roomMention;
    // Converted minecraft raw JSON
    private Component component;
    // Converted minecraft highlighted raw JSON (for mentions)
    private Component componentHighlight;
    // Converted minecraft legacy format coded
    private String legacy;
    // Converted minecraft legacy format coded (for mentions)
    private String legacyHighlight;

    /**
     * Constructor.
     * @param json Object Intermediate JSON formatted message data from
     *                    appservice.
     */
    public IntermediateJSON(Object json) {
        this.json = json;
        mentions = null;
        roomMention = false;
        component = null;
        componentHighlight = null;
        legacy = null;
        legacyHighlight = null;
    }

    /**
     * Get the equivalent text component.
     * @return Component Text component.
     */
    public Component getComponent() {
        if (component == null) {
            if (mentions == null)
                mentions = new HashSet<UUID>();
            component = toComponent(json, null, 0, 0, new StringBuilder(), new StringBuilder(), textWidth, false);
        }
        return component;
    }

    /**
     * Get the equivalent text component for a highlighted message.
     * @return Component Text component.
     */
    public Component getComponentHighlight() {
        if (componentHighlight == null)
            componentHighlight = Component.text("", NamedTextColor.DARK_RED).append(getComponent());
        return componentHighlight;
    }

    /**
     * Get the equivalent legacy format coded string.
     * @return String Minecraft legacy formatting coded string
     */
    public String getLegacy() {
        if (legacy == null)
            legacy = LegacyComponentSerializer.legacySection().serialize(getComponent());
        return legacy;
    }

    /**
     * Get the equivalent legacy format coded string for a highlighted message.
     * @return String Minecraft legacy formatting coded string
     */
    public String getLegacyHighlight() {
        if (legacyHighlight == null)
            legacyHighlight = LegacyComponentSerializer.legacySection().serialize(getComponentHighlight());
        return legacyHighlight;
    }

    /**
     * Get a set of player UUIDs mentioned in the message.
     * @return Set<UUID> Set of player UUIDs
     */
    public Set<UUID> getMentions() {
        getComponent();
        return mentions;
    }

    /**
     * Get whether the entire room was legitimately mentioned in the message.
     * @return boolean True if room was mentioned
     */
    public boolean getRoomMention() {
        getComponent();
        return roomMention;
    }


    // Static configuration

    // Width of text pane (scaled pixels)
    private static int textWidth = 320;
    // Width of hover pane (scaled pixels)
    private static int textWidthHover = 200;
    /*
     * Maximum recursion depth.
     * Matrix specifies a maximum depth of 100 HTML tags.
     * Each tag could emit an object and a nested array for the content.
     * Additionally we should allow for an array at the root level, and for
     * WebClient to wrap that in an inline block in an array to prepend the
     * sender.
     */
    private static int maxDepth = 100*2 + 3;

    /**
     * Convert a chunk of intermediate JSON to a text component.
     * @param json        Object        Intermediate JSON formatted message data from appservice.
     * @param indentation ArrayList     List of indentation components to be inserted after each newline.
     * @param depth       int           Depth of recursion (limited to maxDepth).
     * @param bulletDepth int           Depth of bullets (changes type of bullets).
     * @param curLine     StringBuilder Current line of text (excl. indentation).
     * @param fullCurLine StringBuilder Current line of text (incl. indentation).
     * @param maxWidth    int           Max line width.
     * @param obfuscated  boolean       Whether the component is already obfuscated.
     * @return Component Text component.
     */
    private Component toComponent(/* inputs */
                                  Object json,
                                  /* intermediates */
                                  ArrayList<Component> indentation,
                                  int depth, int bulletDepth,
                                  StringBuilder curLine, StringBuilder fullCurLine,
                                  int maxWidth, boolean obfuscated) {
        // Rather bluntly avoid a stack overflow.
        if (++depth > maxDepth)
            return Component.text("");

        if (json instanceof String) {
            return stringToComponent((String)json, indentation, curLine, fullCurLine);
        } else if (json instanceof JSONArray) {
            JSONArray arr = (JSONArray)json;
            TextComponent.Builder ret = Component.text();
            for (Object obj: arr)
                if (obj != null)
                    ret.append(toComponent(obj, indentation, depth, bulletDepth, curLine, fullCurLine, maxWidth, obfuscated));
            return ret.build();
        } else if (json instanceof JSONObject) {
            JSONObject obj = (JSONObject)json;
            TextComponent.Builder newObj = Component.text();
            Component indentNow = null;
            String type = obj.getString("type");
            switch (type) {
            case "block":
                Component newIndentation = null;
                String blockType = obj.optString("block", "");
                switch (blockType) {
                case "quote":
                    newIndentation = quoteToComponent(obfuscated);
                    indentNow = newIndentation;
                    break;
                case "bullet":
                    String text = bulletToText(obj, bulletDepth);
                    newIndentation = Component.text(indentFor(text, " "));

                    TextComponent bullet = Component.text(text, NamedTextColor.GRAY);
                    indentNow = bullet;

                    ++bulletDepth;
                    break;
                case "inline":
                    newIndentation = Component.text(indentFor(curLine.toString(), " "));
                    break;
                }
                if (newIndentation != null) {
                    if (indentation == null)
                        indentation = new ArrayList<Component>();
                    else
                        indentation = new ArrayList<Component>(indentation);
                    // Append indentation
                    indentation.add(newIndentation);
                }
                break;
            case "style":
                if (styleToComponent(obj, depth, bulletDepth, newObj))
                    obfuscated = true;
                break;
            case "link":
                linkToComponent(obj, newObj);
                break;
            case "mention":
                mentionToComponent(obj, newObj);
                break;
            case "img":
                imgToComponent(obj, newObj);
                break;
            case "horizontalRule":
                hrToComponent(obj, fullCurLine, maxWidth, newObj);
                break;
            }
            if (indentNow != null)
                fullCurLine.append(PlainComponentSerializer.plain().serialize(indentNow));
            if (obj.has("content"))
                newObj.append(toComponent(obj.get("content"), indentation, depth, bulletDepth, curLine, fullCurLine, maxWidth, obfuscated));
            if (indentNow != null) {
                TextComponent.Builder indented = Component.text();
                indented.append(indentNow);
                indented.append(newObj.build());
                return indented.build();
            }
            return newObj.build();
        } else {
            return null;
        }
    }

    /**
     * Convert a string in intermediate JSON to a text component.
     * @param text        String        Intermediate JSON raw string from appservice.
     * @param indentation ArrayList     List of indentation components to be inserted after each newline.
     * @param curLine     StringBuilder Current line of text (excl. indentation).
     * @param fullCurLine StringBuilder Current line of text (incl. indentation).
     * @return Component Text component.
     */
    private static Component stringToComponent(String text,
                                               ArrayList<Component> indentation,
                                               StringBuilder curLine,
                                               StringBuilder fullCurLine) {
        // Split after newlines and inject indentation
        if (indentation != null) {
            String[] lines = text.split("\n", -1);
            if (lines.length > 1) {
                TextComponent.Builder ret = Component.text();
                for (int i = 0; i < lines.length - 1; ++i) {
                    ret.append(Component.text(lines[i] + "\n"));
                    for (Component item: indentation)
                        ret.append(item);
                }
                ret.append(Component.text(lines[lines.length - 1]));

                // Set curLine to last line (excluding indentation)
                curLine.delete(0, curLine.length());
                curLine.append(lines[lines.length - 1]);

                // Include indentation in full line
                fullCurLine.delete(0, fullCurLine.length());
                fullCurLine.append(curLine);
                for (Component item: indentation)
                    fullCurLine.append(PlainComponentSerializer.plain().serialize(item));

                return ret.build();
            }

            // No newlines, append the text to curLine
            curLine.append(text);
            fullCurLine.append(text);
        } else {
            // Check for newlines, set curLine to last line
            int lastIndex = text.lastIndexOf("\n");
            if (lastIndex < 0) {
                curLine.append(text);
                fullCurLine.append(text);
            } else {
                curLine.delete(0, curLine.length());
                curLine.append(text.substring(lastIndex + 1, text.length()));
                fullCurLine.delete(0, curLine.length());
                fullCurLine.append(curLine);
            }
        }
        // Set curLine to last line
        return Component.text(text);
    }

    /**
     * Get a text Component for a quote prefix.
     * @param obfuscated boolean Whether the component is already obfuscated.
     * @return Component Text component.
     */
    private static Component quoteToComponent(boolean obfuscated) {
        TextComponent.Builder quote = Component.text();
        quote.content("| ");
        quote.color(NamedTextColor.GRAY);
        if (!obfuscated)
            quote.decoration(TextDecoration.OBFUSCATED, false);
        return quote.build();
    }

    /**
     * Get a text for a bullet point.
     * @param obj         JSONObject    Intermediate JSON object for bullet point.
     * @param bulletDepth int           Depth of bullets (changes type of bullets).
     * @return String Text used for bullet point.
     */
    private static String bulletToText(JSONObject obj, int bulletDepth) {
        if (obj.has("n")) {
            int num = obj.optInt("n", 0);
            if (bulletDepth == 0)
                return String.format("%d. ", num);
            else if (bulletDepth == 1)
                return toRomanNumerals(num) + ". ";
            else
                return toAlphabeticRadix(num) + ". ";
        } else {
            if (bulletDepth == 0)
                return " \u25e6 "; // White bullet
            else
                return " \u2022 "; // Bullet
        }
    }

    /**
     * Style a TextComponent based on intermediate JSON style object.
     * @param obj         JSONObject Intermediate JSON style object.
     * @param depth       int        Depth of recursion (limited to maxDepth).
     * @param bulletDepth int        Depth of bullets (changes type of bullets).
     * @param newObj      Builder    The TextComponent to build.
     * @return boolean Whether obfuscation has been activated.
     */
    private boolean styleToComponent(JSONObject obj,
                                     int depth, int bulletDepth,
                                     TextComponent.Builder newObj) {
        boolean obfuscated = false;

        if (obj.has("color")) {
            int col = obj.getInt("color");
            newObj.color(TextColor.color(col));
        }
        if (obj.optBoolean("bold", false))
            newObj.decorate(TextDecoration.BOLD);
        if (obj.optBoolean("italic", false))
            newObj.decorate(TextDecoration.ITALIC);
        if (obj.optBoolean("underline", false))
            newObj.decorate(TextDecoration.UNDERLINED);
        if (obj.optBoolean("strike", false))
            newObj.decorate(TextDecoration.STRIKETHROUGH);
        if (obj.optBoolean("spoiler", false)) {
            newObj.decorate(TextDecoration.OBFUSCATED);
            obfuscated = true;
            if (obj.has("content")) {
                Component hoverText = toComponent(obj.get("content"), null, depth, bulletDepth, new StringBuilder(), new StringBuilder(), textWidthHover, false);
                newObj.hoverEvent(HoverEvent.showText(hoverText));
            }
        }
        if (obj.optBoolean("code", false)) {
            // not really monospace, but different
            newObj.font(Key.key("uniform"));
        }
        int heading = obj.optInt("heading", 0);
        if (heading > 0)
            newObj.decorate(TextDecoration.BOLD);

        return obfuscated;
    }

    /**
     * Style a TextComponent based on intermediate JSON link object.
     * @param obj         JSONObject Intermediate JSON link object.
     * @param newObj      Builder    The TextComponent to build.
     */
    private static void linkToComponent(JSONObject obj,
                                        TextComponent.Builder newObj) {
        String href = obj.optString("href", "");
        if (href != "") {
            newObj.hoverEvent(HoverEvent.showText(Component.text(href)));
            try {
                newObj.clickEvent(ClickEvent.openUrl(new URL(href)));
                newObj.decorate(TextDecoration.UNDERLINED);
            } catch (MalformedURLException e) {
            }
        }
    }

    /**
     * Style a TextComponent based on intermediate JSON mention object.
     * Also make a record of mentions for later reference.
     * @param obj         JSONObject Intermediate JSON mention object.
     * @param newObj      Builder    The TextComponent to build.
     */
    private void mentionToComponent(JSONObject obj,
                                    TextComponent.Builder newObj) {
        // Handle room mentions
        if (obj.optBoolean("room", false)) {
            roomMention = true;
            return;
        }

        JSONObject user = obj.optJSONObject("user");
        String bridge = obj.optString("bridge", "");

        UUID uuid = null;
        if (bridge.equals("minecraft")) {
            JSONObject player = obj.optJSONObject("player");
            if (player != null) {
                String uuidStr = player.optString("uuid");
                try {
                    uuid = PoloPlayer.uuidFromString(uuidStr);
                    mentions.add(uuid);
                    // This assumes the player's matrix displayName is a valid input to /tell
                    if (user != null)
                        newObj.clickEvent(ClickEvent.suggestCommand("/tell " + user.optString("displayName") + " "));
                } catch (IllegalArgumentException e) {
                }
            }
        }
        if (user != null) {
            String displayName = user.optString("displayName");
            newObj.insertion(displayName);

            String subTitleText = "Shift-click to paste " + displayName + " to the chat";
            if (uuid != null)
                subTitleText = "Click to whisper to " + displayName + "\n" + subTitleText;

            TextComponent.Builder hoverValue = Component.text();
            hoverValue.append(Component.text(displayName + " (" + user.optString("mxid") + ")\n"));
            hoverValue.append(Component.text(subTitleText, NamedTextColor.GOLD, TextDecoration.ITALIC));
            newObj.hoverEvent(HoverEvent.showText(hoverValue.build()));
        }
    }

    /**
     * Style a TextComponent based on intermediate JSON img object.
     * @param obj         JSONObject Intermediate JSON img object.
     * @param newObj      Builder    The TextComponent to build.
     */
    private static void imgToComponent(JSONObject obj,
                                       TextComponent.Builder newObj) {
        String src = obj.optString("src", "");
        String alt = obj.optString("alt", "");
        String title = obj.optString("title", "");

        if (alt.isEmpty())
            alt = title;
        if (alt.isEmpty())
            alt = "image";
        newObj.content(alt);
        newObj.color(NamedTextColor.GRAY);

        if (!src.isEmpty()) {
            try {
                newObj.clickEvent(ClickEvent.openUrl(new URL(src)));
                newObj.decorate(TextDecoration.UNDERLINED);
            } catch (MalformedURLException e) {
            }
        }

        if (!src.isEmpty() || !title.isEmpty()) {
            TextComponent.Builder hoverText = Component.text();
            if (!title.isEmpty()) {
                hoverText.append(Component.text(title));
                if (!src.isEmpty())
                    hoverText.append(Component.text("\n"));
            }
            if (!src.isEmpty())
                hoverText.append(Component.text("Click to open image", NamedTextColor.GOLD, TextDecoration.ITALIC));

            newObj.hoverEvent(HoverEvent.showText(hoverText.build()));
        }
    }

    /**
     * Style a TextComponent based on intermediate JSON horizontalRule object.
     * @param obj         JSONObject    Intermediate JSON horizontalRule object.
     * @param fullCurLine StringBuilder Current line of text (incl. indentation).
     * @param maxWidth    int           Max line width.
     * @param newObj      Builder       The TextComponent to build.
     */
    private static void hrToComponent(JSONObject obj,
                                      StringBuilder fullCurLine,
                                      int maxWidth,
                                      TextComponent.Builder newObj) {
        // Line to max width
        int width = indentWidth(fullCurLine.toString());
        width = maxWidth - width;
        if (width < 6*3)
            width = 6*3;
        newObj.color(NamedTextColor.GRAY);
        newObj.content(indentTo("-", width, false));
        newObj.decorate(TextDecoration.STRIKETHROUGH);
    }

    // Static helpers

    // Helper for generating roman numerals (for numbered bullets)
    // Based on https://codingnconcepts.com/java/integer-to-roman/

    private static final int[] romanValues = {
        1000, 900,  500, 400,  100, 90,   50,  40,   10,  9,    5,   4,    1
    };
    private static final String[] romanLiterals = {
        "m",  "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"
    };

    private static String toRomanNumerals(int num) {
        if (num < 1 || num >= 4000)
            return String.format("%d", num);

        StringBuilder s = new StringBuilder();

        for (int i = 0; i < romanValues.length; i++) {
            while (num >= romanValues[i]) {
                num -= romanValues[i];
                s.append(romanLiterals[i]);
            }
        }
        return s.toString();
    }

    // Helpers for using whitespace to align multiline text

    private static final String[] charWidths = {
        /* 2 */ "!.,:;i|",
        /* 3 */ "'`l",
        /* 4 */ " I[]t\u25e6\u2022",
        /* 5 */ "\"()*<>fk{}",
        /* 6 */ "", // default
        /* 7 */ "@~",
    };

    private static int indentWidth(String str) {
        int width = 0;
        for (int i = 0; i < str.length(); ++i) {
            int thisWidth = 6;
            for (int j = 0; j < charWidths.length; ++j) {
                if (charWidths[j].indexOf(str.charAt(i)) >= 0) {
                    thisWidth = 2 + j;
                    break;
                }
            }
            width += thisWidth;
        }
        return width;
    }

    private static String indentTo(String with, int width, boolean mid) {
        int withWidth = indentWidth(with);
        if (mid)
            width += withWidth / 2;
        return new String(new char[width / withWidth]).replace("\0", with);
    }

    private static String indentFor(String str, String with) {
        return indentTo(with, indentWidth(str), true);
    }

    private static String toAlphabeticRadix(int num) {
        if (num < 1)
            return String.format("%d", num);

        StringBuffer str = new StringBuffer();
        while (num > 0) {
            --num;
            str.append((char)('a' + (num % 26)));
            num = num / 26;
        }

        str.reverse();
        return str.toString();
    }
}
