package com.booker.client;

import com.booker.exception.AuthenticationException;
import com.booker.model.request.TokenRequest;
import com.booker.model.response.TokenResponse;
import io.qameta.allure.Step;
import io.restassured.response.Response;

public class AuthClient extends BaseClient {

    @Step("Получение токена для пользователя")
    public String getToken(String username, String password) {
        TokenRequest request = new TokenRequest(username, password);
        TokenResponse response = createToken(request).as(TokenResponse.class);
        if (response.getToken() == null || response.getToken().isBlank()) {
            throw new AuthenticationException(
                    "Failed to get token: " + (response.getReason() != null ? response.getReason() : "Unknown error"));
        }
        return response.getToken();
    }

    @Step("Запрос создания токена POST /auth")
    public Response createToken(Object body) {
        return post("/auth", body);
    }
}
