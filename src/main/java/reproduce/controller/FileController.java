package reproduce.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/partners")
public class FileController {


    @PostMapping("/verification/{id}/verification")
    public void verification(ServerHttpRequest request, ServerHttpResponse response)
            throws IOException {
        request.getBody().subscribe();
        response.setStatusCode(HttpStatus.NO_CONTENT);
    }

}

