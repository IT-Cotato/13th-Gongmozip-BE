package org.cotato.gongmozip.domains.review.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "팀원을 표현하는 키워드(중복 선택 가능): LEADERSHIP=리더십이 있는 팀원, GOOD_COMMUNICATOR=소통이 잘되는 팀원, "
                + "CREATIVE=아이디어가 좋은 팀원, PROBLEM_SOLVER=문제해결을 잘하는 팀원, TRUSTWORTHY=믿음직한 팀원, "
                + "PROACTIVE=적극적인 팀원, CONSIDERATE=배려심 있는 팀원")
public enum ReviewKeyword {
    LEADERSHIP,
    GOOD_COMMUNICATOR,
    CREATIVE,
    PROBLEM_SOLVER,
    TRUSTWORTHY,
    PROACTIVE,
    CONSIDERATE
}
