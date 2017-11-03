
package de.qreator.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;

public class VertxSimple {

    public static void main(String[] args) {
        // erstellt eine neue Vertx-Instanz
        Vertx vertx = Vertx.vertx(); 
        
        // Http-Server wird erstellt
        HttpServer server = vertx.createHttpServer(); 

        // ein requestHandler nimmt eine Anfrage entgegen (aktuelle Anfrage in request gespeichert
        server.requestHandler(request -> { 
            // holt sich das Objekt zum Antworten
            HttpServerResponse response = request.response();
            
            // setzt den Header (content-length wird automatisch gesetzt)
            response.putHeader("content-type", "text/plain");

            // schickt die Antwort raus und beendet die Antwort
            response.end("Hello World!");
        });

        // der Server horcht an Port 8080 (ein Browser müsste unter localhost:8080 oder 127.0.0.1:8080 "Hello World!" anzeigen)
        server.listen(8080); 
    }
}
