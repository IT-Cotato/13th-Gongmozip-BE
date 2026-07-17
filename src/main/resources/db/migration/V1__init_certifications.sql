CREATE TABLE IF NOT EXISTS certifications (
    certification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    certification_code VARCHAR(100) NOT NULL,
    certificate_name VARCHAR(150) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_certifications_code UNIQUE (certification_code)
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TOEIC', 'TOEIC', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TOEIC'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TOEIC_SPEAKING', 'TOEIC Speaking', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TOEIC_SPEAKING'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'OPIC', 'OPIc', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'OPIC'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TOEFL_IBT', 'TOEFL iBT', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TOEFL_IBT'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'IELTS', 'IELTS', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'IELTS'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TEPS', 'TEPS', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TEPS'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'NEW_TEPS', 'New TEPS', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'NEW_TEPS'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'HSK', 'HSK', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'HSK'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'HSKK', 'HSKK', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'HSKK'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'JLPT', 'JLPT', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'JLPT'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'JPT', 'JPT', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'JPT'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'SJPT', 'SJPT', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'SJPT'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'FLEX_ENGLISH', 'FLEX 영어', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'FLEX_ENGLISH'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'FLEX_JAPANESE', 'FLEX 일본어', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'FLEX_JAPANESE'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'FLEX_CHINESE', 'FLEX 중국어', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'FLEX_CHINESE'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TOEIC_BRIDGE', 'TOEIC Bridge', 'LANGUAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TOEIC_BRIDGE'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'INFORMATION_PROCESSING_ENGINEER', '정보처리기사', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'INFORMATION_PROCESSING_ENGINEER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'INFORMATION_PROCESSING_INDUSTRIAL_ENGINEER', '정보처리산업기사', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'INFORMATION_PROCESSING_INDUSTRIAL_ENGINEER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'INFORMATION_SECURITY_ENGINEER', '정보보안기사', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'INFORMATION_SECURITY_ENGINEER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'INFORMATION_SECURITY_INDUSTRIAL_ENGINEER', '정보보안산업기사', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'INFORMATION_SECURITY_INDUSTRIAL_ENGINEER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'SQLD', 'SQLD', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'SQLD'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'SQLP', 'SQLP', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'SQLP'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'LINUX_MASTER_LEVEL_1', '리눅스마스터 1급', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'LINUX_MASTER_LEVEL_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'LINUX_MASTER_LEVEL_2', '리눅스마스터 2급', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'LINUX_MASTER_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'NETWORK_ADMIN_LEVEL_1', '네트워크관리사 1급', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'NETWORK_ADMIN_LEVEL_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'NETWORK_ADMIN_LEVEL_2', '네트워크관리사 2급', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'NETWORK_ADMIN_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'COMP_LITERACY_LEVEL_1', '컴퓨터활용능력 1급', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'COMP_LITERACY_LEVEL_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'COMP_LITERACY_LEVEL_2', '컴퓨터활용능력 2급', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'COMP_LITERACY_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'MOS_MASTER', 'MOS Master', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'MOS_MASTER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'MOS_WORD', 'MOS Word', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'MOS_WORD'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'MOS_EXCEL', 'MOS Excel', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'MOS_EXCEL'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'MOS_POWERPOINT', 'MOS PowerPoint', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'MOS_POWERPOINT'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'MOS_ACCESS', 'MOS Access', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'MOS_ACCESS'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ICDL', 'ICDL', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ICDL'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'CCNA', 'CCNA', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'CCNA'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'AWS_CLOUD_PRACTITIONER', 'AWS Certified Cloud Practitioner', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'AWS_CLOUD_PRACTITIONER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'AWS_SOLUTIONS_ARCHITECT_ASSOCIATE', 'AWS Solutions Architect – Associate', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'AWS_SOLUTIONS_ARCHITECT_ASSOCIATE'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'AZURE_FUNDAMENTALS_AZ900', 'Azure Fundamentals(AZ-900)', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'AZURE_FUNDAMENTALS_AZ900'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'GOOGLE_CLOUD_DIGITAL_LEADER', 'Google Cloud Digital Leader', 'COMPUTER_IT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'GOOGLE_CLOUD_DIGITAL_LEADER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ADSP', 'ADsP', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ADSP'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ADP', 'ADP', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ADP'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'BIG_DATA_ANALYSIS_ENGINEER', '빅데이터분석기사', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'BIG_DATA_ANALYSIS_ENGINEER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'SOCIAL_RESEARCH_ANALYST_LEVEL_2', '사회조사분석사 2급', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'SOCIAL_RESEARCH_ANALYST_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'SOCIAL_RESEARCH_ANALYST_LEVEL_1', '사회조사분석사 1급', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'SOCIAL_RESEARCH_ANALYST_LEVEL_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'AICE_BASIC', 'AICE Basic', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'AICE_BASIC'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'AICE_ASSOCIATE', 'AICE Associate', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'AICE_ASSOCIATE'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'AICE_PROFESSIONAL', 'AICE Professional', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'AICE_PROFESSIONAL'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'AIBT', 'AIBT', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'AIBT'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TENSORFLOW_DEVELOPER_CERTIFICATE', 'TensorFlow Developer Certificate', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TENSORFLOW_DEVELOPER_CERTIFICATE'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'SAS_CERTIFIED_SPECIALIST', 'SAS Certified Specialist', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'SAS_CERTIFIED_SPECIALIST'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TABLEAU_DESKTOP_SPECIALIST', 'Tableau Desktop Specialist', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TABLEAU_DESKTOP_SPECIALIST'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TABLEAU_CERTIFIED_DATA_ANALYST', 'Tableau Certified Data Analyst', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TABLEAU_CERTIFIED_DATA_ANALYST'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'POWER_BI_DATA_ANALYST_PL300', 'Power BI Data Analyst(PL-300)', 'DATA_AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'POWER_BI_DATA_ANALYST_PL300'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'GTQ_LEVEL_1', 'GTQ 1급', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'GTQ_LEVEL_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'GTQ_LEVEL_2', 'GTQ 2급', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'GTQ_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'GTQI', 'GTQi', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'GTQI'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ACA_PHOTOSHOP', 'ACA Photoshop', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ACA_PHOTOSHOP'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ACA_ILLUSTRATOR', 'ACA Illustrator', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ACA_ILLUSTRATOR'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ACA_INDESIGN', 'ACA InDesign', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ACA_INDESIGN'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ACP_PHOTOSHOP', 'ACP Photoshop', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ACP_PHOTOSHOP'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ACP_ILLUSTRATOR', 'ACP Illustrator', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ACP_ILLUSTRATOR'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ACP_PREMIERE_PRO', 'ACP Premiere Pro', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ACP_PREMIERE_PRO'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ACP_AFTER_EFFECTS', 'ACP After Effects', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ACP_AFTER_EFFECTS'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'WEB_DESIGN_TECHNICIAN', '웹디자인기능사', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'WEB_DESIGN_TECHNICIAN'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'COMPUTER_GRAPHICS_TECHNICIAN', '컴퓨터그래픽스운용기능사', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'COMPUTER_GRAPHICS_TECHNICIAN'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'COLORIST_ENGINEER', '컬러리스트기사', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'COLORIST_ENGINEER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'COLORIST_INDUSTRIAL_ENGINEER', '컬러리스트산업기사', 'DESIGN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'COLORIST_INDUSTRIAL_ENGINEER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'COMPUTER_ACCOUNTING_LEVEL_1', '전산회계 1급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'COMPUTER_ACCOUNTING_LEVEL_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'COMPUTER_ACCOUNTING_LEVEL_2', '전산회계 2급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'COMPUTER_ACCOUNTING_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'COMPUTER_TAX_LEVEL_1', '전산세무 1급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'COMPUTER_TAX_LEVEL_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'COMPUTER_TAX_LEVEL_2', '전산세무 2급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'COMPUTER_TAX_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'FAT_LEVEL_1', 'FAT 1급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'FAT_LEVEL_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'FAT_LEVEL_2', 'FAT 2급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'FAT_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TAT_LEVEL_1', 'TAT 1급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TAT_LEVEL_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TAT_LEVEL_2', 'TAT 2급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TAT_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ERP_ACCOUNTING', 'ERP정보관리사 (회계)', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ERP_ACCOUNTING'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ERP_HR', 'ERP정보관리사 (인사)', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ERP_HR'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ERP_PRODUCTION', 'ERP정보관리사 (생산)', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ERP_PRODUCTION'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ERP_LOGISTICS', 'ERP정보관리사 (물류)', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ERP_LOGISTICS'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'CREDIT_ANALYST', '신용분석사', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'CREDIT_ANALYST'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'INVESTMENT_ASSET_MANAGER', '투자자산운용사', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'INVESTMENT_ASSET_MANAGER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'SECURITIES_INVESTMENT_ADVISOR_REP', '증권투자권유대행인', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'SECURITIES_INVESTMENT_ADVISOR_REP'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'FUND_INVESTMENT_ADVISOR_REP', '펀드투자권유대행인', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'FUND_INVESTMENT_ADVISOR_REP'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'BANK_FP', '은행FP', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'BANK_FP'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'AFPK', 'AFPK', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'AFPK'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'CFP', 'CFP', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'CFP'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'MAEKUNG_TEST', '매경TEST', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'MAEKUNG_TEST'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TESAT', 'TESAT', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TESAT'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'DISTRIBUTION_MANAGER_LEVEL_2', '유통관리사 2급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'DISTRIBUTION_MANAGER_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'LOGISTICS_MANAGER', '물류관리사', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'LOGISTICS_MANAGER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TRADE_ENGLISH_LEVEL_1', '무역영어 1급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TRADE_ENGLISH_LEVEL_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'TRADE_ENGLISH_LEVEL_2', '무역영어 2급', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'TRADE_ENGLISH_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'INTERNATIONAL_TRADE_SPECIALIST', '국제무역사', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'INTERNATIONAL_TRADE_SPECIALIST'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'CPA', 'CPA', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'CPA'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'CFA', 'CFA', 'MANAGEMENT_OFFICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'CFA'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'KOREAN_HISTORY_TEST', '한국사능력검정시험', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'KOREAN_HISTORY_TEST'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'KBS_KOREAN_TEST', 'KBS한국어능력시험', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'KBS_KOREAN_TEST'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'KLAT', '한국어능력시험(KLAT)', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'KLAT'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'CHINESE_CHARACTER_TEST', '한자능력검정시험', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'CHINESE_CHARACTER_TEST'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'WORD_PROCESSOR', '워드프로세서', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'WORD_PROCESSOR'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'ITQ', 'ITQ', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'ITQ'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'DRIVER_LICENSE_TYPE_1', '운전면허 1종', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'DRIVER_LICENSE_TYPE_1'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'DRIVER_LICENSE_TYPE_2', '운전면허 2종', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'DRIVER_LICENSE_TYPE_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'CAREER_COUNSELOR_LEVEL_2', '직업상담사 2급', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'CAREER_COUNSELOR_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'YOUTH_COUNSELOR', '청소년상담사', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'YOUTH_COUNSELOR'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'SOCIAL_WORKER_LEVEL_2', '사회복지사 2급', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'SOCIAL_WORKER_LEVEL_2'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'CHILDCARE_TEACHER', '보육교사', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'CHILDCARE_TEACHER'
);

INSERT INTO certifications
    (certification_code, certificate_name, category_code, created_at, updated_at)
SELECT 'FIRST_AID_BLS', '응급처치 관련 자격증(BLS 등)', 'OTHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM certifications WHERE certification_code = 'FIRST_AID_BLS'
);
