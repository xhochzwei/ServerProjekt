
package de.qreator.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;


public class VertxWebRouter {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");          
            response.end("Gib in der Adresszeile den Pfad \"daten\" oder \"produkte/Werkzeuge/Hammer\" ein.");
        });

        router.route("/daten").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");          
            response.end("Hier eine Nachricht vom Unterpfad \"/daten\"!");
        });

        router.route("/produkte/:produktTyp/:produktID").handler(routingContext -> {
            String produktTyp=routingContext.request().getParam("produktTyp");
            String produktID=routingContext.request().getParam("produktID");
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");          
            response.end("Die ProduktID ist "+produktID+" und der Produkttyp ist "+produktTyp);
        });

        server.requestHandler(router::accept).listen(8080);
    }
}

