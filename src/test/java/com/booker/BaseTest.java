package com.booker;

import com.booker.client.AuthClient;
import com.booker.client.BookingClient;
import com.booker.client.HealthCheckClient;
import com.booker.util.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseTest {
    protected static String token;
    protected static BookingClient bookingClient;
    protected static BookingClient authBookingClient;
    protected static BookingClient basicAuthBookingClient;
    protected static AuthClient authClient;
    protected static HealthCheckClient healthCheckClient;

    @BeforeAll
    static void setUpFramework() {
        RestAssured.config = RestAssuredConfig.config()
                .objectMapperConfig(new ObjectMapperConfig()
                        .jackson2ObjectMapperFactory((cls, charset) -> new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));
        RestAssured.filters(
                new AllureRestAssured()
        );

        authClient = new AuthClient();
        bookingClient = new BookingClient();
        healthCheckClient = new HealthCheckClient();

        token = authClient.getToken(Config.get("AUTH_USERNAME"), Config.get("AUTH_PASSWORD"));
        authBookingClient = new BookingClient(token);
        basicAuthBookingClient = new BookingClient(Config.get("AUTH_USERNAME"), Config.get("AUTH_PASSWORD"));
    }
}
