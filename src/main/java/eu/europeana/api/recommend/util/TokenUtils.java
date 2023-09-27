package eu.europeana.api.recommend.util;

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
public final class TokenUtils {

    private static final String API_KEY_CLAIM = "azp";
    private static final String USER_CLAIM    = "sub";

    private TokenUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * returns the encoded apikey from the token
     * @param authorizationHeader received authorization header value (including Bearer keyword)
     * @return value of the encoded ApiKey
     * @throws InvalidTokenException if we can't decode the token, or if it doesn't contain an apikey
     */
    public static String getApiKey(String authorizationHeader) throws InvalidTokenException {
        return getClaimFromToken(getTokenFromHeader(authorizationHeader), API_KEY_CLAIM);
    }

    /**
     * Returns the encoded user Id from the token
     * @param authorizationHeader received authorization header value (including Bearer keyword)
     * @return value of the encoded user
     * @throws InvalidTokenException if we can't decode the token, or if it doesn't contain an apikey
     */
    public static String getUserId(String authorizationHeader) throws InvalidTokenException{
        return getClaimFromToken(getTokenFromHeader(authorizationHeader), USER_CLAIM);
    }


    /**
     * Decodes a given token. Note that we do not check validity of the token. We leave that to the recommendation engine
     * returns the claim from the provided token
     * @param token Substring after 'Bearer' in the received token (or authorization header value)
     * @param claim claim to be fetched
     * @return value of encoded claim
     * @throws InvalidTokenException
     */
    private static String getClaimFromToken(String token, String claim) throws InvalidTokenException {
        try {
            DecodedJWT jwt = JWT.decode(token);
            String result= jwt.getClaim(claim).asString();
            if (StringUtils.isBlank(result)) {
                throw new InvalidTokenException("Token does not contain '" +claim + "' field");
            }
            return result;
        } catch (JWTDecodeException e) {
            throw new InvalidTokenException("Error decoding authorization token", e);
        }
    }

    /**
     * returns the substring after the Bearer from the received token (or authorization header value)
     * @param authHeaderValue the value of the authorization header
     * @return
     * @throws InvalidTokenException
     */
    private static String getTokenFromHeader(String authHeaderValue) throws InvalidTokenException {
        if (authHeaderValue.toLowerCase(Locale.GERMAN).startsWith("bearer ")) {
            return authHeaderValue.substring("Bearer ".length());
        }
        throw new InvalidTokenException("Invalid authorization header provided");
    }

}
