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
    private String currentPlanet = "ZBEWTPSM9";
    private Set<String> planets = new LinkedHashSet<>();
    private final String URL = "https://datsedenspace.datsteam.dev/player";
    private final String TOKEN = "66044c57de11b66044c57de121";
    private final String COLLECT_SERVICE_URL = "http://localhost:5000";
    private Set<String> emptyPlanet;

    private int planetsCount;
    private String previousOc = "";
    private int previousCounter = 0;

    @GetMapping("/startGame")
    public ResponseEntity startGame() throws InterruptedException {
        MainJson mainJson = sendRequestToGetPlanet("/universe");
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
            planetTravel();
            return new ResponseEntity<>(string, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public void planetTravel() throws InterruptedException {
        DijkstraShortestPath dijkstraShortestPath = new DijkstraShortestPath(graph);
        List<DefaultWeightedEdge> edgePath;
        Set<String> planet = new LinkedHashSet<>();
        emptyPlanet = new HashSet<>();
        emptyPlanet.add("Earth");
        emptyPlanet.add("Eden");
        while (emptyPlanet.size() != planetsCount) {
            for (String s : planets) {
                if (!emptyPlanet.contains(s)) {
                    var path = dijkstraShortestPath.getPath(currentPlanet, s);
                    if (path != null) {
                        Planets p = new Planets(createSortedPlanetList());
                        for (int i = 0; i < p.getPlanets().size() - 1; i++) {
                            var edge = graph.getEdge(p.getPlanets().get(i), p.getPlanets().get(i + 1));
                            graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + 10);
                        }
                        p.planets.remove(0);
                        String data = gson.toJson(p);
                        planet.clear();
                        String travelPlanet = sendRequestToTravel("/travel", data);
                        ResponseFromService response = sendRequestToService(travelPlanet, "/process_garbage");
                        System.out.println(response.isProcessFurther() + "   " + response.getOccupancy());
                        if (previousOc.equals(response.getOccupancy())) {
                            previousCounter++;
                        }

                        previousOc = response.getOccupancy();

                        if (previousCounter == 3) {
                            previousCounter = 0;
                            List<String> edenPath = dijkstraShortestPath.getPath(currentPlanet, "Eden").getVertexList();
                            currentPlanet = "Eden";
                            for (int i = 0; i < edenPath.size() - 1; i++) {
                                var edge = graph.getEdge(edenPath.get(i), edenPath.get(i + 1));
                                graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + 10);
                            }
                            edenPath.remove(0);
                            p = new Planets(edenPath.stream().toList());
                            data = gson.toJson(p);
                            planet.clear();
                            sendRequestToTravel("/travel", data);
                            System.out.println("EDEN TRAVEL");
                            break;
                        }

                        if (response.isPlanetIsEmpty()) {
                            emptyPlanet.add(currentPlanet);
                        }

                        if (emptyPlanet.size() == 99) {
                            List<String> edenPath = dijkstraShortestPath.getPath(currentPlanet, "Eden").getVertexList();
                            currentPlanet = "Eden";
                            for (int i = 0; i < edenPath.size() - 1; i++) {
                                var edge = graph.getEdge(edenPath.get(i), edenPath.get(i + 1));
                                graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + 10);
                            }
                            edenPath.remove(0);
                            p = new Planets(edenPath.stream().toList());
                            data = gson.toJson(p);
                            planet.clear();
                            sendRequestToTravel("/travel", data);
                            System.out.println("EDEN TRAVEL");
                            break;
                        }

                        if (!response.isProcessFurther()) {
                            List<String> edenPath = dijkstraShortestPath.getPath(currentPlanet, "Eden").getVertexList();
                            currentPlanet = "Eden";
                            for (int i = 0; i < edenPath.size() - 1; i++) {
                                var edge = graph.getEdge(edenPath.get(i), edenPath.get(i + 1));
                                graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + 10);
                            }
                            edenPath.remove(0);
                            p = new Planets(edenPath.stream().toList());
                            data = gson.toJson(p);
                            planet.clear();
                            sendRequestToTravel("/travel", data);
                            System.out.println("EDEN TRAVEL");
                        }
                        System.out.println(emptyPlanet.size());
                        System.out.println();
                    }
                }
                Thread.sleep(250);
            }
        }
    }

    public List<String> createSortedPlanetList() {
        DijkstraShortestPath dijkstraShortestPath = new DijkstraShortestPath(graph);
        List<String> allPlanets = planets.stream()
                .filter(t -> (!t.equals("Earth") && !t.equals("Eden") && !t.equals(currentPlanet))).toList();
        List<DefaultWeightedEdge> edgePath;
        List<ForSortedPlanet> sortedPlanetsRoutes = new ArrayList<>();
        String from = currentPlanet, to;
        double weightSum = 0;
        for (String s : allPlanets) {
            if (!emptyPlanet.contains(s)) {
                weightSum = dijkstraShortestPath.getPath(currentPlanet, s).getWeight();
                List<String> temp = dijkstraShortestPath.getPath(currentPlanet, s).getVertexList();
                sortedPlanetsRoutes.add(new ForSortedPlanet(temp, weightSum));
            }
        }
        sortedPlanetsRoutes.sort(Comparator.comparingDouble(ForSortedPlanet::getWeight));
        currentPlanet = sortedPlanetsRoutes.get(0).getNames().get(sortedPlanetsRoutes.get(0).getNames().size() - 1);
        return sortedPlanetsRoutes.get(0).getNames();
    }

    public ResponseFromService sendRequestToService(String data, String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(data, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(COLLECT_SERVICE_URL + endpoint, entity, String.class);
        String json = response.getBody();
        System.out.print(json);
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