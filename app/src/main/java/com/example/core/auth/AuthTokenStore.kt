package com.example.core.auth

/**
 * What the network layer needs from the session store in order to renew an expired access token.
 * [SessionManager] is the production implementation; tests supply a fake.
 */
interface AuthTokenStore {
    /** The stored access token, regardless of the local session window. Null when signed out. */
    suspend fun currentAccessToken(): String?

    /** The stored refresh token. Null when signed out. */
    suspend fun currentRefreshToken(): String?

    /**
     * Saves the tokens returned by POST auth/refresh. [refreshToken] is null when the server did
     * not rotate it, in which case the existing refresh token is kept.
     */
    suspend fun saveRefreshedTokens(accessToken: String, refreshToken: String?)

    /**
     * The server rejected the refresh token: forget the tokens and the signed-in identity so the
     * app returns to Login. Records stored on the device are NOT touched.
     */
    suspend fun expireSession()
}
