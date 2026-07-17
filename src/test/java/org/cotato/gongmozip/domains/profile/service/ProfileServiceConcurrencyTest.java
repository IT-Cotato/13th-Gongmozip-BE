package org.cotato.gongmozip.domains.profile.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.CreateProfileRequest;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProfileServiceConcurrencyTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @AfterEach
    void cleanUp() {
        profileRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void 동시에_프로필을_생성해도_대표_프로필은_하나만_존재한다() throws Exception {
        Member member = memberRepository.saveAndFlush(Member.builder()
                .email("concurrency@gongmozip.com")
                .password("password")
                .status(MemberStatus.ACTIVE)
                .build());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> createProfileAfterSignal(member, "동시성1", ready, start));
            Future<?> second = executor.submit(() -> createProfileAfterSignal(member, "동시성2", ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        List<Profile> profiles = profileRepository.findAll();
        assertThat(profiles).hasSize(2);
        assertThat(profiles.stream().filter(Profile::isMain)).hasSize(1);
    }

    private void createProfileAfterSignal(Member member, String nickname, CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 시작 대기 시간 초과");
            }
            profileService.createProfile(
                    new CreateProfileRequest(
                            nickname, "학교", 3, "컴퓨터공학", null, 4.0, 4.5, List.of(InterestCategory.IT_AI_TECH), true),
                    member);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
