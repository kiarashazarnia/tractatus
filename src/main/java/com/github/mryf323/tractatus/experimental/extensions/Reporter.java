package com.github.mryf323.tractatus.experimental.extensions;

import com.github.mryf323.tractatus.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public enum Reporter {

    INSTANCE;

    private final ClassLoaderTemplateResolver templateResolver;
    private final TemplateEngine templateEngine;

    Reporter() {
        templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

    public static Reporter getInstance() {
        return INSTANCE;
    }

    public String format(List<ReportableTR> reportableTRs, List<String> clauseDefinitions) {
        Context context = new Context();
        context.setVariable("clause_def_list", clauseDefinitions);
        context.setVariable("reportable_list", reportableTRs);
        return templateEngine.process("template", context);
    }

    public void save(String path, String string) throws Exception {
        File file = new File(path);
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(string);
        out.close();
    }

    public String title(Annotation annotation) {
        return annotation.annotationType().getSimpleName();
    }

    private ReportableTR toReportableTR(CACC annotation) {
        return new ReportableTR(
                title(annotation),
                annotation.predicate(),
                Arrays.asList(
                        "Predicate Value: " + annotation.predicateValue(),
                        "Major Clause: " + annotation.majorClause()
                ),
                toList(annotation.valuations())
        );
    }

    private List<String> toList(Valuation[] valuations) {
        return Arrays.stream(valuations)
                .map(valuation -> String.format("%c = %b", valuation.clause(), valuation.valuation()))
                .collect(Collectors.toList());
    }

    private ReportableTR toReportableTR(ClauseCoverage annotation) {
        return new ReportableTR(
                title(annotation),
                annotation.predicate(),
                Collections.emptyList(),
                toList(annotation.valuations())
        );
    }

    public String formatClauseDef(ClauseDefinition annotation) {
        return String.format(
                "%c := %s",
                annotation.clause(),
                annotation.def()
        );
    }

    private ReportableTR toReportableTR(NearFalsePoint annotation) {
        return new ReportableTR(
                title(annotation),
                annotation.predicate(),
                Arrays.asList(
                        "DNF: " + annotation.dnf(),
                        "Implicant: " + annotation.implicant(),
                        "Clause: " + annotation.clause()
                ),
                toList(annotation.valuations())
        );
    }

    private ReportableTR toReportableTR(UniqueTruePoint annotation) {
        return new ReportableTR(
                title(annotation),
                annotation.predicate(),
                Arrays.asList(
                        "DNF: " + annotation.dnf(),
                        "Implicant: " + annotation.implicant()
                ),
                toList(annotation.valuations())
        );
    }

    public List<ReportableTR> toReportableTestRequirement(Annotation annotation) {
        if(isATestRequirementContainer(annotation))
            return containerAnnotationToReportableTestRequirement(annotation);
        else
            return Arrays.asList(singleAnnotationToReportableTestRequirement(annotation));
    }

    List<ReportableTR> containerAnnotationToReportableTestRequirement(Annotation annotation) {
        if(annotation instanceof UniqueTruePointContainer) {
            return Arrays.stream(((UniqueTruePointContainer) annotation).value())
                    .map(this::singleAnnotationToReportableTestRequirement)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        if(annotation instanceof NearFalsePointContainer) {
            return Arrays.stream(((NearFalsePointContainer) annotation).value())
                    .map(this::singleAnnotationToReportableTestRequirement)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        if(annotation instanceof ClauseCoverageContainer) {
            return Arrays.stream(((ClauseCoverageContainer) annotation).value())
                    .map(this::singleAnnotationToReportableTestRequirement)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        if(annotation instanceof CaccContainer) {
            return Arrays.stream(((CaccContainer) annotation).value())
                    .map(this::singleAnnotationToReportableTestRequirement)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return new ArrayList<ReportableTR>();
    }

    ReportableTR singleAnnotationToReportableTestRequirement(Annotation annotation) {
        if(annotation instanceof CACC) {
            return toReportableTR((CACC) annotation);
        }
        if(annotation instanceof ClauseCoverage) {
            return toReportableTR((ClauseCoverage) annotation);
        }
        if(annotation instanceof NearFalsePoint) {
            return toReportableTR((NearFalsePoint) annotation);
        }
        if(annotation instanceof UniqueTruePoint) {
            return toReportableTR((UniqueTruePoint) annotation);
        }
        return null;
    }

    public static class ReportableTR {
        public final String title;
        public final String predicate;
        public final List<String> explanations;
        public final List<String> valuations;

        public ReportableTR(String title, String predicate, List<String> explanations, List<String> valuations) {
            this.title = title;
            this.predicate = predicate;
            this.explanations = explanations;
            this.valuations = valuations;
        }
    }

    public static class ClauseDefinitionReport {
        final String clause;
        final String definition;


        private ClauseDefinitionReport(String clause, String definition) {
            this.clause = clause;
            this.definition = definition;
        }
    }

    public boolean isATestRequirementContainer(Annotation annotation) {
        return annotation instanceof UniqueTruePointContainer ||
                annotation instanceof NearFalsePointContainer ||
                annotation instanceof ClauseCoverageContainer ||
                annotation instanceof CaccContainer;
    }
}
