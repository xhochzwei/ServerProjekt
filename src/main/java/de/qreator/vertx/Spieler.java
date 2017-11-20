/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.qreator.vertx;

/**
 *
 * @author menze
 */
public class Spieler {

    private String name = "";
    private String id = "";
    private int[] aufgaben;
    private int restlicheAufgabenZahl = 0;
    private int richtig = 0;
    private int falsch = 0;
    private String[] aktuelleAufgabe = {};

    public Spieler(String name, String id,int anzahlAufgaben) {
        this.name = name;
        this.id = id;
        aufgaben = new int[anzahlAufgaben];
        for (int i = 0; i < anzahlAufgaben; i++) {
            aufgaben[i] = i;
        }
        restlicheAufgabenZahl = anzahlAufgaben;
    }

    public int holeNächsteAufgabenNummer() {
        int nr = (int) (Math.random() * restlicheAufgabenZahl);
        int aufgabenNummer = aufgaben[nr];
        restlicheAufgabenZahl--;
        if (restlicheAufgabenZahl >= 0) {
            aufgaben[nr] = aufgaben[restlicheAufgabenZahl];

            return aufgabenNummer;
        } else {
            return -1;
        }
    }
    
    public void setAufgaben(int anzahlAufgaben){
        aufgaben = new int[anzahlAufgaben];
        for (int i = 0; i < anzahlAufgaben; i++) {
            aufgaben[i] = i;
        }
        restlicheAufgabenZahl = anzahlAufgaben;
    }
            

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void richtigGeloest(boolean korrekt) {
        if (korrekt == true) {
            richtig++;
        } else {
            falsch++;
        }
    }

    public int holeRichtig() {
        return richtig;
    }

    public int holeFalsch() {
        return falsch;
    }

    public void setAktuelleAufgabe(String[] z) {
        aktuelleAufgabe = z;
    }

    public String[] getAktuelleAufgabe() {
        return aktuelleAufgabe;
    }
}
