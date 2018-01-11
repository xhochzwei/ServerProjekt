$(document).ready(function () {

    $(document).on("click", "#logout", function () {
        $.post("../anfrage", {
            typ: "logout"
        }, function (data) {
            if (data.typ == "logout") {
                $("body").html("Du bist erfolgreich abgemeldet. Neu laden zum erneuten Anmelden!");
            }
        })
    });
    $(document).on("click", "#funct", function () {
        $.post("../anfrage", {
            typ: "function",
        
            
        },
        
        function(data){
     
                if (data.function == '["user"]') {
                    var konto = data.konto;
                    var name = data.name;
                    var adresse = data.adresse;
                            $("body").html("Einstellungen:<br>")
                                    .append("<br> Name: " + name)
                                    .append("<br> Adresse: " + adresse)
                                    .append("<input type='text' value='' id='AEadr'/>")
                                    .append("  <input type='button' value='ändern' id='AEgo'/>")
                                    .append("<br> Kontostand: " + konto +"€");
                }
                if (data.function == '["admin"]') {
                    $("body").html("Herzlich Willkommen auf der Admin Seite<br>")
                            .append("Überprüfe den Kontostand eines Benutzer: <br><input type='text' value='name' id='Adminname'/>")
                            .append("<input type='button' value='OK' id='geld'/>")
                            .append("<br> OUTPUT:");
                }
       
        }
    );
    });
       $(document).on("click", "#AEgo", function () {    
           $.post("../anfrage", {
                typ: "AEadresse",
                Adresse: $("#AEadr").val()
            },
             function(data){
                
                  $("body").append("<br> OUTPUT: ")
                  if(data.text == "richtig"){
                    if (data.CHANGEadresse == "erfolgreich") {
                               $("body").append("<br> Adresse wurde erfolgreich geändert!")
                    }
                    else{
                        $("body").append("<br> Es ist ein Fehler aufgetreten. Versuchen Sie es nochmal!")
                    }
                }
           
            });
       });
   $(document).on("click", "#geld", function () {    
             $.post("../anfrage", {
                typ: "Geld",
                Kontoname: $("#Adminname").val()
            }, function (data){
               var geld = data.konto
                $("body").append("<br>Der Kontostand beträgt: " + geld);    
            });
          
        });
    $(document).on("click", "#anmeldeknopf", function () {
        $.post("../anfrage", {
            typ: "anmeldedaten",
            anmeldename: $("#anmeldename").val(),
            passwort: $("#passwort").val()
        }, function (data) {
            if (data.typ == "überprüfung") {
                if (data.text == "ok") {
                    $("body").html("Gratulation, du bist angemeldet!")
                            .append("<br><input type='button' value='Einstellungen' id='funct'/>")
                            .append("<br><input type='button' value='logout' id='logout'/>");
                } else {
                    $("body").append("<br>Die Anmeldedaten waren leider falsch!");
                }
            }
        });
    });
    
        $.post("../anfrage",
            {
                typ: "angemeldet"
            },
            function (data) {

                if (data.typ == "angemeldet") {
                    if (data.text == "nein") {
                        $("body").html("Name: <input type='text' id='anmeldename'/><br>")
                                .append("Passwort: <input type='password' id='passwort'/><br>\n")
                                .append("<input type='button' value='OK' id='anmeldeknopf'/>")
                                .append("<input type='button' value='registrieren' id='regknopf' />")
                        

                    } else {
                        $("body").html("Gratulation, du bist angemeldet!")
                                .append("<br><input type='button' value='logout' id='logout'/>");
                    }
                }
            }
        );
  $(document).on("click", "#regknopf", function () {
        $("body").html("Hallo, Sie können sich nun registrieren <br>")     
                .append("Benutzername        : <input type='text' id='regname'/><br>\n")
                .append("Passwort            : <input type='password' id='regpasswort1'/><br>\n")
                .append("Passwort wiederholen: <input type='password' id='regpasswort2'/><br>\n")
                .append("Adresse             : <input type='text' id='regadresse'/><br>\n")
                .append("<input type='button' value='registrieren' id='regbestätigung'/>");
        
    });  
     $(document).on("click", "#regbestätigung", function () {
        if ($("#regpasswort1").val() == $("#regpasswort2").val()) {
            
          
             $.post("../anfrage", {
                typ: "registrierung",
                regname: $("#regname").val(),
                passwort: $("#regpasswort1").val(),
                regadresse: $("#regadresse").val()
     
            }, function (data){
                if (data.typ=="bestätigung") {
                    if (data.text=="richtig") {
                         $("body").append("Ihr Account wurde erstellt")        
                    }
                    else if (data.text=="falsch"){
                        $("body").append("Dieser Benutzer existiert schon")  
                    }
               
            }
                
            }
                    );
          
        }
    });
});

