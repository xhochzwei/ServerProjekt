$(document).ready(function () {
    $("#eingabeKnopf").click(function () {
        $.post("rest",
                {
                    typ: "namenKnopf",
                    name: $("#eingabeName").text()
                },
                function (data, status) {
                    $("body").append("Daten: " + data);
                }
        )
    });
});