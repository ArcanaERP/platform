package com.arcanaerp.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModulithDocumentationParityTest {

    private static final Path MODULE_ROOT = Path.of("src/main/java/com/arcanaerp/platform");
    private static final Path MODULE_MAP = Path.of("docs/modulith-module-map.md");

    private static final Pattern ALLOWED_DEPENDENCIES_PATTERN = Pattern.compile("allowedDependencies = \\{([^}]*)\\}");
    private static final Pattern QUOTED_VALUE_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern CODE_VALUE_PATTERN = Pattern.compile("`([^`]+)`");

    @Test
    void scopeListMatchesDeclaredModules() throws IOException {
        List<String> documentedModules = extractScopeModules(Files.readString(MODULE_MAP));
        List<String> declaredModules = declaredModules();

        assertEquals(
            new LinkedHashSet<>(declaredModules),
            new LinkedHashSet<>(documentedModules),
            "docs/modulith-module-map.md scope list is out of sync with declared modules"
        );
    }

    @Test
    void dependencyGraphMatchesAllowedDependencies() throws IOException {
        Map<String, List<String>> documentedDependencies = extractDependencyGraph(Files.readString(MODULE_MAP));
        Map<String, List<String>> declaredDependencies = declaredDependencies();

        assertEquals(
            declaredDependencies,
            documentedDependencies,
            "docs/modulith-module-map.md dependency graph is out of sync with package-info.java declarations"
        );
    }

    private static List<String> declaredModules() throws IOException {
        try (Stream<Path> paths = Files.list(MODULE_ROOT)) {
            return paths
                .filter(Files::isDirectory)
                .filter(path -> Files.exists(path.resolve("package-info.java")))
                .map(path -> path.getFileName().toString())
                .sorted()
                .toList();
        }
    }

    private static Map<String, List<String>> declaredDependencies() throws IOException {
        Map<String, List<String>> dependencies = new LinkedHashMap<>();
        for (String module : declaredModules()) {
            List<String> allowedDependencies = extractAllowedDependencies(MODULE_ROOT.resolve(module).resolve("package-info.java"));
            if (!allowedDependencies.isEmpty()) {
                dependencies.put(module, allowedDependencies);
            }
        }
        return dependencies;
    }

    private static List<String> extractAllowedDependencies(Path packageInfo) throws IOException {
        String content = Files.readString(packageInfo);
        Matcher matcher = ALLOWED_DEPENDENCIES_PATTERN.matcher(content);
        if (!matcher.find()) {
            return List.of();
        }

        Matcher quotedValues = QUOTED_VALUE_PATTERN.matcher(matcher.group(1));
        List<String> dependencies = new ArrayList<>();
        while (quotedValues.find()) {
            dependencies.add(quotedValues.group(1));
        }
        return dependencies;
    }

    private static List<String> extractScopeModules(String markdown) {
        String section = extractSection(markdown, "## Scope", "## Dependency Graph");
        return section.lines()
            .map(String::trim)
            .filter(line -> line.startsWith("- `") && line.endsWith("`"))
            .map(line -> line.substring(3, line.length() - 1))
            .sorted()
            .toList();
    }

    private static Map<String, List<String>> extractDependencyGraph(String markdown) {
        String section = extractSection(markdown, "## Dependency Graph", "## Module Boundaries");
        return section.lines()
            .map(String::trim)
            .filter(line -> line.startsWith("- `") && line.contains(" -> "))
            .collect(Collectors.toMap(
                ModulithDocumentationParityTest::extractDependencyModule,
                ModulithDocumentationParityTest::extractDependencyTargets,
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private static String extractDependencyModule(String line) {
        int separator = line.indexOf("` -> ");
        return line.substring(3, separator);
    }

    private static List<String> extractDependencyTargets(String line) {
        int separator = line.indexOf("-> ");
        String dependencyList = line.substring(separator + 3).trim();
        return CODE_VALUE_PATTERN.matcher(dependencyList)
            .results()
            .map(match -> match.group(1))
            .toList();
    }

    private static String extractSection(String markdown, String startMarker, String endMarker) {
        int start = markdown.indexOf(startMarker);
        int end = markdown.indexOf(endMarker);
        assertTrue(start >= 0, () -> "Missing markdown section: " + startMarker);
        assertTrue(end >= 0, () -> "Missing markdown section: " + endMarker);
        return markdown.substring(start, end);
    }
}
