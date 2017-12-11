package de.qreator.vertx.VertxDatabase;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VertxDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger("de.qreator.vertx.VertxDatabase.Start");

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        Future<String> starteDatenbankVerticle = Future.future();
        vertx.deployVerticle(new DatenbankVerticle(), starteDatenbankVerticle.completer());

        starteDatenbankVerticle.compose(id -> {
            Future<String> starteHttpVerticle = Future.future();

            vertx.deployVerticle("de.qreator.vertx.VertxDatabase.HttpVerticle", new DeploymentOptions().setInstances(2), starteHttpVerticle.completer());
            return starteHttpVerticle;
        }).setHandler(starteProgramm -> {
            if (starteProgramm.succeeded()) {
                LOGGER.info("Programm wurde erfolgreich gestartet");
            } else {
                LOGGER.error("Es gab Probleme beim Programmstart: " + starteProgramm.cause());
            }
        });

    }
}
//http://localhost:8080/static/passwort.html
