package org.cotato.gongmozip.domains.profile.service;

/**
 * 자격증 초기 데이터 시딩은 Flyway 마이그레이션(V1__init_certifications.sql)을 통해 처리됩니다.
 * 기동 시 CommandLineRunner 기반 시딩 로직은 제거되었습니다.
 */
public class CertificationSeeder {
    private CertificationSeeder() {}
}
