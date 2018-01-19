package reproduce;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootApplication
public class Application {

    private static final File ONE_MB_FILE = new File(Application.class.getResource("/1mb.zip").getFile());

    public static void main(String[] args) throws UnirestException {

        System.setProperty("io.netty.noUnsafe", "true");
        System.setProperty("io.netty.noPreferDirect", "true");
        System.setProperty("io.netty.leakDetection.level", "paranoid");
        System.setProperty("io.netty.leakDetection.targetRecords", "20");

        SpringApplication.run(Application.class, args);



        List<Integer> range = IntStream
                .rangeClosed(0, 2000)
                .boxed()
                .collect(Collectors.toList());

        Flux
                .fromIterable(range)
                .parallel(3)
                .runOn(Schedulers.parallel())
                .log()
                .map(i -> {
                    try {
                        return executeRequest();
                    } catch (UnirestException e) {
                        return e.getMessage();
                    }
                })
                .log()
                .subscribe();


    }

    private static HttpResponse<JsonNode> executeRequest() throws UnirestException {
        return Unirest.post("http://localhost:8080/abc/d")
                .field("kycZip", ONE_MB_FILE)
                .asJson();
    }

}
