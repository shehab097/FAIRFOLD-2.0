#!/usr/bin/env python3
"""Emits SQL for the Part 2.3 demo: one company, one job, 10 candidates.

The shell wrapper (scripts/seed-demo.sh) pipes this into MariaDB. Resumes are
inserted as already-processed ready rows so the first Screen All click works
immediately; if dummy-data CVs exist the wrapper also copies them into storage.
"""
import uuid

PASSWORD_HASH = "$2b$10$P/ZnhIpCBA0Yxco3IU5zYez9p1C5wXYomuSB5HBqnzah6rfQBd2Zi"
COMPANY = "30000000-0000-0000-0000-000000000001"
HR = "31000000-0000-0000-0000-000000000001"
JOB = "33000000-0000-0000-0000-000000000001"
NS = uuid.UUID("6f9619ff-8b86-d011-b42d-00c04fc964ff")


def uid(value):
    return str(uuid.uuid5(NS, value))


def q(value):
    return "'" + str(value).replace("'", "''") + "'"


CANDIDATES = [
    {
        "first": "Ayesha", "last": "Rahman", "years": 6, "location": "Dhaka, Bangladesh",
        "skills": [("Java", "expert", 6), ("Spring Boot", "expert", 5), ("PostgreSQL", "expert", 5),
                   ("Docker", "intermediate", 3), ("AWS", "intermediate", 3)],
        "text": ("Built Java and Spring Boot services with PostgreSQL and Docker on AWS. "
                 "Led migrations and improved API latency for a payments platform. "
                 "Reviewed code and mentored two engineers."),
    },
    {
        "first": "Rohan", "last": "Das", "years": 4, "location": "Kolkata, India",
        "skills": [("Java", "intermediate", 4), ("Spring Boot", "intermediate", 3),
                   ("MySQL", "intermediate", 3), ("Docker", "beginner", 1)],
        "text": ("Delivered Java and Spring Boot services backed by MySQL. "
                 "Containerised applications with Docker and wrote integration tests. "
                 "Worked closely with product and QA."),
    },
    {
        "first": "Mitu", "last": "Sultana", "years": 3, "location": "Dhaka, Bangladesh",
        "skills": [("Java", "intermediate", 3), ("Spring Boot", "intermediate", 2),
                   ("PostgreSQL", "beginner", 1)],
        "text": ("She led the Java and Spring Boot team for a billing service. "
                 "She designed PostgreSQL schemas and reviewed pull requests. "
                 "Introduced automated tests for the checkout flow."),
    },
    {
        "first": "Farhan", "last": "Ahmed", "years": 5, "location": "Chattogram, Bangladesh",
        "skills": [("JavaScript", "expert", 5), ("React", "expert", 4), ("Node.js", "intermediate", 3)],
        "text": ("Built React and Node.js applications for a large marketplace. "
                 "Owned the component library and improved accessibility. "
                 "Shipped features with TypeScript and automated end-to-end tests."),
    },
    {
        "first": "Sadia", "last": "Islam", "years": 7, "location": "Dhaka, Bangladesh",
        "skills": [("Java", "expert", 7), ("Spring Boot", "expert", 6), ("AWS", "expert", 4),
                   ("Docker", "expert", 4), ("PostgreSQL", "expert", 5)],
        "text": ("Architected Java and Spring Boot platforms on AWS with Docker and PostgreSQL. "
                 "Designed event-driven services processing millions of requests daily. "
                 "Mentored a team and owned reliability reviews."),
    },
    {
        "first": "Tanvir", "last": "Hasan", "years": 2, "location": "Sylhet, Bangladesh",
        "skills": [("Python", "intermediate", 2), ("Django", "intermediate", 2),
                   ("PostgreSQL", "beginner", 1)],
        "text": ("Developed Python and Django services with PostgreSQL. "
                 "Automated reporting jobs and built internal dashboards. "
                 "Collaborated with operations on data quality."),
    },
    {
        "first": "Nabila", "last": "Akter", "years": 2.5, "location": "Dhaka, Bangladesh",
        "skills": [("Java", "intermediate", 2.5), ("REST API", "intermediate", 2),
                   ("Docker", "beginner", 1)],
        "text": ("Implemented Java REST API endpoints and containerised them with Docker. "
                 "Wrote unit tests and fixed production defects. "
                 "Documented API contracts for partner teams."),
    },
    {
        "first": "Imran", "last": "Kabir", "years": 1, "location": "Rajshahi, Bangladesh",
        "skills": [("Java", "beginner", 1), ("Spring Boot", "beginner", 1),
                   ("PostgreSQL", "beginner", 1), ("AWS", "beginner", 1)],
        "text": ("Used Java and Spring Boot for a small internal service. "
                 "Trained on PostgreSQL and AWS fundamentals. "
                 "Completed a mentored onboarding programme."),
    },
    {
        "first": "Priya", "last": "Dey", "years": 4, "location": "Khulna, Bangladesh",
        "skills": [("Java", "intermediate", 4), ("Spring Boot", "intermediate", 3),
                   ("PostgreSQL", "intermediate", 3), ("REST API", "intermediate", 3)],
        "text": ("Developed Java and Spring Boot services backed by PostgreSQL. "
                 "Designed REST API contracts and added validation. "
                 "Partnered with frontend engineers on release planning."),
    },
    {
        "first": "Sabbir", "last": "Alam", "years": 3, "location": "Barishal, Bangladesh",
        "skills": [("Java", "intermediate", 3), ("Docker", "intermediate", 2),
                   ("AWS", "beginner", 1), ("REST API", "intermediate", 2)],
        "text": ("Built Java REST API services and Docker images for deployments. "
                 "Ran releases on AWS and monitored application health. "
                 "Improved logging and alerting across services."),
    },
]

lines = []
lines.append("-- FairFold Part 2.3 demo seed: one company, one job, ten candidates")
lines.append("-- Login: demo.hr@fairfold.dev / Password123!")
lines.append("SET FOREIGN_KEY_CHECKS = 1;")
lines.append("DELETE FROM applications WHERE job_id = " + q(JOB) + " OR candidate_id IN ("
             + ", ".join(q(uid(f"candidate-{i}")) for i in range(1, 11)) + ");")
lines.append(f"DELETE FROM jobs WHERE id = {q(JOB)};")
lines.append("DELETE FROM resumes WHERE user_id IN ("
             + ", ".join(q(uid(f"candidate-{i}")) for i in range(1, 11)) + ");")
lines.append("DELETE FROM candidate_skills WHERE user_id IN ("
             + ", ".join(q(uid(f"candidate-{i}")) for i in range(1, 11)) + ");")
lines.append("DELETE FROM candidate_profiles WHERE user_id IN ("
             + ", ".join(q(uid(f"candidate-{i}")) for i in range(1, 11)) + ");")
lines.append(f"DELETE FROM users WHERE id = {q(HR)} OR id IN ("
             + ", ".join(q(uid(f"candidate-{i}")) for i in range(1, 11)) + ");")
lines.append(f"DELETE FROM companies WHERE id = {q(COMPANY)};")

lines.append(f"""
INSERT INTO companies (id, name, industry, size, show_name_publicly, tagline, about, website, headquarters)
VALUES ({q(COMPANY)}, 'FairFold Demo Labs', 'Software', '11-50', true,
        'A demo company for the anonymised screening walkthrough.',
        '### About Demo Labs
FairFold Demo Labs exists to show the screening workflow end to end.',
        'https://demo.fairfold.local', 'Dhaka, Bangladesh');
""".strip())

lines.append(f"""
INSERT INTO users (id, email, password_hash, first_name, last_name, role, company_id,
                   email_verified, failed_login_count, consent_at)
VALUES ({q(HR)}, 'demo.hr@fairfold.dev', {q(PASSWORD_HASH)}, 'Demo', 'HR', 'EMPLOYER_HR',
        {q(COMPANY)}, true, 0, NOW(3));
""".strip())

candidate_lines = []
for i, candidate in enumerate(CANDIDATES, start=1):
    candidate_id = uid(f"candidate-{i}")
    profile_id = candidate_id
    resume_id = uid(f"resume-{i}")
    application_id = uid(f"application-{i}")
    email = f"demo.candidate{i}@fairfold.dev"
    candidate_lines.append(f"""
INSERT INTO users (id, email, password_hash, first_name, last_name, role, email_verified,
                   failed_login_count, consent_at)
VALUES ({q(candidate_id)}, {q(email)}, {q(PASSWORD_HASH)}, {q(candidate['first'])},
        {q(candidate['last'])}, 'CANDIDATE', true, 0, NOW(3));

INSERT INTO candidate_profiles (user_id, phone, title, location, remote_preference,
                                total_experience_years, education_level, previous_job_titles)
VALUES ({q(profile_id)}, '+88017123456{i:02d}', 'Software Engineer', {q(candidate['location'])},
        'any', {candidate['years']}, 'bachelor', JSON_ARRAY('Software Engineer'));

INSERT INTO resumes (id, user_id, original_filename, storage_key, size_bytes, status,
                     stripped_text, is_current, current_user_key)
VALUES ({q(resume_id)}, {q(candidate_id)}, 'demo-cv-{i}.pdf', {q(f'demo-resume-{i}.pdf')},
        2048, 'ready', {q(candidate['text'])}, true, {q(candidate_id)});

INSERT INTO applications (id, job_id, candidate_id, resume_id, status, created_at)
VALUES ({q(application_id)}, {q(JOB)}, {q(candidate_id)}, {q(resume_id)}, 'applied',
        DATE_SUB(NOW(3), INTERVAL {11 - i} MINUTE));
""".strip())
    for skill, level, years in candidate["skills"]:
        cskill = uid(f"cskill-{i}-{skill}")
        candidate_lines.append(f"""
INSERT INTO candidate_skills (id, user_id, skill_id, level, years, verified)
SELECT {q(cskill)}, {q(candidate_id)}, id, {q(level)}, {years}, false
FROM skills WHERE name = {q(skill)};
""".strip())

lines.append(f"""
INSERT INTO jobs (id, company_id, created_by, title, description, requirements, experience_level,
                  city, country, remote, status, hf_min_years, hf_country, cutoff_score, published_at)
VALUES ({q(JOB)}, {q(COMPANY)}, {q(HR)}, 'Senior Backend Engineer',
        '### About the role
Build reliable backend services for a growing product team. You will own services end to end,
review code and work with product and design.',
        '### Requirements
- Strong Java and Spring Boot
- PostgreSQL and Docker
- AWS and REST API experience',
        'senior', 'Dhaka', 'Bangladesh', false, 'active', 2, NULL, 60, NOW(3));
""".strip())

for skill, kind, must_have in [
    ("Java", "required", True),
    ("Spring Boot", "required", False),
    ("PostgreSQL", "required", False),
    ("Docker", "required", False),
    ("AWS", "nice_to_have", False),
    ("REST API", "nice_to_have", False),
]:
    lines.append(f"""
INSERT INTO job_skills (id, job_id, skill_id, kind, must_have)
SELECT {q(uid(f"jobskill-{skill}"))}, {q(JOB)}, id, {q(kind)}, {str(must_have).lower()}
FROM skills WHERE name = {q(skill)};
""".strip())

for position, question in enumerate([
    "How do you design and operate reliable backend services?",
    "Describe a migration or performance improvement you led.",
    "How do you review code and mentor other engineers?",
]):
    lines.append(f"""
INSERT INTO job_screening_questions (id, job_id, position, text)
VALUES ({q(uid(f"question-{position}"))}, {q(JOB)}, {position}, {q(question)});
""".strip())

lines.extend(candidate_lines)
print("\n".join(lines))
