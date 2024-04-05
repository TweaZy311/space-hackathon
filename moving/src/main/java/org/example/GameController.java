package org.example;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.entity.FromToPlanet;
import org.example.entity.Planet;
import org.example.entity.MainJson;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.SimpleGraph;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class GameController {

    private static final String url = "https://datsedenspace.datsteam.dev";
    private static final String token = "66044c57de11b66044c57de121";

    @GetMapping("/startGame")
    public void startGame() {
        MainJson mainJson = sendRequest("/player/universe");
        var graph = new Multigraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        for (FromToPlanet fromToPlanet : mainJson.getUniverse().getFromToPlanets()) {
            graph.addVertex(fromToPlanet.getFrom());
            graph.addVertex(fromToPlanet.getTo());
            DefaultWeightedEdge edge = graph.addEdge(fromToPlanet.getFrom(), fromToPlanet.getTo());
            graph.setEdgeWeight(edge, fromToPlanet.getCost());
        }
     }

     public static MainJson sendRequest(String endpoint) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url + endpoint).addHeader("X-Auth-Token", token).build();
        MainJson mainJson = null;
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Bad request " + response.message());
            }

            String responseString = response.body().string();
            Gson gson = new Gson();
            mainJson = gson.fromJson(responseString, MainJson.class);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            return mainJson;
        }
    }

}