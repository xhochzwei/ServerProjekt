/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.qreator.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 *
 * @author menze
 */
public class VertxDatabase {

    public static SessionStore store;
    public static SessionHandler sessionHandler;
    private static SQLConnection connection = null;
    private static JDBCClient client = null;

    public static boolean ueberpruefeDaten(String name, String passwort) {

        return (name.equals("User")) && (passwort.equals("geheim"));
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        JsonObject config = new JsonObject()
                .put("url", "jdbc:h2:~/datenbank")
                .put("driver_class", "org.h2.Driver");

        client = JDBCClient.createShared(vertx, config);

        client.getConnection(res -> {
            if (res.succeeded()) {

                connection = res.result();
                connection.execute("create table if not exists user(name varchar(20) not null, passwort varchar(20) not null,primary key (name))", handler -> {
                    connection.execute("insert into user (name,passwort) values('User','geheim')", handler2 -> {
                        connection.close();
                        System.out.println("Datenbank befüllt");
                    });
                });

                System.out.println("Verbindung zur Datenbank wurde hergestellt");

            } else {
                System.out.println("Problem bei der Verbindung zur Datenbank");
            }
        });

        HttpServer server = vertx.createHttpServer();

        store = LocalSessionStore.create(vertx);
        sessionHandler = SessionHandler.create(store);

        Router router = Router.router(vertx);

        router.route().handler(CookieHandler.create());
        router.route().handler(sessionHandler);

        router.post().handler(BodyHandler.create());

        router.post("/anfrage").handler(routingContext -> {
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
            } else if (typ.equals("anmeldedaten")) {
                String name = routingContext.request().getParam("anmeldename");
                String passwort = routingContext.request().getParam("passwort");
                jo.put("typ", "überprüfung");
                if (ueberpruefeDaten(name, passwort) == true) {
                    jo.put("text", "ok");
                    session.put("angemeldet", "ja");
                } else {
                    jo.put("text", false);
                }
            } else if (typ.equals("logout")) {
                session.put("angemeldet", null);
                jo.put("typ", "logout");
            }
            response.end(Json.encodePrettily(jo));
        });

        router.route("/static/geheim/*").handler(routingContext -> {
            Session session = routingContext.session();
            if (session.get("angemeldet") == null) {
                routingContext.response().setStatusCode(303);

                routingContext.response().putHeader("Location", "/static/passwort.html");

                routingContext.response().end();
            } else {
                routingContext.next();
            }
        });

        router.route("/static/*").handler(StaticHandler.create().setDefaultContentEncoding("UTF-8").setCachingEnabled(false));

        // router::accept akzeptiert eine Anfrage und leitet diese an den Router weiter
        server.requestHandler(router::accept).listen(8080, "0.0.0.0");
    }
}
