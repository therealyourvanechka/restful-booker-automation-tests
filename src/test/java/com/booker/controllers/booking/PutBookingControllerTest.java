package com.booker.controllers.booking;

import com.booker.model.request.BookingRequest;
import com.booker.model.response.BookingResponse;
import com.booker.util.BookingDataFactory;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Epic("Restful Booker API")
@Feature("Booking")
class PutBookingControllerTest extends BaseBookingControllerTest {

    @Test
    @Tag("positive")
    @Story("Обновление брони")
    @DisplayName("PUT /booking/{id} — полное обновление с токеном")
    void shouldUpdateBookingWithToken() {
        createTestBooking();

        BookingRequest updateRequest = BookingDataFactory.defaultBooking()
                .firstname(BookingDataFactory.randomFirstName())
                .totalprice(BookingDataFactory.randomPrice())
                .build();

        Response response = authBookingClient.updateBooking(createdBookingId, updateRequest);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
        response.then().body(matchesJsonSchemaInClasspath("schemas/booking-schema.json"));

        BookingResponse updated = response.as(BookingResponse.class);
        assertSoftly(softly -> {
            softly.assertThat(updated.getFirstname()).as("Имя после обновления").isEqualTo(updateRequest.getFirstname());
            softly.assertThat(updated.getTotalprice()).as("Цена после обновления").isEqualTo(updateRequest.getTotalprice());
        });
    }

    @Test
    @Tag("positive")
    @Story("Обновление брони")
    @DisplayName("PUT /booking/{id} — полное обновление с Basic Auth")
    void shouldUpdateBookingWithBasicAuth() {
        createTestBooking();

        BookingRequest updateRequest = BookingDataFactory.defaultBooking()
                .firstname(BookingDataFactory.randomFirstName())
                .totalprice(BookingDataFactory.randomPrice())
                .build();

        Response response = basicAuthBookingClient.updateBooking(createdBookingId, updateRequest);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
        response.then().body(matchesJsonSchemaInClasspath("schemas/booking-schema.json"));

        BookingResponse updated = response.as(BookingResponse.class);
        assertSoftly(softly -> {
            softly.assertThat(updated.getFirstname()).as("Имя").isEqualTo(updateRequest.getFirstname());
            softly.assertThat(updated.getTotalprice()).as("Цена").isEqualTo(updateRequest.getTotalprice());
        });
    }

    @Test
    @Tag("negative")
    @Story("Обновление брони")
    @DisplayName("PUT /booking/{id} — без авторизации возвращает 403")
    void shouldNotUpdateBookingWithoutToken() {
        BookingRequest request = createTestBooking();

        Response response = bookingClient.updateBooking(createdBookingId, request);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @Tag("negative")
    @Story("Обновление брони")
    @DisplayName("PUT /booking/{id} — неполный payload, ожидается 400")
    void shouldNotUpdateBookingWithPartialPayload() {
        createTestBooking();

        Map<String, Object> partialPayload = new HashMap<>();
        partialPayload.put("firstname", BookingDataFactory.randomFirstName());

        Response response = authBookingClient.updateBooking(createdBookingId, partialPayload);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }
}
