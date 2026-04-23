package com.indicium;
import com.indicium.models.Evidence;
import java.io.File;

public class Main
{
    public static void main(String[] args)
    {
        String path = "D://FAST 2024//Semester 4//SDA//Semester Project Work//Project//Indicium//pom.xml";
        File file = new File(path);

        Evidence ev = new Evidence(file);
        ev.linkWithCase(2);
        ev.linkWithCase(5);
        ev.setStatus(0);

        ev.displayEvidence();
    }
}
