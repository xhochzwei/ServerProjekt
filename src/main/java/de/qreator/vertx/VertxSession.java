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
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author menze
 */
public class VertxSession {

    public static int anzahlAufgaben = 3;
    static List<Spieler> spielerListe = new ArrayList<>();
    static boolean spielLaeuft = false;
    static Aufgaben aufgaben = null;

    public static void neuesSpiel() {
        spielerListe = new ArrayList<>();
        aufgaben = new Aufgaben(anzahlAufgaben);
    }

    public static SessionStore store;
    public static SessionHandler sessionHandler;

    public static void main(String[] args) {

        aufgaben = new Aufgaben(anzahlAufgaben);

        Vertx vertx = Vertx.vertx();

        HttpServer server = vertx.createHttpServer();
        store = LocalSessionStore.create(vertx);
        sessionHandler = SessionHandler.create(store);

        Router router = Router.router(vertx);

        router.route().handler(CookieHandler.create());
        router.route().handler(sessionHandler);

        router.route("/static/*").handler(StaticHandler.create().setDefaultContentEncoding("UTF-8").setCachingEnabled(false));

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        PermittedOptions addressesPermitted = new PermittedOptions().setAddress("vertxbeispiel.spielsteuerung");
        PermittedOptions addressesPermitted2 = new PermittedOptions().setAddressRegex("vertxbeispiel.spieler\\..+");
        PermittedOptions addressesPermitted3 = new PermittedOptions().setAddress("vertxbeispiel.alle");

        BridgeOptions options = new BridgeOptions().addInboundPermitted(addressesPermitted).addInboundPermitted(addressesPermitted2)
                .addOutboundPermitted(addressesPermitted3).addOutboundPermitted(addressesPermitted).addOutboundPermitted(addressesPermitted2);
        sockJSHandler.bridge(options);

        router.route("/eventbus/*").handler(sockJSHandler);

        router.route("/anfrage").handler(routingContext -> {
            String typ = routingContext.request().getParam("typ");

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json");
            Session session = routingContext.session();

            JsonObject jo = new JsonObject();

            if (typ.equals("namenKnopf") && spielLaeuft == false) {
                String name = routingContext.request().getParam("name");
                String id = name + "_" + (int) (Math.random() * 10000);
                spielerListe.add(new Spieler(name, id, anzahlAufgaben));

                session.put("spielernr", id);

                jo.put("typ", "anmeldung");
                jo.put("name", name);
                jo.put("id", id);

                vertx.eventBus().send("vertxbeispiel.spielsteuerung", jo);
                response.end(Json.encodePrettily(jo));
            } else if (typ.equals("starteKnopf") && spielLaeuft == false) {
                anzahlAufgaben = Integer.parseInt(routingContext.request().getParam("anzahlAufgaben"));
                aufgaben = new Aufgaben(anzahlAufgaben);
                vertx.eventBus().publish("vertxbeispiel.alle", "start");
                jo.put("typ", "bestätigung");
                jo.put("art", "OK");
                response.end(Json.encodePrettily(jo));
                spielLaeuft = true;
            } else if (typ.equals("neuesSpiel")) {

                neuesSpiel();

                store = LocalSessionStore.create(vertx);
                sessionHandler = SessionHandler.create(store);
                spielLaeuft = false;
                jo.put("typ", "bestätigung");
                jo.put("art", "OK");
                response.end(Json.encodePrettily(jo));
            } else if (typ.equals("holeAufgabeNeu") && spielLaeuft == true) {
                String sn = session.get("spielernr");
                int spielernr = -1;
                for (int i = 0; i < spielerListe.size(); i++) {
                    if (spielerListe.get(i).getID().equals(sn)) {
                        spielernr = i;
                        i = spielerListe.size();
                    }
                }
                Spieler s = spielerListe.get(spielernr);

                String aufgabeAlt = s.getAktuelleAufgabe()[0];
                String loesung = s.getAktuelleAufgabe()[1];
                String loesungSpieler = routingContext.request().getParam("loesung").trim();
                s.richtigGeloest(loesung.equals(loesungSpieler));

                int aufgabenNummer = s.holeNächsteAufgabenNummer();
                if (aufgabenNummer >= 0) {
                    String[] aufgabe = aufgaben.holeAufgabe(aufgabenNummer);

                    jo.put("typ", "aufgabe");
                    jo.put("aufgabeAlt", aufgabeAlt);
                    jo.put("loesung", loesung);
                    jo.put("richtig", loesung.equals(loesungSpieler));
                    jo.put("richtigGesamt", s.holeRichtig());
                    jo.put("falschGesamt", s.holeFalsch());
                    jo.put("text", aufgabe[0]);
                    System.out.println(aufgabe[0] + " " + aufgabe[1]);
                    s.setAktuelleAufgabe(aufgabe);
                    //vertx.eventBus().send("vertxbeispiel.spieler."+s.getID(), s.getName());
                    response.end(Json.encodePrettily(jo));
                } else {

                    jo.put("typ", "fertig");
                    response.end(Json.encodePrettily(jo));
                }
                jo = new JsonObject();
                jo.put("typ", "highscore");
                jo.put("anzahlAufgaben", anzahlAufgaben);
                JsonArray ja = new JsonArray();
                // Infoscreen auf neuesten Stand bringen
                for (Spieler spieler : spielerListe) {
                    JsonObject sp = new JsonObject();
                    sp.put("name", spieler.getName());
                    sp.put("richtig", spieler.holeRichtig());
                    sp.put("falsch", spieler.holeFalsch());
                    ja.add(sp);
                }
                jo.put("daten", ja);
                vertx.eventBus().send("vertxbeispiel.spielsteuerung", jo);

            } else if (typ.equals("holeAufgabe") && spielLaeuft == true) {

                String sn = session.get("spielernr");
                int spielernr = -1;
                for (int i = 0; i < spielerListe.size(); i++) {
                    if (spielerListe.get(i).getID().equals(sn)) {
                        spielernr = i;
                        i = spielerListe.size();
                    }
                }
                if (spielernr != -1) {
                    Spieler s = spielerListe.get(spielernr);
                    int aufgabenNummer = s.holeNächsteAufgabenNummer();
                    if (aufgabenNummer >= 0) {
                        String[] aufgabe = aufgaben.holeAufgabe(aufgabenNummer);
                        jo.put("typ", "aufgabe");
                        jo.put("text", aufgabe[0]);
                        s.setAktuelleAufgabe(aufgabe);
                    } else {
                        jo.put("typ", "fertig");
                    }
                    response.end(Json.encodePrettily(jo));

                } else {
                    jo.put("typ", "fehler");
                    response.end(Json.encodePrettily(jo));
                }
                //vertx.eventBus().send("vertxbeispiel.spieler."+s.getID(), s.getName());

            }

        });

        server.requestHandler(router::accept).listen(8080, "0.0.0.0");
    }
}
