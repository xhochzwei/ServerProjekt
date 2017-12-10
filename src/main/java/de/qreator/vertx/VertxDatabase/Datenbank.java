package de.qreator.vertx.VertxDatabase;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
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
    public static final String SQL_ÜBERPRÜFE_PASSWORT = "select passwort from user where name=?";
    public static final String SQL_ÜBERPRÜFE_EXISTENZ_USER = "select name from user where name=?";
    
      public enum ErrorCodes {
    KEINE_AKTION,
    SCHLECHTE_AKTION,
    DATENBANK_FEHLER
  }

    // Logger erzeugen, wobei gilt: TRACE < DEBUG < INFO <  WARN < ERROR
    private static final Logger LOGGER = LoggerFactory.getLogger("de.qreator.vertx.VertxDatabase.Datenbank");

    public static JDBCClient dbClient;
    
    
    public void onMessage(Message<JsonObject> message) {

    if (!message.headers().contains("action")) {
      LOGGER.error("Keine action-Header übergeben!",
        message.headers(), message.body().encodePrettily());
      message.fail(ErrorCodes.KEINE_AKTION.ordinal(), "Keine Aktion im Header übergeben");
      return;
    }
    String action = message.headers().get("action");

    switch (action) {
      case "all-pages":
      //  fetchAllPages(message);
        break;
      case "get-page":
       // fetchPage(message);
        break;
      case "create-page":
       // createPage(message);
        break;
      case "save-page":
       // savePage(message);
        break;
      case "delete-page":
       // deletePage(message);
        break;
      default:
        message.fail(ErrorCodes.SCHLECHTE_AKTION.ordinal(), "Schlechte Aktion: " + action);
    }
  }
    

    public static Future<Void> erstelleDatenbank() {
        Future<Void> erstellenFuture = Future.future();
        LOGGER.info("Datenbank neu anlegen, falls nicht vorhanden.");
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

    public static Future<Void> erstelleUser(String name, String passwort) {
        Future<Void> erstellenFuture = Future.future();

        dbClient.getConnection(res -> {
            if (res.succeeded()) {

                SQLConnection connection = res.result();

                connection.queryWithParams(SQL_ÜBERPRÜFE_EXISTENZ_USER, new JsonArray().add(name), abfrage -> {
                    if (abfrage.succeeded()) {
                        List<JsonArray> zeilen = abfrage.result().getResults();
                        if (zeilen.isEmpty()) { // User existiert noch nicht
                            LOGGER.info("Erstelle einen User mit dem Namen " + name + " und dem Passwort " + passwort);
                            connection.execute("insert into user(name,passwort) values('" + name + "','" + passwort + "')", erstellen -> {
                                if (erstellen.succeeded()) {
                                    LOGGER.info("User " + name + " erfolgreich erstellt");
                                    erstellenFuture.complete();
                                } else {
                                    LOGGER.info(erstellen.cause().toString());
                                    erstellenFuture.fail(erstellen.cause());
                                }
                            });
                        } else {
                            LOGGER.info("User mit dem Namen " + name + " existiert bereits.");
                            //erstellenFuture.fail("User existiert bereits!"); 
                            erstellenFuture.complete();
                        }
                    } else {
                        erstellenFuture.fail(abfrage.cause());
                    }

                });
            } else {
                LOGGER.error("Problem bei der Verbindung zur Datenbank");
                erstellenFuture.fail(res.cause());
            }
        });
        return erstellenFuture;
    }

    public static void überprüfeUser(String name, String passwort, RoutingContext routingContext) {
        LOGGER.info("Überprüfe, ob der Nutzer " + name + " mit dem Passwort " + passwort + " sich anmelden kann.");
        Future<Void> abfrageFuture = Future.future();
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json");
        JsonObject jo = new JsonObject();
        jo.put("typ", "überprüfung");
        Session session = routingContext.session();

        dbClient.queryWithParams(SQL_ÜBERPRÜFE_PASSWORT, new JsonArray().add(name), abfrage -> {
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

    }
}
