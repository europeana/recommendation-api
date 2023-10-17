package eu.europeana.api.recommend.model;

import eu.europeana.api.recommend.common.RecordId;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Recommended item, containing recordId and similarity score.
 * Similarity score is a floating point number. Milvus returns scores from 0 to 1 (0 not relevant, 1 most relevant/equal to)
 * but we can modify with a weight factor so score can become larger than 1
 * @author Patrick Ehlert
 */
public class Recommendation implements Comparable<Recommendation>, Serializable {

    @Serial
    private static final long serialVersionUID = 8682997926513140944L;

    private RecordId recordId;
    private float score;

    /**
     * Initialize a new Recommendation
     * @param recordId the id of the recommended item
     * @param score floating point number from 0-1 indicating similarity (1 = most similar, 0 = least similar)
     */
    public Recommendation(@NotNull RecordId recordId, float score) {
        this.recordId = recordId;
        this.score = score;
    }

    public RecordId getRecordId() {
        return recordId;
    }

    public float getScore() {
        return score;
    }

    /**
     * Merges two recommendations by updating the score of this recommendation
     * @param toMerge the other recommendation to merge into this recommendation
     */
    public void merge(Recommendation toMerge) {
        if (!this.recordId.equals(toMerge.recordId)) {
            throw new IllegalStateException("Recommendation {} cannot be merged with recommendation {} because they have different ids");
        }
        this.score = this.score + toMerge.score;
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "recordId=" + recordId +
                ", score=" + score +
                '}';
    }

    @Override
    public int compareTo(@NotNull Recommendation recommendation) {
        return Float.compare(this.score, recommendation.score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Recommendation that = (Recommendation) o;
        return Float.compare(that.score, score) == 0 && recordId.equals(that.recordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId, score);
    }
}
