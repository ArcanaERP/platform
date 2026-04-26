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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModulithDocumentationParityTest {

    private static final Path MODULE_ROOT = Path.of("src/main/java/com/arcanaerp/platform");
    private static final Path MODULE_MAP = Path.of("docs/modulith-module-map.md");
    private static final Path README = Path.of("README.md");
    private static final Path AGREEMENTS_DATA_MODEL = Path.of("docs/agreements-data-model.md");
    private static final Path COMMUNICATION_EVENTS_DATA_MODEL = Path.of("docs/communication-events-data-model.md");
    private static final Path INVENTORY_DATA_MODEL = Path.of("docs/inventory-data-model.md");
    private static final Path INVOICING_DATA_MODEL = Path.of("docs/invoicing-data-model.md");
    private static final Path PAYMENTS_DATA_MODEL = Path.of("docs/payments-data-model.md");
    private static final Path WORKEFFORT_DATA_MODEL = Path.of("docs/workeffort-data-model.md");
    private static final Path AGREEMENTS_CONTROLLER = Path.of(
        "src/main/java/com/arcanaerp/platform/agreements/web/AgreementsController.java"
    );
    private static final Path COMMUNICATION_EVENTS_CONTROLLER = Path.of(
        "src/main/java/com/arcanaerp/platform/communicationevents/web/CommunicationEventsController.java"
    );
    private static final Path COMMUNICATION_EVENT_REFERENCE_DATA_CONTROLLER = Path.of(
        "src/main/java/com/arcanaerp/platform/communicationevents/web/CommunicationEventReferenceDataController.java"
    );
    private static final Path INVENTORY_CONTROLLER = Path.of(
        "src/main/java/com/arcanaerp/platform/inventory/web/InventoryController.java"
    );
    private static final Path INVOICES_CONTROLLER = Path.of(
        "src/main/java/com/arcanaerp/platform/invoicing/web/InvoicesController.java"
    );
    private static final Path PAYMENTS_CONTROLLER = Path.of(
        "src/main/java/com/arcanaerp/platform/payments/web/PaymentsController.java"
    );
    private static final Path WORKEFFORT_CONTROLLER = Path.of(
        "src/main/java/com/arcanaerp/platform/workeffort/web/WorkEffortsController.java"
    );

    private static final Pattern ALLOWED_DEPENDENCIES_PATTERN = Pattern.compile("allowedDependencies = \\{([^}]*)\\}");
    private static final Pattern QUOTED_VALUE_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern CODE_VALUE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern REQUEST_MAPPING_PATTERN = Pattern.compile("@RequestMapping\\(\"([^\"]*)\"\\)");
    private static final Pattern HTTP_MAPPING_PATTERN = Pattern.compile("@(Get|Post|Patch)Mapping(?:\\(\"([^\"]*)\"\\))?");

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

    @Test
    void paymentsHttpSurfaceMatchesControllerMappings() throws IOException {
        assertHttpSurfaceMatchesControllerMappings(
            "Payments",
            "/api/payments",
            PAYMENTS_CONTROLLER,
            PAYMENTS_DATA_MODEL
        );
    }

    @Test
    void inventoryHttpSurfaceMatchesControllerMappings() throws IOException {
        assertHttpSurfaceMatchesControllerMappings(
            "Inventory",
            "/api/inventory",
            INVENTORY_CONTROLLER,
            INVENTORY_DATA_MODEL
        );
    }

    @Test
    void invoicingHttpSurfaceMatchesControllerMappings() throws IOException {
        assertHttpSurfaceMatchesControllerMappings(
            "Invoicing",
            "/api/invoices",
            INVOICES_CONTROLLER,
            INVOICING_DATA_MODEL
        );
    }

    @Test
    void agreementsHttpSurfaceMatchesControllerMappings() throws IOException {
        assertHttpSurfaceMatchesControllerMappings(
            "Agreements",
            "/api/agreements",
            AGREEMENTS_CONTROLLER,
            AGREEMENTS_DATA_MODEL
        );
    }

    @Test
    void workEffortHttpSurfaceMatchesControllerMappings() throws IOException {
        assertHttpSurfaceMatchesControllerMappings(
            "Work Effort",
            "/api/work-efforts",
            WORKEFFORT_CONTROLLER,
            WORKEFFORT_DATA_MODEL
        );
    }

    @Test
    void communicationEventsHttpSurfaceMatchesControllerMappings() throws IOException {
        assertHttpSurfaceMatchesControllerMappings(
            "Communication Events",
            "/api/communication-events",
            COMMUNICATION_EVENTS_DATA_MODEL,
            COMMUNICATION_EVENTS_CONTROLLER,
            COMMUNICATION_EVENT_REFERENCE_DATA_CONTROLLER
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

    private static Set<String> extractHttpMappings(String javaSource) {
        Matcher requestMapping = REQUEST_MAPPING_PATTERN.matcher(javaSource);
        assertTrue(requestMapping.find(), "Missing @RequestMapping base path in controller source");
        String basePath = requestMapping.group(1);

        LinkedHashSet<String> mappings = new LinkedHashSet<>();
        Matcher mappingMatcher = HTTP_MAPPING_PATTERN.matcher(javaSource);
        while (mappingMatcher.find()) {
            String method = mappingMatcher.group(1).toUpperCase();
            String relativePath = mappingMatcher.group(2) == null ? "" : mappingMatcher.group(2);
            mappings.add(method + " " + basePath + relativePath);
        }
        return mappings;
    }

    private static Set<String> extractModuleMapHttpSurface(String markdown, String moduleName) {
        String rowPrefix = "| " + moduleName + " |";
        String row = markdown.lines()
            .filter(line -> line.startsWith(rowPrefix))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing module map row: " + moduleName));

        String[] columns = row.split("\\|", -1);
        assertTrue(columns.length >= 6, () -> "Malformed module map row for module: " + moduleName);
        return CODE_VALUE_PATTERN.matcher(columns[5])
            .results()
            .map(match -> normalizeDocumentedMapping(match.group(1)))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> extractCodeMappings(String markdownFragment, String apiPrefix) {
        return CODE_VALUE_PATTERN.matcher(markdownFragment)
            .results()
            .map(match -> normalizeDocumentedMapping(match.group(1)))
            .filter(mapping -> mapping.startsWith("GET " + apiPrefix)
                || mapping.startsWith("POST " + apiPrefix)
                || mapping.startsWith("PATCH " + apiPrefix))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeDocumentedMapping(String mapping) {
        int querySeparator = mapping.indexOf('?');
        if (querySeparator >= 0) {
            mapping = mapping.substring(0, querySeparator);
        }
        int annotationSeparator = mapping.indexOf(" (");
        if (annotationSeparator >= 0) {
            mapping = mapping.substring(0, annotationSeparator);
        }
        return mapping.trim();
    }

    private static void assertHttpSurfaceMatchesControllerMappings(
        String moduleName,
        String apiPrefix,
        Path controllerPath,
        Path dataModelPath
    ) throws IOException {
        assertHttpSurfaceMatchesControllerMappings(moduleName, apiPrefix, dataModelPath, new Path[] {controllerPath});
    }

    private static void assertHttpSurfaceMatchesControllerMappings(
        String moduleName,
        String apiPrefix,
        Path dataModelPath,
        Path... controllerPaths
    ) throws IOException {
        Set<String> controllerMappings = new LinkedHashSet<>();
        for (Path controllerPath : controllerPaths) {
            controllerMappings.addAll(extractHttpMappings(Files.readString(controllerPath)));
        }
        Set<String> readmeMappings = extractBulletMappingsByLabel(Files.readString(README), moduleName + ":", apiPrefix);
        Set<String> dataModelMappings = extractBulletMappingsInSection(
            Files.readString(dataModelPath),
            "## Minimal HTTP Surface",
            "##",
            apiPrefix
        );
        Set<String> moduleMapMappings = extractModuleMapHttpSurface(Files.readString(MODULE_MAP), moduleName);
        String controllerNames = Stream.of(controllerPaths).map(path -> path.getFileName().toString()).collect(Collectors.joining(", "));

        assertEquals(
            controllerMappings,
            readmeMappings,
            "README " + moduleName.toLowerCase() + " HTTP surface is out of sync with "
                + controllerNames + " mappings"
        );
        assertEquals(
            controllerMappings,
            dataModelMappings,
            dataModelPath + " minimal HTTP surface is out of sync with " + controllerNames + " mappings"
        );
        assertEquals(
            controllerMappings,
            moduleMapMappings,
            "docs/modulith-module-map.md " + moduleName.toLowerCase()
                + " HTTP surface is out of sync with " + controllerNames + " mappings"
        );
    }

    private static Set<String> extractBulletMappingsByLabel(String markdown, String label, String apiPrefix) {
        int start = markdown.indexOf(label);
        assertTrue(start >= 0, () -> "Missing markdown label: " + label);
        String afterLabel = markdown.substring(start + label.length());
        Matcher nextLabelMatcher = Pattern.compile("\\n[A-Z][A-Za-z /-]+:\\n").matcher(afterLabel);
        int end = nextLabelMatcher.find() ? start + label.length() + nextLabelMatcher.start() : markdown.length();
        return extractCodeMappings(markdown.substring(start, end), apiPrefix);
    }

    private static Set<String> extractBulletMappingsInSection(
        String markdown,
        String startMarker,
        String endMarkerPrefix,
        String apiPrefix
    ) {
        int start = markdown.indexOf(startMarker);
        assertTrue(start >= 0, () -> "Missing markdown section: " + startMarker);
        int end = markdown.indexOf("\n" + endMarkerPrefix, start + startMarker.length());
        if (end < 0) {
            end = markdown.length();
        }
        return extractCodeMappings(markdown.substring(start, end), apiPrefix);
    }

    private static String extractSection(String markdown, String startMarker, String endMarker) {
        int start = markdown.indexOf(startMarker);
        int end = markdown.indexOf(endMarker);
        assertTrue(start >= 0, () -> "Missing markdown section: " + startMarker);
        assertTrue(end >= 0, () -> "Missing markdown section: " + endMarker);
        return markdown.substring(start, end);
    }
}
