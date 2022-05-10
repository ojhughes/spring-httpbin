package com.vmware.tanzu.httpbin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Values.*;

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
        var headerMap = getHeaderMap(exchange);
        return Mono.just(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(headerMap));
    }

    @GetMapping(path = "/ip", produces = APPLICATION_JSON)
    public Mono<String> ip(ServerWebExchange exchange)  {
        return Mono.just(objectMapper.createObjectNode().put("origin", getRemoteAddress(exchange)).toString());
    }

    @GetMapping(path = "/get", produces = APPLICATION_JSON)
    public Mono<String> get(@RequestParam Map<String,String> params, ServerWebExchange exchange) throws JsonProcessingException {
        ObjectNode response = buildResponseJson(objectMapper, params, getHeaderMap(exchange), getRemoteAddress(exchange), exchange);
        return Mono.just(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
    }

    @PostMapping(path = "/post", produces = APPLICATION_JSON, consumes = "application/x-www-form-urlencoded")
    public Mono<String> postForm(@RequestParam Map<String,String> params, ServerWebExchange exchange) throws JsonProcessingException {
        ObjectNode response = buildResponseJson(objectMapper, params, getHeaderMap(exchange), getRemoteAddress(exchange), exchange);
        response.set("data",  objectMapper.createObjectNode());
        response.set("json",  objectMapper.createObjectNode());
        return exchange.getFormData()
               .map(MultiValueMap::toSingleValueMap)
               .map(formData -> response.set("form", objectMapper.valueToTree(formData)).toString());
    }

    @PostMapping(path = "/post", produces = APPLICATION_JSON, consumes = "*/*")
    public Mono<String> postData(@RequestParam Map<String,String> params, @RequestBody String body, ServerWebExchange exchange) throws JsonProcessingException {
        ObjectNode response = buildResponseJson(objectMapper, params, getHeaderMap(exchange), getRemoteAddress(exchange), exchange);
        response.put("data", body);
        response.set("form",  objectMapper.createObjectNode());
        response.set("json",  objectMapper.createObjectNode());
        return Mono.just(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));

    }

    @PostMapping(path = "/post", produces = APPLICATION_JSON, consumes = APPLICATION_JSON)
    public Mono<String> postJson(@RequestParam Map<String,String> params, @RequestBody JsonNode body, ServerWebExchange exchange) throws JsonProcessingException {
        ObjectNode response = buildResponseJson(objectMapper, params, getHeaderMap(exchange), getRemoteAddress(exchange), exchange);
        response.set("json", body);
        response.set("form",  objectMapper.createObjectNode());
        response.set("data",  objectMapper.createObjectNode());
        return Mono.just(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
    }

    private ObjectNode buildResponseJson(ObjectMapper objectMapper, Map<String, String> params, Map<String, String> exchange, String exchange1, ServerWebExchange exchange2) {
        var response = objectMapper.createObjectNode();
        if(!params.isEmpty()){
            response.set("args", objectMapper.valueToTree(params));
        }
        response.set("headers", objectMapper.valueToTree(exchange));
        response.put("origin", exchange1);
        response.put("url", exchange2.getRequest().getURI().toString());
        return response;
    }

    private Map<String, String> getHeaderMap(ServerWebExchange exchange) {
        return exchange
                .getRequest()
                .getHeaders()
                .toSingleValueMap();
    }

    private String getRemoteAddress(ServerWebExchange exchange) {
        String remoteAddress;
        if(exchange.getRequest().getRemoteAddress().getHostString().equals("0:0:0:0:0:0:0:1")) {
            remoteAddress = "127.0.0.1";
        }
        else {
            remoteAddress = exchange.getRequest().getLocalAddress().getHostString();
        }
        return remoteAddress;
    }
}
