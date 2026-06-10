package com.booker.controllers.booking;

import com.booker.BaseTest;
import com.booker.model.request.BookingRequest;
import com.booker.util.BookingDataFactory;
import org.junit.jupiter.api.AfterEach;

abstract class BaseBookingControllerTest extends BaseTest {

    protected static final int NON_EXISTENT_ID = (int) (System.currentTimeMillis() / 1000);

    protected int createdBookingId = 0;

    protected BookingRequest createTestBooking() {
        BookingRequest request = BookingDataFactory.defaultBooking().build();
        createdBookingId = bookingClient.createBooking(request).getBookingid();
        return request;
    }

    @AfterEach
    void tearDown() {
        if (createdBookingId != 0) {
            authBookingClient.deleteBooking(createdBookingId);
            createdBookingId = 0;
        }
    }
}
