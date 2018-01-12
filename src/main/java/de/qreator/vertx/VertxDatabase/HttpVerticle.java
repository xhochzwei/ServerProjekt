/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.qreator.vertx.VertxDatabase;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author menze
 */
public class HttpVerticle extends AbstractVerticle {

    private int port = 8080;
    private static final Logger LOGGER = LoggerFactory.getLogger("de.qreator.vertx.VertxDatabase.HttpServer");
    private static final String EB_ADRESSE = "vertxdatabase.eventbus";
    private static int money;

    public void start(Future<Void> startFuture) throws Exception {

        HttpServer server = vertx.createHttpServer();

        LocalSessionStore store = LocalSessionStore.create(vertx);
        SessionHandler sessionHandler = SessionHandler.create(store);

        Router router = Router.router(vertx);

        router.route().handler(CookieHandler.create());
        router.route().handler(sessionHandler);
        router.post().handler(BodyHandler.create());
        router.post("/anfrage").handler(this::anfragenHandler);
        router.route("/static/geheim/*").handler(this::geheimeSeiten);
        router.route("/static/*").handler(StaticHandler.create().setDefaultContentEncoding("UTF-8").setCachingEnabled(false));

        server.requestHandler(router::accept).listen(port, "0.0.0.0", listener -> {
            if (listener.succeeded()) {
                LOGGER.info("Http-Server auf Port " + port + " gestartet");
                startFuture.complete();
            } else {
                startFuture.fail(listener.cause());
            }
        });
    }

    private void geheimeSeiten(RoutingContext routingContext) {
        LOGGER.info("Router für geheime Seiten");
        Session session = routingContext.session();   
        if (session.get("angemeldet") == null) { // wenn nicht angemeldet, dann Passwort verlangen
            String name = session.get("name");
            LOGGER.info(name);
            routingContext.response().setStatusCode(303);
            routingContext.response().putHeader("Location", "/static/passwort.html");
            routingContext.response().end();
        } else {
            LOGGER.info("Weiterleitung zum nächsten Router");
            routingContext.next(); // sonst weiter zum nächsten Router
        }
    }

    private void anfragenHandler(RoutingContext routingContext) {
        
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
            String name = session.get("name");
            LOGGER.info("name" + name);
            if (angemeldet != null && angemeldet.equals("ja")) {
                LOGGER.info("User ist angemeldet.");
       
               jo.put("text", "ja");
            } else {
                LOGGER.info("User ist NICHT angemeldet. Somit Passworteingabe erforderlich");
                jo.put("text", "nein");
            }
            response.end(Json.encodePrettily(jo));
            
        }
        else if (typ.equals("erstelleItem")){
            LOGGER.info("Erstelle ein Shopitem");
            String name = routingContext.request().getParam("Itemname");
            String preis = routingContext.request().getParam("Itempreis");
            JsonObject request = new JsonObject().put("name", name).put("preis", preis);
            DeliveryOptions opt = new DeliveryOptions().addHeader("action", "erstelleItem");
            vertx.eventBus().send(EB_ADRESSE, request, opt, reply -> {
                if (reply.succeeded()) {
                    LOGGER.info("test");
                   JsonObject body = (JsonObject) reply.result().body();
                   String result = body.getString("ersItem");
                    if (result.equals("ja")) {
                        jo.put("text", "Itemerstellt").put("itemers", "ja");
                        LOGGER.info("Shopitem erstellt");
                    }
                    
                    else if (result.equals("existiert")){
                        jo.put("text", "Itemerstellt").put("itemers", "nein");
                        LOGGER.info("Shopitem existiert");
                    }
                    else {
                        jo.put("text", "Itemerstellt").put("itemers","fehler");
                        LOGGER.error("Fehler beim Erstellen eines Shopitems");
                        
                    }
                    response.end(Json.encodePrettily(jo));
                   
                }
                
 
            });
            
        }
        else if (typ.equals("anmeldedaten")) {
            String name = routingContext.request().getParam("anmeldename");
            String passwort = routingContext.request().getParam("passwort");
            LOGGER.info("Anmeldeanfrage von User " + name + " mit dem Passwort " + passwort);

            JsonObject request = new JsonObject().put("name", name).put("passwort", passwort);

            DeliveryOptions options = new DeliveryOptions().addHeader("action", "ueberpruefe-passwort");
            vertx.eventBus().send(EB_ADRESSE, request, options, reply -> {
                if (reply.succeeded()) {
                    JsonObject body = (JsonObject) reply.result().body();
                    if (body.getBoolean("passwortStimmt") == true) {
                        session.put("angemeldet", "ja").put("name", name);
                        
                      
                        jo.put("typ", "überprüfung").put("text", "ok");
                        
                    } else {
                        jo.put("typ", "überprüfung").put("text", "nein");
                    }
                    response.end(Json.encodePrettily(jo));
                } else {
                    jo.put("typ", "überprüfung").put("text", "nein");
                    response.end(Json.encodePrettily(jo));
                }
            });
            
        }
        else if (typ.equals("function")){
            String name = session.get("name");
            jo.put("name", name);
            JsonObject request = new JsonObject().put("name", name);
            DeliveryOptions function2 = new DeliveryOptions().addHeader("action", "getFunction");
            vertx.eventBus().send(EB_ADRESSE, request,function2, reply ->{
                if (reply.succeeded()) {
                    JsonObject dbfunction = (JsonObject) reply.result().body();
                    String func = dbfunction.getString("function");
                    jo.put("function", func);
                       
                    
                }
            });
            JsonObject request2 = new JsonObject().put("name", name);
            DeliveryOptions konto2 = new DeliveryOptions().addHeader("action", "getKonto");
            vertx.eventBus().send(EB_ADRESSE, request2,konto2, reply ->{
                if (reply.succeeded()) {
                    JsonObject dbkonto = (JsonObject) reply.result().body();
                    String knt = dbkonto.getString("konto");
                    jo.put("konto", knt);
                       
                    
                }
            });
                        JsonObject REadr = new JsonObject().put("name", name);
            DeliveryOptions adr2 = new DeliveryOptions().addHeader("action", "getAdresse");
            vertx.eventBus().send(EB_ADRESSE, REadr,adr2, reply ->{
                if (reply.succeeded()) {
                    JsonObject dbkonto = (JsonObject) reply.result().body();
                    String adre = dbkonto.getString("adresse");
                    jo.put("adresse", adre);
                       response.end(Json.encodePrettily(jo));
                    
                }
            });
        }
    
        else if (typ.equals("AEadresse")){
            LOGGER.info("Adresse wird geändert");
            String name = session.get("name");
            String adresse = routingContext.request().getParam("Adresse");
            JsonObject request  = new JsonObject().put("name", name).put("adresse", adresse);
            DeliveryOptions options = new DeliveryOptions().addHeader("action", "changeAdresse");
            vertx.eventBus().send(EB_ADRESSE, request, options, reply -> {
                if (reply.succeeded()) {
                    JsonObject control = (JsonObject) reply.result().body();
                    if (control.getBoolean("changeAdresseControl").equals("es tut")) {
                        jo.put("text", "richtig").put("CHANGEadresse","erfolgreich");
                        LOGGER.info("Adresse erfolgreich geändert");
                        response.end(Json.encodePrettily(jo));
                    }
                    else{
                        jo.put("CHANGEadresse", Boolean.FALSE);
                        response.end(Json.encodePrettily(jo));
                    }
                    
                }
            });
        }
        else if(typ.equals("Geld")){
            LOGGER.info("Kontostand wird überprüft");
            String name = routingContext.request().getParam("Kontoname");
            JsonObject request = new JsonObject().put("name", name);
            DeliveryOptions options = new DeliveryOptions().addHeader("action", "getKonto");
            vertx.eventBus().send(EB_ADRESSE, request, options, reply -> {
                if (reply.succeeded()) {
                    JsonObject dbkonto = (JsonObject) reply.result().body();
                    String konto = dbkonto.getString("konto");
                    jo.put("konto", konto);
                     response.end(Json.encodePrettily(jo));
                }
                else{
                    jo.put("konto", "fehler"); 
                    LOGGER.error("" + reply.cause());
                response.end(Json.encodePrettily(jo));
                }
            });
                }
        else if (typ.equals("registrierung")) {
            LOGGER.info("daten erhalten");
            String name=routingContext.request().getParam("regname");
            String passwort=routingContext.request().getParam("passwort");
            String adresse = routingContext.request().getParam("regadresse");
            JsonObject request = new JsonObject().put("name", name).put("passwort", passwort).put("adresse", adresse);
            DeliveryOptions options = new DeliveryOptions().addHeader("action", "erstelleUser");
            vertx.eventBus().send(EB_ADRESSE, request, options, reply -> {
              
                if (reply.succeeded()) {
                        LOGGER.info("Reg: Datenübermittlung erfolgt");       
                    JsonObject test = (JsonObject) reply.result().body();
                
                    if (test.getBoolean("REGsuccess")== true) {
                        jo.put("typ", "bestätigung").put("text", "richtig");
                    }
                    else{
                        LOGGER.info("user exists");
                        jo.put("typ", "bestätigung").put("text", "falsch");
                    }
                     response.end(Json.encodePrettily(jo));
                    LOGGER.info("Reg: Datenübermittlung fertig");
                }
                else{
                    LOGGER.error("REG: Datenbankantwort FEHLER");
                }
            });
        }
        /*else if (typ.equals("shop")){
            LOGGER.info("shop wird aufgerufen");
            
            DeliveryOptions options = new DeliveryOptions().addHeader("action", "getKonto");
            int kontostand=dbkonto.getString("konto");
            vertx.eventBus().send(EB_ADRESSE, request, options, reply -> {
                if (reply.succeeded()) {
                    if (kontostand>preis){
                       set kontostand=kontostand-preis;
                    }else{
                        
                    }
                }
                
            });
        }*/
        
        
        else if (typ.equals("logout")) {
            LOGGER.info("Logout-Anfrage");
            session.put("angemeldet", null).put("name", null);
            
            jo.put("typ", "logout");
            response.end(Json.encodePrettily(jo));
            jo.put("typ", "überprüfung").put("text", "ok");
        }
    
    }

 
}
