package com.booker.controllers;

import com.booker.BaseTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Restful Booker API")
@Feature("HealthCheck")
class HealthCheckControllerTest extends BaseTest {

    @Test
    @Tag("positive")
    @Story("Сервер доступен")
    @DisplayName("GET /ping — возвращает 201 Created")
    void shouldReturn201OnPing() {
        Response response = healthCheckClient.ping();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_CREATED);
        assertThat(response.body().asString()).isEqualTo("Created");
    }
}