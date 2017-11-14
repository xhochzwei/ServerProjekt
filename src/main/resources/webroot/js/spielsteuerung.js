$(document).ready(function () {


    $.ajax({url: "../anfrage", data:
                {
                    typ: "neuesSpiel",
                },
        success: function (data) {
            if (data.typ == "bestätigung" && data.art == "OK") {
                console.log("Neues Spiel!");
            }
        }
    });


    var eb = new EventBus('/eventbus', {"vertxbus_ping_interval": 300000});
    eb.enableReconnect(true);


    eb.onopen = function () {
        eb.registerHandler('vertxbeispiel.spielsteuerung', function (error, message) {
            if (message.body.typ == "anmeldung") {
                $("body").append("<div>" + message.body.name + "</div>");
            } else if (message.body.typ == "highscore") {
                var namen = message.body.daten;
                $("body").html("<canvas id='bild' width='" + (window.innerWidth * 0.9) + "px' height='" + (namen.length * 20) + "px'></canvas>");
                var context = $("#bild")[0].getContext("2d");
                context.font = "16px Arial";
                var anzahlAufgaben = message.body.anzahlAufgaben;
                var breite = $("#bild").width();
                for (var i = 0; i < namen.length; i++) {

                    context.fillStyle = "#aaffaa";
                    var balkenBreite = namen[i].richtig / anzahlAufgaben * breite;
                    context.fillRect(0, 20 * i, balkenBreite, 20);
                    context.fillStyle = "#ffaaaa";
                    context.fillRect(balkenBreite, 20 * i, namen[i].falsch / anzahlAufgaben * breite, 20);
                    context.fillStyle = "#000000";
                    context.fillText(namen[i].name, 5, 20 * i + 14);
                }
            }
        });


    }

    $(document).on('keypress', '#anzahlAufgaben', function (event) {
        if (event.which === 13) {

            $("#startKnopf").trigger("click");
        }
    });

    $("#startKnopf").click(function () {
        $.ajax({url: "../anfrage", data:
                    {
                        typ: "starteKnopf",
                        anzahlAufgaben: $("#anzahlAufgaben").val()

                    },
            success: function (data) {
                if (data.typ == "bestätigung" && data.art == "OK") {
                    $("body").html("Los geht's!");
                }
            }
        });
    });
});