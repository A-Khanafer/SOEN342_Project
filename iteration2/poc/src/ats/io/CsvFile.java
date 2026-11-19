package ats.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Small CSV reader/writer: UTF-8, quoted commas/newlines, doubled quotes, CRLF or LF. */
final class CsvFile {
    private CsvFile() { }

    static List<Map<String, String>> read(Path path, String[] required, String... optional)
            throws IOException {
        if (path == null || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IOException("CSV source must be a readable regular file: " + path);
        }
        String input = Files.readString(path, StandardCharsets.UTF_8);
        if (input.startsWith("\uFEFF")) input = input.substring(1);
        List<List<String>> records = parse(input);
        if (records.isEmpty()) throw new IOException("CSV file is empty: " + path);
        List<String> header = records.remove(0);
        Set<String> allowed = new HashSet<>(Arrays.asList(required));
        allowed.addAll(Arrays.asList(optional));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < header.size(); i++) {
            String name = header.get(i).trim();
            if (!allowed.contains(name) || !seen.add(name)) {
                throw new IOException("Unknown or duplicate CSV header: '" + name + "'");
            }
            header.set(i, name);
        }
        for (String name : required) {
            if (!seen.contains(name)) throw new IOException("Missing CSV header: " + name);
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            List<String> values = records.get(i);
            if (values.size() != header.size()) {
                throw new IOException("CSV record " + (i + 2) + " has " + values.size()
                        + " fields; expected " + header.size());
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < header.size(); j++) row.put(header.get(j), values.get(j).trim());
            rows.add(row);
        }
        return rows;
    }

    private static List<List<String>> parse(String input) throws IOException {
        List<List<String>> records = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closed = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < input.length() && input.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closed = true;
                    }
                } else field.append(c);
            } else if (c == ',' || c == '\n' || c == '\r') {
                row.add(field.toString());
                field.setLength(0);
                closed = false;
                if (c != ',') {
                    if (!(row.size() == 1 && row.get(0).isBlank())) records.add(row);
                    row = new ArrayList<>();
                    if (c == '\r' && i + 1 < input.length() && input.charAt(i + 1) == '\n') i++;
                }
            } else if (c == '"' && field.length() == 0 && !closed) {
                quoted = true;
            } else {
                if (closed || c == '"') throw new IOException("Malformed CSV quoting near character " + (i + 1));
                field.append(c);
            }
        }
        if (quoted) throw new IOException("Unterminated quoted CSV field");
        if (closed || field.length() != 0 || !row.isEmpty()) {
            row.add(field.toString());
            records.add(row);
        }
        return records;
    }

    static String record(List<String> values) {
        List<String> escaped = new ArrayList<>();
        for (String value : values) {
            if (value == null) value = "";
            if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
                value = '"' + value.replace("\"", "\"\"") + '"';
            }
            escaped.add(value);
        }
        return String.join(",", escaped) + "\r\n";
    }
}
