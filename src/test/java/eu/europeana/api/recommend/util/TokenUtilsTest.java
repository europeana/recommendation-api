package eu.europeana.api.recommend.util;

import eu.europeana.api.recommend.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TokenUtilsTest {

    private static final String BEARER_KEYWORD = "Bearer ";
    private static final String TOKEN_DUMMY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huZG9lIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJhenAiOiJhcGlrZXkifQ.tYbSlYk9K5NVQU74Jf-C0zmQscB6lm_Uj3QdKwGZjHY";
    private static final String TOKEN_NO_APIKEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huZG9lIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.Ks_BdfH4CWilyzLNk8S2gDARFhuxIauLa8PwhdEQhEo";
    private static final String TOKEN_NO_USERID = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjIsImF6cCI6ImFwaWtleSJ9.cTrWdsgHawiMlbYjD2spiCx6f6nrCn8GssXUFFwuUcw";

    @Test
    public void testGetUserId() throws InvalidTokenException {
        assertEquals("johndoe", TokenUtils.getUserId(BEARER_KEYWORD + TOKEN_DUMMY));

        assertThrows(InvalidTokenException.class, () -> TokenUtils.getUserId( BEARER_KEYWORD + TOKEN_NO_USERID));
    }

    @Test
    public void testGetApiKey() throws InvalidTokenException {
        assertEquals("apikey", TokenUtils.getApiKey(BEARER_KEYWORD + TOKEN_DUMMY));

        assertThrows(InvalidTokenException.class, () -> TokenUtils.getApiKey(BEARER_KEYWORD + TOKEN_NO_APIKEY));
    }

    @Test
    public void testNoBearer() throws InvalidTokenException {
        assertThrows(InvalidTokenException.class, () -> TokenUtils.getApiKey(TOKEN_DUMMY));
    }
}
