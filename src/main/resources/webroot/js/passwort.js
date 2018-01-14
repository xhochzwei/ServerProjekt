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
                            .append("Zu den Itemshopeinstellungen<br>")
                            .append("<input type='button' value='GO' id='Adminshop'/>")
                            .append("<br>Kontostände: <br> Überprüfe Kontostand von: <input type='text' value='name' id='Adminname'/>").append("<input type='button' value='OK' id='geld'/>")
                            .append("<br>Setze Kontostand von: <input type='text' value='Name' id='KontoName'/>").append(" zu: <input type='text' value='Betrag' id='Kontobetrag'/>").append("<input type='button' value='setzen' id='zeigeUser'/>")
                            
                            
                            .append("<br><br> OUTPUT:");
                }
       
        }
    );
    });
           $(document).on("click","#zeigeUser",function(){
           $.post("../anfrage", {
                typ: "setzeKonto",
                Name: $("#KontoName").val(),
                Betrag: $("#Kontobetrag").val()
               
            
            },
            function(data){
                    if (data.text == "updateKonto"){
                    if (data.setzeKonto == "success"){
                        $("body").append("<br>Kontostand wurde erfolgreich übernommen")
                    }
                    else{
                        $("body").append("<br>Fehler beim Übernehmen des Kontostandes: " + data.uptKonto)
                    }
            }}
            );
        });
       $(document).on("click", "#Adminshop", function(){
            $("body").html("Erstelle Hier ein neues Shop Item")
                    .append("<br><input type='text' value='Name' id='Itemname'/>")
                    .append("<br><input type='text' value='Preis' id='Itempreis'/>")
                    .append("<input type='button' value='erstellen' id='Itemerstellen'/>")
            
                    .append("Delete Item: <br><input type='text' value='Name' id='Itemname2'/>")
                    .append("<br><input type='button' value='löschen' id='Itemlöschen'/>")
                    .append("<br>OUTPUT:")
       });
       $(document).on("click","#Itemlöschen",function(){
           $.post("../anfrage", {
                typ: "löscheItem",
                Itemname: $("#Itemname2").val(),
            
            },
            function(data){
                if (data.text=="ItemGelöscht"){
                   
                        if (data.itemdelete == "ja"){
                            
                                     $("body").append("<br>Item wurde gelöscht");
                        }
                        if (data.itemdelete == "fehler") {
                            $("body").append("<br>Fehler beim Löschen!");
                            
                        }
                    
                }
            });
       });
       $(document).on("click","#Itemerstellen",function(){
           $.post("../anfrage", {
                typ: "erstelleItem",
                Itemname: $("#Itemname").val(),
                Itempreis: $("#Itempreis").val()
            },
            function(data){
                if (data.text=="Itemerstellt"){
                   
                        if (data.itemers == "ja"){
                            
                                     $("body").append("<br>Item wurde erstellt");
                        }
                        if (data.itemers == "fehler") {
                            $("body").append("<br>Fehler beim Erstellen!");
                            
                        }
                        if (data.itemers == "nein"){
                             $("body").append("<br>Dieses Shopitem gibt es schon. Versuchen Sie es mit einem anderen Namen.")
                        }
                }
            });
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
                            .append("<br><input type='button' value='logout' id='logout'/>")
                            .append("<br><input type='button' value='shop' id='shop'/>")
                }   else {
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
                        $("body").html("Willkommen zurrück!")
                                 .append("<br><input type='button' value='Einstellungen' id='funct'/>")
                                 .append("<br><input type='button' value='logout' id='logout'/>")
                                 .append("<br><input type='button' value='shop' id='shop'/>");
                    }
                }
            }
        );
    $(document).on("click", "#shop", function () {
        $("body").html("Willkommen im Shop <br>")
                .append("Suchen: <input type='text' id='search'/><br>")
                .append("<br><input type='button' value='Suchen' id='suche'/>");
    });
    $(document).on("click", "#suche", function(){
                $.post("../anfrage", {
                    typ: "Shopoffnen",
                    search: $("#search").val()
                },
                        function(data){
                           
                                if (data.ItemPreis == "nonexistent"){
                                    $("body").append("<br>Diesen Artikel gibt es nicht")
                                }
                                else{
                                    var preis = data.ItemPreis123
                                    var konto = data.Kontostand
                                    $("body").append("<br>Der Artikel kostet: "+preis + "€" + " Ihr Kontostand beträgt: " + konto + "€");
                                    
                                }
                            }
                        
                        );
                
    });
    
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
        else {
            $("body").append("<br>Die Passwörter stimmen nicht überein! Bitte nochmal versuchen");
        }
    });
});

