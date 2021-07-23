package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.lojasrenner.rlog.transport.order.business.exception.BrokerBadRequestException;

@RestController
public class HelperController {

    public static final String ENUM_PACKAGE = "br.com.lojasrenner.bfl.broker.infrastructure.api.v1.enums.";

    @CrossOrigin
    @GetMapping(value = "/v1/helper/enum/{name}", produces = { "application/json" })
    public List<String> listEnumValues(@PathVariable("name") String name) {
        Class<?> aClass = null;
        try {
            aClass = Class.forName(ENUM_PACKAGE + name);
        } catch (ClassNotFoundException e) {
            throw new BrokerBadRequestException("Enum not found. Check if you are using the right name.");
        }

        if (!aClass.isEnum())
            throw new BrokerBadRequestException("That class is not an Enum");

        List<String> values = new ArrayList<>();
        for (Object value : aClass.getEnumConstants()) {
            values.add(((Enum)value).name());
        }

        return values;
    }

}
