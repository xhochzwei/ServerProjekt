package de.qreator.vertx.VertxDatabase;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

public class VertxDatabase {

    public static SessionStore store;
    public static SessionHandler sessionHandler;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        JsonObject config = new JsonObject()
                .put("url", "jdbc:h2:~/datenbank")
                .put("driver_class", "org.h2.Driver");

        Datenbank.dbClient = JDBCClient.createShared(vertx, config);

        Future<Void> startFuture = Datenbank.erstelleDatenbank().compose(db -> Datenbank.erstelleUser("user", "geheim"));

        startFuture.setHandler(datenbank -> {
            if (datenbank.succeeded()) {
                HttpServer server = vertx.createHttpServer();

                store = LocalSessionStore.create(vertx);
                sessionHandler = SessionHandler.create(store);

                Router router = Router.router(vertx);

                router.route().handler(CookieHandler.create());
                router.route().handler(sessionHandler);
                router.post().handler(BodyHandler.create());
                router.post("/anfrage").handler(Handler::anfragenHandler);
                router.route("/static/geheim/*").handler(Handler::geheimeSeiten);
                router.route("/static/*").handler(StaticHandler.create().setDefaultContentEncoding("UTF-8").setCachingEnabled(false));

                server.requestHandler(router::accept).listen(8080, "0.0.0.0");
            } else {
                System.out.println("Folgender Fehler aufgetreten: "+datenbank.cause().getMessage());
            }
        });
    }
}
