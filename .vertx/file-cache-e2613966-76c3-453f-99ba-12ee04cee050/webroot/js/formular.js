$(document).ready(function () {
    $("#eingabeKnopf").click(function () {
        $.post("post",
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