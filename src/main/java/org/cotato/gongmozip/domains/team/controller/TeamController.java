package org.cotato.gongmozip.domains.team.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.ChatbotToggleRequest;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.ChatRoomListResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.TeamMembersResponse;
import org.cotato.gongmozip.domains.team.enums.ChatRoomSortType;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.exception.codes.TeamSuccessCode;
import org.cotato.gongmozip.domains.team.service.TeamService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Team", description = "팀(채팅방) 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "채팅방 목록 조회 (sort: LATEST=최신 메시지 순(기본값), UNREAD=안읽은 메시지 순)")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @GetMapping("/teams")
    public ResponseEntity<BaseResponse<ChatRoomListResponse>> getChatRooms(
            @RequestParam(name = "sort", required = false, defaultValue = "LATEST") ChatRoomSortType sort,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatRoomListResponse response = teamService.getMyChatRooms(userDetails.getMemberId(), sort);
        return BaseResponseFormatter.success(TeamSuccessCode.CHAT_ROOM_LIST_RETRIEVED, response);
    }

    @Operation(summary = "대화상대 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @GetMapping("/teams/{teamId}/members")
    public ResponseEntity<BaseResponse<TeamMembersResponse>> getTeamMembers(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamMembersResponse response = teamService.getTeamMembers(teamId, userDetails.getMemberId());
        return BaseResponseFormatter.success(TeamSuccessCode.TEAM_MEMBERS_RETRIEVED, response);
    }

    @Operation(summary = "채팅방 나가기")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @DeleteMapping("/teams/{teamId}/members/me")
    public ResponseEntity<BaseResponse<Void>> leaveTeam(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.leaveTeam(teamId, userDetails.getMemberId());
        return BaseResponseFormatter.success(TeamSuccessCode.LEFT_CHAT_ROOM);
    }

    @Operation(summary = "챗봇 추가/삭제")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @PatchMapping("/teams/{teamId}/chatbot")
    public ResponseEntity<BaseResponse<Void>> toggleChatbot(
            @PathVariable("teamId") Long teamId,
            @RequestBody ChatbotToggleRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.toggleChatbot(teamId, userDetails.getMemberId(), request.enabled());
        return BaseResponseFormatter.success(TeamSuccessCode.CHATBOT_TOGGLED);
    }
}
