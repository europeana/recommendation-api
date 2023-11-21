package eu.europeana.api.recommend.service;

import eu.europeana.api.recommend.SearchApiMockResponse;
import eu.europeana.api.recommend.common.RecordId;
import eu.europeana.api.recommend.common.model.EmbeddingResponse;
import eu.europeana.api.recommend.common.model.RecordVectors;
import eu.europeana.api.recommend.exception.EntityNotFoundException;
import eu.europeana.api.recommend.exception.RecommendException;
import eu.europeana.api.recommend.exception.RecordNotFoundException;
import eu.europeana.api.recommend.exception.SetNotFoundException;
import eu.europeana.api.recommend.model.Set;
import eu.europeana.api.recommend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { RecommendService.class })
public class RecommendServiceTest {

    private static final RecordId RECORD_ID1 = new RecordId("a", "1");
    private static final Float[] VECTOR1 = new Float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
    private static final Recommendation RECOMMENDATION1 = new Recommendation(RECORD_ID1, 0.5f);
    private static final Map<String, Recommendation> RECOMMEND_MAP = Map.of(RECORD_ID1.toString(), RECOMMENDATION1);

    private static final EmbeddingsApiMockResponse EMBED_RESPONSE = new EmbeddingsApiMockResponse(new RecordVectors[]{new RecordVectors("1", VECTOR1)}, "200");


    private static final String SET_ID = "1234";
    private static final Set SET1 = new Set(SET_ID, Map.of("en", "my title"), Map.of("en", "my description"), null, null);

    private static final String ENTITY_TYPE = "agent";
    private static final int ENTITY_ID = 7;
    private static final Entity ENTITY1 = new Entity(ENTITY_TYPE, String.valueOf(ENTITY_ID), Map.of("en", "my title"), Map.of("en", List.of("my description")));
    private static final SetSearch ENTITY_SET_EXISTS = new SetSearchMockResponse(1, new Set[]{SET1});
    private static final SetSearch ENTITY_SET_NOT_EXISTS = new SetSearchMockResponse(0, null);

    @MockBean
    MilvusService milvusService;
    @MockBean
    EmbeddingsService embeddingsService;
    @MockBean
    SearchApiService searchApiService;
    @MockBean
    SetApiService setApiService;
    @MockBean
    EntityApiService entityApiService;

    @Autowired
    RecommendService recommendService;

    @BeforeEach
    public void init()  {
        mockMilvus();
        mockSearchApi();
        mockSetApi();
        mockEntityApi();
    }


    private static class EmbeddingsApiMockResponse extends EmbeddingResponse {
        public EmbeddingsApiMockResponse(RecordVectors[] data, String status) {
            this.data = data;
            this.status = status;
        }
    }

    private static class SetSearchMockResponse extends SetSearch {
        public SetSearchMockResponse(int total, Set[] items) {
            this.total = total;
            this.items = items;
        }
    }

    private void mockMilvus() {
        List<Float> vector = Arrays.stream(VECTOR1).toList();
        when(milvusService.getVectorForRecord(any())).thenReturn(Collections.emptyList()); // default return empty list;
        when(milvusService.getVectorForRecord(RECORD_ID1)).thenReturn(vector);

        when(milvusService.getSimilarRecords(any(), anyInt(), any(), anyInt())).thenReturn(Collections.emptyMap());
        when(milvusService.getSimilarRecords(eq(List.of(vector)), anyInt(), any(), anyInt())).thenReturn(RECOMMEND_MAP);
    }

    private void mockSearchApi() {
        when(searchApiService.checkRecordExists(any(), any(), any())).thenReturn(false);
        when(searchApiService.checkRecordExists(eq(RECORD_ID1), any(), any())).thenReturn(true);

        when(searchApiService.generateResponse(any(), anyInt(), any(), any())).thenReturn(Mono.just(new SearchApiResponse("test")));
        when(searchApiService.generateResponse(eq(List.of(RECOMMENDATION1)), anyInt(), any(), any())).thenReturn(Mono.just(new SearchApiMockResponse("test", 1, 1)));
    }

    private void mockSetApi() {
        when(setApiService.getSetData(any(), any(), any())).thenAnswer( arguments -> {
            if (arguments.getArgument(0).equals(SET_ID)) {
                return Mono.just(SET1);
            } else {
                throw new RuntimeException("Set not found");
            }
        });

        when(setApiService.getSetDataForEntity(any(), any(), any())).thenReturn(Mono.just(ENTITY_SET_NOT_EXISTS));
        when(setApiService.getSetDataForEntity(eq("http://data.europeana.eu/agent/7"), any(), any())).thenReturn(Mono.just(ENTITY_SET_EXISTS));
    }

    private void mockEntityApi() {
        when(entityApiService.getEntity(any(), anyInt(), any(), any())).thenAnswer( arguments -> {
            if (arguments.getArgument(0).equals(ENTITY_TYPE) &&
                    arguments.getArgument(1).equals(ENTITY_ID) ) {
                return Mono.just(ENTITY1);
            } else {
                throw new RuntimeException("Entity not found");
            }
        });
    }

    @Test
    public void mergeAndSortRecommendationsTest() {
        Recommendation r1 = new Recommendation(new RecordId("a", "1"), 0.5f);
        Recommendation r2 = new Recommendation(new RecordId("a", "2"), 0.2f);
        Recommendation r3 = new Recommendation(new RecordId("a", "3"), 0.1f);

        Recommendation r4 = new Recommendation(new RecordId("a", "2"),  0.6f);
        Recommendation r5 = new Recommendation(new RecordId("a", "1"),  0.2f);

        // wrap it in HashMap because we need mutable maps
        Map<String, Recommendation> map1 = new HashMap<>(Map.of(r1.getRecordId().getMilvusId(), r1,
                r2.getRecordId().getMilvusId(), r2,
                r3.getRecordId().getMilvusId(), r3));
        Map<String, Recommendation> map2 = new HashMap<>(Map.of(r4.getRecordId().getMilvusId(), r4,
                r5.getRecordId().getMilvusId(), r5));

        List<Recommendation> merged = recommendService.mergeAndSortRecommendations(map1, map2);
        assertEquals(3, merged.size());
        assertEquals(new Recommendation(new RecordId("a", "2"), 0.8f), merged.get(0));
        assertEquals(new Recommendation(new RecordId("a", "1"), 0.7f), merged.get(1));
        assertEquals(new Recommendation(new RecordId("a", "3"), 0.1f), merged.get(2));
    }

    @Test
    public void testRecordRecommendations() throws RecommendException {
        SearchApiResponse response = recommendService.getRecommendationsForRecord(
                RECORD_ID1, 10, 1, null, "test", null).block();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getItemsCount());
    }

    @Test
    public void testRecordRecommendationsNotFound() {
        assertThrows(RecordNotFoundException.class, () -> recommendService.getRecommendationsForRecord(
                new RecordId("a", "notfound"), 10, 1, null, "test", null));
    }

    @Test
    public void testSetRecommendations() throws RecommendException {
        // Mock Embeddings API to return result
        when(embeddingsService.getVectorForSet(SET1)).thenReturn(Mono.just(EMBED_RESPONSE));
        SearchApiResponse response = recommendService.getRecommendationsForSet(
                SET_ID, 10, 1, null, "test", null).block();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getItemsCount());
    }

    @Test
    public void testSetRecommendationsFailingEmbeddings() throws RecommendException {
        // Mock Embeddings API to fail
        when(embeddingsService.getVectorForSet(SET1)).thenThrow(new RuntimeException("Error accessing Embeddings API"));
        SearchApiResponse response = recommendService.getRecommendationsForSet(
                SET_ID, 10, 1, null, "test", null).block();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(0, response.getItemsCount());
    }

    @Test
    public void testSetRecommendationsNotFound() {
        assertThrows(SetNotFoundException.class, () -> recommendService.getRecommendationsForSet(
                "0000", 10, 1, null, "test", null));
    }

    @Test
    public void testEntityRecommendations() throws RecommendException {
        // Mock Embeddings API to return results
        when(embeddingsService.getVectorForEntity(ENTITY1)).thenReturn(Mono.just(EMBED_RESPONSE));
        SearchApiResponse response = recommendService.getRecommendationsForEntity(
               "agent", 7, 1, "test", null).block();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getItemsCount());
    }

    @Test
    public void testEntityRecommendationsFailingEmbeddings() throws RecommendException {
        // Mock Embeddings API to fail
        when(embeddingsService.getVectorForEntity(ENTITY1)).thenThrow(new RuntimeException("Error accessing Embeddings API"));
        SearchApiResponse response = recommendService.getRecommendationsForEntity(
                "agent", 7, 1, "test", null).block();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(0, response.getItemsCount());
    }

    @Test
    public void testEntityRecommendationsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> recommendService.getRecommendationsForEntity(
                "concept", 999, 1, "test", null).block());
    }
}
