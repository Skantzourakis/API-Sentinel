package com.apisentinel.scanner;

import com.apisentinel.entity.FindingSeverity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.apisentinel.entity.FindingSeverity.CRITICAL;
import static com.apisentinel.entity.FindingSeverity.HIGH;
import static com.apisentinel.entity.FindingSeverity.LOW;
import static com.apisentinel.entity.FindingSeverity.MEDIUM;
import static org.assertj.core.api.Assertions.assertThat;

class RiskScoreTest {

    private static List<FindingSeverity> findings(int critical, int high, int medium, int low) {
        List<FindingSeverity> severities = new ArrayList<>();
        severities.addAll(Collections.nCopies(critical, CRITICAL));
        severities.addAll(Collections.nCopies(high, HIGH));
        severities.addAll(Collections.nCopies(medium, MEDIUM));
        severities.addAll(Collections.nCopies(low, LOW));
        return severities;
    }

    @Test
    void noFindingsMeansNoRisk() {
        assertThat(RiskScore.calculate(List.of())).isZero();
    }

    @Test
    void theMostSevereFindingDecidesTheBand() {
        assertThat(RiskScore.calculate(findings(1, 0, 0, 0))).isBetween(75, 100);
        assertThat(RiskScore.calculate(findings(0, 1, 0, 0))).isBetween(50, 74);
        assertThat(RiskScore.calculate(findings(0, 0, 1, 0))).isBetween(25, 49);
        assertThat(RiskScore.calculate(findings(0, 0, 0, 1))).isBetween(5, 24);
    }

    @Test
    void manyLesserFindingsNeverOutrankOneMoreSevereFinding() {
        int manyMedium = RiskScore.calculate(findings(0, 0, 36, 0));
        int oneHigh = RiskScore.calculate(findings(0, 1, 0, 0));

        assertThat(manyMedium).isEqualTo(49);
        assertThat(oneHigh).isGreaterThan(manyMedium);
    }

    @Test
    void moreFindingsRaiseTheScoreWithinTheBand() {
        assertThat(RiskScore.calculate(findings(0, 2, 0, 0))).isGreaterThan(RiskScore.calculate(findings(0, 1, 0, 0)));
        assertThat(RiskScore.calculate(findings(0, 6, 4, 4))).isEqualTo(68);
    }

    @Test
    void sampleSpecificationScoresNinetySix() {
        // 1 critical, 6 high, 3 medium, 4 low: band 75, weight 42, 75 + 21.
        assertThat(RiskScore.calculate(findings(1, 6, 3, 4))).isEqualTo(96);
    }
}
