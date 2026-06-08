package com.booker.util;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public class Specifications {
    
    public static RequestSpecification getRequestSpec() {
        return new RequestSpecBuilder()
            .setBaseUri(Config.get("BASE_URL"))
            .setContentType(ContentType.JSON)
            .addHeader("Accept", "application/json")
            .build();
    }
}
