package org.eclipse.rdf4j.spring.demo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.demo.model.Artist;
import org.eclipse.rdf4j.spring.demo.model.Painting;
import org.eclipse.rdf4j.spring.demo.service.ArtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;
import java.util.Set;

@SpringBootApplication
public class ArtDemoCli implements CommandLineRunner {
    @Autowired ArtService artService;

    public static void main(String[] args) {
        SpringApplication.run(ArtDemoCli.class, args).close();
    }

    @Override
    public void run(String... args) {
        System.out.println("\nData read from 'artists.ttl':");
        Map<Artist, Set<Painting>> paintingsMap = artService.getPaintingsGroupedByArtist();
        listPaintingsByArtist(paintingsMap);
        System.out.println("\nNow adding some data...");
        addPaintingWithArtist();
        System.out.println("\nReloaded data:");
        paintingsMap = artService.getPaintingsGroupedByArtist();
        listPaintingsByArtist(paintingsMap);
        System.out.println("\n");
    }

    private void addPaintingWithArtist() {
        Artist a = new Artist();
        a.setFirstName("Jan");
        a.setLastName("Vermeer");
        IRI artistId = artService.addArtist(a);
        Painting p = new Painting();
        p.setTitle("View of Delft");
        p.setTechnique("oil on canvas");
        p.setArtistId(artistId);
        artService.addPainting(p);
    }

    private void listPaintingsByArtist(Map<Artist, Set<Painting>> paintingsMap) {
        for (Artist a : paintingsMap.keySet()) {
            System.out.println(String.format("%s %s", a.getFirstName(), a.getLastName()));
            for (Painting p : paintingsMap.get(a)) {
                System.out.println(String.format("\t%s (%s)", p.getTitle(), p.getTechnique()));
            }
        }
    }
}
