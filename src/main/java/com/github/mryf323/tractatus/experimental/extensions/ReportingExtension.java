package com.github.mryf323.tractatus.experimental.extensions;

import com.github.mryf323.tractatus.ClauseDefinition;
import com.github.mryf323.tractatus.ClauseDefinitionContainer;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

public class ReportingExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final Reporter reporter = Reporter.getInstance();
    private static final Logger log = LoggerFactory.getLogger(ReportingExtension.class);
    // todo check if this path is problematic in non-unix os
    private static final String REPORT_PATH = "./tractatus/";
    private final List<Reporter.ReportableTR> reportableTRs = new ArrayList<>();
    private final List<String> clauseDefinitions = new ArrayList<>();

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        try {
            String report = reporter.format(reportableTRs, clauseDefinitions);
            String path = getPath(context);
            reporter.save(path, report);
        } catch (Exception e) {
            log.error("after all error", e);
        }
    }

    private String getPath(ExtensionContext context) {
        String reportId = context.getRequiredTestClass().getName();
        String path = REPORT_PATH + reportId + ".html";
        return path;
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        try {
            AnnotatedElement element = context.getElement().get();
            for (Annotation annotation : element.getAnnotations()) {
                List<Reporter.ReportableTR> addingTRs = reporter.toReportableTestRequirement(annotation);
                addingTRs.stream()
                        .filter(Objects::nonNull)
                        .forEach(this.reportableTRs::add);
            }
        } catch (Exception e) {
            log.error("after each error", e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        try {
            makeReportPath();
            Optional<AnnotatedElement> element = context.getElement();
            for (Annotation annotation : context.getElement().get().getAnnotations()) {
                if (annotation instanceof ClauseDefinition) {
                    clauseDefinitions.add(reporter.formatClauseDef((ClauseDefinition) annotation));
                }
                if (annotation instanceof ClauseDefinitionContainer) {
                    Arrays.stream(((ClauseDefinitionContainer) annotation).value())
                            .map(reporter::formatClauseDef)
                            .forEach(clauseDefinitions::add);
                }
            }
        } catch (Exception e) {
            log.error("before all error", e);
        }
    }

    private void makeReportPath() {
        File reportDir = new File(REPORT_PATH);
        if(!reportDir.exists()) {
            reportDir.mkdir();
        }
        assert reportDir.exists();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {

    }
}
