package org.example;

import com.google.gson.Gson;
import org.example.entity.*;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
public class GameController {

    Gson gson = new Gson();
    private DefaultDirectedWeightedGraph graph;
    private String currentPlanet = "Eden";
    private Set<String> planets = new HashSet<>();
    private final String URL = "https://datsedenspace.datsteam.dev/player";
    private final String TOKEN = "66044c57de11b66044c57de121";
    private final String COLLECT_SERVICE_URL = "http://localhost:5000";

    private int planetsCount;

    @GetMapping("/startGame")
    public ResponseEntity startGame() {
        MainJson mainJson = sendRequestToGetPlanet("/universe");
//        currentPlanet = mainJson.getPlanet().getName();
        if (mainJson != null) {
            graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
            for (List<Object> fromToPlanet : mainJson.getUniverse()) {
                String vertex1 = String.valueOf(fromToPlanet.get(0));
                String vertex2 = String.valueOf(fromToPlanet.get(1));
                graph.addVertex(vertex1);
                graph.addVertex(vertex2);
                var edge = graph.addEdge(vertex1, vertex2);
                graph.setEdgeWeight(edge, (Double) fromToPlanet.get(2));
                planets.add(vertex1);
                planets.add(vertex2);
            }
            planetsCount = graph.vertexSet().size();
            String string = "";
            for (Object edge : graph.edgeSet()) {
                string += graph.getEdgeSource(edge) + " -> " + graph.getEdgeTarget(edge) + "<br>";
            }
            returnHome();
            planetTravel();
            return new ResponseEntity<>(string, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public void planetTravel() {
        DijkstraShortestPath dijkstraShortestPath = new DijkstraShortestPath(graph);
        List<DefaultWeightedEdge> edgePath;
        Set<String> planet = new LinkedHashSet<>();
        Set<String> emptyPlanet = new HashSet<>();
        emptyPlanet.add("Earth");
        emptyPlanet.add("Eden");
        while (emptyPlanet.size() != planetsCount) {
            for (String s : planets) {
                if (!emptyPlanet.contains(s)) {
                    var path = dijkstraShortestPath.getPath(currentPlanet, s);
                    if (path != null) {
                        edgePath = path.getEdgeList();
                        for (DefaultWeightedEdge edge : edgePath) {
                            planet.add(String.valueOf(graph.getEdgeTarget(edge)));
                            graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + 10);
                            currentPlanet = String.valueOf(graph.getEdgeTarget(edge));
                        }
                        Planets p = new Planets(planet.stream().toList());
                        String data = gson.toJson(p);
                        planet.clear();
                        String travelPlanet = sendRequestToTravel("/travel", data);
                        System.out.println(travelPlanet);
//                        System.out.println(gson.toJson(travelPlanet));
                        ResponseFromService response = sendRequestToService(travelPlanet, "/process_garbage");
                        if (response.isPlanetIsEmpty()) {
                            emptyPlanet.add(currentPlanet);
                        }
                        if (!response.isProcessFurther()) {
                            path = dijkstraShortestPath.getPath(currentPlanet, "Eden");
                            edgePath = path.getEdgeList();
                            for (DefaultWeightedEdge edge : edgePath) {
                                planet.add(String.valueOf(graph.getEdgeTarget(edge)));
                                graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + 10);
                            }
                            currentPlanet = "Eden";
                            p = new Planets(planet.stream().toList());
                            data = gson.toJson(p);
                            planet.clear();
                            sendRequestToTravel("/travel", data);
                        }
                    }
                }
            }
        }
    }

    public ResponseFromService sendRequestToService(String data, String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(data, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(COLLECT_SERVICE_URL + endpoint, entity, String.class);
        String json = response.getBody();
        ResponseFromService responseFromService = gson.fromJson(json, ResponseFromService.class);
        return responseFromService;
    }

    public String sendRequestToTravel(String endpoint, String data) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Auth-Token", TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(data, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(URL + endpoint, entity, String.class);
        String json = response.getBody();
        TravelPlanet travelPlanet = gson.fromJson(json, TravelPlanet.class);
        return response.getBody();
    }

    public MainJson sendRequestToGetPlanet(String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Auth-Token", TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(URL + endpoint, HttpMethod.GET, entity, String.class);
        String json = response.getBody();
        MainJson mainJson = gson.fromJson(json, MainJson.class);
        return mainJson;
    }

    public void returnHome() {
        Set<String> planet = new LinkedHashSet<>();
        DijkstraShortestPath dijkstraShortestPath = new DijkstraShortestPath(graph);
        List<DefaultWeightedEdge> edgePath;
        var path = dijkstraShortestPath.getPath(currentPlanet, "Earth");
        edgePath = path.getEdgeList();
        for (DefaultWeightedEdge edge : edgePath) {
                planet.add(String.valueOf(graph.getEdgeTarget(edge)));
                graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + 10);
                currentPlanet = String.valueOf(graph.getEdgeTarget(edge));
            }
        Planets p = new Planets(planet.stream().toList());
        String data = gson.toJson(p);
        planet.clear();
        sendRequestToTravel("/travel", data);
        currentPlanet = "Earth";
    }

}