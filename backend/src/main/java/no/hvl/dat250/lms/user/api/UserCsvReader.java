package no.hvl.dat250.lms.user.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import no.hvl.dat250.lms.user.service.UserImportRow;

/**
 * Reads the user list for an import from a CSV file.
 *
 * <p>Format: UTF-8 (a byte order mark, as Excel writes it, is ignored), a header row with
 * the columns {@code email}, {@code firstName} and {@code lastName} and optionally
 * {@code access}, in any order and case. Fields are separated by commas, or by semicolons
 * (what Excel writes with Norwegian settings), detected from the header. Fields may be
 * quoted with {@code "}; a quote inside a quoted field is written {@code ""}. Empty lines
 * are skipped; other columns are ignored.
 *
 * <p>Only the file's structure is checked here. Whether a row is a valid user is decided
 * by the service, per row, so one bad row does not stop the import.
 */
public final class UserCsvReader {
    /** Upper bound on data rows per file. */
    public static final int MAX_ROWS = 5000;

    private UserCsvReader() {}

    public static List<UserImportRow> read(InputStream in) throws IOException {
        String text = decodeUtf8(in.readAllBytes());
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        String[] lines = text.split("\r\n|\n|\r", -1);
        if (lines.length == 0 || lines[0].isBlank()) {
            throw new InvalidUserCsvException("The file is empty; expected a header row: email,firstName,lastName,access");
        }

        String header = lines[0];
        char delimiter = header.indexOf(';') >= 0 && header.indexOf(',') < 0 ? ';' : ',';
        Map<String, Integer> columns = new HashMap<>();
        List<String> names = split(header, delimiter, 1);
        for (int i = 0; i < names.size(); i++) {
            columns.putIfAbsent(names.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        for (String required : List.of("email", "firstname", "lastname")) {
            if (!columns.containsKey(required)) {
                throw new InvalidUserCsvException("Missing column '" + required
                        + "' in the header; expected: email,firstName,lastName,access");
            }
        }

        List<UserImportRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            if (rows.size() == MAX_ROWS) {
                throw new InvalidUserCsvException("Too many rows; at most " + MAX_ROWS + " users per file");
            }
            int line = i + 1;
            List<String> fields = split(lines[i], delimiter, line);
            rows.add(new UserImportRow(line,
                    field(fields, columns.get("email")),
                    field(fields, columns.get("firstname")),
                    field(fields, columns.get("lastname")),
                    field(fields, columns.get("access"))));
        }
        return rows;
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException ex) {
            throw new InvalidUserCsvException("The file is not UTF-8; save it as \"CSV UTF-8\"");
        }
    }

    /** The trimmed field, or "" for a missing column or field. */
    private static String field(List<String> fields, Integer index) {
        if (index == null || index >= fields.size()) {
            return "";
        }
        return fields.get(index).trim();
    }

    private static List<String> split(String line, char delimiter, int lineNumber) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"' && current.toString().isBlank()) {
                quoted = true;
                current.setLength(0);
            } else if (c == delimiter) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (quoted) {
            throw new InvalidUserCsvException("Line " + lineNumber + ": a quoted field is not closed");
        }
        fields.add(current.toString());
        return fields;
    }
}
