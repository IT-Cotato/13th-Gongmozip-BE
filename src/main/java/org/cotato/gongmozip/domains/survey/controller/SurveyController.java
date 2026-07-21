package org.cotato.gongmozip.domains.survey.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Survey", description = "설문 관련 API")
@RestController
@RequestMapping("/api/survey")
@RequiredArgsConstructor
public class SurveyController {}
