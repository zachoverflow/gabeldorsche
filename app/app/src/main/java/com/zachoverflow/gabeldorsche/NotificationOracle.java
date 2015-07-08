package com.zachoverflow.gabeldorsche;

import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationOracle {
    public static final String CONFIG_FILE = "/sdcard/gabeldorsche.config";

    private static final String LOG_TAG = "Gabeldorsche";
    private static final short CONCATENATE_DELAY_MS = 500;

    private HashMap<String, PackageConfig> packages;
    private HashMap<String, SenderConfig> senders;
    private String urgentRegex;
    private Vibe urgentVibe;

    private RecentNotifications recents;

    private static class PackageConfig {
        String name;
        String senderExtra;
        String senderRegex;
        String textExtra;
        Vibe vibe;
    }

    private static class SenderConfig {
        String name;
        Vibe vibe;
    }

    private enum FileSection {
        UNKNOWN,
        PACKAGES,
        SENDERS,
        URGENT_PREFIX
    }

    public NotificationOracle(RecentNotifications recents) {
        packages = new HashMap<>();
        senders = new HashMap<>();
        this.recents = recents;
        loadFile(CONFIG_FILE);
    }

    public Vibe generateVibeFor(StatusBarNotification notification) {
        PackageConfig packageConfig = this.packages.get(notification.getPackageName());
        if (packageConfig == null) {
            recents.add(notification, false);
            Log.i(LOG_TAG, "Ignoring notification for " + notification.getPackageName());
            return null;
        }

        recents.add(notification, true);

        Bundle extras = notification.getNotification().extras;
        String sender = null;
        if (packageConfig.senderExtra != null) {
            sender = extras.get(packageConfig.senderExtra).toString();
            if (packageConfig.senderRegex != null) {
                Matcher matcher = Pattern.compile(packageConfig.senderRegex).matcher(sender);
                if (matcher.find())
                    sender = matcher.group(1);
                else
                    sender = null;
            }
        }

        String text = null;
        if (packageConfig.textExtra != null)
            text = extras.get(packageConfig.textExtra).toString();

        Vibe prefixVibe = null;
        if (text != null && text.matches(this.urgentRegex)) {
            prefixVibe = this.urgentVibe;
            Log.i(LOG_TAG, "notification classifies as urgent");
        }

        Vibe senderVibe = null;
        if (sender != null) {
            SenderConfig senderConfig = senders.get(sender.trim());
            if (senderConfig != null) {
                senderVibe = senderConfig.vibe;
                Log.i(LOG_TAG, "notification has matching sender: " + senderConfig.name);
            }
        }

        return Vibe.concatenate(CONCATENATE_DELAY_MS, prefixVibe, packageConfig.vibe, senderVibe);
    }

    // Loads a gabeldorsche config file from disk. The parser is kinda hacky, but it works.
    // Will be replaced with a UI at some point.
    private void loadFile(String file) {
        FileSection currentSection = FileSection.UNKNOWN;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = nextNonCommentLine(reader);
            while (line != null) {
                // See first if the line is a section header
                FileSection section = tryParseSection(line, currentSection);
                if (section != null) {
                    currentSection = section;
                    line = nextNonCommentLine(reader);
                    continue;
                }

                switch (currentSection) {
                    case PACKAGES:
                        line = tryParsePackage(line, reader);
                        break;
                    case SENDERS:
                        line = tryParseSender(line, reader);
                        break;
                    case URGENT_PREFIX:
                        line = tryParseUrgentPrefix(line, reader);
                        break;
                    default:
                        // Don't know how to handle this line, so skip it
                        line = nextNonCommentLine(reader);
                        break;
                }

                // A null line indicates either the current line was ignored or the reader
                // has reached the end of the file. If the former is the case, we need to
                // fetch the next line. If the latter is the case, fetching the next
                // line will just return null again so no harm no foul.
                if (line == null)
                    line = nextNonCommentLine(reader);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error reading config file: " + e.getMessage());
        }
    }

    // Tries to parse a section header from the provided line
    // Returns the parsed section header, or null if one could not be parsed.
    private FileSection tryParseSection(String line, FileSection currentSection) {
        if (!line.startsWith("<<") || !line.endsWith(">>"))
            return null;

        String name = line.substring(2, line.length() - 2);
        switch (name.toUpperCase()) {
            case "PACKAGES":
                return FileSection.PACKAGES;
            case "SENDERS":
                return FileSection.SENDERS;
            case "URGENT-PREFIX":
                return FileSection.URGENT_PREFIX;
            default:
                Log.e(LOG_TAG, "Unknown section name: " + name);
                return FileSection.UNKNOWN;
        }
    }

    // Tries to parse a package config starting from the provided line.
    // Returns the line it stopped parsing on, or null if nothing was parsed.
    private String tryParsePackage(String line, BufferedReader reader) throws IOException {
        if (!line.startsWith("[") || !line.endsWith("]"))
            return null;

        PackageConfig config = new PackageConfig();
        config.vibe = new Vibe();
        config.name = line.substring(1, line.length() - 1);

        final String SENDER_KEY = ":sender=";
        final String SENDER_REGEX_KEY = ":sender-regex=";
        final String TEXT_KEY = ":text=";

        // Read out the config options
        while ((line = nextNonCommentLine(reader)) != null) {
            line = line.trim();
            if (line.startsWith(SENDER_KEY))
                config.senderExtra = line.substring(SENDER_KEY.length());
            else if (line.startsWith(SENDER_REGEX_KEY))
                config.senderRegex = line.substring(SENDER_REGEX_KEY.length());
            else if (line.startsWith(TEXT_KEY))
                config.textExtra = line.substring(TEXT_KEY.length());
            else
                break;
        }

        this.packages.put(config.name, config);
        return tryParseVibe(line, reader, config.vibe);
    }

    // Tries to parse a sender config starting from the provided line.
    // Returns the line it stopped parsing on, or null if nothing was parsed.
    private String tryParseSender(String line, BufferedReader reader) throws IOException {
        if (!line.startsWith("[") || !line.endsWith("]"))
            return null;

        SenderConfig config = new SenderConfig();
        config.vibe = new Vibe();
        config.name = line.substring(1, line.length() - 1);

        line = nextNonCommentLine(reader);
        this.senders.put(config.name, config);
        return tryParseVibe(line, reader, config.vibe);
    }

    // Tries to parse an urgent-prefix config starting from the provided line.
    // Returns the line it stopped parsing on, or null if nothing was parsed.
    private String tryParseUrgentPrefix(String line, BufferedReader reader) throws IOException {
        if (!line.startsWith("[") || !line.endsWith("]"))
            return null;

        urgentVibe = new Vibe();
        urgentRegex = line.substring(1, line.length() - 1);

        line = nextNonCommentLine(reader);
        return tryParseVibe(line, reader, urgentVibe);
    }

    // Tries to parse a vibe starting from the provided line.
    // Returns the line it stopped parsing on.
    private String tryParseVibe(String line, BufferedReader reader, Vibe vibe) throws IOException {
        Vibe.Location location = null;

        if (line == null)
            return null;

        do {
            boolean parsed = true;
            switch (line.toUpperCase()) {
                case "FRONT-LEFT":
                    location = Vibe.Location.FRONT_LEFT;
                    break;
                case "FRONT-RIGHT":
                    location = Vibe.Location.FRONT_RIGHT;
                    break;
                case "BACK-RIGHT":
                    location = Vibe.Location.BACK_RIGHT;
                    break;
                case "BACK-LEFT":
                    location = Vibe.Location.BACK_LEFT;
                    break;
                default:
                    if (location != null && line.length() > 0) {
                        String[] values = line.split(":");
                        if (values.length == 2) {
                            try {
                                float value = Float.parseFloat(values[0]);
                                short duration = Short.parseShort(values[1]);
                                vibe.at(location).add(value, duration);
                                break;
                            } catch (NumberFormatException e) {
                                // The log message below is probably enough in this case
                            }
                        }

                        Log.e(LOG_TAG, "line not understood: " + line);
                    }

                    parsed = false;
                    break;
            }

            if (!parsed)
                break;
        } while ((line = nextNonCommentLine(reader)) != null);

        return line;
    }

    // Returns the next non-comment line from the reader, trimmed for convenience.
    private String nextNonCommentLine(BufferedReader reader) throws IOException {
        String line;
        do {
            line = reader.readLine();
            if (line != null)
                line = line.trim();
        } while (line != null && line.startsWith("#"));

        return line;
    }
}
