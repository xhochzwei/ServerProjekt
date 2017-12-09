package de.qreator.vertx.VertxDatabase;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger("de.qreator.vertx.VertxDatabase.Handler");

    public static void geheimeSeiten(RoutingContext routingContext) {
        LOGGER.info("Router für geheime Seiten");
        Session session = routingContext.session();
        if (session.get("angemeldet") == null) { // wenn nicht angemeldet, dann Passwort verlangen
            routingContext.response().setStatusCode(303);

            routingContext.response().putHeader("Location", "/static/passwort.html");

            routingContext.response().end();
        } else {
            LOGGER.info("Weiterleitung zum nächsten Router");
            routingContext.next(); // sonst weiter zum nächsten Router
        }
    }

    public static void anfragenHandler(RoutingContext routingContext) {
        LOGGER.info("Router für Anfragen");
        String typ = routingContext.request().getParam("typ");
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json");
        JsonObject jo = new JsonObject();
        Session session = routingContext.session();

        if (typ.equals("angemeldet")) {
            LOGGER.info("Anfrage, ob User angemeldet ist.");
            String angemeldet = session.get("angemeldet");
            jo.put("typ", "angemeldet");
            if (angemeldet != null && angemeldet.equals("ja")) {
                LOGGER.info("User ist angemeldet.");
                jo.put("text", "ja");
            } else {
                LOGGER.info("User ist NICHT angemeldet. Somit Passworteingabe erforderlich");
                jo.put("text", "nein");
            }
            response.end(Json.encodePrettily(jo));
        } else if (typ.equals("anmeldedaten")) {
            String name = routingContext.request().getParam("anmeldename");
            String passwort = routingContext.request().getParam("passwort");
            LOGGER.info("Anmeldeanfrage von User " + name + " mit dem Passwort " + passwort);
            Datenbank.überprüfeUser(name, passwort, routingContext);
        } else if (typ.equals("logout")) {
            LOGGER.info("Logout-Anfrage");
            session.put("angemeldet", null);
            jo.put("typ", "logout");
            response.end(Json.encodePrettily(jo));
        }
    }
}
