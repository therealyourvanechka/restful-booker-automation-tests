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
class PatchBookingControllerTest extends BaseBookingControllerTest {

    @Test
    @Tag("positive")
    @Story("Частичное обновление")
    @DisplayName("PATCH /booking/{id} — обновлённое поле изменилось, остальные не тронуты")
    void shouldPartiallyUpdateBooking() {
        BookingRequest request = createTestBooking();

        int newPrice = BookingDataFactory.randomPrice();
        Map<String, Object> partialUpdate = new HashMap<>();
        partialUpdate.put("totalprice", newPrice);

        Response response = authBookingClient.partialUpdateBooking(createdBookingId, partialUpdate);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
        response.then().body(matchesJsonSchemaInClasspath("schemas/booking-schema.json"));

        BookingResponse updated = response.as(BookingResponse.class);
        assertSoftly(softly -> {
            softly.assertThat(updated.getTotalprice()).as("Цена обновилась").isEqualTo(newPrice);
            softly.assertThat(updated.getFirstname()).as("Имя не изменилось").isEqualTo(request.getFirstname());
            softly.assertThat(updated.getLastname()).as("Фамилия не изменилась").isEqualTo(request.getLastname());
        });
    }

    @Test
    @Tag("negative")
    @Story("Частичное обновление")
    @DisplayName("PATCH /booking/{id} — без авторизации возвращает 403")
    void shouldNotPartiallyUpdateBookingWithoutToken() {
        createTestBooking();

        Map<String, Object> partialUpdate = new HashMap<>();
        partialUpdate.put("totalprice", BookingDataFactory.randomPrice());

        Response response = bookingClient.partialUpdateBooking(createdBookingId, partialUpdate);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }
}
