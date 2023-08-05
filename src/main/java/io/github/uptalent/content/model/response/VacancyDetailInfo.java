package io.github.uptalent.content.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class VacancyDetailInfo extends ContentDetailInfo {
    private Set<String> skills;
}
