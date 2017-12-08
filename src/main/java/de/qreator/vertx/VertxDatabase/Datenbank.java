package de.qreator.vertx.VertxDatabase;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Datenbank {

    public static final String SQL_NEUE_TABELLE = "create table if not exists user(id int auto_increment,name varchar(20) not null, passwort varchar(20) not null,primary key(name))";
    public static final String SQL_ÜBERPRÜFE_PASSWORT = "SELECT passwort from user where name=?";

    private static final Logger LOGGER = LoggerFactory.getLogger("de.qreator.vertx.VertxDatabase.Datenbank");

    public static JDBCClient dbClient;

    public static Future<Void> erstelleDatenbank() {
        Future<Void> erstellenFuture = Future.future();

        dbClient.getConnection(res -> {
            if (res.succeeded()) {

                SQLConnection connection = res.result();
                connection.execute(SQL_NEUE_TABELLE, erstellen -> {
                    if (erstellen.succeeded()) {

                        erstellenFuture.complete();
                    } else {
                        LOGGER.error(erstellen.cause().toString());
                        erstellenFuture.fail(erstellen.cause());
                    }

                });
            } else {
                LOGGER.error("Problem bei der Verbindung zur Datenbank");
            }
        });
        return erstellenFuture;
    }

    public static Future<Void> erstelleStandardUser(String name, String passwort) {
        Future<Void> erstellenFuture = Future.future();

        dbClient.getConnection(res -> {
            if (res.succeeded()) {

                SQLConnection connection = res.result();
                connection.execute("insert into user(name,passwort) values('" + name + "','" + passwort + "')", erstellen -> {
                    if (erstellen.succeeded()) {
                        erstellenFuture.complete();
                    } else {
                        LOGGER.info(erstellen.cause().toString());
                        erstellenFuture.fail(erstellen.cause());
                    }
                });
            } else {
                LOGGER.error("Problem bei der Verbindung zur Datenbank");
            }
        });
        return erstellenFuture;
    }

    public static void überprüfeUser(String name, String passwort, RoutingContext routingContext) {
        Future<Void> abfrageFuture = Future.future();
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json");
        JsonObject jo = new JsonObject();
        jo.put("typ", "überprüfung");
        Session session = routingContext.session();
        dbClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.queryWithParams(SQL_ÜBERPRÜFE_PASSWORT, new JsonArray().add(name), abfrage -> {
                    if (abfrage.succeeded()) {
                        List<JsonArray> zeilen = abfrage.result().getResults();
                        if (zeilen.size() == 1) {
                            String passwortDB = zeilen.get(0).getString(0);

                            if (passwortDB.equals(passwort)) {
                                session.put("angemeldet", "ja");
                                jo.put("text", "ok");
                                LOGGER.info("Anmeldename und Passwort stimmen überein");
                            } else {
                                jo.put("text", false);
                            }
                        } else {
                            jo.put("text", false);
                        }
                        response.end(Json.encodePrettily(jo));
                        abfrageFuture.complete();
                    } else {
                        jo.put("text", false);
                        response.end(Json.encodePrettily(jo));
                        abfrageFuture.fail(abfrage.cause());
                    }
                });
            } else {
                LOGGER.error("Problem bei der Verbindung zur Datenbank");
                jo.put("text", false);
                response.end(Json.encodePrettily(jo));
            }
        });

    }
}
