package de.qreator.vertx.VertxDatabase;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLRowStream;
import java.util.List;
import javafx.scene.chart.XYChart.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatenbankVerticle extends AbstractVerticle {

    private static final String SQL_NEUE_TABELLE_SHOP =         "create table if not exists item(id int auto_increment, name varchar(20) not null, preis int not null,primary key(name))";
    private static final String SQL_NEUE_TABELLE =              "create table if not exists user(id int auto_increment,name varchar(20) not null, passwort varchar(20) not null,adresse varchar(20) not null,money int not null,function varchar(20) not null,primary key(name))";
    private static final String SQL_ÜBERPRÜFE_PASSWORT =        "select passwort from user where name=?";
    private static final String SQL_ÜBERPRÜFE_EXISTENZ_USER =   "select name from user where name=?";
    private static final String SQL_ÜBERPRÜFE_FUNCTION =        "select function from user where name =?";
    private static final String SQL_DELETE_ITEM =               "delete from item where name =?";
    private static final String SQL_DELETE_USER =               "delete from user where name =?";
    private static final String SQL_ÜBERPRÜFE_KONTO =           "select money from user where name =?";
    private static final String SQL_ÜBERPRÜFE_ADRESSE =         "select adresse from user where name =?";
    private static final String SQL_ÜBERPRÜFE_ITEM =            "select name from item where name =?";
    private static final String SQL_DELETE =                    "drop table item";
    private static final String SQL_ÜBERPRÜFE_PREIS     =       "select preis from item where name =?";
    private static final String USER_EXISTIERT = "USER_EXISITIERT";
    private static final String SQL_ÜBERPRÜFE_ITEMNAME = "select name from item where name =?";
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
        
        erstelleShopDB();

        datenbankFuture.setHandler(db -> {
            if (db.succeeded()) {
                LOGGER.info("Datenbank initialisiert");
                vertx.eventBus().consumer(EB_ADRESSE, this::onMessage);
                erstelleUser("Admin", "passwort", "unknown", "admin", 9999);
                erstelleUser("test", "passwort", "unknown", "user", 25);
                
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
            case "getPreis":
                getPreis(message);
                break;
            case "Shopausgeben":
                Shopausgeben(message);
                break;
            case "uptKonto":
                uptKonto(message);
                break;
            
            case "löscheItem":
                löscheItem(message);
                break;
            case "erstelleItem":
                 erstelleItem(message);
                 break;
            case "ueberpruefe-passwort":
                überprüfeUser(message);
                break;
            case "erstelleUser":
                erstelleNeuenUser(message);
                break;
            case "getFunction":
                getFunction(message);
                break;
            case "getKonto":
                getKonto(message);
                break;
            case "getAdresse":
                getAdresse(message);
                break;
            case "changeAdresse":
                uptAdresse(message);

            default:
                message.fail(ErrorCodes.SCHLECHTE_AKTION.ordinal(), "Schlechte Aktion: " + action);
        }
    }

    private void löscheDatenbank() {

        dbClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.execute(SQL_DELETE, löschen -> {
                    if (löschen.succeeded()) {
                        LOGGER.info("Datenbank erfolgreich gelöscht");

                    } else {
                        LOGGER.error("Löschen der Datenbank fehlgeschlagen " + löschen.cause());

                    }
                });
            }

        });
    }
        private void uptKonto(Message<JsonObject> message) {
        String name = message.body().getString("name");
        int konto = message.body().getInteger("konto");
        String konto2 = "" + konto;
        LOGGER.info(name + "");
        dbClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.execute("update user set money = " + konto +  " where name = " + "'" + name + "'" + "", abfrage -> {
                    if (abfrage.succeeded()) {  
                        message.reply(new JsonObject().put("uptKonto", "success"));
                        
                        
                    }
                    else{
                        LOGGER.error("" + abfrage.cause());
                        message.reply(new JsonObject().put("uptKonto", abfrage.cause().toString()));
                    }
 
                });
            }
 
        });
    }
    private void getPreis(Message <JsonObject> message){
        String itemname = message.body().getString("Gegenstand");
        dbClient.getConnection(res ->{
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.queryWithParams(SQL_ÜBERPRÜFE_ITEM, new JsonArray().add(itemname), abfrage ->{
                     if (abfrage.succeeded()) {
                         
                         List<JsonArray> liste = abfrage.result().getResults();
                         
                         if (liste.isEmpty()) {
                             LOGGER.info("ein solches Item existiert nicht");
                             message.reply(new JsonObject().put("ItemPreis", "nonexistent"));
                         }
                     else{
                         LOGGER.info("In der Datenbank sind Einträge");
                        
                 
               
                connection.queryStreamWithParams(SQL_ÜBERPRÜFE_PREIS, new JsonArray().add(itemname), ab ->{
                    if (ab.succeeded()) {
                        String zeilen = ab.result().toString();
                        LOGGER.info(zeilen);
                       
                            message.reply(new JsonObject().put("ItemPreis", zeilen));
                    }
                });
                     }}
                        });
                
            }
 
        });
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
        String adresse = message.body().getString("adresse");
        String function = message.body().getString("function");
        if (function == null) {
            function = "user";
        }
        int money = 0;
        Future<Void> userErstelltFuture = erstelleUser(name, passwort, adresse, function, money);
        userErstelltFuture.setHandler(reply -> {
            if (reply.succeeded()) {
                LOGGER.info("REG: reply (positive) sent");
                message.reply(new JsonObject().put("REGsuccess", Boolean.TRUE));
            } else {
                String grund = reply.cause().toString();
                LOGGER.info(grund);
                if (grund.equals(USER_EXISTIERT)) {
                    LOGGER.info("REG: reply (negative) sent");
                    message.reply(new JsonObject().put("REGsuccess", Boolean.FALSE));
                }
            }

        });
    }
    
    
    private void  erstelleShopDB() {
    
        LOGGER.info("Shop Datenbank wird erstellt");
        
         dbClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.execute(SQL_NEUE_TABELLE_SHOP, erstellen ->{
                    
                    if (erstellen.succeeded()) {
                       
                        LOGGER.info("Shop Datenbank erfolgreich erstellt!");
                    } else {
                        
                        LOGGER.error(erstellen.cause().toString());
                    
                    }
                });   
            }
         });
    }
    private void  Shopausgeben(Message<JsonObject> message) {
        String name = message.body().getString("search");
        dbClient.getConnection(res -> {
            if (res.succeeded()) {
                 SQLConnection connection = res.result();
                connection.queryWithParams(SQL_ÜBERPRÜFE_ITEM, new JsonArray().add(name), abfrage ->{
                     if (abfrage.succeeded()) {
                         LOGGER.info("In der Datenbank sind Einträge");
                         List<JsonArray> liste = abfrage.result().getResults();
                         
                         if (liste.isEmpty()) {
                             LOGGER.info("ein solches Item existiert nicht");
                             message.reply(new JsonObject().put("ItemExistenz", Boolean.FALSE));
                         }
                     }else{
                         LOGGER.info("In der Datenbank sind Einträge");
                         message.reply(new JsonObject().put("ItemExistenz", Boolean.TRUE));
                     }
                });
            }
        });
    }
    
    private void löscheItem(Message<JsonObject> message){
        String name = message.body().getString("name");
        dbClient.getConnection(res -> {
            if(res.succeeded()){
                SQLConnection connection = res.result();
                connection.queryWithParams(SQL_DELETE_ITEM, new JsonArray().add(name), lösche ->{
                    if (lösche.succeeded()) {
                        message.reply(new JsonObject().put("deleteItem", "success"));
                    }
                });
            }
        });
    }
    
    private void erstelleItem(Message<JsonObject> message){
        
        String name = message.body().getString("name");
        String preis2 = message.body().getString("preis");
        int preis = Integer.parseInt(preis2);
        dbClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                
                connection.queryWithParams(SQL_ÜBERPRÜFE_ITEMNAME, new JsonArray().add(name), abfrage ->{
            
                    if (abfrage.succeeded()) {
                       
                        List<JsonArray> liste = abfrage.result().getResults();
                        if (liste.isEmpty()) {
                            
                            connection.execute("insert into item(name,preis) values ('" + name + "','" + preis + "')", erstellen ->{
                                if (erstellen.succeeded()) {
                                  
                                    message.reply(new JsonObject().put("ersItem","ja"));
                                }
                                else{
                                    message.reply(new JsonObject().put("ersItem", "fehler"));
                                    LOGGER.error("Fehler beim Einfügen eines Shopitems: " + erstellen.cause());
                                }
                            });
                        }
                        else{
                            LOGGER.info("Shopitem existiert schon");
                            message.reply(new JsonObject().put("ersItem", "existiert"));
                            
                        }
                    }
                    else{
                        LOGGER.error("" + abfrage.cause());
                    }
 
                });
            }
            else{
                LOGGER.error("Verbindung fehlgeschlagen: " + res.cause());
            }
        });
    }
    
    
    
    private void getKonto(Message<JsonObject> message) {
        String name = message.body().getString("name");
        dbClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.querySingleWithParams(SQL_ÜBERPRÜFE_KONTO, new JsonArray().add(name), abfrage -> {
                    if (abfrage.succeeded()) {
                        String zeilen = abfrage.result().toString();
                       
                            message.reply(new JsonObject().put("konto", zeilen));
                            LOGGER.info("KONTO: Der Kontostand von " + name + " beträgt " + zeilen);
                            
                        

                    }

                });
            } else {
                LOGGER.error("KONTO: Fehler bei der Verbindung mit der Datenbank: " + res.cause());
            }
        });
    }
    private void getAdresse(Message<JsonObject> message) {
        String name = message.body().getString("name");
        dbClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.querySingleWithParams(SQL_ÜBERPRÜFE_ADRESSE, new JsonArray().add(name), abfrage -> {
                    if (abfrage.succeeded()) {
                        String zeilen = abfrage.result().toString();

                            message.reply(new JsonObject().put("adresse", zeilen));
                            LOGGER.info("ADRESSE: Die Adresse von " + name + " ist " + zeilen);
                    }

                });
            } else {
                LOGGER.error("ADRESSE: Fehler bei der Verbindung mit der Datenbank: " + res.cause());
            }
        });
    }
    private void getFunction(Message<JsonObject> message) {
        String name = message.body().getString("name");
        dbClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.querySingleWithParams(SQL_ÜBERPRÜFE_FUNCTION, new JsonArray().add(name), abfrage -> {

                    if (abfrage.succeeded()) {
                        String zeilen = abfrage.result().toString();
                        if (zeilen.isEmpty()) {
                            LOGGER.error("FUNC: Diesen User gibt es nicht");
                        } else {
                            String function = zeilen;//.get(0).toString();
                            message.reply(new JsonObject().put("function", function));
                        }
                    } else {
                        LOGGER.error("FUNC: Antwortfehler");
                    }

                });

            }

        });
    }

    private Future<Void> erstelleUser(String name, String passwort, String adresse, String function, Integer money) {

        Future<Void> erstellenFuture = Future.future();

        dbClient.getConnection(res -> {
            if (res.succeeded()) {

                SQLConnection connection = res.result();

                connection.queryWithParams(SQL_ÜBERPRÜFE_EXISTENZ_USER, new JsonArray().add(name), abfrage -> {
                    if (abfrage.succeeded()) {
                        LOGGER.error("jetzt kommt der 1. fehler");
                        List<JsonArray> zeilen = abfrage.result().getResults();       
                        if (zeilen.isEmpty()) { // User existiert noch nicht
                            LOGGER.info("Erstelle einen User mit dem Namen " + name + " und dem Passwort " + passwort);
                            connection.execute("insert into user(name,passwort,adresse,money,function) values('" + name + "','" + passwort + "','" + adresse + "','" + money + "','" + function + "')", erstellen -> {
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
private void uptAdresse(Message<JsonObject> message){
    LOGGER.info("Adresse wird geändert");
    String name = message.body().getString("name");
    String adresse = message.body().getString("adresse");
    LOGGER.info(adresse);
    dbClient.getConnection(res -> {
            if (res.succeeded()) {

                SQLConnection connection = res.result();
                connection.execute("update user set adresse = " + "'" + adresse + "'" +  " where name = "+ "'" + name + "'" + "", change -> {
                    if (change.succeeded()) {
                        message.reply(new JsonObject().put("changeAdresseControl", "es tut"));
                        LOGGER.info("ADRESSE: erfolgreich");
                    }
                    else{
                         message.reply(new JsonObject().put("changeAdresseControl", Boolean.FALSE));
                         LOGGER.error("" + change.cause());
                    }
                });
            }
    });
}
}
