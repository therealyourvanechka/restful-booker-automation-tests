package com.booker.client;

import io.qameta.allure.Step;
import io.restassured.response.Response;

public class HealthCheckClient extends BaseClient {

    @Step("Отправка запроса HealthCheck (GET /ping)")
    public Response ping() {
        return get("/ping");
    }
}
