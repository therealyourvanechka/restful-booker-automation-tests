package com.booker.controllers;

import com.booker.BaseTest;
import com.booker.model.request.TokenRequest;
import com.booker.model.response.TokenResponse;
import com.booker.util.Config;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Restful Booker API")
@Feature("Authorization")
class AuthControllerTest extends BaseTest {

    private static final String INVALID_PASSWORD = "wrongpassword";
    private static final String EMPTY_JSON = "{}";

    @Test
    @Tag("positive")
    @Story("Успешная авторизация")
    @DisplayName("POST /auth — создание токена с валидными кредами")
    void shouldCreateTokenWithValidCredentials() {
        TokenRequest request = new TokenRequest(Config.get("AUTH_USERNAME"), Config.get("AUTH_PASSWORD"));

        Response response = authClient.createToken(request);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
        response.then().body(matchesJsonSchemaInClasspath("schemas/token-success-schema.json"));

        TokenResponse tokenResp = response.as(TokenResponse.class);
        assertThat(tokenResp.getToken()).isNotEmpty();
    }

    @Test
    @Tag("negative")
    @Story("Неуспешная авторизация")
    @DisplayName("POST /auth — невалидные креды возвращают reason Bad credentials")
    void shouldNotCreateTokenWithInvalidCredentials() {
        TokenRequest request = new TokenRequest(Config.get("AUTH_USERNAME"), INVALID_PASSWORD);

        Response response = authClient.createToken(request);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
        response.then().body(matchesJsonSchemaInClasspath("schemas/token-error-schema.json"));

        TokenResponse tokenResp = response.as(TokenResponse.class);
        assertThat(tokenResp.getReason()).isEqualTo("Bad credentials");
        assertThat(tokenResp.getToken()).isNull();
    }

    @Test
    @Tag("negative")
    @Story("Неуспешная авторизация")
    @DisplayName("POST /auth — пустое тело возвращает reason Bad credentials")
    void shouldNotCreateTokenWithEmptyBody() {
        Response response = authClient.createToken(EMPTY_JSON);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
        response.then().body(matchesJsonSchemaInClasspath("schemas/token-error-schema.json"));

        TokenResponse tokenResp = response.as(TokenResponse.class);
        assertThat(tokenResp.getReason()).isEqualTo("Bad credentials");
        assertThat(tokenResp.getToken()).isNull();
    }
}