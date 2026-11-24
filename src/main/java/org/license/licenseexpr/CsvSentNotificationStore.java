package org.license.licenseexpr;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Remembers sent notifications in a local CSV file (PLAN §4). A missing file means nothing was sent yet.
 * Each {@link #recordSent} appends one line immediately, so a run that fails partway keeps what it already sent.
 */
public class CsvSentNotificationStore implements SentNotificationStore {

    static final String HEADER = "license_id,expiration_date,status";

    private final Path file;

    public CsvSentNotificationStore(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    @Override
    public Set<NotificationKey> loadSent() {
        if (Files.notExists(file)) {
            return Set.of();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read sent-notifications file " + file, e);
        }

        Set<NotificationKey> sent = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i);
            if (lineNumber == 1) {
                if (!line.equals(HEADER)) {
                    throw malformed(lineNumber, "expected header '" + HEADER + "'");
                }
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            sent.add(parse(line, lineNumber));
        }
        return sent;
    }

    @Override
    public void recordSent(NotificationKey key) {
        String line = key.licenseId() + "," + key.expirationDate() + "," + key.status().label() + "\n";
        try {
            boolean needsHeader = Files.notExists(file) || Files.size(file) == 0;
            Files.writeString(file, needsHeader ? HEADER + "\n" + line : line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not record sent notification in " + file, e);
        }
    }

    private NotificationKey parse(String line, int lineNumber) {
        String[] fields = line.split(",", -1);
        if (fields.length != 3) {
            throw malformed(lineNumber, "expected 3 fields but found " + fields.length);
        }
        try {
            return new NotificationKey(fields[0], LocalDate.parse(fields[1]), WindowStatus.fromLabel(fields[2]));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw malformed(lineNumber, e.getMessage());
        }
    }

    private IllegalStateException malformed(int lineNumber, String reason) {
        return new IllegalStateException(
                "Malformed sent-notifications file " + file + " at line " + lineNumber + ": " + reason);
    }
}
