package reproduce.controller;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.NettyPipeline;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;

import java.net.URI;
import java.net.URISyntaxException;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setAlreadyRouted;

@RestController
@RequestMapping("/abc")
public class TestController {

    private HttpClient httpClient = HttpClient.create();


    @RequestMapping(method = RequestMethod.POST, path = "/d")
    public Mono<String> initialEndpoint(ServerHttpRequest request, ServerHttpResponse response) {
        DefaultServerWebExchange exchange = new DefaultServerWebExchange(request, response, new
                DefaultWebSessionManager(),
                ServerCodecConfigurer.create(), new AcceptHeaderLocaleContextResolver());

        return filter(exchange).map(x -> x.status().toString());
    }

    //NettyRoutingFilter - extracted
    private Mono<HttpClientResponse> filter(ServerWebExchange exchange)  {

        URI requestUrl = getRequestUrl();

        String scheme = requestUrl.getScheme();
        if (isAlreadyRouted(exchange) || (!scheme.equals("http") && !scheme.equals("https"))) {
            return Mono.empty();
        }
        setAlreadyRouted(exchange);

        ServerHttpRequest request = exchange.getRequest();

        final HttpMethod method = HttpMethod.valueOf(request.getMethod().toString());
        final String url = requestUrl.toString();

        final DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
        request.getHeaders().forEach(httpHeaders::set);
        String transferEncoding = request.getHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING);
        boolean chunkedTransfer = "chunked".equalsIgnoreCase(transferEncoding);



        return this.httpClient.request(method, url, req -> {
            final HttpClientRequest proxyRequest = req.options(NettyPipeline.SendOptions::flushOnEach)
                    .headers(httpHeaders)
                    .chunkedTransfer(chunkedTransfer)
                    .failOnClientError(false);


            return proxyRequest.sendHeaders() //I shouldn't need this
                    .send(request.getBody()
                            .map(DataBuffer::asByteBuffer)
                            .map(Unpooled::wrappedBuffer));
        }).doOnNext(res -> {
                    exchange.getResponse().setStatusCode(HttpStatus.valueOf(res.status().code()));
                    return;
        });

    }


    private URI getRequestUrl() {
        try {
            return new URI("http://localhost:8080/partners/verification/219101aa-50d1-4114-b46c-f4bc0c3fdc15" +
                    "/verification");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

}
