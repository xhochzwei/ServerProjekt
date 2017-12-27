package de.qreator.vertx.VertxDatabase;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatenbankVerticle extends AbstractVerticle {

    private static final String SQL_NEUE_TABELLE = "create table if not exists user(id int auto_increment,name varchar(20) not null, passwort varchar(20) not null,primary key(name))";
    private static final String SQL_ÜBERPRÜFE_PASSWORT = "select passwort from user where name=?";
    private static final String SQL_ÜBERPRÜFE_EXISTENZ_USER = "select name from user where name=?";
    private static final String USER_EXISTIERT="USER_EXISITIERT";
    private static boolean test;

    private static final String EB_ADRESSE = "vertxdatabase.eventbus";

    private enum ErrorCodes {
        KEINE_AKTION,
        SCHLECHTE_AKTION,
        DATENBANK_FEHLER
    }

    // Logger erzeugen, wobei gilt: TRACE < DEBUG < INFO <  WARN < ERROR
    private static final Logger LOGGER = LoggerFactory.getLogger("de.qreator.vertx.VertxDatabase.Datenbank");

    private JDBCClient dbClient;

    public void start(Future<Void> startFuture) throws Exception {
        JsonObject config = new JsonObject()
                .put("url", "jdbc:h2:~/datenbank")
                .put("driver_class", "org.h2.Driver");

        dbClient = JDBCClient.createShared(vertx, config);

        Future<Void> datenbankFuture = erstelleDatenbank(); //.compose(db -> erstelleUser("user", "geheim"));

        datenbankFuture.setHandler(db -> {
            if (db.succeeded()) {
                LOGGER.info("Datenbank initialisiert");
                vertx.eventBus().consumer(EB_ADRESSE, this::onMessage);
                startFuture.complete();
            } else {
                LOGGER.info("Probleme beim Initialisieren der Datenbank");
                startFuture.fail(db.cause());
            }
        });
    }

    public void onMessage(Message<JsonObject> message) {

        if (!message.headers().contains("action")) {
            LOGGER.error("Kein action-Header übergeben!",
                    message.headers(), message.body().encodePrettily());
            message.fail(ErrorCodes.KEINE_AKTION.ordinal(), "Keine Aktion im Header übergeben");
            return;
        }
        String action = message.headers().get("action");

        switch (action) {
            case "ueberpruefe-passwort":
                überprüfeUser(message);
                break;
            case "erstelleUser":
                erstelleNeuenUser(message);
                break;

            default:
                message.fail(ErrorCodes.SCHLECHTE_AKTION.ordinal(), "Schlechte Aktion: " + action);
        }
    }

    private Future<Void> erstelleDatenbank() {
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

            
    private void erstelleNeuenUser(Message<JsonObject> message) {
        String name = message.body().getString("name");
        String passwort = message.body().getString("passwort");
        Future<Void> userErstelltFuture=erstelleUser(name,passwort);
        userErstelltFuture.setHandler(reply->{
           if (reply.succeeded()){
                LOGGER.info("REG: reply (positive) sent");
               message.reply(new JsonObject().put("REGsuccess", Boolean.TRUE));
           } else {
               String grund=reply.cause().toString();
               if (grund.equals(USER_EXISTIERT)|| test==false){
                   LOGGER.info("REG: reply (negative) sent");
                   message.reply(new JsonObject().put("REGsuccess", Boolean.FALSE));
                   if (test==false) {
                       LOGGER.info("des falsche");
                   }
               }
           }
          
        });
  
    }
 
    private Future<Void> erstelleUser(String name, String passwort) {
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
                            erstellenFuture.fail(USER_EXISTIERT);
                            test=false;
                    
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

    private void überprüfeUser(Message<JsonObject> message) {

        String name = message.body().getString("name");
        String passwort = message.body().getString("passwort");

        LOGGER.info("Überprüfe, ob der Nutzer " + name + " mit dem Passwort " + passwort + " sich anmelden kann.");

        dbClient.queryWithParams(SQL_ÜBERPRÜFE_PASSWORT, new JsonArray().add(name), abfrage -> {
            if (abfrage.succeeded()) {
                List<JsonArray> zeilen = abfrage.result().getResults();
                if (zeilen.size() == 1) {
                    String passwortDB = zeilen.get(0).getString(0);

                    if (passwortDB.equals(passwort)) {
                        message.reply(new JsonObject().put("passwortStimmt", Boolean.TRUE));
                        LOGGER.info("Anmeldename und Passwort stimmen überein");
                    } else {
                        message.reply(new JsonObject().put("passwortStimmt", Boolean.FALSE));
                    }
                } else {
                    LOGGER.info("Anmeldename und Passwort stimmen NICHT überein");
                    message.reply(new JsonObject().put("passwortStimmt", Boolean.FALSE));
                }
            } else {
                message.reply(new JsonObject().put("passwortStimmt", Boolean.FALSE));
            }
        });
    }
}
