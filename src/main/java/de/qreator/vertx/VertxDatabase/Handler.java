package de.qreator.vertx.VertxDatabase;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public class Handler {

    public static void geheimeSeiten(RoutingContext routingContext) {
        Session session = routingContext.session();
        if (session.get("angemeldet") == null) { // wenn nicht angemeldet, dann Passwort verlangen
            routingContext.response().setStatusCode(303);

            routingContext.response().putHeader("Location", "/static/passwort.html");

            routingContext.response().end();
        } else {
            routingContext.next(); // sonst weiter zum nächsten Router
        }
    }

    public static void anfragenHandler(RoutingContext routingContext) {
        String typ = routingContext.request().getParam("typ");
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json");
        JsonObject jo = new JsonObject();
        Session session = routingContext.session();

        if (typ.equals("angemeldet")) {
            String angemeldet = session.get("angemeldet");
            jo.put("typ", "angemeldet");
            if (angemeldet != null && angemeldet.equals("ja")) {
                jo.put("text", "ja");
            } else {

                jo.put("text", "nein");
            }
            response.end(Json.encodePrettily(jo));
        } else if (typ.equals("anmeldedaten")) {
            String name = routingContext.request().getParam("anmeldename");
            String passwort = routingContext.request().getParam("passwort");
            Datenbank.überprüfeUser(name, passwort, routingContext);
        } else if (typ.equals("logout")) {
            session.put("angemeldet", null);
            jo.put("typ", "logout");
            response.end(Json.encodePrettily(jo));
        }
    }
}
