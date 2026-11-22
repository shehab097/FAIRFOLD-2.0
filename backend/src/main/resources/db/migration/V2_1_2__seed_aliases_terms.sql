-- V2_1_2__seed_aliases_terms.sql
-- Skill aliases and stripping terms used by resume processing.

-- ---------------------------------------------------------------------------
-- Skill aliases (alias -> canonical skill). Aliases are UNIQUE and compared
-- case-insensitively through the collation.
-- ---------------------------------------------------------------------------
INSERT INTO skill_aliases (skill_id, alias)
SELECT s.id, a.alias
FROM skills s
         JOIN (SELECT 'JavaScript' AS skill, 'JS' AS alias UNION ALL
               SELECT 'JavaScript', 'ES6' UNION ALL
               SELECT 'JavaScript', 'ECMAScript' UNION ALL
               SELECT 'JavaScript', 'Java Script' UNION ALL
               SELECT 'TypeScript', 'TS' UNION ALL
               SELECT 'Node.js', 'Node' UNION ALL
               SELECT 'Node.js', 'NodeJS' UNION ALL
               SELECT 'Node.js', 'Node JS' UNION ALL
               SELECT 'React', 'ReactJS' UNION ALL
               SELECT 'React', 'React.js' UNION ALL
               SELECT 'React', 'React JS' UNION ALL
               SELECT 'Angular', 'AngularJS' UNION ALL
               SELECT 'Angular', 'Angular.js' UNION ALL
               SELECT 'Vue.js', 'Vue' UNION ALL
               SELECT 'Vue.js', 'VueJS' UNION ALL
               SELECT 'Vue.js', 'Vue JS' UNION ALL
               SELECT 'Next.js', 'NextJS' UNION ALL
               SELECT 'Next.js', 'Next JS' UNION ALL
               SELECT 'Express.js', 'Express' UNION ALL
               SELECT 'Express.js', 'ExpressJS' UNION ALL
               SELECT 'Spring Boot', 'Springboot' UNION ALL
               SELECT 'Hibernate', 'Hibernate ORM' UNION ALL
               SELECT 'PostgreSQL', 'Postgres' UNION ALL
               SELECT 'PostgreSQL', 'Postgre SQL' UNION ALL
               SELECT 'PostgreSQL', 'PG' UNION ALL
               SELECT 'SQL Server', 'MS SQL' UNION ALL
               SELECT 'SQL Server', 'MSSQL' UNION ALL
               SELECT 'SQL Server', 'Microsoft SQL Server' UNION ALL
               SELECT 'MongoDB', 'Mongo' UNION ALL
               SELECT 'MongoDB', 'Mongo DB' UNION ALL
               SELECT 'Kubernetes', 'K8s' UNION ALL
               SELECT 'Go', 'Golang' UNION ALL
               SELECT 'Python', 'Py' UNION ALL
               SELECT 'Python', 'Python3' UNION ALL
               SELECT 'C#', 'CSharp' UNION ALL
               SELECT 'C#', 'C Sharp' UNION ALL
               SELECT 'C#', 'C-Sharp' UNION ALL
               SELECT 'C++', 'Cpp' UNION ALL
               SELECT 'C++', 'C Plus Plus' UNION ALL
               SELECT '.NET', 'dotnet' UNION ALL
               SELECT '.NET', '.Net' UNION ALL
               SELECT '.NET', 'ASP.NET' UNION ALL
               SELECT '.NET', 'ASP.NET Core' UNION ALL
               SELECT 'Ruby on Rails', 'Rails' UNION ALL
               SELECT 'Ruby on Rails', 'RoR' UNION ALL
               SELECT 'Tailwind CSS', 'Tailwind' UNION ALL
               SELECT 'Tailwind CSS', 'TailwindCSS' UNION ALL
               SELECT 'Google Cloud', 'GCP' UNION ALL
               SELECT 'Google Cloud', 'Google Cloud Platform' UNION ALL
               SELECT 'AWS', 'Amazon Web Services' UNION ALL
               SELECT 'Azure', 'MS Azure' UNION ALL
               SELECT 'Azure', 'Microsoft Azure' UNION ALL
               SELECT 'Terraform', 'TF' UNION ALL
               SELECT 'REST API', 'REST' UNION ALL
               SELECT 'REST API', 'Rest API' UNION ALL
               SELECT 'REST API', 'RESTful API' UNION ALL
               SELECT 'GraphQL', 'Graph QL' UNION ALL
               SELECT 'CI/CD', 'CICD' UNION ALL
               SELECT 'CI/CD', 'CI CD' UNION ALL
               SELECT 'Machine Learning', 'ML' UNION ALL
               SELECT 'Deep Learning', 'DL' UNION ALL
               SELECT 'Natural Language Processing', 'NLP' UNION ALL
               SELECT 'Big Data', 'BigData' UNION ALL
               SELECT 'Power BI', 'PowerBI' UNION ALL
               SELECT 'JUnit', 'JUnit5' UNION ALL
               SELECT 'JUnit', 'JUnit 5' UNION ALL
               SELECT 'Maven', 'Apache Maven' UNION ALL
               SELECT 'Gradle', 'Gradle Build Tool' UNION ALL
               SELECT 'GitHub', 'Github' UNION ALL
               SELECT 'MySQL', 'My SQL' UNION ALL
               SELECT 'SQLite', 'SQL Lite' UNION ALL
               SELECT 'MariaDB', 'Maria DB' UNION ALL
               SELECT 'Elasticsearch', 'Elastic Search' UNION ALL
               SELECT 'DynamoDB', 'Dynamo DB' UNION ALL
               SELECT 'Linux', 'GNU/Linux' UNION ALL
               SELECT 'Apache', 'Apache HTTP Server' UNION ALL
               SELECT 'Selenium', 'Selenium WebDriver' UNION ALL
               SELECT 'Cypress', 'Cypress.io' UNION ALL
               SELECT 'Figma', 'Figma Design' UNION ALL
               SELECT 'Jira', 'Atlassian Jira' UNION ALL
               SELECT 'Postman', 'Postman API' UNION ALL
               SELECT 'Agile', 'Agile Methodology' UNION ALL
               SELECT 'Scrum', 'Scrum Framework' UNION ALL
               SELECT 'Leadership', 'Team Leadership' UNION ALL
               SELECT 'Problem Solving', 'Problem-Solving' UNION ALL
               SELECT 'Data Science', 'DataScience' UNION ALL
               SELECT 'NumPy', 'Numpy' UNION ALL
               SELECT 'Spark', 'Apache Spark' UNION ALL
               SELECT 'Hadoop', 'Apache Hadoop' UNION ALL
               SELECT 'Tableau', 'Tableau Desktop' UNION ALL
               SELECT 'Dart', 'Dart Lang' UNION ALL
               SELECT 'R', 'R Language' UNION ALL
               SELECT 'SQL', 'Structured Query Language' UNION ALL
               SELECT 'HTML', 'HTML5' UNION ALL
               SELECT 'CSS', 'CSS3' UNION ALL
               SELECT 'Bash', 'Shell Scripting' UNION ALL
               SELECT 'Bash', 'Shell' UNION ALL
               SELECT 'Laravel', 'Laravel Framework' UNION ALL
               SELECT 'Django', 'Django Framework' UNION ALL
               SELECT 'Flask', 'Flask Framework' UNION ALL
               SELECT 'FastAPI', 'Fast API' UNION ALL
               SELECT 'Bootstrap', 'Bootstrap CSS' UNION ALL
               SELECT 'Oracle', 'Oracle Database' UNION ALL
               SELECT 'Cassandra', 'Apache Cassandra' UNION ALL
               SELECT 'Firebase', 'Google Firebase' UNION ALL
               SELECT 'Webpack', 'Webpack 5' UNION ALL
               SELECT 'Vite', 'ViteJS' UNION ALL
               SELECT 'Rust', 'Rust Lang' UNION ALL
               SELECT 'Scala', 'Scala Lang' UNION ALL
               SELECT 'Perl', 'Perl 5' UNION ALL
               SELECT 'Kotlin', 'Kotlin JVM' UNION ALL
               SELECT 'Docker', 'Docker Engine'
              ) a ON a.skill = s.name;

-- ---------------------------------------------------------------------------
-- Strip terms: institutions
-- ---------------------------------------------------------------------------
INSERT INTO strip_terms (kind, term) VALUES
    ('institution', 'University of Dhaka'),
    ('institution', 'Dhaka University'),
    ('institution', 'BUET'),
    ('institution', 'Bangladesh University of Engineering and Technology'),
    ('institution', 'North South University'),
    ('institution', 'BRAC University'),
    ('institution', 'Independent University Bangladesh'),
    ('institution', 'Independent University, Bangladesh'),
    ('institution', 'University of Chittagong'),
    ('institution', 'Chittagong University'),
    ('institution', 'Rajshahi University'),
    ('institution', 'University of Rajshahi'),
    ('institution', 'Jahangirnagar University'),
    ('institution', 'Khulna University'),
    ('institution', 'Islamic University of Technology'),
    ('institution', 'Daffodil International University'),
    ('institution', 'American International University-Bangladesh'),
    ('institution', 'American International University Bangladesh'),
    ('institution', 'East West University'),
    ('institution', 'United International University'),
    ('institution', 'Ahsanullah University of Science and Technology'),
    ('institution', 'Military Institute of Science and Technology'),
    ('institution', 'Shahjalal University of Science and Technology'),
    ('institution', 'Jagannath University'),
    ('institution', 'Dhaka University of Engineering and Technology'),
    ('institution', 'National University'),
    ('institution', 'Stamford University Bangladesh'),
    ('institution', 'Primeasia University'),
    ('institution', 'Southeast University'),
    ('institution', 'IUBAT'),
    ('institution', 'Green University of Bangladesh'),
    ('institution', 'Uttara University'),
    ('institution', 'City University'),
    ('institution', 'Manarat International University'),
    ('institution', 'ASA University Bangladesh'),
    ('institution', 'Atish Dipankar University of Science and Technology'),
    ('institution', 'Chittagong University of Engineering and Technology'),
    ('institution', 'Khulna University of Engineering and Technology'),
    ('institution', 'Rajshahi University of Engineering and Technology'),
    ('institution', 'Patuakhali Science and Technology University'),
    ('institution', 'Bangladesh Agricultural University'),
    ('institution', 'Bangabandhu Sheikh Mujibur Rahman Agricultural University'),
    ('institution', 'Sher-e-Bangla Agricultural University'),
    ('institution', 'Chittagong Medical College'),
    ('institution', 'Dhaka Medical College'),
    ('institution', 'Sir Salimullah Medical College'),
    ('institution', 'Mymensingh Medical College'),
    ('institution', 'Rajshahi Medical College'),
    ('institution', 'Sylhet MAG Osmani Medical College'),
    ('institution', 'Holy Family Red Crescent Medical College'),
    ('institution', 'Bangladesh Institute of Technology'),
    ('institution', 'Bangladesh Open University'),
    ('institution', 'Bangladesh National University'),
    ('institution', 'Harvard University'),
    ('institution', 'Stanford University'),
    ('institution', 'MIT'),
    ('institution', 'Massachusetts Institute of Technology'),
    ('institution', 'University of Cambridge'),
    ('institution', 'University of Oxford'),
    ('institution', 'Imperial College London'),
    ('institution', 'University College London'),
    ('institution', 'University of Toronto'),
    ('institution', 'University of Melbourne'),
    ('institution', 'National University of Singapore'),
    ('institution', 'Nanyang Technological University'),
    ('institution', 'Tsinghua University'),
    ('institution', 'Peking University'),
    ('institution', 'Indian Institute of Technology Bombay'),
    ('institution', 'Indian Institute of Technology Delhi'),
    ('institution', 'University of Delhi'),
    ('institution', 'University of Mumbai'),
    ('institution', 'University of Karachi'),
    ('institution', 'National University of Sciences and Technology'),
    ('institution', 'Lahore University of Management Sciences'),
    ('institution', 'University of Engineering and Technology Lahore'),
    ('institution', 'University of Moratuwa'),
    ('institution', 'University of Colombo'),
    ('institution', 'University of Peradeniya'),
    ('institution', 'Asian Institute of Technology'),
    ('institution', 'Chulalongkorn University'),
    ('institution', 'University of Indonesia'),
    ('institution', 'University of the Philippines'),
    ('institution', 'Seoul National University'),
    ('institution', 'Korea University'),
    ('institution', 'University of Tokyo'),
    ('institution', 'Kyoto University'),
    ('institution', 'Osaka University'),
    ('institution', 'University of Sydney'),
    ('institution', 'University of New South Wales'),
    ('institution', 'Monash University'),
    ('institution', 'Australian National University'),
    ('institution', 'University of Auckland'),
    ('institution', 'University of Cape Town'),
    ('institution', 'University of the Witwatersrand'),
    ('institution', 'Cairo University'),
    ('institution', 'American University in Cairo'),
    ('institution', 'University of Nairobi'),
    ('institution', 'University of Lagos'),
    ('institution', 'University of Ghana'),
    ('institution', 'Makerere University'),
    ('institution', 'University of Edinburgh'),
    ('institution', 'Kings College London'),
    ('institution', 'University of Manchester'),
    ('institution', 'University of Birmingham'),
    ('institution', 'Technical University of Munich'),
    ('institution', 'ETH Zurich'),
    ('institution', 'Delft University of Technology'),
    ('institution', 'KU Leuven'),
    ('institution', 'University of Amsterdam'),
    ('institution', 'University of Copenhagen'),
    ('institution', 'Lund University'),
    ('institution', 'KTH Royal Institute of Technology'),
    ('institution', 'University of Helsinki'),
    ('institution', 'Trinity College Dublin'),
    ('institution', 'University of Dublin'),
    ('institution', 'University of Sao Paulo'),
    ('institution', 'National Autonomous University of Mexico'),
    ('institution', 'University of Buenos Aires'),
    ('institution', 'University of Chile'),
    ('institution', 'University of Zurich'),
    ('institution', 'Ecole Polytechnique'),
    ('institution', 'Sorbonne University'),
    ('institution', 'Heidelberg University'),
    ('institution', 'RWTH Aachen University'),
    ('institution', 'University of Vienna'),
    ('institution', 'University of Warsaw'),
    ('institution', 'Charles University'),
    ('institution', 'Eotvos Lorand University'),
    ('institution', 'University of Belgrade'),
    ('institution', 'University of Zagreb'),
    ('institution', 'University of Bucharest'),
    ('institution', 'Bogazici University'),
    ('institution', 'Middle East Technical University'),
    ('institution', 'American University of Beirut'),
    ('institution', 'Lebanese American University'),
    ('institution', 'University of Jordan'),
    ('institution', 'King Saud University'),
    ('institution', 'King Fahd University of Petroleum and Minerals'),
    ('institution', 'United Arab Emirates University'),
    ('institution', 'American University of Sharjah'),
    ('institution', 'Qatar University'),
    ('institution', 'Kuwait University'),
    ('institution', 'Sultan Qaboos University'),
    ('institution', 'University of Bahrain');

-- ---------------------------------------------------------------------------
-- Strip terms: employers
-- ---------------------------------------------------------------------------
INSERT INTO strip_terms (kind, term) VALUES
    ('employer', 'Grameenphone'),
    ('employer', 'Robi Axiata'),
    ('employer', 'Banglalink'),
    ('employer', 'Teletalk'),
    ('employer', 'BTCL'),
    ('employer', 'bKash'),
    ('employer', 'Nagad'),
    ('employer', 'Dutch-Bangla Bank'),
    ('employer', 'BRAC Bank'),
    ('employer', 'The City Bank'),
    ('employer', 'City Bank'),
    ('employer', 'Eastern Bank'),
    ('employer', 'Islami Bank Bangladesh'),
    ('employer', 'Sonali Bank'),
    ('employer', 'Janata Bank'),
    ('employer', 'IFIC Bank'),
    ('employer', 'Prime Bank'),
    ('employer', 'Standard Chartered Bangladesh'),
    ('employer', 'Standard Chartered'),
    ('employer', 'HSBC Bangladesh'),
    ('employer', 'HSBC'),
    ('employer', 'Commercial Bank of Ceylon'),
    ('employer', 'Southeast Bank'),
    ('employer', 'Bank Asia'),
    ('employer', 'Mercantile Bank'),
    ('employer', 'Pubali Bank'),
    ('employer', 'Trust Bank'),
    ('employer', 'United Commercial Bank'),
    ('employer', 'Social Islami Bank'),
    ('employer', 'First Security Islami Bank'),
    ('employer', 'Export Import Bank of Bangladesh'),
    ('employer', 'Habib Bank'),
    ('employer', 'National Bank'),
    ('employer', 'One Bank'),
    ('employer', 'Dhaka Bank'),
    ('employer', 'Premier Bank'),
    ('employer', 'AB Bank'),
    ('employer', 'Mutual Trust Bank'),
    ('employer', 'Padma Bank'),
    ('employer', 'NRB Bank'),
    ('employer', 'NRB Commercial Bank'),
    ('employer', 'Shimanto Bank'),
    ('employer', 'Meghna Bank'),
    ('employer', 'Midland Bank'),
    ('employer', 'Community Bank Bangladesh'),
    ('employer', 'Citibank Bangladesh'),
    ('employer', 'Standard Bank'),
    ('employer', 'Bank Alfalah'),
    ('employer', 'Al-Arafah Islami Bank'),
    ('employer', 'Union Bank'),
    ('employer', 'Modhumoti Bank'),
    ('employer', 'Global Islami Bank'),
    ('employer', 'Pathao'),
    ('employer', 'ShopUp'),
    ('employer', 'Foodpanda Bangladesh'),
    ('employer', 'Foodpanda'),
    ('employer', 'Daraz Bangladesh'),
    ('employer', 'Daraz'),
    ('employer', 'Chaldal'),
    ('employer', 'Pickaboo'),
    ('employer', 'Sheba.xyz'),
    ('employer', '10 Minute School'),
    ('employer', 'Shohoz'),
    ('employer', 'Truck Lagbe'),
    ('employer', 'Jatri'),
    ('employer', 'Praava Health'),
    ('employer', 'Maya'),
    ('employer', 'Augmedix Bangladesh'),
    ('employer', 'Therap BD'),
    ('employer', 'Enosis Solutions'),
    ('employer', 'Brain Station 23'),
    ('employer', 'TigerIT Bangladesh'),
    ('employer', 'BJIT'),
    ('employer', 'Nascenia'),
    ('employer', 'Kaz Software'),
    ('employer', 'Vivasoft'),
    ('employer', 'Riseup Labs'),
    ('employer', 'Genex Infosys'),
    ('employer', 'DataSoft Systems'),
    ('employer', 'Leads Corporation'),
    ('employer', 'Millennium Information Solution'),
    ('employer', 'Cefalo Bangladesh'),
    ('employer', 'Samsung Bangladesh'),
    ('employer', 'Samsung Electronics'),
    ('employer', 'Samsung'),
    ('employer', 'Unilever Bangladesh'),
    ('employer', 'Unilever'),
    ('employer', 'Nestle Bangladesh'),
    ('employer', 'Nestle'),
    ('employer', 'British American Tobacco Bangladesh'),
    ('employer', 'British American Tobacco'),
    ('employer', 'Google'),
    ('employer', 'Microsoft'),
    ('employer', 'Amazon'),
    ('employer', 'Meta'),
    ('employer', 'Apple'),
    ('employer', 'Netflix'),
    ('employer', 'IBM'),
    ('employer', 'Oracle'),
    ('employer', 'SAP'),
    ('employer', 'Adobe'),
    ('employer', 'Salesforce'),
    ('employer', 'Intel'),
    ('employer', 'NVIDIA'),
    ('employer', 'Cisco'),
    ('employer', 'Dell'),
    ('employer', 'Accenture'),
    ('employer', 'Deloitte'),
    ('employer', 'PwC'),
    ('employer', 'Ernst & Young'),
    ('employer', 'KPMG'),
    ('employer', 'McKinsey & Company'),
    ('employer', 'Boston Consulting Group'),
    ('employer', 'Bain & Company'),
    ('employer', 'Goldman Sachs'),
    ('employer', 'JPMorgan Chase'),
    ('employer', 'Morgan Stanley'),
    ('employer', 'Bank of America'),
    ('employer', 'Wells Fargo'),
    ('employer', 'Visa'),
    ('employer', 'Mastercard'),
    ('employer', 'PayPal'),
    ('employer', 'Stripe'),
    ('employer', 'Shopify'),
    ('employer', 'Uber'),
    ('employer', 'Airbnb'),
    ('employer', 'Booking.com'),
    ('employer', 'Spotify'),
    ('employer', 'LinkedIn'),
    ('employer', 'GitHub'),
    ('employer', 'GitLab'),
    ('employer', 'Atlassian'),
    ('employer', 'Slack'),
    ('employer', 'Zoom'),
    ('employer', 'Dropbox'),
    ('employer', 'Twilio'),
    ('employer', 'Cloudflare'),
    ('employer', 'DigitalOcean'),
    ('employer', 'VMware'),
    ('employer', 'Red Hat'),
    ('employer', 'Canonical'),
    ('employer', 'Elastic'),
    ('employer', 'Datadog'),
    ('employer', 'Snowflake'),
    ('employer', 'Palantir'),
    ('employer', 'OpenAI'),
    ('employer', 'Anthropic'),
    ('employer', 'DeepMind'),
    ('employer', 'Sony'),
    ('employer', 'Toyota'),
    ('employer', 'Honda'),
    ('employer', 'Siemens'),
    ('employer', 'Bosch'),
    ('employer', 'Philips'),
    ('employer', 'PepsiCo'),
    ('employer', 'Coca-Cola'),
    ('employer', 'McDonalds'),
    ('employer', 'Starbucks'),
    ('employer', 'Walmart'),
    ('employer', 'Target'),
    ('employer', 'IKEA'),
    ('employer', 'Zara'),
    ('employer', 'H&M'),
    ('employer', 'Nike'),
    ('employer', 'Adidas'),
    ('employer', 'Shell'),
    ('employer', 'BP'),
    ('employer', 'Chevron'),
    ('employer', 'ExxonMobil'),
    ('employer', 'TotalEnergies');
