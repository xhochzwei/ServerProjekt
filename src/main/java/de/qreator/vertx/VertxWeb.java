
package de.qreator.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

public class VertxWeb {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        // alle Anfragen werden von diesem Handler beantwortet
        router.route().handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");          
            response.end("Hello World from Vert.x-Web!");
        });

        // router::accept akzeptiert eine Anfrage und leitet diese an den Router weiter
        server.requestHandler(router::accept).listen(8080);
    }
}
