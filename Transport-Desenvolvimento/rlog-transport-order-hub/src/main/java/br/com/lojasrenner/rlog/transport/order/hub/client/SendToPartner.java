package br.com.lojasrenner.rlog.transport.order.hub.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "sendToPartner", url = "https://testetadeu.requestcatcher.com/test")
public interface SendToPartner {


    @PostMapping
    String sendPartnerMethod(@RequestBody Object body);
}
