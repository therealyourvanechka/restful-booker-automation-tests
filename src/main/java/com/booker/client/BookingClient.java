package com.booker.client;

import com.booker.model.request.BookingRequest;
import com.booker.model.response.BookingIdResponse;
import com.booker.model.response.CreateBookingResponse;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import java.util.List;
import java.util.Map;

public class BookingClient extends BaseClient {

    public BookingClient() {
        super();
    }

    public BookingClient(String token) {
        super(token);
    }

    public BookingClient(String username, String password) {
        super(username, password);
    }

    @Step("Создание брони POST /booking")
    public CreateBookingResponse createBooking(BookingRequest booking) {
        return post("/booking", booking)
                .then().statusCode(HttpStatus.SC_OK)
                .extract().as(CreateBookingResponse.class);
    }

    @Step("Отправка сырого POST /booking")
    public Response createBookingRaw(Object booking) {
        return post("/booking", booking);
    }

    @Step("Поиск бронирований с фильтрами GET /booking")
    public List<BookingIdResponse> getBookingsWithFilters(Map<String, Object> queryParams) {
        return get("/booking", queryParams)
                .then().statusCode(HttpStatus.SC_OK)
                .extract().body()
                .jsonPath().getList(".", BookingIdResponse.class);
    }

    @Step("Отправка сырого GET /booking")
    public Response getBookingsRaw() {
        return get("/booking");
    }

    @Step("Отправка сырого GET /booking с фильтрами")
    public Response getBookingsRaw(Map<String, Object> queryParams) {
        return get("/booking", queryParams);
    }

    @Step("Отправка сырого GET /booking/{id}")
    public Response getBookingRaw(Object id) {
        return getById("/booking", id);
    }

    @Step("Полное обновление брони PUT /booking/{id}")
    public Response updateBooking(int id, Object body) {
        return put("/booking", id, body);
    }

    @Step("Частичное обновление брони PATCH /booking/{id}")
    public Response partialUpdateBooking(int id, Object body) {
        return patch("/booking", id, body);
    }

    @Step("Удаление брони DELETE /booking/{id}")
    public Response deleteBooking(int id) {
        return delete("/booking", id);
    }
}
