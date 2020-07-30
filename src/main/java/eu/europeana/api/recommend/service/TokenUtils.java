package eu.europeana.api.recommend.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import eu.europeana.api.recommend.exception.InvalidTokenException;
import io.micrometer.core.instrument.util.StringUtils;

import java.util.Locale;

/**
 * Utility class for retrieving the API key from a token
 *
 * @author Patrick Ehlert
 * Created on 29 Jul 2020
 */
public class TokenUtils {

    private static final String API_KEY_CLAIM = "azp";

    private TokenUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Decodes a given token. Note that we do not check validity of the token. We leave that to the recommendation engine
     * @param token received token (or authorization header value)
     * @return value of the encoded ApiKey
     * @throws InvalidTokenException if we can't decode the token, or if it doesn't contain an apikey
     */
    public static String getApiKey(String token) throws InvalidTokenException {
        String tkn = token;
        if (token.toLowerCase(Locale.GERMAN).startsWith("bearer ")) {
            tkn = token.substring("Bearer ".length());
        }
        try {
            DecodedJWT jwt = JWT.decode(tkn);
            String result= jwt.getClaim(API_KEY_CLAIM).asString();
            if (StringUtils.isBlank(result)) {
                throw new InvalidTokenException("Token does not contain '" +API_KEY_CLAIM + "' field");
            }
            return result;
        } catch (JWTDecodeException e) {
            throw new InvalidTokenException("Error decoding authorization token", e);
        }
    }

}
