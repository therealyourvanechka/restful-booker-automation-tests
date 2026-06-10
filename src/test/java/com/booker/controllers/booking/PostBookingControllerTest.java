package com.booker.controllers.booking;

import com.booker.model.request.BookingRequest;
import com.booker.model.response.CreateBookingResponse;
import com.booker.util.BookingDataFactory;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Epic("Restful Booker API")
@Feature("Booking")
class PostBookingControllerTest extends BaseBookingControllerTest {

    @Test
    @Tag("positive")
    @Story("Создание брони")
    @DisplayName("POST /booking — успешное создание, данные в ответе совпадают с запросом")
    void shouldCreateBookingWithAllFields() {
        BookingRequest request = BookingDataFactory.defaultBooking().build();

        Response rawResponse = bookingClient.createBookingRaw(request);
        assertThat(rawResponse.statusCode()).isEqualTo(HttpStatus.SC_OK);
        rawResponse.then().body(matchesJsonSchemaInClasspath("schemas/create-booking-schema.json"));

        CreateBookingResponse response = rawResponse.as(CreateBookingResponse.class);
        createdBookingId = response.getBookingid();

        assertSoftly(softly -> {
            softly.assertThat(response.getBooking().getFirstname()).as("Имя").isEqualTo(request.getFirstname());
            softly.assertThat(response.getBooking().getLastname()).as("Фамилия").isEqualTo(request.getLastname());
            softly.assertThat(response.getBooking().getTotalprice()).as("Цена").isEqualTo(request.getTotalprice());
            softly.assertThat(response.getBooking().getDepositpaid()).as("Депозит").isEqualTo(request.getDepositpaid());
            softly.assertThat(response.getBooking().getBookingdates().getCheckin()).as("Дата заезда").isEqualTo(request.getBookingdates().getCheckin());
            softly.assertThat(response.getBooking().getBookingdates().getCheckout()).as("Дата выезда").isEqualTo(request.getBookingdates().getCheckout());
            softly.assertThat(response.getBooking().getAdditionalneeds()).as("Доп. пожелания").isEqualTo(request.getAdditionalneeds());
        });

        Response getResponse = bookingClient.getBookingRaw(createdBookingId);
        assertThat(getResponse.statusCode())
                .as("Бронь доступна по GET после создания")
                .isEqualTo(HttpStatus.SC_OK);
    }

    // @Disabled("Баг API: возвращает 500 вместо 400 при отсутствии обязательного поля firstname")
    @Test
    @Tag("negative")
    @Story("Создание брони")
    @DisplayName("POST /booking — отсутствует обязательное поле firstname, ожидается 400")
    void shouldReturn400WhenFirstnameMissing() {
        BookingRequest request = BookingDataFactory.defaultBooking()
                .firstname(null)
                .build();

        Response response = bookingClient.createBookingRaw(request);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }
}
