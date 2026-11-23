package com.fairfold.assessment;

import com.fairfold.assessment.dto.QuestionOption;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Local-only demo content: three assessments with ten multiple-choice
 * questions each and fifteen learning resources. Real content is created by
 * the admin; this runs only when no assessment exists yet.
 */
@Component
@Profile("local")
public class DemoContentSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoContentSeeder.class);

    private record DemoQuestion(String text, List<String> options, int correctIndex) {
    }

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final LearningResourceRepository resourceRepository;
    private final SkillRepository skillRepository;

    public DemoContentSeeder(AssessmentRepository assessmentRepository,
                             AssessmentQuestionRepository questionRepository,
                             LearningResourceRepository resourceRepository,
                             SkillRepository skillRepository) {
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.resourceRepository = resourceRepository;
        this.skillRepository = skillRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (assessmentRepository.count() > 0) {
            return;
        }
        Skill javaScript = skill("JavaScript");
        Skill sql = skill("SQL");
        Skill python = skill("Python");
        if (javaScript == null || sql == null || python == null) {
            return;
        }

        seedAssessment(javaScript, "JavaScript Fundamentals", 30, 70, javaScriptQuestions());
        seedAssessment(sql, "SQL Essentials", 25, 60, sqlQuestions());
        seedAssessment(python, "Python Basics", 30, 70, pythonQuestions());

        seedResources(javaScript, List.of(
                new DemoResource("JavaScript Guide", "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide", "free", "English", "beginner"),
                new DemoResource("Eloquent JavaScript", "https://eloquentjavascript.net/", "free", "English", "intermediate"),
                new DemoResource("You Don't Know JS Yet", "https://github.com/getify/You-Dont-Know-JS", "free", "English", "advanced"),
                new DemoResource("JavaScript.info", "https://javascript.info/", "free", "English", "beginner"),
                new DemoResource("Frontend Masters JavaScript Path", "https://frontendmasters.com/learn/javascript/", "paid", "English", "advanced")));
        seedResources(sql, List.of(
                new DemoResource("SQLBolt", "https://sqlbolt.com/", "free", "English", "beginner"),
                new DemoResource("Mode SQL Tutorial", "https://mode.com/sql-tutorial/", "free", "English", "intermediate"),
                new DemoResource("Use The Index, Luke", "https://use-the-index-luke.com/", "free", "English", "advanced"),
                new DemoResource("PostgreSQL Exercises", "https://pgexercises.com/", "free", "English", "intermediate"),
                new DemoResource("SQL Performance Explained", "https://sql-performance-explained.com/", "paid", "English", "advanced")));
        seedResources(python, List.of(
                new DemoResource("Python Tutorial", "https://docs.python.org/3/tutorial/", "free", "English", "beginner"),
                new DemoResource("Automate the Boring Stuff", "https://automatetheboringstuff.com/", "free", "English", "beginner"),
                new DemoResource("Real Python", "https://realpython.com/", "free", "English", "intermediate"),
                new DemoResource("Fluent Python", "https://www.oreilly.com/library/view/fluent-python-2nd/9781492056348/", "paid", "English", "advanced"),
                new DemoResource("Python Practice Problems", "https://www.practicepython.org/", "free", "English", "intermediate")));

        log.info("Seeded demo assessments and resources");
    }

    private void seedAssessment(Skill skill, String title, int durationMinutes, int passMark,
                                List<DemoQuestion> questions) {
        Assessment assessment = new Assessment();
        assessment.setTitle(title);
        assessment.setSkillId(skill.getId());
        assessment.setDurationMinutes(durationMinutes);
        assessment.setPassMark(passMark);
        assessment.setActive(true);
        assessmentRepository.save(assessment);

        List<AssessmentQuestion> rows = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            DemoQuestion demo = questions.get(i);
            List<QuestionOption> options = new ArrayList<>();
            for (int o = 0; o < demo.options().size(); o++) {
                options.add(new QuestionOption(String.valueOf((char) ('a' + o)), demo.options().get(o)));
            }
            AssessmentQuestion row = new AssessmentQuestion();
            row.setAssessmentId(assessment.getId());
            row.setPosition(i);
            row.setText(demo.text());
            row.setOptions(options);
            row.setCorrectOptionId(String.valueOf((char) ('a' + demo.correctIndex())));
            rows.add(row);
        }
        questionRepository.saveAll(rows);
    }

    private record DemoResource(String title, String url, String cost, String language, String level) {
    }

    private void seedResources(Skill skill, List<DemoResource> resources) {
        for (DemoResource demo : resources) {
            LearningResource resource = new LearningResource();
            resource.setSkillId(skill.getId());
            resource.setTitle(demo.title());
            resource.setUrl(demo.url());
            resource.setCost(demo.cost());
            resource.setLanguage(demo.language());
            resource.setLevel(demo.level());
            resource.setActive(true);
            resourceRepository.save(resource);
        }
    }

    private Skill skill(String name) {
        return skillRepository.findByNameIgnoreCase(name).orElse(null);
    }

    private static List<DemoQuestion> javaScriptQuestions() {
        return List.of(
                q("Which keyword declares a block-scoped variable?",
                        List.of("var", "let", "function", "with"), 1),
                q("What does `===` compare?",
                        List.of("Value only", "Type only", "Value and type", "Object identity"), 2),
                q("Which method adds an element to the end of an array?",
                        List.of("push()", "pop()", "shift()", "slice()"), 0),
                q("What is `typeof null` in JavaScript?",
                        List.of("'null'", "'object'", "'undefined'", "'boolean'"), 1),
                q("Which function creates a new array by transforming every element?",
                        List.of("forEach()", "filter()", "map()", "reduce()"), 2),
                q("What does a Promise represent?",
                        List.of("A synchronous value", "An eventual result", "A DOM node", "A CSS animation"), 1),
                q("Which operator returns the left value when it is not null or undefined?",
                        List.of("&&", "||", "??", "?."), 2),
                q("What is a closure?",
                        List.of("A loop that never ends",
                                "A function with access to its outer scope",
                                "A JSON object",
                                "A browser event"), 1),
                q("Which method removes the last element of an array?",
                        List.of("shift()", "pop()", "splice(0, 1)", "unshift()"), 1),
                q("What does `Array.prototype.filter` return?",
                        List.of("A single value", "A boolean", "A new array", "The original array"), 2));
    }

    private static List<DemoQuestion> sqlQuestions() {
        return List.of(
                q("Which clause filters rows before grouping?",
                        List.of("HAVING", "WHERE", "ORDER BY", "LIMIT"), 1),
                q("Which join returns only matching rows from both tables?",
                        List.of("LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "FULL JOIN"), 2),
                q("Which aggregate counts non-null values?",
                        List.of("SUM", "COUNT", "AVG", "MAX"), 1),
                q("What does `GROUP BY` do?",
                        List.of("Sorts rows", "Combines rows into groups", "Joins tables", "Deletes duplicates"), 1),
                q("Which clause filters groups after aggregation?",
                        List.of("WHERE", "HAVING", "ON", "USING"), 1),
                q("Which keyword removes duplicate rows from a result?",
                        List.of("UNIQUE", "DISTINCT", "ONLY", "SEPARATE"), 1),
                q("Which statement changes existing rows?",
                        List.of("INSERT", "UPDATE", "ALTER", "CREATE"), 1),
                q("What is a primary key?",
                        List.of("Any indexed column", "A unique identifier for a row", "A foreign table", "A view"), 1),
                q("Which clause limits the number of returned rows?",
                        List.of("TOP", "LIMIT", "SAMPLE", "COUNT"), 1),
                q("What does `NULL` mean in SQL?",
                        List.of("Zero", "An empty string", "An unknown value", "False"), 2));
    }

    private static List<DemoQuestion> pythonQuestions() {
        return List.of(
                q("Which keyword defines a function in Python?",
                        List.of("func", "def", "function", "lambda"), 1),
                q("What is the result of `len([1, 2, 3])`?",
                        List.of("2", "3", "4", "Error"), 1),
                q("Which type is immutable?",
                        List.of("list", "dict", "set", "tuple"), 3),
                q("Which statement handles exceptions?",
                        List.of("try/except", "catch/throw", "if/else", "with/as"), 0),
                q("What does `range(3)` produce?",
                        List.of("[1, 2, 3]", "0, 1, 2", "0, 1, 2, 3", "1, 2"), 1),
                q("Which operator checks identity?",
                        List.of("==", "is", "in", "="), 1),
                q("What is a dict comprehension?",
                        List.of("A loop", "A compact way to build a dictionary", "A module", "A class"), 1),
                q("Which method adds an item to a list?",
                        List.of("add()", "append()", "insertLast()", "push()"), 1),
                q("What does `__init__` do?",
                        List.of("Deletes an object", "Initializes a new object", "Imports a module", "Prints a value"), 1),
                q("Which built-in opens a file safely with automatic closing?",
                        List.of("open()", "file()", "with open() as f", "read()"), 2));
    }

    private static DemoQuestion q(String text, List<String> options, int correctIndex) {
        return new DemoQuestion(text, options, correctIndex);
    }
}
