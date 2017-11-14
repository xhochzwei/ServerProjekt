/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.qreator.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class VertxWebFormular {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("Gib in der Adresszeile den Pfad \"daten\" oder \"produkte/Werkzeuge/Hammer\" oder \"static/index.html\" oder \"static/formular.html\" ein.");
        });

        
        router.route("/anfrage").handler(routingContext -> {
            String typ = routingContext.request().getParam("typ");
            String name = routingContext.request().getParam("name");
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json");
            JsonObject jo = new JsonObject();

            if (typ.equals("namenKnopf")) {
                jo.put("typ", "antwort");
                jo.put("text", "Der Text war " + name);
            }
            response.end(Json.encodePrettily(jo));
        });

        // statische html-Dateien werden über den Dateipfad static ausgeliefert
      
        router.route("/static/*").handler(StaticHandler.create().setDefaultContentEncoding("UTF-8"));

        // alle Anfragen, die mit /daten beginnen werden von diesem Handler beantwortet
        router.route("/daten").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hier eine Nachricht vom Unterpfad \"/daten\"!");
        });

        // alle Anfragen der Form /produkte/Werkzeuge/Hammer1  werden von diesem Handler beantwortet
        router.route("/produkte/:produktTyp/:produktID").handler(routingContext -> {
            String produktTyp = routingContext.request().getParam("produktTyp");
            String produktID = routingContext.request().getParam("produktID");
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("Die ProduktID ist " + produktID + " und der Produkttyp ist " + produktTyp);
        });

        // router::accept akzeptiert eine Anfrage und leitet diese an den Router weiter
        server.requestHandler(router::accept).listen(8080,"0.0.0.0");
    }
}
