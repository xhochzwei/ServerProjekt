$(document).ready(function () {
    $("#eingabeKnopf").click(function () {
        $.ajax({url:"../post", data:
                {
                    typ: "namenKnopf",
                    name: $("#eingabeName").text()
                },
                success: function (data) {
                    $("body").append("Daten: " + data.text);
                }
            });
    });
});