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
public class Aufgaben {
    private String [] aufgaben;
    private String [] lösungen;
    
    public Aufgaben(int anzahl){
        aufgaben=new String[anzahl];
        lösungen=new String[anzahl];
        for (int i=0;i<anzahl;i++){
            int rechenart=(int)(Math.random()*5);
            if (rechenart==0){
                int zahl1=(int)(Math.random()*200+1);
                int zahl2=(int)(Math.random()*200+1);
                aufgaben[i]=zahl1+" + "+zahl2;
                lösungen[i]=""+(zahl1+zahl2);
            } else if (rechenart==1){
                int zahl1=(int)(Math.random()*200+1);
                int zahl2=(int)(Math.random()*200+1);
                aufgaben[i]=zahl1+" - "+zahl2;
                lösungen[i]=""+(zahl1-zahl2);
            } else if (rechenart==2){
                int zahl1=(int)(Math.random()*19+2);
                int zahl2=(int)(Math.random()*19+2);
                aufgaben[i]=zahl1+" x "+zahl2;
                lösungen[i]=""+(zahl1*zahl2);
            } else if (rechenart==3){
                int zahl1=(int)(Math.random()*19+2);
                int zahl2=(int)(Math.random()*20+2);
                aufgaben[i]=(zahl1*zahl2)+" : "+zahl2;
                lösungen[i]=""+zahl1;
            } else if (rechenart==4){
                int zahl1=(int)(Math.random()*25+1);
                aufgaben[i]="Quadratwurzel von "+(zahl1*zahl1);
                lösungen[i]=""+zahl1;
            }
        }
    }
    public String[] holeAufgabe(int nr){
        String [] rg={aufgaben[nr],lösungen[nr]};
        return rg;
    }
}
