package no.hvl.dat250.lms.content.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;

/** A left-to-right association; pair collection order has no grading significance. */
@Embeddable
public class QuizMatch {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "left_item_id", nullable = false)
    private QuizItem leftItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "right_item_id", nullable = false)
    private QuizItem rightItem;

    protected QuizMatch() {
    }

    public QuizMatch(QuizItem leftItem, QuizItem rightItem) {
        if (leftItem == null || rightItem == null
                || leftItem.getKind() != QuizItemKind.MATCHING_LEFT
                || rightItem.getKind() != QuizItemKind.MATCHING_RIGHT
                || !Objects.equals(leftItem.getQuestion().getId(), rightItem.getQuestion().getId())) {
            throw new IllegalArgumentException("A match must join a left and right item from the same question");
        }
        this.leftItem = leftItem;
        this.rightItem = rightItem;
    }

    public QuizItem getLeftItem() { return leftItem; }
    public QuizItem getRightItem() { return rightItem; }

    /** Used by both the question key and student answer to enforce full one-to-one coverage. */
    public static void validateComplete(List<QuizItem> items, List<QuizMatch> matches) {
        if (items == null || matches == null || matches.size() < 2 || matches.size() > 20) {
            throw new IllegalArgumentException("Matching requires 2–20 complete pairs");
        }
        Set<UUID> leftIds = new HashSet<>();
        Set<UUID> rightIds = new HashSet<>();
        for (QuizItem item : items) {
            if (item == null || item.getId() == null) {
                throw new IllegalArgumentException("Matching items must be persisted");
            }
            Set<UUID> side;
            if (item.getKind() == QuizItemKind.MATCHING_LEFT) {
                side = leftIds;
            } else if (item.getKind() == QuizItemKind.MATCHING_RIGHT) {
                side = rightIds;
            } else {
                throw new IllegalArgumentException("Matching cannot use ordering items");
            }
            if (!side.add(item.getId())) {
                throw new IllegalArgumentException("Matching items must be distinct");
            }
        }
        Set<UUID> usedLeft = new HashSet<>();
        Set<UUID> usedRight = new HashSet<>();
        for (QuizMatch match : matches) {
            if (match == null || !usedLeft.add(match.leftItem.getId()) || !usedRight.add(match.rightItem.getId())) {
                throw new IllegalArgumentException("Each matching item must be used exactly once");
            }
        }
        if (leftIds.size() != matches.size() || rightIds.size() != matches.size()
                || !leftIds.equals(usedLeft) || !rightIds.equals(usedRight)) {
            throw new IllegalArgumentException("Matches must cover both sides completely");
        }
    }
}
