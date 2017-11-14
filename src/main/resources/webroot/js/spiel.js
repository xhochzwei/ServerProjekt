$(document).ready(function () {


    $(document).on('keypress', '#textfeld', function (event) {
        if (event.which === 13) {

            $("#knopf").trigger("click");
        }
    });
    
        $(document).on('keypress', '#eingabeName', function (event) {
        if (event.which === 13) {

            $("#eingabeKnopf").trigger("click");
        }
    });

    $(document).on("click", "#knopf", function () {
        $.ajax({url: "../anfrage", data: {
                typ: "holeAufgabeNeu",
                loesung: $("#textfeld").val()},
            success: function (data) {
                if (data.typ == "aufgabe") {

                    stelleAufgabeDar(data.text);

                    $("body").prepend("<br>Richtige Aufgaben: " + data.richtigGesamt + "/" + (data.richtigGesamt + data.falschGesamt) + "<br><br>").
                            prepend("<br>Die richtige Lösung war: " + data.aufgabeAlt + " = " + data.loesung).
                            prepend("Dein Ergebnis war " + (data.richtig == true ? "RICHTIG" : "FALSCH") + "<br>");
                } else if (data.typ == "fertig") {
                    $("body").html("Fertig! Du hast alle Aufgaben geschafft!");
                }
            }
        });
    });
    function stelleAufgabeDar(aufgabe) {
        $("body").html("<div id='aufgabentext'>" + aufgabe + "</div>")
                .append("<input type='text' id='textfeld'></input>")
                .append("<input type='button' id='knopf' value='OK'></intpu>");
                $("#textfeld").focus();
    }

    var eb = new EventBus('../eventbus');
    eb.enableReconnect(true);
    eb.onopen = function () {
        eb.registerHandler('vertxbeispiel.alle', function (error, message) {
            console.log('received a message: ' + JSON.stringify(message));
            if (message.body == "start") {
                $.ajax({url: "../anfrage",
                    data: {
                        typ: "holeAufgabe"},
                    success: function (data) {
                        if (data.typ == "aufgabe") {
                            stelleAufgabeDar(data.text);
                        }
                    }

                });
            }
        });
    }
    $("#eingabeKnopf").click(function () {
        $.ajax({url: "../anfrage", data:
                    {
                        typ: "namenKnopf",
                        name: $("#eingabeName").val()
                    },
            success: function (data) {
                $("body").html("Bitte kurz warten, bis alle angemeldet sind!");
                eb.registerHandler('vertxbeispiel.spieler.' + data.id, function (error, message) {
                    console.log('received a message: ' + JSON.stringify(message));

                });
            }
        });
    });
});