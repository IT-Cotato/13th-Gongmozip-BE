package org.cotato.gongmozip.domains.profile.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.profile.entity.Certification;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.cotato.gongmozip.domains.profile.repository.CertificationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CertificationSeeder implements CommandLineRunner {

    private final CertificationRepository certificationRepository;

    @Override
    public void run(String... args) throws Exception {
        if (certificationRepository.count() == 0) {
            List<Certification> defaultCertifications = List.of(
                    // (1) 어학 자격증
                    createCert("TOEIC", "TOEIC", CertificationCategory.LANGUAGE),
                    createCert("TOEIC_SPEAKING", "TOEIC Speaking", CertificationCategory.LANGUAGE),
                    createCert("OPIC", "OPIc", CertificationCategory.LANGUAGE),
                    createCert("TOEFL_IBT", "TOEFL iBT", CertificationCategory.LANGUAGE),
                    createCert("IELTS", "IELTS", CertificationCategory.LANGUAGE),
                    createCert("TEPS", "TEPS", CertificationCategory.LANGUAGE),
                    createCert("NEW_TEPS", "New TEPS", CertificationCategory.LANGUAGE),
                    createCert("HSK", "HSK", CertificationCategory.LANGUAGE),
                    createCert("HSKK", "HSKK", CertificationCategory.LANGUAGE),
                    createCert("JLPT", "JLPT", CertificationCategory.LANGUAGE),
                    createCert("JPT", "JPT", CertificationCategory.LANGUAGE),
                    createCert("SJPT", "SJPT", CertificationCategory.LANGUAGE),
                    createCert("FLEX_ENGLISH", "FLEX 영어", CertificationCategory.LANGUAGE),
                    createCert("FLEX_JAPANESE", "FLEX 일본어", CertificationCategory.LANGUAGE),
                    createCert("FLEX_CHINESE", "FLEX 중국어", CertificationCategory.LANGUAGE),
                    createCert("TOEIC_BRIDGE", "TOEIC Bridge", CertificationCategory.LANGUAGE),

                    // (2) 컴퓨터 / IT 자격증
                    createCert("INFORMATION_PROCESSING_ENGINEER", "정보처리기사", CertificationCategory.COMPUTER_IT),
                    createCert(
                            "INFORMATION_PROCESSING_INDUSTRIAL_ENGINEER",
                            "정보처리산업기사",
                            CertificationCategory.COMPUTER_IT),
                    createCert("INFORMATION_SECURITY_ENGINEER", "정보보안기사", CertificationCategory.COMPUTER_IT),
                    createCert(
                            "INFORMATION_SECURITY_INDUSTRIAL_ENGINEER", "정보보안산업기사", CertificationCategory.COMPUTER_IT),
                    createCert("SQLD", "SQLD", CertificationCategory.COMPUTER_IT),
                    createCert("SQLP", "SQLP", CertificationCategory.COMPUTER_IT),
                    createCert("LINUX_MASTER_LEVEL_1", "리눅스마스터 1급", CertificationCategory.COMPUTER_IT),
                    createCert("LINUX_MASTER_LEVEL_2", "리눅스마스터 2급", CertificationCategory.COMPUTER_IT),
                    createCert("NETWORK_ADMIN_LEVEL_1", "네트워크관리사 1급", CertificationCategory.COMPUTER_IT),
                    createCert("NETWORK_ADMIN_LEVEL_2", "네트워크관리사 2급", CertificationCategory.COMPUTER_IT),
                    createCert("COMP_LITERACY_LEVEL_1", "컴퓨터활용능력 1급", CertificationCategory.COMPUTER_IT),
                    createCert("COMP_LITERACY_LEVEL_2", "컴퓨터활용능력 2급", CertificationCategory.COMPUTER_IT),
                    createCert("MOS_MASTER", "MOS Master", CertificationCategory.COMPUTER_IT),
                    createCert("MOS_WORD", "MOS Word", CertificationCategory.COMPUTER_IT),
                    createCert("MOS_EXCEL", "MOS Excel", CertificationCategory.COMPUTER_IT),
                    createCert("MOS_POWERPOINT", "MOS PowerPoint", CertificationCategory.COMPUTER_IT),
                    createCert("MOS_ACCESS", "MOS Access", CertificationCategory.COMPUTER_IT),
                    createCert("ICDL", "ICDL", CertificationCategory.COMPUTER_IT),
                    createCert("CCNA", "CCNA", CertificationCategory.COMPUTER_IT),
                    createCert(
                            "AWS_CLOUD_PRACTITIONER",
                            "AWS Certified Cloud Practitioner",
                            CertificationCategory.COMPUTER_IT),
                    createCert(
                            "AWS_SOLUTIONS_ARCHITECT_ASSOCIATE",
                            "AWS Solutions Architect – Associate",
                            CertificationCategory.COMPUTER_IT),
                    createCert(
                            "AZURE_FUNDAMENTALS_AZ900",
                            "Azure Fundamentals(AZ-900)",
                            CertificationCategory.COMPUTER_IT),
                    createCert(
                            "GOOGLE_CLOUD_DIGITAL_LEADER",
                            "Google Cloud Digital Leader",
                            CertificationCategory.COMPUTER_IT),

                    // (3) 데이터분석 / AI 자격증
                    createCert("ADSP", "ADsP", CertificationCategory.DATA_AI),
                    createCert("ADP", "ADP", CertificationCategory.DATA_AI),
                    createCert("BIG_DATA_ANALYSIS_ENGINEER", "빅데이터분석기사", CertificationCategory.DATA_AI),
                    createCert("SOCIAL_RESEARCH_ANALYST_LEVEL_2", "사회조사분석사 2급", CertificationCategory.DATA_AI),
                    createCert("SOCIAL_RESEARCH_ANALYST_LEVEL_1", "사회조사분석사 1급", CertificationCategory.DATA_AI),
                    createCert("AICE_BASIC", "AICE Basic", CertificationCategory.DATA_AI),
                    createCert("AICE_ASSOCIATE", "AICE Associate", CertificationCategory.DATA_AI),
                    createCert("AICE_PROFESSIONAL", "AICE Professional", CertificationCategory.DATA_AI),
                    createCert("AIBT", "AIBT", CertificationCategory.DATA_AI),
                    createCert(
                            "TENSORFLOW_DEVELOPER_CERTIFICATE",
                            "TensorFlow Developer Certificate",
                            CertificationCategory.DATA_AI),
                    createCert("SAS_CERTIFIED_SPECIALIST", "SAS Certified Specialist", CertificationCategory.DATA_AI),
                    createCert(
                            "TABLEAU_DESKTOP_SPECIALIST", "Tableau Desktop Specialist", CertificationCategory.DATA_AI),
                    createCert(
                            "TABLEAU_CERTIFIED_DATA_ANALYST",
                            "Tableau Certified Data Analyst",
                            CertificationCategory.DATA_AI),
                    createCert(
                            "POWER_BI_DATA_ANALYST_PL300",
                            "Power BI Data Analyst(PL-300)",
                            CertificationCategory.DATA_AI),

                    // (4) 디자인 자격증
                    createCert("GTQ_LEVEL_1", "GTQ 1급", CertificationCategory.DESIGN),
                    createCert("GTQ_LEVEL_2", "GTQ 2급", CertificationCategory.DESIGN),
                    createCert("GTQI", "GTQi", CertificationCategory.DESIGN),
                    createCert("ACA_PHOTOSHOP", "ACA Photoshop", CertificationCategory.DESIGN),
                    createCert("ACA_ILLUSTRATOR", "ACA Illustrator", CertificationCategory.DESIGN),
                    createCert("ACA_INDESIGN", "ACA InDesign", CertificationCategory.DESIGN),
                    createCert("ACP_PHOTOSHOP", "ACP Photoshop", CertificationCategory.DESIGN),
                    createCert("ACP_ILLUSTRATOR", "ACP Illustrator", CertificationCategory.DESIGN),
                    createCert("ACP_PREMIERE_PRO", "ACP Premiere Pro", CertificationCategory.DESIGN),
                    createCert("ACP_AFTER_EFFECTS", "ACP After Effects", CertificationCategory.DESIGN),
                    createCert("WEB_DESIGN_TECHNICIAN", "웹디자인기능사", CertificationCategory.DESIGN),
                    createCert("COMPUTER_GRAPHICS_TECHNICIAN", "컴퓨터그래픽스운용기능사", CertificationCategory.DESIGN),
                    createCert("COLORIST_ENGINEER", "컬러리스트기사", CertificationCategory.DESIGN),
                    createCert("COLORIST_INDUSTRIAL_ENGINEER", "컬러리스트산업기사", CertificationCategory.DESIGN),

                    // (5) 경영 / 사무 자격증
                    createCert("COMPUTER_ACCOUNTING_LEVEL_1", "전산회계 1급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("COMPUTER_ACCOUNTING_LEVEL_2", "전산회계 2급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("COMPUTER_TAX_LEVEL_1", "전산세무 1급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("COMPUTER_TAX_LEVEL_2", "전산세무 2급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("FAT_LEVEL_1", "FAT 1급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("FAT_LEVEL_2", "FAT 2급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("TAT_LEVEL_1", "TAT 1급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("TAT_LEVEL_2", "TAT 2급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("ERP_ACCOUNTING", "ERP정보관리사 (회계)", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("ERP_HR", "ERP정보관리사 (인사)", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("ERP_PRODUCTION", "ERP정보관리사 (생산)", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("ERP_LOGISTICS", "ERP정보관리사 (물류)", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("CREDIT_ANALYST", "신용분석사", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("INVESTMENT_ASSET_MANAGER", "투자자산운용사", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert(
                            "SECURITIES_INVESTMENT_ADVISOR_REP", "증권투자권유대행인", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("FUND_INVESTMENT_ADVISOR_REP", "펀드투자권유대행인", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("BANK_FP", "은행FP", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("AFPK", "AFPK", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("CFP", "CFP", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("MAEKUNG_TEST", "매경TEST", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("TESAT", "TESAT", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("DISTRIBUTION_MANAGER_LEVEL_2", "유통관리사 2급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("LOGISTICS_MANAGER", "물류관리사", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("TRADE_ENGLISH_LEVEL_1", "무역영어 1급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("TRADE_ENGLISH_LEVEL_2", "무역영어 2급", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("INTERNATIONAL_TRADE_SPECIALIST", "국제무역사", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("CPA", "CPA", CertificationCategory.MANAGEMENT_OFFICE),
                    createCert("CFA", "CFA", CertificationCategory.MANAGEMENT_OFFICE),

                    // (6) 기타
                    createCert("KOREAN_HISTORY_TEST", "한국사능력검정시험", CertificationCategory.OTHER),
                    createCert("KBS_KOREAN_TEST", "KBS한국어능력시험", CertificationCategory.OTHER),
                    createCert("KLAT", "한국어능력시험(KLAT)", CertificationCategory.OTHER),
                    createCert("CHINESE_CHARACTER_TEST", "한자능력검정시험", CertificationCategory.OTHER),
                    createCert("WORD_PROCESSOR", "워드프로세서", CertificationCategory.OTHER),
                    createCert("ITQ", "ITQ", CertificationCategory.OTHER),
                    createCert("DRIVER_LICENSE_TYPE_1", "운전면허 1종", CertificationCategory.OTHER),
                    createCert("DRIVER_LICENSE_TYPE_2", "운전면허 2종", CertificationCategory.OTHER),
                    createCert("CAREER_COUNSELOR_LEVEL_2", "직업상담사 2급", CertificationCategory.OTHER),
                    createCert("YOUTH_COUNSELOR", "청소년상담사", CertificationCategory.OTHER),
                    createCert("SOCIAL_WORKER_LEVEL_2", "사회복지사 2급", CertificationCategory.OTHER),
                    createCert("CHILDCARE_TEACHER", "보육교사", CertificationCategory.OTHER),
                    createCert("FIRST_AID_BLS", "응급처치 관련 자격증(BLS 등)", CertificationCategory.OTHER));
            certificationRepository.saveAll(defaultCertifications);
        }
    }

    private Certification createCert(String code, String name, CertificationCategory category) {
        return Certification.builder()
                .certificationCode(code)
                .certificateName(name)
                .categoryCode(category)
                .build();
    }
}
