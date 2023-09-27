package eu.europeana.api.recommend.model;

import eu.europeana.api.recommend.common.RecordId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RecommendationTest {

    @Test
    public void testMerge() {
        Recommendation rec1 = new Recommendation(new RecordId("a", "b"), 2);
        Recommendation rec2 = new Recommendation(new RecordId("a", "b"), 3.5f);
        rec1.merge(rec2);
        assertEquals("/a/b", rec1.getRecordId().getEuropeanaId());
        assertEquals(5.5, rec1.getScore());

        Recommendation rec3 = new Recommendation(new RecordId("a", "c"), 1);
        assertThrows(IllegalStateException.class, () -> rec1.merge(rec3));
        assertEquals("a/b", rec1.getRecordId().getMilvusId());
        assertEquals(5.5, rec1.getScore());
    }
}
