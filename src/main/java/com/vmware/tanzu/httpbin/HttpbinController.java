package com.vmware.tanzu.httpbin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;

@RestController
public class HttpbinController {

    private final ObjectMapper objectMapper;

    public HttpbinController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/status/{code}")
    public ResponseEntity<Void> status(@PathVariable String code){
        var httpStatus = HttpStatus.resolve(Integer.parseInt(code));
        return new ResponseEntity<>(httpStatus);
    }

    @GetMapping(path = "/headers", produces = APPLICATION_JSON)
    public Mono<String> headers(ServerWebExchange exchange) throws JsonProcessingException {
        Map<String, String> headerMap = exchange
                .getRequest()
                .getHeaders()
                .toSingleValueMap();
        return Mono.just(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(headerMap));
    }

    @GetMapping(path = "/ip", produces = APPLICATION_JSON)
    public Mono<String> ip(ServerWebExchange exchange)  {
        String remoteAddress;
        if(exchange.getRequest().getLocalAddress().getHostString().equals("0:0:0:0:0:0:0:1")) {
            remoteAddress = "127.0.0.1";
        }
        else {
            remoteAddress = exchange.getRequest().getLocalAddress().getHostString();
        }
        return Mono.just(objectMapper.createObjectNode().put("origin", remoteAddress).toString());
    }
}
